package com.maorou.SpringAIDemo.functions;
import com.maorou.SpringAIDemo.utils.CodeRunResult;
import org.junit.jupiter.api.Test;

import com.maorou.SpringAIDemo.workspace.WorkspaceStrategy;
import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Path;

class CodeSandboxToolsTest {

    private final WorkspaceStrategy
            workspaceStrategy = new WorkspaceStrategy() {
        private final Path baseDir = Path.of("target", "test-code-sandbox");
        @Override
        public Path ragDocsDir() {
            return baseDir.resolve("rag-docs");
        }

        @Override
        public Path ragStoreFile() {
            return baseDir.resolve("rag-store")
                    .resolve("simple-vector-store.json");
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
        public Path dynamicToolsFile() {
            return baseDir.resolve("dynamic-tools.json");
        }
    };



    @Test
    void runJavaCodeReturnsSuccessForSimpleProgram(){

        CodeSandboxTools codeSandboxTools = new CodeSandboxTools(workspaceStrategy);
        String code = """
          public class Main {
              public static void main(String[] args) {
                  System.out.println(1 + 2);
              }
          }
          """;
        CodeRunResult result = codeSandboxTools.runJavaCode(code);
        assertThat(result.success()).isTrue();
        assertThat(result.stage()).isEqualTo("run");
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.output()).contains("3");
        assertThat(result.error()).isEmpty();


    }

    @Test
    void runJavaCodeBlocksRuntimeExec(){

        CodeSandboxTools codeSandboxTools = new CodeSandboxTools(workspaceStrategy);
        String code = """
          public class Main {
              public static void main(String[] args) {
                  Runtime.getRuntime();
              }
          }
          """;
        CodeRunResult result = codeSandboxTools.runJavaCode(code);
        assertThat(result.success()).isFalse();
        assertThat(result.stage()).isEqualTo("validate");
        assertThat(result.exitCode()).isNull();
        assertThat(result.error()).contains("Runtime.getRuntime");
    }
    @Test
    void runJavaCodeReturnsCompileFailureForInvalidProgram(){

        CodeSandboxTools codeSandboxTools = new CodeSandboxTools(workspaceStrategy);
        String code = """
        public class Main {
            public static void main(String[] args) {
                System.out.println(1 + );
            }
        }
        """;
        CodeRunResult result = codeSandboxTools.runJavaCode(code);

        assertThat(result.success()).isFalse();
        assertThat(result.stage()).isEqualTo("compile");
        assertThat(result.exitCode()).isNotEqualTo(0);
        assertThat(result.error()).isNotBlank();
    }

    @Test
    void runJavaCodeReturnsTimeoutForInfiniteLoop() {
        CodeSandboxTools codeSandboxTools = new CodeSandboxTools(workspaceStrategy);
        String code = """
        public class Main {
            public static void main(String[] args) {
                while(true){
                }
            }
        }
        """;
        CodeRunResult result = codeSandboxTools.runJavaCode(code);

        assertThat(result.success()).isFalse();
        assertThat(result.stage()).isEqualTo("timeout");
        assertThat(result.exitCode()).isNull();
        assertThat(result.error()).contains("timeout");
    }

}
