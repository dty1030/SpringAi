package com.maorou.SpringAIDemo.service;

import com.maorou.SpringAIDemo.workspace.LocalWorkspaceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class ReviewSaveService {
    @Autowired
    LocalWorkspaceStrategy localWorkspaceStrategy;
    @Autowired
    ChatClient reviewOrganizerAgent;
    @Autowired
    RagService ragService;

    public String save(String symbol, String name, String marketData, String conclusion) throws Exception{

        //拿到根目录
        Path baseDir = localWorkspaceStrategy.allowedFileBaseDir();
        //拼出reviews 文件夹
        Path reviewDir = baseDir.resolve("reviews");
        //如果没有reviews文件夹，则创建
        Files.createDirectories(reviewDir);

        String[] lines = marketData.strip().split("\\R");
        String startDate = lines[0].trim().split("\\s+")[0];
        String endDate = lines[lines.length - 1].trim().split("\\s+")[0];  // ③ 最后一行 → 第一个字段 = 结束日期
        String safeName = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        String safeEndDate = endDate.replaceAll("[\\\\/:*?\"<>|]", "_");
        String safeStartDate = startDate.replaceAll("[\\\\/:*?\"<>|]", "_");
        String reviewFileName = safeStartDate + "-" + safeEndDate + "-" + symbol + "-" + safeName + ".md";
        Path file = reviewDir.resolve(reviewFileName);
        String prompt =
                """
                股票代码：%s, 走势数据: %s,  复盘结论: %s
                """
                        .formatted(symbol, marketData, conclusion);
        String aiPart = reviewOrganizerAgent.prompt(prompt).call().content();

        String markdown = """                                                                                                                                                                                                             
          股票代码：%s
          股票名称：%s
          时间区间：%s 至 %s

          【走势数据】
          %s

          【我的复盘结论】
          %s
          [AI总结]
          %s                                                                                                                                                                                                                   
          """
                .formatted(symbol, safeName, safeStartDate, safeEndDate, marketData, conclusion, aiPart);
        Files.writeString(file, markdown);
        ragService.reload();
        return file.toString();

    }
}
