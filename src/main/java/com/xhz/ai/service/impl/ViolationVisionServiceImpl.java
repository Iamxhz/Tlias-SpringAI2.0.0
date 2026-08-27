package com.xhz.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xhz.ai.config.TliasVisionProperties;
import com.xhz.ai.service.ViolationVisionService;
import com.xhz.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 下载证据图并以 data URL 调用 DashScope 兼容模式视觉接口。
 */
@Service
public class ViolationVisionServiceImpl implements ViolationVisionService {

    private static final Logger log = LoggerFactory.getLogger(ViolationVisionServiceImpl.class);
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final String ANALYZE_PROMPT = """
            你是教务违纪证据识别助手。只根据图片内容识别，不要编造库里的学员 ID。
            请识别：可见的学员姓名、学号/桌牌文字；违纪情节（迟到、早退、不穿校服、玩手机、睡觉、不交作业、旷课等）。
            看不清的字段填 unknown。
            只返回 JSON，不要 Markdown：
            {"students":[{"name":"","studentNo":"","confidence":"high|medium|low"}],\
            "behaviors":[{"type":"","detail":""}],\
            "suggestedRule":"对应规章条款简述，例如玩手机扣5分",\
            "notes":"不确定之处"}
            """;

    private final TliasVisionProperties properties;
    private final String embeddingApiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;

    public ViolationVisionServiceImpl(TliasVisionProperties properties,
                                      @Value("${spring.ai.openai.embedding.api-key:}") String embeddingApiKey) {
        this.properties = properties;
        this.embeddingApiKey = embeddingApiKey;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public String analyzeEvidence(String imageUrl, String imageBase64) {
        if (!properties.isEnabled()) {
            throw new BusinessException("视觉识别未启用（tlias.vision.enabled=false）。");
        }
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException("未配置视觉模型 API Key：请设置 tlias.vision.api-key，或与 DashScope embedding key 共用。");
        }
        ImagePayload image = resolveImage(imageUrl, imageBase64);
        String dataUrl = "data:" + image.mimeType() + ";base64," + Base64.getEncoder().encodeToString(image.bytes());

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)),
                                Map.of("type", "text", "text", ANALYZE_PROMPT)
                        )
                ))
        );

        String baseUrl = trimSlash(properties.getBaseUrl());
        String raw;
        try {
            raw = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .headers(headers -> {
                        headers.setBearerAuth(apiKey);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("视觉模型调用失败: {}", e.getMessage());
            throw new BusinessException("视觉识别失败：" + e.getMessage());
        }
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException("视觉识别失败：模型返回为空。");
        }
        return extractContent(raw);
    }

    private String resolveApiKey() {
        if (StringUtils.hasText(properties.getApiKey())) {
            return properties.getApiKey().trim();
        }
        return embeddingApiKey == null ? "" : embeddingApiKey.trim();
    }

    private ImagePayload resolveImage(String imageUrl, String imageBase64) {
        if (StringUtils.hasText(imageBase64)) {
            return decodeBase64(imageBase64.trim());
        }
        return downloadImage(imageUrl);
    }

    private static ImagePayload decodeBase64(String raw) {
        String mime = "image/jpeg";
        String payload = raw;
        if (raw.startsWith("data:")) {
            int comma = raw.indexOf(',');
            if (comma < 0) {
                throw new BusinessException("证据图 Base64 格式无效。");
            }
            String header = raw.substring(5, comma).toLowerCase();
            if (header.contains("image/png")) {
                mime = "image/png";
            } else if (header.contains("image/webp")) {
                mime = "image/webp";
            } else if (header.contains("image/gif")) {
                mime = "image/gif";
            }
            payload = raw.substring(comma + 1);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("证据图 Base64 解码失败。");
        }
        if (bytes.length == 0) {
            throw new BusinessException("证据图为空。");
        }
        if (bytes.length > MAX_BYTES) {
            throw new BusinessException("证据图超过 5MB，请压缩后重试。");
        }
        return new ImagePayload(bytes, mime);
    }

    private ImagePayload downloadImage(String imageUrl) {
        URI uri = parseAllowedUri(imageUrl);
        byte[] bytes;
        String contentType;
        try {
            var response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(byte[].class);
            bytes = response.getBody();
            MediaType mediaType = response.getHeaders().getContentType();
            contentType = mediaType != null ? mediaType.toString() : null;
        } catch (Exception e) {
            throw new BusinessException("无法读取证据图：" + e.getMessage());
        }
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("证据图为空。");
        }
        if (bytes.length > MAX_BYTES) {
            throw new BusinessException("证据图超过 5MB，请压缩后重试。");
        }
        return new ImagePayload(bytes, resolveMime(uri.toString(), contentType));
    }

    private static URI parseAllowedUri(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new BusinessException("证据图地址为空。");
        }
        URI uri;
        try {
            uri = URI.create(imageUrl.trim());
        } catch (Exception e) {
            throw new BusinessException("证据图地址无效。");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
            throw new BusinessException("证据图必须是 http(s) 地址。");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        boolean ossLike = host.contains("aliyuncs.com") || host.contains("aliyun") || host.contains("oss");
        if (!ossLike && !"localhost".equals(host) && !"127.0.0.1".equals(host)) {
            throw new BusinessException("仅允许使用本系统上传后的对象存储地址作为证据图。");
        }
        return uri;
    }

    private static String resolveMime(String url, String contentType) {
        if (contentType != null) {
            String mime = contentType.split(";")[0].trim().toLowerCase();
            if (mime.startsWith("image/")) {
                return mime;
            }
        }
        String lower = url.toLowerCase();
        if (lower.contains(".png")) {
            return "image/png";
        }
        if (lower.contains(".webp")) {
            return "image/webp";
        }
        if (lower.contains(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }

    private String extractContent(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                JsonNode error = root.path("error").path("message");
                if (error.isTextual()) {
                    throw new BusinessException("视觉识别失败：" + error.asText());
                }
                throw new BusinessException("视觉识别失败：响应中没有内容。");
            }
            if (content.isTextual()) {
                return stripFence(content.asText());
            }
            if (content.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode part : content) {
                    if (part.has("text")) {
                        sb.append(part.get("text").asText());
                    } else if (part.isTextual()) {
                        sb.append(part.asText());
                    }
                }
                return stripFence(sb.toString());
            }
            return stripFence(content.toString());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("视觉识别结果解析失败。");
        }
    }

    private static String stripFence(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNl = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                return trimmed.substring(firstNl + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private static String trimSlash(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record ImagePayload(byte[] bytes, String mimeType) {
    }
}
