package com.maorou.SpringAIDemo.functions;

import org.springframework.ai.tool.annotation.Tool;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeTools {

    @Tool(description = "获取当前日期和时间。 当用户询问'现在几点''今天几号'’当前日期'等时间相关问题时，必须调用此工具",
        returnDirect = true)
    public String getCurrentTime(){

        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String time = localDateTime.format(formatter);
        return time;
    }
}
