package com.maorou.SpringAIDemo.functions;
import com.maorou.SpringAIDemo.workspace.WorkspaceStrategy;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


public class LocalFileTools {


    private final WorkspaceStrategy workspaceStrategy;
    public LocalFileTools(WorkspaceStrategy workspaceStrategy){
        this.workspaceStrategy = workspaceStrategy;
    }

    @Tool(description = "List file and directory names\n" +
            "  under a given absolute local directory path. Use this\n" +
            "  before reading a file when the user asks to inspect a\n" +
            "  directory.")
    public Response listFiles(@ToolParam(description="Absolute local directory\n" +
            "  path to list") String path){
            try{
            File file = new File(path);
            if (!file.exists()) {
                return new Response("该路径在电脑中不存在，请确认是否输入正确！", false);
            }
            if (!file.isDirectory()) {
                return new Response("这只是一个普通文件，不是文件夹，无法列出目录。", false);
            }

            String[] list = file.list();
            if (list == null || list.length == 0) {
                return new Response("这个文件夹里面是空的。", true);
            }

            // 把文件数组拼成字符串返回
            return new Response(String.join(", ", list), true);

        } catch (Exception e) {
            return new Response("读取失败，发生了错误: " + e.getMessage(), false);
        }
    }

    @Tool(description = "Read the text content of a given\n" +
            "  absolute local file path. Use this after locating a\n" +
            "  file that the user wants to inspect or summarize.")
    public String readFile(@ToolParam(description="要读取的文件的绝对路径") String path){
        // 读文件内容 → return 出去
        try {
            File file = new File(path);
            if (!file.exists()){
                return "该路径在电脑中不存在，请确认是否输入正确";
            }
            if (file.isDirectory()) {
                return "这是一个文件夹，无法读取";
            }

            return Files.readString(file.toPath());
        } catch (IOException e) {
            return "读取失败，发生了错误: " + e.getMessage();
            //throw new RuntimeException(e);
        }


    }

    @Tool(description = "Create or overwrite a local file\n" +
            "  under the configured workspace directory with the given text content.\n" +
            "  Only use this when the user explicitly asks to write a\n" +
            "  file.")
    public String writeFile(@ToolParam(description = "要写入的文件的绝对路径") String path,
        @ToolParam(description = "要写入的内容") String content){
        try {
            //设置白名单路径
            File allowBase = workspaceStrategy.allowedFileBaseDir()
                    .toFile()
                    .getCanonicalFile();
            File file = new File(path).getCanonicalFile();
            if (!file.toPath().startsWith(allowBase.toPath())){
                return "Not Allowed";
            }
            Files.writeString(file.toPath(), content);

            return "文件已经写入: " + path;
        } catch (IOException e) {
            return "写入失败，发生了错误: " + e.getMessage();
        }
    }
//    @Tool(description = "Get file infos of the giben absolute local file path, Only use this when the user explicitly asks to " +
//            "get the file infos")
//    public String getFileInfo(@ToolParam(description = "Absloute path of the file directory") String path){
//        try {
//            String res = "";
//            //设置白名单路径
//            File allowBase = workspaceStrategy.allowedFileBaseDir()
//                    .toFile()
//                    .getCanonicalFile();
//            File file = new File(path).getCanonicalFile();
//            if (!file.toPath().startsWith(allowBase.toPath())){
//                return "Not Allowed";
//            }
//            if (!file.exists()){
//                return "该路径在电脑中不存在，请确认是否输入正确";
//            }
//            res += "exists: true\n";
//            if (file.isDirectory()) {
//                res += "type: directory\n";
//            } else if (file.isFile()) {
//                res += "type: file\n";
//            }
//            long lastModifiedTime = file.lastModified();
//            String lastModified = Instant.ofEpochMilli(lastModifiedTime)
//                            .atZone(ZoneId.systemDefault())
//                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//            long fileSize = Files.size(file.toPath());
//
//            return res + "\n" + "Space Usage: " + fileSize + "\n" + "Last Modified Time: "+ lastModified;
//        } catch (IOException e) {
//            return "写入失败，发生了错误: " + e.getMessage();
//        }
//    }

    @Tool(description = "Get file infos of the giben absolute local file path, Only use this when the user explicitly asks to " +
            "get the file infos")
    public FileInfo getFileInfo(@ToolParam(description = "Absloute path of the file directory") String path){
        try {
            String filetype = "";

            //设置白名单路径
            File allowBase = workspaceStrategy.allowedFileBaseDir()
                    .toFile()
                    .getCanonicalFile();
            File file = new File(path).getCanonicalFile();
            if (!file.toPath().startsWith(allowBase.toPath())){
                return new FileInfo(false, null, null, null, "Not Allowed");
            }
            if (!file.exists()){
                return new FileInfo(false,null, null, null, "file not found");
            }
            if (file.isDirectory()) {
                filetype = "directory";
            } else if (file.isFile()) {
                filetype = "file";
            }
            long lastModifiedTime = file.lastModified();
            String lastModified = Instant.ofEpochMilli(lastModifiedTime)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            long fileSize = Files.size(file.toPath());

            return new FileInfo(true, filetype, fileSize, lastModified, null);
        } catch (IOException e) {
            return new FileInfo(false, null, null, null, "error: " + e.getMessage());
        }
    }
    //定义返回给大模型的结果结构
    public record Response(String fileList, boolean success) {}

    public record FileInfo(boolean exists, String type, Long sizeBytes, String lastModified, String note){}




    // 3. 【怎么做逻辑】：实现 Function 接口，把输入转化为输出
//    public static class FileScannerFunction implements Function<Request, Response> {
//        @Override
//        public Response apply(Request request) {
//            // 框架会自动把大模型生成的参数注入到 request 里
//            String targetPath = request.path();
//
//            try {
//                File file = new File(targetPath);
//                if (!file.exists()) {
//                    return new Response("该路径在电脑中不存在，请确认是否输入正确！", false);
//                }
//                if (!file.isDirectory()) {
//                    return new Response("这只是一个普通文件，不是文件夹，无法列出目录。", false);
//                }
//
//                String[] list = file.list();
//                if (list == null || list.length == 0) {
//                    return new Response("这个文件夹里面是空的。", true);
//                }
//
//                // 把文件数组拼成字符串返回
//                return new Response(String.join(", ", list), true);
//
//            } catch (Exception e) {
//                return new Response("读取失败，发生了错误: " + e.getMessage(), false);
//            }
//        }
//    }
}
