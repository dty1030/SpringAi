package com.maorou.SpringAIDemo.workspace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "app.workspace.mode", havingValue = "local", matchIfMissing = true)
public class LocalWorkspaceStrategy implements WorkspaceStrategy {

    private final Path baseDir;

    public LocalWorkspaceStrategy(
            @Value("${app.workspace.base-dir:D:\\ai-workspace}") String baseDir) {
        this.baseDir = Path.of(baseDir);
    }

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

    @Override
    public Path dynamicToolsFile(){
        return baseDir.resolve("math-tools.json");
    }
}
