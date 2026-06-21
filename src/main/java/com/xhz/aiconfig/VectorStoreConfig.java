package com.xhz.aiconfig;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置 — Spring AI 2.0.0
 *
 * 当前使用 SimpleVectorStore（本地 JSON 文件持久化），
 * 如需切换到 RedisVectorStore，请注释掉此 Bean，
 * 并确保 classpath 有 spring-ai-starter-vector-store-redis 且 Jedis ≥ 7.4.1。
 *
 * 启动时由 {@link com.xhz.initializer.TliasRulesInitializer} 负责数据初始化。
 */
@Configuration
public class VectorStoreConfig {

    /**
     * SimpleVectorStore — 基于项目根目录 tlias-rules-vectors.json 持久化
     *
     * Embedding 由阿里云 DashScope text-embedding-v4（2048 维）完成
     */
    //    @Bean
    //    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    //        return SimpleVectorStore.builder(embeddingModel).build();
    //    }
}
