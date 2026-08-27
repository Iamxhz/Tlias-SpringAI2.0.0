package com.xhz.ai.service;

/**
 * 违纪证据图视觉识别：抽出姓名/学号/情节，不写库。
 */
public interface ViolationVisionService {

    /**
     * @param imageUrl    已上传的图片 HTTPS 地址（通常为 OSS，可为空）
     * @param imageBase64 前端直传的图片 Base64（可带 data URL 前缀）；优先于下载 URL
     */
    String analyzeEvidence(String imageUrl, String imageBase64);
}
