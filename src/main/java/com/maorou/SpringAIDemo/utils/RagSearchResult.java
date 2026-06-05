package com.maorou.SpringAIDemo.utils;

public record RagSearchResult(
        int rank,
        Double score,
        String source,
        Object chunkIndex,
        String text
) {}
