package io.jenkins.plugins.codex;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Action;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action for stage-level Codex analysis.
 * Automatically analyzes stage execution and provides insights.
 */
public class CodexAnalysisAction implements Action {

    private final Run<?, ?> run;
    private final String stageName;
    private final String analysisResult;
    private final String analysisType;
    private final long timestamp;

    public CodexAnalysisAction(Run<?, ?> run, String stageName, String analysisResult, String analysisType) {
        this.run = run;
        this.stageName = stageName;
        this.analysisResult = analysisResult;
        this.analysisType = analysisType;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String getIconFileName() {
        return "codex-analysis.svg";
    }

    @Override
    public String getDisplayName() {
        return "Codex Analysis - " + stageName;
    }

    @Override
    public String getUrlName() {
        return "codex-analysis-" + stageName.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }

    public String getStageName() {
        return stageName;
    }

    public String getAnalysisResult() {
        return analysisResult;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Perform stage-level analysis
     */
    public static CodexAnalysisAction analyzeStage(Run<?, ?> run, String stageName,
                                                 TaskListener listener, Launcher launcher,
                                                 EnvVars environment, FilePath workspace) {
        try {
            // Gather stage context
            AnalysisContext context = gatherStageContext(run, stageName, listener, environment, workspace);

            // Determine analysis type based on stage name
            String analysisType = determineAnalysisType(stageName);

            // Execute analysis
            CodexCliExecutor executor = new CodexCliExecutor(launcher, listener, environment, workspace);
            if (!executor.isCodexAvailable()) {
                listener.error("Codex CLI not available for stage analysis");
                return new CodexAnalysisAction(run, stageName, "Codex CLI not available", analysisType);
            }

            String contextString = context.buildFocusedContext(analysisType);
            CodexCliExecutor.CodexAnalysisResult result = executor.executeAnalysis(
                contextString,
                analysisType,
                "Analyze this Jenkins pipeline stage execution and provide insights, recommendations, and potential issues.",
                new HashMap<>()
            );

            if (result.isSuccess()) {
                listener.getLogger().println("=== STAGE ANALYSIS COMPLETE ===");
                listener.getLogger().println("Stage: " + stageName);
                listener.getLogger().println("Analysis Type: " + analysisType);
                listener.getLogger().println("Result: " + result.getOutput());
                return new CodexAnalysisAction(run, stageName, result.getOutput(), analysisType);
            } else {
                listener.error("Stage analysis failed: " + result.getError());
                return new CodexAnalysisAction(run, stageName, "Analysis failed: " + result.getError(), analysisType);
            }

        } catch (Exception e) {
            listener.error("Error during stage analysis: " + e.getMessage());
            return new CodexAnalysisAction(run, stageName, "Analysis error: " + e.getMessage(), "error");
        }
    }

    /**
     * Gather context information for stage analysis
     */
    private static AnalysisContext gatherStageContext(Run<?, ?> run, String stageName,
                                                    TaskListener listener, EnvVars environment,
                                                    FilePath workspace) throws IOException {

        // Gather recent logs (last 50 lines)
        List<String> recentLogs = new ArrayList<>();
        if (run instanceof WorkflowRun) {
            WorkflowRun workflowRun = (WorkflowRun) run;
            try {
                // This is a simplified approach - in practice, you'd want to parse the actual logs
                recentLogs.add("Stage '" + stageName + "' execution context");
                recentLogs.add("Build #" + run.getNumber() + " - " + run.getResult());
            } catch (Exception e) {
                listener.getLogger().println("Warning: Could not gather detailed logs: " + e.getMessage());
            }
        }

        // Build environment map
        Map<String, String> envMap = new HashMap<>();
        if (environment != null) {
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                envMap.put(entry.getKey(), entry.getValue());
            }
        }

        return new AnalysisContext(
            run, listener, stageName, null,
            "Stage execution analysis", envMap, recentLogs,
            workspace != null ? workspace.getRemote() : null
        );
    }

    /**
     * Determine analysis type based on stage name
     */
    private static String determineAnalysisType(String stageName) {
        if (stageName == null) {
            return "general";
        }

        String lowerStageName = stageName.toLowerCase();

        if (lowerStageName.contains("build") || lowerStageName.contains("compile")) {
            return "build_analysis";
        } else if (lowerStageName.contains("test") || lowerStageName.contains("testing")) {
            return "test_analysis";
        } else if (lowerStageName.contains("deploy") || lowerStageName.contains("deployment")) {
            return "deployment_analysis";
        } else if (lowerStageName.contains("security") || lowerStageName.contains("scan")) {
            return "security_analysis";
        } else if (lowerStageName.contains("performance") || lowerStageName.contains("benchmark")) {
            return "performance_analysis";
        } else if (lowerStageName.contains("quality") || lowerStageName.contains("check")) {
            return "quality_analysis";
        } else {
            return "general";
        }
    }

    /**
     * Get analysis summary for display
     */
    public String getAnalysisSummary() {
        if (analysisResult == null || analysisResult.trim().isEmpty()) {
            return "No analysis available";
        }

        // Extract first few lines as summary
        String[] lines = analysisResult.split("\n");
        StringBuilder summary = new StringBuilder();
        int maxLines = 3;

        for (int i = 0; i < Math.min(lines.length, maxLines); i++) {
            if (lines[i].trim().length() > 0) {
                summary.append(lines[i].trim()).append(" ");
            }
        }

        if (lines.length > maxLines) {
            summary.append("...");
        }

        return summary.toString();
    }

    /**
     * Check if analysis indicates issues
     */
    public boolean hasIssues() {
        if (analysisResult == null) {
            return false;
        }

        String lowerResult = analysisResult.toLowerCase();
        return lowerResult.contains("error") ||
               lowerResult.contains("warning") ||
               lowerResult.contains("issue") ||
               lowerResult.contains("problem") ||
               lowerResult.contains("failed") ||
               lowerResult.contains("critical");
    }

    /**
     * Get issue count estimate
     */
    public int getIssueCount() {
        if (analysisResult == null) {
            return 0;
        }

        String lowerResult = analysisResult.toLowerCase();
        int count = 0;

        // Simple heuristic to count potential issues
        count += countOccurrences(lowerResult, "error");
        count += countOccurrences(lowerResult, "warning");
        count += countOccurrences(lowerResult, "issue");
        count += countOccurrences(lowerResult, "problem");

        return count;
    }

    private int countOccurrences(String text, String word) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(word, index)) != -1) {
            count++;
            index += word.length();
        }
        return count;
    }
}
