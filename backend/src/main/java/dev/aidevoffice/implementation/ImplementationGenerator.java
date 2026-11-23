package dev.aidevoffice.implementation;

public interface ImplementationGenerator {
    ImplementationProposal generate(String demand, String approvedPlanJson);

    String model();
}
