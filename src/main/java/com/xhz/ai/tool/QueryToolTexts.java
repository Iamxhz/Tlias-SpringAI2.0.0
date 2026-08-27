package com.xhz.ai.tool;

import com.xhz.pojo.Clazz;
import com.xhz.pojo.Emp;
import com.xhz.pojo.Student;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class QueryToolTexts {

    private QueryToolTexts() {
    }

    static String jobName(Integer job) {
        if (job == null) {
            return "未知";
        }
        return switch (job) {
            case 1 -> "班主任";
            case 2 -> "讲师";
            case 3 -> "学工主管";
            case 4 -> "教研主管";
            case 5 -> "咨询师";
            default -> "其他";
        };
    }

    static String genderName(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        return gender == 1 ? "男" : gender == 2 ? "女" : "未知";
    }

    static String degreeName(Integer degree) {
        if (degree == null) {
            return "未知";
        }
        return switch (degree) {
            case 1 -> "初中";
            case 2 -> "高中";
            case 3 -> "大专";
            case 4 -> "本科";
            case 5 -> "硕士";
            case 6 -> "博士";
            default -> "未知";
        };
    }

    static String subjectName(Integer subject) {
        if (subject == null) {
            return "未知";
        }
        return switch (subject) {
            case 1 -> "Java";
            case 2 -> "前端";
            case 3 -> "大数据";
            case 4 -> "Python";
            case 5 -> "Go";
            default -> "其他";
        };
    }

    static String formatEmp(Emp emp) {
        if (emp == null) {
            return "未找到该员工。";
        }
        return String.format("ID=%d，姓名=%s，用户名=%s，职位=%s，部门=%s，手机=%s，入职=%s",
                emp.getId(),
                emp.getName(),
                emp.getUsername(),
                jobName(emp.getJob()),
                emp.getDeptName() == null ? "未分配" : emp.getDeptName(),
                emp.getPhone(),
                emp.getEntryDate());
    }

    static String formatStudent(Student student) {
        if (student == null) {
            return "未找到该学员。";
        }
        return String.format("ID=%d，学号=%s，姓名=%s，班级=%s，学历=%s，违纪次数=%s，违纪扣分=%s",
                student.getId(),
                student.getNo(),
                student.getName(),
                student.getClazzName() == null ? "未分班" : student.getClazzName(),
                degreeName(student.getDegree()),
                student.getViolationCount(),
                student.getViolationScore());
    }

    static String formatClazz(Clazz clazz) {
        if (clazz == null) {
            return "未找到该班级。";
        }
        return String.format("ID=%d，名称=%s，教室=%s，学科=%s，班主任=%s，开课=%s，结课=%s",
                clazz.getId(),
                clazz.getName(),
                clazz.getRoom(),
                subjectName(clazz.getSubject()),
                clazz.getMasterName() == null ? (clazz.getMasterId() == null ? "未指定" : "ID=" + clazz.getMasterId()) : clazz.getMasterName(),
                clazz.getBeginDate(),
                clazz.getEndDate());
    }

    static String pairList(List<?> names, List<?> values) {
        if (names == null || values == null || names.isEmpty()) {
            return "没有统计数据。";
        }
        int n = Math.min(names.size(), values.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append("；");
            }
            sb.append(names.get(i)).append("=").append(values.get(i));
        }
        return sb.toString();
    }

    static String namedValueMaps(List<Map> rows) {
        if (rows == null || rows.isEmpty()) {
            return "没有统计数据。";
        }
        return rows.stream()
                .map(m -> String.valueOf(m.get("name")) + "=" + m.get("value"))
                .collect(Collectors.joining("；"));
    }
}
