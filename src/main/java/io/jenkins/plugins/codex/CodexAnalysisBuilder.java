package io.jenkins.plugins.codex;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for freestyle jobs to perform Codex analysis.
 * Can analyze build output, logs, or any specified content.
 */
public class CodexAnalysisBuilder extends Builder {

    private final String content;
    private final String analysisType;
    private final String prompt;
    private final String model;
    private final int timeoutSeconds;
    private final boolean includeBuildContext;
    private final boolean failOnError;
    private final String additionalParams;

    @DataBoundConstructor
    public CodexAnalysisBuilder(String content, String analysisType, String prompt,
                               String model, int timeoutSeconds, boolean includeBuildContext,
                               boolean failOnError, String additionalParams) {
        this.content = content;
        this.analysisType = analysisType != null ? analysisType : "general";
        this.prompt = prompt;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 120;
        this.includeBuildContext = includeBuildContext;
        this.failOnError = failOnError;
        this.additionalParams = additionalParams;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return performAnalysis(build, launcher, listener, build.getWorkspace());
    }

    public boolean performAnalysis(Run<?, ?> run, Launcher launcher, TaskListener listener, FilePath workspace) throws InterruptedException, IOException {
        try {
            listener.getLogger().println("Starting Codex analysis...");

            // Get job-level configuration if available
            CodexAnalysisJobProperty jobConfig = null;
            if (run.getParent() instanceof Job) {
                jobConfig = ((Job<?, ?>) run.getParent()).getProperty(CodexAnalysisJobProperty.class);
            }

            // Check if Codex CLI is available
            EnvVars environment = run.getEnvironment(listener);
            CodexCliExecutor executor = new CodexCliExecutor(launcher, listener, environment, workspace, jobConfig);

            if (!executor.isCodexAvailable()) {
                String error = "Codex CLI is not available. Please ensure it's installed and configured.";
                listener.error(error);
                if (failOnError) {
                    return false;
                }
                return true;
            }

            // Prepare content for analysis
            String contentToAnalyze = content;
            if (contentToAnalyze == null || contentToAnalyze.trim().isEmpty()) {
                contentToAnalyze = "No specific content provided for analysis.";
            }

            // Include build context if requested
            if (includeBuildContext) {
                AnalysisContext analysisContext = new AnalysisContext(
                    run, listener, null, "codexAnalysisBuilder",
                    contentToAnalyze, environment, null,
                    workspace != null ? workspace.getRemote() : null
                );
                contentToAnalyze = analysisContext.buildFocusedContext(analysisType);
            }

            // Parse additional parameters
            Map<String, String> params = parseAdditionalParams(additionalParams);

            // Use job-level model if specified, otherwise use step-level model, otherwise use job default
            String effectiveModel = model;
            if ((effectiveModel == null || effectiveModel.trim().isEmpty()) && jobConfig != null) {
                effectiveModel = jobConfig.getEffectiveDefaultModel();
            }
            if (effectiveModel != null && !effectiveModel.trim().isEmpty()) {
                params.put("model", effectiveModel);
            }

            // Use job-level timeout if specified, otherwise use step-level timeout, otherwise use job default
            int effectiveTimeout = timeoutSeconds;
            if (effectiveTimeout <= 0 && jobConfig != null) {
                effectiveTimeout = jobConfig.getEffectiveTimeoutSeconds();
            }
            if (effectiveTimeout > 0) {
                params.put("timeout", String.valueOf(effectiveTimeout));
            }

            // Execute analysis
            CodexCliExecutor.CodexAnalysisResult result = executor.executeAnalysis(
                contentToAnalyze,
                analysisType,
                prompt,
                params
            );

            if (result.isSuccess()) {
                listener.getLogger().println("=== CODEX ANALYSIS RESULT ===");
                listener.getLogger().println("Analysis Type: " + analysisType);
                listener.getLogger().println("Result:");
                listener.getLogger().println(result.getOutput());
                listener.getLogger().println("=== END ANALYSIS ===");

                // Add action to build for later reference
                run.addAction(new CodexAnalysisAction(run, "Build Analysis", result.getOutput(), analysisType));

                return true;
            } else {
                String error = "Codex analysis failed: " + result.getError();
                listener.error(error);
                if (failOnError) {
                    return false;
                }
                return true;
            }

        } catch (Exception e) {
            String error = "Error during Codex analysis: " + e.getMessage();
            listener.error(error);
            if (failOnError) {
                return false;
            }
            return true;
        }
    }

    /**
     * Parse additional parameters from string format
     */
    private Map<String, String> parseAdditionalParams(String paramsString) {
        Map<String, String> params = new HashMap<>();

        if (paramsString == null || paramsString.trim().isEmpty()) {
            return params;
        }

        // Simple parsing of key=value pairs separated by newlines or semicolons
        String[] lines = paramsString.split("[\\r\\n;]+");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int equalsIndex = line.indexOf('=');
            if (equalsIndex > 0) {
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();
                params.put(key, value);
            }
        }

        return params;
    }

    // Getters
    public String getContent() { return content; }
    public String getAnalysisType() { return analysisType; }
    public String getPrompt() { return prompt; }
    public String getModel() { return model; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public boolean isIncludeBuildContext() { return includeBuildContext; }
    public boolean isFailOnError() { return failOnError; }
    public String getAdditionalParams() { return additionalParams; }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true; // Applicable to all project types
        }

        @Override
        public String getDisplayName() {
            return "Codex Analysis";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/codex-analysis/help.html";
        }

        /**
         * Validate analysis type
         */
        public FormValidation doCheckAnalysisType(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.warning("Analysis type is empty, will use 'general'");
            }

            String[] validTypes = {
                "general", "build_analysis", "test_analysis", "deployment_analysis",
                "security_analysis", "performance_analysis", "quality_analysis"
            };

            for (String validType : validTypes) {
                if (validType.equals(value.trim())) {
                    return FormValidation.ok();
                }
            }

            return FormValidation.warning("Unknown analysis type. Valid types: " + String.join(", ", validTypes));
        }

        /**
         * Validate timeout
         */
        public FormValidation doCheckTimeoutSeconds(@QueryParameter String value) {
            try {
                int timeout = Integer.parseInt(value);
                if (timeout <= 0) {
                    return FormValidation.error("Timeout must be positive");
                }
                if (timeout > 3600) {
                    return FormValidation.warning("Very long timeout may cause build delays");
                }
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("Invalid timeout value");
            }
        }

        /**
         * Get available analysis types
         */
        public String[] getAnalysisTypes() {
            return new String[]{
                "general",
                "build_analysis",
                "test_analysis",
                "deployment_analysis",
                "security_analysis",
                "performance_analysis",
                "quality_analysis"
            };
        }

        /**
         * Get available models from configuration
         */
        public String[] getAvailableModels() {
            // Default model options (no global default model anymore)
            return new String[]{
                "kimi-k2",
                "gpt-4",
                "gpt-4-turbo",
                "gpt-3.5-turbo",
                "claude-3-opus",
                "claude-3-sonnet",
                "claude-3-haiku",
                "gemini-pro",
                "gemini-pro-vision"
            };
        }
    }
}
