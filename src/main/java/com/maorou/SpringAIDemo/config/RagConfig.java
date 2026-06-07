package com.maorou.SpringAIDemo.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class RagConfig {

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();

        File storeFile = new File("D:\\ai-workspace\\rag-store\\simple-vector-store.json");
        if (storeFile.exists()) {
            vectorStore.load(storeFile);
        }
        return vectorStore;
    }
}
