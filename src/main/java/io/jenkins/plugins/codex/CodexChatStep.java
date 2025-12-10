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
 * Pipeline step for interactive Codex chat.
 * Enables interactive chat sessions with Codex CLI that are logged to the console.
 */
public class CodexChatStep extends Step {

    private String initialMessage;
    private String context;
    private String model;
    private int timeoutSeconds = 120;
    private Map<String, String> additionalParams = new HashMap<>();

    @DataBoundConstructor
    public CodexChatStep() {}

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(this, context);
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<Void> {

        private final CodexChatStep step;

        public Execution(CodexChatStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
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
                return null;
            }

            // Prepare context if requested
            String chatContext = step.context;
            if (chatContext == null || chatContext.trim().isEmpty()) {
                // Build default context from build information
                AnalysisContext analysisContext = new AnalysisContext(
                    run, listener, null, "codexChat",
                    "Interactive chat session", environment, null,
                    workspace != null ? workspace.getRemote() : null
                );
                chatContext = analysisContext.buildContextString();
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

            // Execute interactive chat
            try {
                executor.executeInteractiveChat(
                    step.initialMessage,
                    chatContext,
                    params
                );
            } catch (Exception e) {
                listener.error("Error during Codex chat: " + e.getMessage());
                throw e;
            }

            return null;
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @org.kohsuke.stapler.DataBoundSetter
    public void setInitialMessage(String initialMessage) {
        this.initialMessage = initialMessage;
    }

    @org.kohsuke.stapler.DataBoundSetter
    public void setContext(String context) {
        this.context = context;
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
    public void setAdditionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams != null ? additionalParams : new HashMap<>();
    }

    // Getters
    public String getInitialMessage() { return initialMessage; }
    public String getContext() { return context; }
    public String getModel() { return model; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public Map<String, String> getAdditionalParams() { return additionalParams; }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "codexChat";
        }

        @Override
        public String getDisplayName() {
            return "Codex Interactive Chat";
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
