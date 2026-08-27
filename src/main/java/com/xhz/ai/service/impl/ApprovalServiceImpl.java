package com.xhz.ai.service.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.xhz.ai.dto.ApprovalType;
import com.xhz.ai.dto.PendingApproval;
import com.xhz.ai.service.ApprovalService;
import com.xhz.exception.BusinessException;
import com.xhz.pojo.Dept;
import com.xhz.pojo.Emp;
import com.xhz.pojo.Student;
import com.xhz.pojo.param.EmpAddParam;
import com.xhz.service.DeptService;
import com.xhz.service.EmpService;
import com.xhz.service.StudentService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审批单存 Redis：TTL 过期；处理后写入 done 标记，防止重复确认。
 */
@Service
public class ApprovalServiceImpl implements ApprovalService {

    private static final String PENDING_KEY = "tlias:approval:pending:";
    private static final String DONE_KEY = "tlias:approval:done:";
    private static final Duration PENDING_TTL = Duration.ofMinutes(30);
    private static final Duration DONE_TTL = Duration.ofHours(24);

    private final StudentService studentService;
    private final EmpService empService;
    private final DeptService deptService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ApprovalServiceImpl(StudentService studentService,
                               EmpService empService,
                               DeptService deptService,
                               StringRedisTemplate redis) {
        this.studentService = studentService;
        this.empService = empService;
        this.deptService = deptService;
        this.redis = redis;
        SimpleModule javaTime = new SimpleModule();
        javaTime.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers)
                    throws java.io.IOException {
                gen.writeString(value.toString());
            }
        });
        javaTime.addDeserializer(Instant.class, new JsonDeserializer<>() {
            @Override
            public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
                return Instant.parse(p.getValueAsString());
            }
        });
        this.objectMapper = new ObjectMapper().registerModule(javaTime);
    }

    @Override
    public PendingApproval createViolationScore(String runId, String conversationId,
                                                Integer studentId, Integer score) {
        return save(new PendingApproval(newId(), ApprovalType.VIOLATION_SCORE, runId, conversationId,
                studentId, score, null, null, Instant.now()));
    }

    @Override
    public PendingApproval createEmpSave(String runId, String conversationId, EmpAddParam param) {
        return save(new PendingApproval(newId(), ApprovalType.EMP_SAVE, runId, conversationId,
                null, null, null, toJson(param), Instant.now()));
    }

    @Override
    public PendingApproval createEmpDelete(String runId, String conversationId, List<Integer> empIds) {
        return save(new PendingApproval(newId(), ApprovalType.EMP_DELETE, runId, conversationId,
                null, null, List.copyOf(empIds), null, Instant.now()));
    }

    @Override
    public PendingApproval createDeptSave(String runId, String conversationId, String deptName) {
        return save(new PendingApproval(newId(), ApprovalType.DEPT_SAVE, runId, conversationId,
                null, null, null, deptPayload(null, deptName), Instant.now()));
    }

    @Override
    public PendingApproval createDeptUpdate(String runId, String conversationId, Integer deptId, String deptName) {
        return save(new PendingApproval(newId(), ApprovalType.DEPT_UPDATE, runId, conversationId,
                null, null, null, deptPayload(deptId, deptName), Instant.now()));
    }

    @Override
    public PendingApproval createDeptDelete(String runId, String conversationId, Integer deptId) {
        return save(new PendingApproval(newId(), ApprovalType.DEPT_DELETE, runId, conversationId,
                null, null, null, deptPayload(deptId, null), Instant.now()));
    }

    @Override
    public PendingApproval createSetAlarm(String runId, String conversationId,
                                          String alarmTime, String eventDescription) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("alarmTime", alarmTime == null ? "" : alarmTime);
        payload.put("eventDescription", eventDescription == null ? "" : eventDescription);
        return save(new PendingApproval(newId(), ApprovalType.SET_ALARM, runId, conversationId,
                null, null, null, toJson(payload), Instant.now()));
    }

    @Override
    public String approve(String approvalId) {
        PendingApproval item = takePending(approvalId);
        String result = switch (item.type()) {
            case VIOLATION_SCORE -> executeViolationScore(item);
            case EMP_SAVE -> executeEmpSave(item);
            case EMP_DELETE -> executeEmpDelete(item);
            case DEPT_SAVE -> executeDeptSave(item);
            case DEPT_UPDATE -> executeDeptUpdate(item);
            case DEPT_DELETE -> executeDeptDelete(item);
            case SET_ALARM -> executeSetAlarm(item);
        };
        markDone(approvalId, "APPROVED");
        return result;
    }

    @Override
    public void reject(String approvalId) {
        takePending(approvalId);
        markDone(approvalId, "REJECTED");
    }

    private PendingApproval save(PendingApproval item) {
        redis.opsForValue().set(PENDING_KEY + item.approvalId(), toJson(item), PENDING_TTL);
        return item;
    }

    private PendingApproval takePending(String approvalId) {
        String json = redis.opsForValue().getAndDelete(PENDING_KEY + approvalId);
        if (json == null) {
            if (Boolean.TRUE.equals(redis.hasKey(DONE_KEY + approvalId))) {
                throw new BusinessException("确认单不存在或已处理：" + approvalId);
            }
            throw new BusinessException("确认单不存在或已过期：" + approvalId);
        }
        return fromJson(json, PendingApproval.class);
    }

    private void markDone(String approvalId, String status) {
        redis.opsForValue().set(DONE_KEY + approvalId, status, DONE_TTL);
    }

    private String executeViolationScore(PendingApproval item) {
        Student updated = studentService.addViolationScore(item.studentId(), item.score());
        return String.format("扣分成功！学员【%s】当前累计违纪 %d 次，总违纪扣分 %d 分。",
                updated.getName(), updated.getViolationCount(), updated.getViolationScore());
    }

    private String executeEmpSave(PendingApproval item) {
        EmpAddParam param = fromJson(item.payloadJson(), EmpAddParam.class);
        Emp emp = empService.addEmp(param);
        int exprCount = param.exprList() != null ? param.exprList().size() : 0;
        return String.format("员工保存成功！系统已生成员工ID：%d，姓名：%s，并成功登记了 %d 条历史工作经历。",
                emp.getId(), emp.getName(), exprCount);
    }

    private String executeEmpDelete(PendingApproval item) {
        List<Integer> empIds = item.empIds();
        if (empIds == null || empIds.isEmpty()) {
            throw new BusinessException("确认单缺少员工ID列表：" + item.approvalId());
        }
        int deleted = empService.deleteEmpByIds(empIds);
        return String.format("删除成功！已删除 %d 名员工，目标 ID=%s。", deleted, empIds);
    }

    private String executeDeptSave(PendingApproval item) {
        JsonNode node = readPayload(item);
        Dept dept = new Dept();
        dept.setName(text(node, "name"));
        deptService.save(dept);
        return String.format("部门新增成功：%s。", dept.getName());
    }

    private String executeDeptUpdate(PendingApproval item) {
        JsonNode node = readPayload(item);
        Dept dept = new Dept();
        dept.setId(intVal(node, "id"));
        dept.setName(text(node, "name"));
        deptService.update(dept);
        return String.format("部门修改成功：ID=%d，名称=%s。", dept.getId(), dept.getName());
    }

    private String executeDeptDelete(PendingApproval item) {
        JsonNode node = readPayload(item);
        Integer id = intVal(node, "id");
        deptService.deleteById(id);
        return String.format("部门删除成功：ID=%d。", id);
    }

    private String executeSetAlarm(PendingApproval item) {
        JsonNode node = readPayload(item);
        String alarmTime = text(node, "alarmTime");
        String eventDescription = text(node, "eventDescription");
        System.out.println("⏰ 班主任已确认闹钟，将在 [" + alarmTime + "] 提醒：[" + eventDescription + "]");
        return "闹钟已确认设置，将于 " + alarmTime + " 提醒：" + eventDescription;
    }

    private JsonNode readPayload(PendingApproval item) {
        try {
            return objectMapper.readTree(item.payloadJson() == null ? "{}" : item.payloadJson());
        } catch (JsonProcessingException e) {
            throw new BusinessException("确认单载荷解析失败：" + item.approvalId());
        }
    }

    private String deptPayload(Integer id, String name) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (id != null) {
            map.put("id", id);
        }
        if (name != null) {
            map.put("name", name);
        }
        return toJson(map);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Integer intVal(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asInt();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("确认单序列化失败");
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new BusinessException("确认单反序列化失败");
        }
    }

    private static String newId() {
        return "apv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
