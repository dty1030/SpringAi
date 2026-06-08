package com.maorou.SpringAIDemo.functions;

import com.maorou.SpringAIDemo.workspace.WorkspaceStrategy;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class CodeSandboxTools {

    private static final int RUN_TIMEOUT_SECONDS = 3;
    private static final int MAX_OUTPUT_LENGTH = 2000;

    private final WorkspaceStrategy workspaceStrategy;

    public CodeSandboxTools(WorkspaceStrategy workspaceStrategy) {
        this.workspaceStrategy = workspaceStrategy;
    }

    @Tool(description = "Run a small Java program in a restricted local sandbox directory.")
    public String runJavaCode(
            @ToolParam(description = "Complete Java source code. It must define a public class named Main.")
            String code) {
        try {
            // Use a fixed workspace so generated code never runs from the project directory.
            Path sandboxDir = workspaceStrategy.codeSandboxDir();
            Files.createDirectories(sandboxDir);

            // The submitted code must compile as Main.java because javac/java use the class name.
            Path sourceFile = sandboxDir.resolve("Main.java");
            Files.writeString(sourceFile, code);

            // Compile the source file first. A non-zero exit code means javac found errors.
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", "Main.java");
            compileBuilder.directory(sandboxDir.toFile());
            Process compileProcess = compileBuilder.start();

            int compileExitCode = compileProcess.waitFor();
            String compileError = readProcessError(compileProcess);
            if (compileExitCode != 0) {
                return "Java compile failed.\n" + limitOutput(compileError);
            }

            // Run the compiled Main class from the same sandbox directory.
            ProcessBuilder runBuilder = new ProcessBuilder("java", "Main");
            runBuilder.directory(sandboxDir.toFile());
            Process runProcess = runBuilder.start();

            // Prevent infinite loops or long-running programs from blocking the request forever.
            boolean finished = runProcess.waitFor(RUN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                runProcess.destroyForcibly();
                return "Java run timeout.";
            }

            int runExitCode = runProcess.exitValue();
            String output = readProcessOutput(runProcess);
            String error = readProcessError(runProcess);
            if (runExitCode != 0) {
                return "Java run failed.\n" + limitOutput(error);
            }

            return "Java run success.\nOutput:\n" + limitOutput(output);
        } catch (Exception e) {
            return "Execution failed: " + e.getMessage();
        }
    }

    private String readProcessOutput(Process process) throws Exception {
        // stdout contains normal program output, such as System.out.println(...).
        return new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    private String readProcessError(Process process) throws Exception {
        // stderr contains compiler/runtime errors, stack traces, and System.err output.
        return new String(
                process.getErrorStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    private String limitOutput(String text) {
        // Keep tool results small enough for the model, logs, and frontend to handle.
        if (text == null) {
            return "";
        }
        if (text.length() <= MAX_OUTPUT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_OUTPUT_LENGTH) + "\n... output truncated ...";
    }
}
