package com.maorou.SpringAIDemo.service;

import com.maorou.SpringAIDemo.utils.RagStatus;
import com.maorou.SpringAIDemo.workspace.WorkspaceStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RagService {

    @Autowired
    WorkspaceStrategy workspaceStrategy;

    @Autowired
    EmbeddingModel embeddingModel;



    private SimpleVectorStore vectorStore;

    private int rawDocumentCount;
    private int chunkCount;

    @PostConstruct
    public void init() {
        vectorStore = createEmptyVectorStore();
        File storeFile = getStoreFile();
        if (storeFile.exists()) {
            vectorStore.load(storeFile);
        }
    }

    public SimpleVectorStore getVectorStore() {
        return vectorStore;
    }

    public RagStatus reload() throws IOException {
        File dir = workspaceStrategy.ragDocsDir().toFile();
        List<Document> documents = new ArrayList<>();

        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".txt")) {
                        String text = Files.readString(file.toPath());
                        Document document = new Document(text,
                                Map.of("source", file.getName(),
                                        "path", file.getAbsolutePath()));
                        documents.add(document);
                    }
                }
            }
        }
        if (documents.isEmpty()) {
            clearVectorStore();
            log.info("No RAG documents found. Cleared vector store.");
            return status();
        }


        TokenTextSplitter splitter =
                TokenTextSplitter.builder()
                        .withChunkSize(80)
                        .withMinChunkSizeChars(20)
                        .withMinChunkLengthToEmbed(5)
                        .withMaxNumChunks(100)
                        .build();
        List<Document> chunks = splitter.split(documents);

        SimpleVectorStore newVectorStore = createEmptyVectorStore();
        newVectorStore.add(chunks);

        File storeFile = getStoreFile();
        File parentDir = storeFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
        newVectorStore.save(storeFile);
        vectorStore = newVectorStore;
        rawDocumentCount = documents.size();
        chunkCount = chunks.size();


        return status();
    }

    public RagStatus status() {
        File storeFile = getStoreFile();

        return new RagStatus(
                rawDocumentCount,
                chunkCount,
                workspaceStrategy.getClass().getSimpleName(),
                workspaceStrategy.ragDocsDir().toString(),
                storeFile.getAbsolutePath(),
                storeFile.exists(),
                storeFile.exists() ? storeFile.length() : 0
        );
    }

    public List<Document> search(String query) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(3)
                .similarityThresholdAll()
                .build();
        List<Document> matches = vectorStore.similaritySearch(searchRequest);
        for (int i = 0; i < matches.size(); i++) {
            Document doc = matches.get(i);
            String text = doc.getText();
            if (text.length() > 120) {
                text = text.substring(0, 120);
            }
            log.info("RAG hit #{} score={} source={} text={}",
                    i + 1,
                    doc.getScore(),
                    doc.getMetadata().get("source"),
                    text);
        }
        return matches;
    }

    private SimpleVectorStore createEmptyVectorStore() {
        return SimpleVectorStore.builder(embeddingModel)
                .build();
    }

    private File getStoreFile() {
        return workspaceStrategy.ragStoreFile().toFile();
    }

    private void clearVectorStore() throws IOException{
        vectorStore = createEmptyVectorStore();
        File storeFile = getStoreFile();
        if (storeFile.exists()){
            Files.delete(storeFile.toPath());
        }
        rawDocumentCount = 0;
        chunkCount = 0;
    }
}
