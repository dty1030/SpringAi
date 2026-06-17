package com.maorou.SpringAIDemo.utils;

import java.util.List;

public record MyViewJsonReport(
        CurrentFacts currentFacts,
        List<ReviewPattern> matchedReviewPatterns,
        List<PrincipleCheck> principleChecks,
        Decision decision,
        List<String> warnings
) {

    public record CurrentFacts(
            boolean priceBelowMa20,
            boolean volumeClearlyExpanded,
            boolean volumeMildlyExpanded,
            int consecutiveBearishDays,
            String evidence
    ) {}


    public record ReviewPattern(
            String pattern,
            String matchLevel,
            String reason
    ) {}

    public record PrincipleCheck(
            String principle,
            String status,
            String evidence
    ) {}

    public record Decision(
            String action,
            String confidence,
            String reason
    ) {}
}


