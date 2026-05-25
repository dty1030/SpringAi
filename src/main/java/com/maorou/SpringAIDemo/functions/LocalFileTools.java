package com.maorou.SpringAIDemo.functions;

public class LocalFileTools {

    // 1. 【怎么写参数】：用 record 定义输入。大模型会根据字段名 "path" 去用户的聊天里抓取路径
    // ⚠️ 避坑：大模型是根据参数类型来生成 JSON 的。
    public record Request(String path) {}

    // 2. 【怎么写结果】：定义返回给大模型的结果结构
    public record Response(String fileList, boolean success) {}


    // 3. 【怎么做逻辑】：实现 Function 接口，把输入转化为输出
    public static class FileScannerFunction implements Function<Request, Response> {
        @Override
        public Response apply(Request request) {
            // 框架会自动把大模型生成的参数注入到 request 里
            String targetPath = request.path();

            try {
                File file = new File(targetPath);
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
    }
}
