package com.xhz.initializer;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * 教务规则向量数据初始化器 — Spring AI 2.0.0
 *
 * 启动时自动执行，兼容两种 VectorStore 实现：
 *
 * SimpleVectorStore 路径：
 * 1. 检查本地缓存文件 tlias-rules-vectors.json 是否存在
 * 2. 存在 → load() 零 Token 消耗加载
 * 3. 不存在 → 读 txt → 切块 → accept() → save()
 *
 * RedisVectorStore 路径（Jedis ≥ 7.4.1）：
 * 1. 检查 Redis 标记位 tlias_rules:init 是否存在
 * 2. 存在 → 跳过（零 Token 消耗）
 * 3. 不存在 → 读 txt → 切块 → accept() → 写标记位
 *
 * 规则变更后如何重建？在 Redis CLI 执行 DEL tlias_rules:init，重启应用即可。
 *
 * @see SimpleVectorStore
 * @see RedisVectorStore
 */
@Component
public class TliasRulesInitializer implements CommandLineRunner {

    /** Redis 标记位 Key — 存在表示已初始化 */
    private static final String REDIS_INIT_FLAG = "tlias_rules:init";

    private final VectorStore vectorStore;

    /** classpath 下的教务规则原文 */
    @Value("classpath:tlias-rules.txt")
    private Resource rulesResource;

    /** SimpleVectorStore 专用：本地向量缓存文件 */
    private final File vectorCacheFile = new File("tlias-rules-vectors.json");

    public TliasRulesInitializer(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {

        // ==================== SimpleVectorStore ====================
        if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {

            if (vectorCacheFile.exists()) {
                System.out.println(">> [RAG] 读取本地向量缓存文件...");
                simpleVectorStore.load(vectorCacheFile);
                return;
            }

            System.out.println(">> [RAG] 无本地缓存，开始解析教务规则文件...");
            List<Document> splitDocs = loadAndSplitRules();
            if (splitDocs == null) return;

            System.out.println(">> [RAG] 正在调用 Embedding API 生成向量...");
            simpleVectorStore.accept(splitDocs);
            simpleVectorStore.save(vectorCacheFile);
            System.out.println(">> [RAG] 向量化完成并保存至: " + vectorCacheFile.getAbsolutePath());
            return;
        }

        // ==================== RedisVectorStore ====================
        if (vectorStore instanceof RedisVectorStore redisVectorStore) {
            var jedis = redisVectorStore.getJedisClient();

            // 标记位存在 → 已初始化，跳过
            if (jedis.get(REDIS_INIT_FLAG) != null) {
                System.out.println(">> [RAG] Redis 索引标记位存在，已初始化，跳过");
                return;
            }

            // 标记位不存在 → 切块 → 向量化 → 写标记位
            System.out.println(">> [RAG] Redis 标记位不存在，开始解析教务规则文件...");
            List<Document> splitDocs = loadAndSplitRules();
            if (splitDocs == null) return;

            System.out.println(">> [RAG] 正在调用 Embedding API 生成向量并写入 Redis...");
            vectorStore.accept(splitDocs);

            jedis.set(REDIS_INIT_FLAG, "1");
            System.out.println(">> [RAG] 向量化完成，已存入 Redis，标记位已设置");
            return;
        }

        // ==================== 未知类型 ====================
        System.out.println(">> [RAG] 未知 VectorStore 类型: " + vectorStore.getClass().getName()
                + "，跳过初始化");
    }

    /**
     * 读取 classpath:tlias-rules.txt → TokenTextSplitter 切块
     *
     * 切块参数：chunkSize=150 tokens, minChunkSizeChars=20, keepSeparator=true
     *
     * @return 切块后的 Document 列表，文件不存在时返回 null
     */
    private List<Document> loadAndSplitRules() {
        if (!rulesResource.exists()) {
            System.err.println(">> [RAG 错误] 未找到 classpath:tlias-rules.txt");
            return null;
        }

        // 核心变化：将 TextReader 换成 TikaDocumentReader
        TikaDocumentReader tikaReader = new TikaDocumentReader(rulesResource);

        // Tika 会在底层自动判断是 PDF 还是 Word，并提取出纯文本
        List<Document> documents = tikaReader.read();

        // 完美衔接之前的切块逻辑
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(200)
                .withMinChunkSizeChars(20)
                .withKeepSeparator(true)
                .build();

        return splitter.apply(documents);
    }
}
