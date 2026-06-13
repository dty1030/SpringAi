package com.maorou.SpringAIDemo.controller;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

@RestController
public class NlpController {

    @Autowired
    EmbeddingModel embeddingModel;

    @GetMapping("/api/nlp/similarity")
    public Map<String, Object> similarity(@RequestParam String a, @RequestParam String b){

        float[] va = embeddingModel.embed(a);
        float[] vb = embeddingModel.embed(b);

        return Map.of(
                "Dimension", va.length,
                "First 5 numbers of a", Arrays.toString(Arrays.copyOf(va, 5)),
                //两点离得有多近
                "Cosine Similarity", cosine(va, vb));

    }

    //Cosine Similarity = Dot product(x, y) / (|x| * |y|)
    private double cosine(float[] x, float[] y){
        float dotProduct = 0;
        //x 长度的平方
        float nx = 0;
        //y 长度的平方
        float ny = 0;
        for (int i = 0; i < x.length; i++){
            dotProduct += x[i] * y[i];
            nx += x[i] * x[i];
            ny += y[i] * y[i];
        }
        nx = (float) Math.sqrt(nx);
        ny = (float) Math.sqrt(ny);
        return dotProduct / (nx * ny);

    }
}
