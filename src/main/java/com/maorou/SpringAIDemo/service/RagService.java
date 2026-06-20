package com.maorou.SpringAIDemo.service;

import com.maorou.SpringAIDemo.utils.RagStatus;
import com.maorou.SpringAIDemo.workspace.WorkspaceStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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


        loadDocumentsFromDir(workspaceStrategy.ragDocsDir(),
                ".txt",
                "rag-doc",
                documents);
        loadDocumentsFromDir(
                workspaceStrategy.allowedFileBaseDir().resolve("reviews"),
                ".md",
                "review",
                documents
        );

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

        TokenTextSplitter parentSplitter = TokenTextSplitter.builder()
                .withChunkSize(400).withMinChunkSizeChars(150)
                .withMinChunkLengthToEmbed(5).withMaxNumChunks(100).build();
        TokenTextSplitter childSplitter = TokenTextSplitter.builder()
                .withChunkSize(80).withMinChunkSizeChars(20)
                .withMinChunkLengthToEmbed(5).withMaxNumChunks(100).build();


        List<Document> parents = parentSplitter.split(documents);   // ① 先切中等父块
        List<Document> chunks = new ArrayList<>();
        for (Document parent : parents) {
            // ② 【你写】造个带标签的新 Document:内容=父块原文,metadata 的 parent 也=父块原文
            Document tagged = new Document(parent.getText(),
                    Map.of("source", String.valueOf(parent.getMetadata().get("source")),
            "parent", parent.getText()));
            //    提示:new Document(parent.getText(),
            //              Map.of("source", String.valueOf(parent.getMetadata().get("source")),
            //                     "parent", ____ ))


            // ③ 【你写】把这个父块切成小子块,加进 chunks(子块自动继承 parent 标签)
            chunks.addAll(splitter.split(tagged));
        }

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
    //topK 表示
    public List<Document> search(String query, int topK ) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
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


    private void loadDocumentsFromDir(Path dir, String suffix, String type, List<Document> documents) throws IOException{
        if (!Files.exists(dir)){
            return;
        }

        File[] files = dir.toFile().listFiles();
        if (files == null){
            return;
        }

        for (File file: files){
            if (file.isFile() && file.getName().endsWith(suffix)){
                String text = Files.readString(file.toPath());
                Document document = new Document(text,
                        Map.of("source", file.getName(),
                                "path", file.getAbsolutePath(),
                                "type", type,
                                "parent", text));
                documents.add(document);

            }
        }
    }
}
