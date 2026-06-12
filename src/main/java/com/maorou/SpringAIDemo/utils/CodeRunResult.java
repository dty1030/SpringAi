package com.maorou.SpringAIDemo.utils;

public record CodeRunResult(
        boolean success,
        String stage,
        Integer exitCode,
        String output,
        String error
) {
}
