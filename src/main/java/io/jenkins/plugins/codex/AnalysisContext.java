package io.jenkins.plugins.codex;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context information for Codex analysis.
 * Gathers relevant information from the current pipeline execution.
 */
public class AnalysisContext {

    private final Run<?, ?> run;
    private final TaskListener listener;
    private final String stageName;
    private final String stepName;
    private final String content;
    private final Map<String, String> environment;
    private final List<String> recentLogs;
    private final String workspacePath;

    public AnalysisContext(Run<?, ?> run, TaskListener listener, String stageName, String stepName,
                          String content, Map<String, String> environment, List<String> recentLogs,
                          String workspacePath) {
        this.run = run;
        this.listener = listener;
        this.stageName = stageName;
        this.stepName = stepName;
        this.content = content;
        this.environment = environment != null ? environment : new HashMap<>();
        this.recentLogs = recentLogs != null ? recentLogs : new ArrayList<>();
        this.workspacePath = workspacePath;
    }

    /**
     * Build context string for Codex analysis
     */
    public String buildContextString() {
        StringBuilder context = new StringBuilder();

        // Pipeline information
        context.append("=== JENKINS PIPELINE ANALYSIS CONTEXT ===\n\n");

        if (stageName != null) {
            context.append("Stage: ").append(stageName).append("\n");
        }

        if (stepName != null) {
            context.append("Step: ").append(stepName).append("\n");
        }

        // Build information
        if (run != null) {
            context.append("Build: #").append(run.getNumber()).append("\n");
            context.append("Job: ").append(run.getParent().getFullName()).append("\n");
            context.append("Status: ").append(run.getResult()).append("\n");
        }

        // Workspace information
        if (workspacePath != null) {
            context.append("Workspace: ").append(workspacePath).append("\n");
        }

        // Environment variables (filtered for security)
        if (!environment.isEmpty()) {
            context.append("\n=== ENVIRONMENT VARIABLES ===\n");
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                String key = entry.getKey();
                // Filter out sensitive environment variables
                if (!isSensitiveVariable(key)) {
                    context.append(key).append("=").append(entry.getValue()).append("\n");
                }
            }
        }

        // Recent logs
        if (!recentLogs.isEmpty()) {
            context.append("\n=== RECENT LOGS ===\n");
            for (String log : recentLogs) {
                context.append(log).append("\n");
            }
        }

        // Content to analyze
        if (content != null && !content.trim().isEmpty()) {
            context.append("\n=== CONTENT TO ANALYZE ===\n");
            context.append(content).append("\n");
        }

        return context.toString();
    }

    /**
     * Build a focused context for specific analysis types
     */
    public String buildFocusedContext(String analysisType) {
        StringBuilder context = new StringBuilder();

        switch (analysisType.toLowerCase()) {
            case "build_analysis":
                context.append("=== BUILD ANALYSIS ===\n");
                context.append("Analyzing build process and output for potential issues and improvements.\n\n");
                break;
            case "test_analysis":
                context.append("=== TEST ANALYSIS ===\n");
                context.append("Analyzing test results and coverage for quality assessment.\n\n");
                break;
            case "deployment_analysis":
                context.append("=== DEPLOYMENT ANALYSIS ===\n");
                context.append("Analyzing deployment process and configuration.\n\n");
                break;
            case "security_analysis":
                context.append("=== SECURITY ANALYSIS ===\n");
                context.append("Analyzing code and configuration for security vulnerabilities.\n\n");
                break;
            case "performance_analysis":
                context.append("=== PERFORMANCE ANALYSIS ===\n");
                context.append("Analyzing performance metrics and bottlenecks.\n\n");
                break;
            default:
                context.append("=== GENERAL ANALYSIS ===\n");
                context.append("Analyzing pipeline execution for insights and recommendations.\n\n");
        }

        context.append(buildContextString());
        return context.toString();
    }

    /**
     * Get analysis suggestions based on context
     */
    public String getAnalysisSuggestions() {
        StringBuilder suggestions = new StringBuilder();

        if (run != null && run.getResult() != null) {
            Result result = run.getResult();
            if (result == Result.FAILURE) {
                suggestions.append("Build failed - focus on error analysis and troubleshooting.\n");
            } else if (result == Result.UNSTABLE) {
                suggestions.append("Build unstable - analyze test failures and warnings.\n");
            } else if (result == Result.SUCCESS) {
                suggestions.append("Build successful - focus on optimization and best practices.\n");
            } else {
                suggestions.append("Build in progress - monitor for potential issues.\n");
            }
        }

        if (stageName != null) {
            switch (stageName.toLowerCase()) {
                case "build":
                    suggestions.append("Build stage - analyze compilation, dependencies, and build artifacts.\n");
                    break;
                case "test":
                    suggestions.append("Test stage - analyze test coverage, failures, and quality metrics.\n");
                    break;
                case "deploy":
                    suggestions.append("Deploy stage - analyze deployment process and configuration.\n");
                    break;
                case "security":
                    suggestions.append("Security stage - analyze security scans and vulnerabilities.\n");
                    break;
            }
        }

        return suggestions.toString();
    }

    private boolean isSensitiveVariable(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("token") ||
               lowerKey.contains("key") ||
               lowerKey.contains("credential") ||
               lowerKey.contains("auth");
    }

    // Getters
    public Run<?, ?> getRun() { return run; }
    public TaskListener getListener() { return listener; }
    public String getStageName() { return stageName; }
    public String getStepName() { return stepName; }
    public String getContent() { return content; }
    public Map<String, String> getEnvironment() { return environment; }
    public List<String> getRecentLogs() { return recentLogs; }
    public String getWorkspacePath() { return workspacePath; }
}
