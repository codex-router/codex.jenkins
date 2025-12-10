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
 * Builder for freestyle jobs to perform interactive Codex chat.
 * Enables interactive chat sessions with Codex CLI that are logged to the console.
 */
public class CodexChatBuilder extends Builder {

    private final String initialMessage;
    private final String context;
    private final String model;
    private final int timeoutSeconds;
    private final String additionalParams;

    @DataBoundConstructor
    public CodexChatBuilder(String initialMessage, String context, String model,
                           int timeoutSeconds, String additionalParams) {
        this.initialMessage = initialMessage;
        this.context = context;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 120;
        this.additionalParams = additionalParams;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return performChat(build, launcher, listener, build.getWorkspace());
    }

    public boolean performChat(Run<?, ?> run, Launcher launcher, TaskListener listener, FilePath workspace) throws InterruptedException, IOException {
        try {
            listener.getLogger().println("Starting Codex interactive chat...");

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
                return false;
            }

            // Prepare context
            String chatContext = context;
            if (chatContext == null || chatContext.trim().isEmpty()) {
                // Build default context from build information
                AnalysisContext analysisContext = new AnalysisContext(
                    run, listener, null, "codexChat",
                    "Interactive chat session", environment, null,
                    workspace != null ? workspace.getRemote() : null
                );
                chatContext = analysisContext.buildContextString();
            }

            // Parse additional parameters
            Map<String, String> params = parseAdditionalParams(additionalParams);

            // Use job-level model if specified, otherwise use builder-level model, otherwise use job default
            String effectiveModel = model;
            if ((effectiveModel == null || effectiveModel.trim().isEmpty()) && jobConfig != null) {
                effectiveModel = jobConfig.getEffectiveDefaultModel();
            }
            if (effectiveModel != null && !effectiveModel.trim().isEmpty()) {
                params.put("model", effectiveModel);
            }

            // Use job-level timeout if specified, otherwise use builder-level timeout, otherwise use job default
            int effectiveTimeout = timeoutSeconds;
            if (effectiveTimeout <= 0 && jobConfig != null) {
                effectiveTimeout = jobConfig.getEffectiveTimeoutSeconds();
            }
            if (effectiveTimeout > 0) {
                params.put("timeout", String.valueOf(effectiveTimeout));
            }

            // Execute interactive chat
            executor.executeInteractiveChat(
                initialMessage,
                chatContext,
                params
            );

            return true;

        } catch (Exception e) {
            String error = "Error during Codex chat: " + e.getMessage();
            listener.error(error);
            return false;
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
    public String getInitialMessage() { return initialMessage; }
    public String getContext() { return context; }
    public String getModel() { return model; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public String getAdditionalParams() { return additionalParams; }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true; // Applicable to all project types
        }

        @Override
        public String getDisplayName() {
            return "Codex Interactive Chat";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/codex-analysis/help.html";
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
    }
}
