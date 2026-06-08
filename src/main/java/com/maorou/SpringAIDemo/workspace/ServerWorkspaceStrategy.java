package com.maorou.SpringAIDemo.workspace;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "app.workspace.mode", havingValue = "server")
public class ServerWorkspaceStrategy implements WorkspaceStrategy {

    private final Path baseDir = Path.of("/opt/ai-workspace");
    @Override
    public Path ragDocsDir() {
        return baseDir.resolve("rag-docs");
    }

    @Override
    public Path ragStoreFile() {
        return baseDir.resolve("rag-store").resolve("simple-vector-store.json");
    }

    @Override
    public Path codeSandboxDir() {
        return baseDir.resolve("code-sandbox");
    }

    @Override
    public Path allowedFileBaseDir() {
        return baseDir;
    }

}
