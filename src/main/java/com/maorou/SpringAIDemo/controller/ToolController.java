package com.maorou.SpringAIDemo.controller;

import com.maorou.SpringAIDemo.functions.MathToolFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
public class ToolController {

    @Autowired
    MathToolFactory mathToolFactory;

    @PostMapping("/api/tools/reload")
    public List<String> reload() throws IOException {
        mathToolFactory.reload();
        return mathToolFactory.buildTools().stream()
                .map(toolCallback -> toolCallback.getToolDefinition().name())
                .toList();
    }
}
