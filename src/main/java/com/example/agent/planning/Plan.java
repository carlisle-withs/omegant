package com.example.agent.planning;

import java.util.List;

/**
 * 执行计划
 */
public record Plan(
        String goal,
        List<PlanStep> steps,
        int currentStep
) {
    public Plan(String goal, List<PlanStep> steps) {
        this(goal, steps, 0);
    }

    public int totalSteps() {
        return steps.size();
    }

    public boolean isComplete() {
        return currentStep >= steps.size();
    }

    public PlanStep getCurrentStep() {
        if (isComplete()) {
            return null;
        }
        return steps.get(currentStep);
    }

    public Plan nextStep() {
        return new Plan(goal, steps, currentStep + 1);
    }
}
