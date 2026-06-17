package com.maorou.SpringAIDemo.controller;

import com.maorou.SpringAIDemo.service.RagService;
import com.maorou.SpringAIDemo.workspace.LocalWorkspaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

@RestController
@RequestMapping("/api/review")
public class ReviewController
{
    @Autowired
    LocalWorkspaceStrategy localWorkspaceStrategy;

    @Autowired
    ChatClient reviewOrganizerAgent;
    @Autowired
    RagService ragService;


    @PostMapping("/save")
    public ReviewSaveResult save(@RequestBody ReviewSaveRequest request)
            throws IOException{
        //拿到根目录
        Path baseDir = localWorkspaceStrategy.allowedFileBaseDir();
        //拼出reviews 文件夹
        Path reviewDir = baseDir.resolve("reviews");
        //如果没有reviews文件夹，则创建
        Files.createDirectories(reviewDir);

        String safeName = request.name().replaceAll("[\\\\/:*?\"<>|]", "_");
        String safeStartDate = request.startDate().replaceAll("[\\\\/:*?\"<>|]", "_");
        String safeEndDate = request.endDate().replaceAll("[\\\\/:*?\"<>|]", "_");
        String reviewFileName = safeStartDate
                + "-"
                + safeEndDate
                + "-"
                + request.symbol()
                + "-"
                + safeName
                + ".md";
        /*
        防止覆盖版(测试用)
          String reviewFileName = LocalDateTime.now()
          .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
          + "-"
          + request.symbol()
          + "-"
          + safeName
          + ".md";
         */
        Path file = reviewDir.resolve(reviewFileName);




        String prompt = """
          股票代码：%s
          股票名称：%s
          时间区间：%s 至 %s

          【走势数据】
          %s

          【我的复盘结论】
          %s
          """.formatted(
                request.symbol(),
                request.name(),
                request.startDate(),
                request.endDate(),
                request.marketData(),
                request.conclusion()
        );

        String aiPart = reviewOrganizerAgent.prompt(prompt).call().content();

        String markdown = """
          # 复盘案例：%s（%s）

          ## 股票与区间
          - 股票代码：%s
          - 股票名称：%s
          - 时间区间：%s 至 %s

          ## 原始走势数据
          ```text
          %s
          ```

          ## 我的复盘结论
          %s

          %s
          """.formatted(
                request.name(),
                request.symbol(),
                request.symbol(),
                request.name(),
                request.startDate(),
                request.endDate(),
                request.marketData(),
                request.conclusion(),
                aiPart
        );


        Files.writeString(file, markdown);
        ragService.reload();
        return new ReviewSaveResult(file.toString(), "保存成功，RAG 已刷新");

    }



    record ReviewSaveRequest(
            String symbol,
            String name,
            String startDate,
            String endDate,
            String marketData,
            String conclusion
    ) {}

    record ReviewSaveResult(
            String path,
            String message
    ) {}




}
