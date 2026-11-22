package dev.aidevoffice.agents;

public interface TeamAnalysisGenerator {
    TeamStepArtifact generate(TeamRole role, String demand, String priorContext);
    String model();
}
