package io.jenkins.plugins.codex;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Pipeline step for Codex analysis.
 * Can be used in Pipeline scripts to analyze content, logs, or any text.
 */
public class CodexAnalysisStep extends Step {

    private String content;
    private String analysisType = "general";
    private String prompt;
    private String model;
    private int timeoutSeconds = 120;
    private boolean includeContext = true;
    private Map<String, String> additionalParams = new HashMap<>();

    @DataBoundConstructor
    public CodexAnalysisStep() {}

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(this, context);
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<String> {

        private final CodexAnalysisStep step;

        public Execution(CodexAnalysisStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected String run() throws Exception {
            StepContext context = getContext();
            Run<?, ?> run = context.get(Run.class);
            TaskListener listener = context.get(TaskListener.class);
            Launcher launcher = context.get(Launcher.class);
            EnvVars environment = context.get(EnvVars.class);
            FilePath workspace = context.get(FilePath.class);

            if (listener == null) {
                throw new IOException("TaskListener not available");
            }

            // Get job-level configuration if available
            CodexAnalysisJobProperty jobConfig = null;
            if (run.getParent() instanceof Job) {
                jobConfig = ((Job<?, ?>) run.getParent()).getProperty(CodexAnalysisJobProperty.class);
            }

            // Check if Codex CLI is available
            CodexCliExecutor executor = new CodexCliExecutor(launcher, listener, environment, workspace, jobConfig);
            if (!executor.isCodexAvailable()) {
                listener.error("Codex CLI is not available. Please ensure it's installed and configured.");
                return "Codex CLI not available";
            }

            // Prepare content for analysis
            String contentToAnalyze = step.content;
            if (contentToAnalyze == null || contentToAnalyze.trim().isEmpty()) {
                contentToAnalyze = "No specific content provided for analysis.";
            }

            // Build analysis context if requested
            if (step.includeContext) {
                AnalysisContext analysisContext = new AnalysisContext(
                    run, listener, null, "codexAnalysis",
                    contentToAnalyze, environment, null,
                    workspace != null ? workspace.getRemote() : null
                );
                contentToAnalyze = analysisContext.buildFocusedContext(step.analysisType);
            }

            // Prepare additional parameters
            Map<String, String> params = new HashMap<>(step.additionalParams);

            // Use job-level model if specified, otherwise use step-level model, otherwise use job default
            String effectiveModel = step.model;
            if ((effectiveModel == null || effectiveModel.trim().isEmpty()) && jobConfig != null) {
                effectiveModel = jobConfig.getEffectiveDefaultModel();
            }
            if (effectiveModel != null && !effectiveModel.trim().isEmpty()) {
                params.put("model", effectiveModel);
            }

            // Use job-level timeout if specified, otherwise use step-level timeout, otherwise use job default
            int effectiveTimeout = step.timeoutSeconds;
            if (effectiveTimeout <= 0 && jobConfig != null) {
                effectiveTimeout = jobConfig.getEffectiveTimeoutSeconds();
            }
            if (effectiveTimeout > 0) {
                params.put("timeout", String.valueOf(effectiveTimeout));
            }

            // Execute analysis
            try {
                CodexCliExecutor.CodexAnalysisResult result = executor.executeAnalysis(
                    contentToAnalyze,
                    step.analysisType,
                    step.prompt,
                    params
                );

                if (result.isSuccess()) {
                    listener.getLogger().println("=== CODEX ANALYSIS RESULT ===");
                    listener.getLogger().println(result.getOutput());
                    listener.getLogger().println("=== END ANALYSIS ===");
                    return result.getOutput();
                } else {
                    listener.error("Codex analysis failed: " + result.getError());
                    return "Analysis failed: " + result.getError();
                }

            } catch (Exception e) {
                listener.error("Error during Codex analysis: " + e.getMessage());
                return "Analysis error: " + e.getMessage();
            }
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @org.kohsuke.stapler.DataBoundSetter
    public void setContent(String content) {
        this.content = content;
    }

    @org.kohsuke.stapler.DataBoundSetter
    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    @org.kohsuke.stapler.DataBoundSetter
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @org.kohsuke.stapler.DataBoundSetter
    public void setModel(String model) {
        this.model = model;
    }

    @org.kohsuke.stapler.DataBoundSetter
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @org.kohsuke.stapler.DataBoundSetter
    public void setIncludeContext(boolean includeContext) {
        this.includeContext = includeContext;
    }

    @org.kohsuke.stapler.DataBoundSetter
    public void setAdditionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams != null ? additionalParams : new HashMap<>();
    }

    // Getters
    public String getContent() { return content; }
    public String getAnalysisType() { return analysisType; }
    public String getPrompt() { return prompt; }
    public String getModel() { return model; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public boolean isIncludeContext() { return includeContext; }
    public Map<String, String> getAdditionalParams() { return additionalParams; }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "codexAnalysis";
        }

        @Override
        public String getDisplayName() {
            return "Codex Analysis";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return false;
        }

        @Override
        public boolean isAdvanced() {
            return false;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Set.of(Run.class, TaskListener.class, Launcher.class, EnvVars.class, FilePath.class);
        }
    }
}
