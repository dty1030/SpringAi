package com.maorou.SpringAIDemo.utils;

public record RagStatus(int rawDocumentCount,
                        int chunkCount,
                        String workspaceStrategy,
                        String ragDocsDir,
                        String storeFile,
                        boolean storeFileExists,
                        long storeFileSizeBytes) {


}
