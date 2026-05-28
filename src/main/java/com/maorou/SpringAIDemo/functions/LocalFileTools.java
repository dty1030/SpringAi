package com.maorou.SpringAIDemo.functions;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class LocalFileTools {

    @Tool(description = "用于读取并列出本地电脑中指定绝对路径（文件夹/目录）下的所有文件和子文件夹的名称")
    public Response listFiles(@ToolParam(description="要扫描的本地绝对路径") String path){
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

    @Tool(description = "用于读取本地电脑中指定绝对路径文件中的内容")
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

    @Tool(description = "用于创建或覆盖写入本地电脑中指定绝对路径文件以及其内容")
    public String writeFile(@ToolParam(description = "要写入的文件的绝对路径") String path,
        @ToolParam(description = "要写入的内容") String content){
        try {
            //设置白名单路径
            File allowBase = new File("D:\\ai-workspace").getCanonicalFile();
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

    // 2. 【怎么写结果】：定义返回给大模型的结果结构
    public record Response(String fileList, boolean success) {}




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
