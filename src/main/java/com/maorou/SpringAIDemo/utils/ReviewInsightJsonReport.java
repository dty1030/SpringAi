package com.maorou.SpringAIDemo.utils;

import java.util.List;

public record ReviewInsightJsonReport(
        CurrentFacts currentFacts,
        List<SimilarCase> similarCases,
        String similarityLevel,
        List<String> riskNotes,
        String conclusion
) {
    public record CurrentFacts(
            List<MovingAverageFact> movingAverages,
            boolean volumeClearlyExpanded,
            boolean volumeMildlyExpanded,
            int consecutiveBearishDays,
            String evidence
    ) {}

    public record MovingAverageFact(
            int period,
            double value,
            boolean above,
            double gapPct
    ) {}

    public record SimilarCase(
            String source,
            String corePattern,
            List<String> similarities,
            List<String> missingConditions
    ) {}
}