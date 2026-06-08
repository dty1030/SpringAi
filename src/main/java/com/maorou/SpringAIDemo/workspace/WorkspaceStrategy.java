package com.maorou.SpringAIDemo.workspace;

import java.nio.file.Path;

public interface WorkspaceStrategy {

    Path ragDocsDir();

    Path ragStoreFile();

    Path codeSandboxDir();

    Path allowedFileBaseDir();
}
