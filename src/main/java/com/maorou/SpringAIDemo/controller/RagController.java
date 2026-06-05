package com.maorou.SpringAIDemo.controller;

import com.maorou.SpringAIDemo.service.RagService;
import com.maorou.SpringAIDemo.utils.RagSearchResult;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    RagService ragService;

    @GetMapping("/status")
    public String status() {
        return ragService.status();
    }

    @PostMapping("/reload")
    public String reload() throws IOException {
        return ragService.reload();
    }

    @GetMapping("/search")
    public List<RagSearchResult> search(@RequestParam String query) {
        List<Document> documents = ragService.search(query);
        List<RagSearchResult> results = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);

            String text = doc.getText();
            if (text.length() > 200) {
                text = text.substring(0, 200);
            }

            results.add(new RagSearchResult(
                    i + 1,
                    doc.getScore(),
                    String.valueOf(doc.getMetadata().get("source")),
                    doc.getMetadata().get("chunk_index"),
                    text
            ));
        }
        return results;
    }
}
