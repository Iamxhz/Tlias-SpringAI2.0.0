package com.xhz.ai.controller;

import com.xhz.ai.dto.AgentEvent;
import com.xhz.ai.dto.ApprovalRequest;
import com.xhz.ai.dto.ChatRequest;
import com.xhz.ai.service.AgentChatService;
import com.xhz.ai.service.ApprovalService;
import com.xhz.pojo.Result;
import com.xhz.utils.AliyunOSSOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

/**
 * 班主任 Agent 控制器：聊天走 AgentChatService，确认走统一 ApprovalService。
 */
@RestController
@RequestMapping("/ai")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentChatService agentChatService;
    private final ApprovalService approvalService;
    private final AliyunOSSOperator aliyunOSSOperator;

    public AgentController(AgentChatService agentChatService,
                           ApprovalService approvalService,
                           AliyunOSSOperator aliyunOSSOperator) {
        this.agentChatService = agentChatService;
        this.approvalService = approvalService;
        this.aliyunOSSOperator = aliyunOSSOperator;
    }

    /**
     * 违纪证据图上传。走 /ai/**，可被 nginx 的 /ai/ 代理；OSS 失败时返回业务错误，不抛 HTML。
     */
    @PostMapping("/upload")
    public Result<String> uploadEvidence(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的证据图。");
        }
        try {
            String originalFilename = file.getOriginalFilename();
            String extName = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extName;
            String url = aliyunOSSOperator.upload(file.getBytes(), uniqueFileName);
            return Result.success(url);
        } catch (Exception e) {
            log.warn("证据图上传 OSS 失败：{}", e.getMessage());
            return Result.fail("证据图存对象存储失败：" + e.getMessage());
        }
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentEvent> agentChat(@RequestBody ChatRequest request) {
        String message = request != null ? request.getMessage() : null;
        String conversationId = request != null ? request.getConversationId() : null;
        String imageUrl = request != null ? request.getImageUrl() : null;
        String imageBase64 = request != null ? request.getImageBase64() : null;
        return agentChatService.agentStreamChat(message, conversationId, imageUrl, imageBase64);
    }

    /**
     * 统一 HITL 确认入口（不区分业务类型，由 pending.type 分发）。
     */
    @PostMapping("/approve")
    public Result<Map<String, String>> approve(@RequestBody ApprovalRequest request) {
        if (request == null || request.getApprovalId() == null || request.getApprovalId().isBlank()) {
            return Result.fail("approvalId 不能为空");
        }
        boolean approved = request.getApproved() == null || Boolean.TRUE.equals(request.getApproved());
        if (approved) {
            String resultText = approvalService.approve(request.getApprovalId());
            return Result.success(Map.of(
                    "approvalId", request.getApprovalId(),
                    "status", "APPROVED",
                    "message", resultText
            ));
        }
        approvalService.reject(request.getApprovalId());
        return Result.success(Map.of(
                "approvalId", request.getApprovalId(),
                "status", "REJECTED",
                "message", "已取消本次申请，数据库未修改。"
        ));
    }
}
