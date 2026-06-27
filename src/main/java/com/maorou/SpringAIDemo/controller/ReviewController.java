package com.maorou.SpringAIDemo.controller;

import com.maorou.SpringAIDemo.service.RagService;
import com.maorou.SpringAIDemo.service.ReviewSaveService;
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
    ReviewSaveService reviewSaveService;


    @PostMapping("/save")
    public ReviewSaveResult save(@RequestBody ReviewSaveRequest request)
            throws Exception {
        String path = reviewSaveService.save(
                request.symbol(),
                request.name(),
                request.marketData(),
                request.conclusion()
        );

        return new ReviewSaveResult(path, "保存成功，RAG 已刷新");

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
