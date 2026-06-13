package com.maorou.SpringAIDemo.controller;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
public class NlpController {

    @Autowired
    EmbeddingModel embeddingModel;


    private final Encoding enc = Encodings.newDefaultEncodingRegistry()
            .getEncoding(EncodingType.CL100K_BASE);

    @GetMapping("/api/nlp/tokenize")
    public Map<String, Object> tokenize(@RequestParam String text){
        IntArrayList ids = enc.encode(text);

        List<String> pieces = new ArrayList<>();
        for (int i = 0; i< ids.size(); i++){
            IntArrayList one = new IntArrayList();
            one.add(ids.get(i));
            pieces.add(enc.decode(one));
        }
        return Map.of(
                "text", text,
                "token数", ids.size(),
                "token_ids", ids.boxed(),
                "每个token的片段", pieces
        );
    }

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
