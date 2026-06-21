package com.xhz;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class EmbeddingConnectTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    void testEmbedding() {
        String text = "Tlias 智能教务系统学员张三";
        float[] vector = embeddingModel.embed(text);

        System.out.println("向量维度: " + vector.length);
        System.out.println("前5个值: " + Arrays.toString(Arrays.copyOf(vector, 5)));
        assert vector.length == 1536;   // text-embedding-v2 的维度
    }
}