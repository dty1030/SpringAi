package com.maorou.SpringAIDemo.utils;

public class JsonTextUtils {

    public static String extractJsonObject(String text){
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalArgumentException("模型没有返回合法 JSON 对象: " + text);
        }

        return text.substring(start, end + 1);
    }
}
