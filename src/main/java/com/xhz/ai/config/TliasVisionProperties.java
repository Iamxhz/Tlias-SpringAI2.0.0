package com.xhz.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 违纪证据图识别。主 Chat 模型是文本模型，看图单独走兼容 OpenAI 的视觉接口（默认 DashScope qwen-vl）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "tlias.vision")
public class TliasVisionProperties {

    private boolean enabled = true;

    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /** 留空则回退到 spring.ai.openai.embedding.api-key */
    private String apiKey = "";

    private String model = "qwen-vl-plus";
}
