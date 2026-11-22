package dev.aidevoffice.agents;

public interface AnalysisGenerator {
    GeneratedAnalysis generate(String demand);

    record GeneratedAnalysis(AnalysisDraft draft, String model, Integer promptTokens, Integer completionTokens) {}
}
