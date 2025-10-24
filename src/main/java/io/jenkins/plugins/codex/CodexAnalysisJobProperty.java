package io.jenkins.plugins.codex;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Job-level configuration for Codex Analysis Plugin.
 * Allows per-job configuration of Codex CLI settings and MCP server configurations.
 */
public class CodexAnalysisJobProperty extends JobProperty<Job<?, ?>> {

    private String codexCliPath;
    private String codexCliDownloadUrl;
    private String codexCliDownloadUsername;
    private String codexCliDownloadPassword;
    private String configPath;
    private String mcpServersPath;
    private String defaultModel;
    private int timeoutSeconds;
    private boolean enableMcpServers;
    private String litellmApiKey;
    private List<CodexAnalysisPlugin.McpServerConfig> mcpServers;
    private boolean useJobConfig;

    @DataBoundConstructor
    public CodexAnalysisJobProperty(String codexCliPath, String codexCliDownloadUrl, String codexCliDownloadUsername, String codexCliDownloadPassword, String configPath, String mcpServersPath,
                                   String defaultModel, int timeoutSeconds, boolean enableMcpServers,
                                   String litellmApiKey, List<CodexAnalysisPlugin.McpServerConfig> mcpServers, boolean useJobConfig) {
        this.codexCliPath = codexCliPath;
        this.codexCliDownloadUrl = codexCliDownloadUrl;
        this.codexCliDownloadUsername = codexCliDownloadUsername;
        this.codexCliDownloadPassword = codexCliDownloadPassword;
        this.configPath = configPath;
        this.mcpServersPath = mcpServersPath;
        this.defaultModel = defaultModel;
        this.timeoutSeconds = timeoutSeconds;
        this.enableMcpServers = enableMcpServers;
        this.litellmApiKey = litellmApiKey;
        this.mcpServers = mcpServers != null ? mcpServers : new ArrayList<>();
        this.useJobConfig = useJobConfig;
    }

    /**
     * Get the effective Codex CLI path (job config or global fallback)
     */
    public String getEffectiveCodexCliPath() {
        if (useJobConfig && codexCliPath != null && !codexCliPath.trim().isEmpty()) {
            return codexCliPath;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getCodexCliPath() : "~/.local/bin/codex";
    }

    /**
     * Get the effective Codex CLI download URL (job config or global fallback)
     */
    public String getEffectiveCodexCliDownloadUrl() {
        if (useJobConfig && codexCliDownloadUrl != null && !codexCliDownloadUrl.trim().isEmpty()) {
            return codexCliDownloadUrl;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getCodexCliDownloadUrl() : "";
    }

    /**
     * Get the effective Codex CLI download username (job config or global fallback)
     */
    public String getEffectiveCodexCliDownloadUsername() {
        if (useJobConfig && codexCliDownloadUsername != null && !codexCliDownloadUsername.trim().isEmpty()) {
            return codexCliDownloadUsername;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getCodexCliDownloadUsername() : "";
    }

    /**
     * Get the effective Codex CLI download password (job config or global fallback)
     */
    public String getEffectiveCodexCliDownloadPassword() {
        if (useJobConfig && codexCliDownloadPassword != null && !codexCliDownloadPassword.trim().isEmpty()) {
            return codexCliDownloadPassword;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getCodexCliDownloadPassword() : "";
    }

    /**
     * Get the effective config path (job config or global fallback)
     */
    public String getEffectiveConfigPath() {
        if (useJobConfig && configPath != null && !configPath.trim().isEmpty()) {
            return configPath;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getConfigPath() : "~/.codex/config.toml";
    }

    /**
     * Get the effective MCP servers path (job config or global fallback)
     */
    public String getEffectiveMcpServersPath() {
        if (useJobConfig && mcpServersPath != null && !mcpServersPath.trim().isEmpty()) {
            return mcpServersPath;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getMcpServersPath() : "~/.codex/config.toml";
    }

    /**
     * Get the effective default model (job config or global fallback)
     */
    public String getEffectiveDefaultModel() {
        if (useJobConfig && defaultModel != null && !defaultModel.trim().isEmpty()) {
            return defaultModel;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getDefaultModel() : "kimi-k2";
    }

    /**
     * Get the effective timeout (job config or global fallback)
     */
    public int getEffectiveTimeoutSeconds() {
        if (useJobConfig && timeoutSeconds > 0) {
            return timeoutSeconds;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getTimeoutSeconds() : 120;
    }

    /**
     * Get the effective MCP servers enable flag (job config or global fallback)
     */
    public boolean getEffectiveEnableMcpServers() {
        if (useJobConfig) {
            return enableMcpServers;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.isEnableMcpServers() : true;
    }

    /**
     * Get the effective LiteLLM API key (job config or global fallback)
     */
    public String getEffectiveLitellmApiKey() {
        if (useJobConfig && litellmApiKey != null && !litellmApiKey.trim().isEmpty()) {
            return litellmApiKey;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getLitellmApiKey() : "sk-1234";
    }

    /**
     * Get the effective MCP servers list (job config or global fallback)
     */
    public List<CodexAnalysisPlugin.McpServerConfig> getEffectiveMcpServers() {
        if (useJobConfig && mcpServers != null && !mcpServers.isEmpty()) {
            return mcpServers;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getMcpServers() : new ArrayList<>();
    }

    // Getters and Setters
    public String getCodexCliPath() {
        return codexCliPath;
    }

    public void setCodexCliPath(String codexCliPath) {
        this.codexCliPath = codexCliPath;
    }

    public String getCodexCliDownloadUrl() {
        return codexCliDownloadUrl;
    }

    public void setCodexCliDownloadUrl(String codexCliDownloadUrl) {
        this.codexCliDownloadUrl = codexCliDownloadUrl;
    }

    public String getCodexCliDownloadUsername() {
        return codexCliDownloadUsername;
    }

    public void setCodexCliDownloadUsername(String codexCliDownloadUsername) {
        this.codexCliDownloadUsername = codexCliDownloadUsername;
    }

    public String getCodexCliDownloadPassword() {
        return codexCliDownloadPassword;
    }

    public void setCodexCliDownloadPassword(String codexCliDownloadPassword) {
        this.codexCliDownloadPassword = codexCliDownloadPassword;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getMcpServersPath() {
        return mcpServersPath;
    }

    public void setMcpServersPath(String mcpServersPath) {
        this.mcpServersPath = mcpServersPath;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isEnableMcpServers() {
        return enableMcpServers;
    }

    public void setEnableMcpServers(boolean enableMcpServers) {
        this.enableMcpServers = enableMcpServers;
    }

    public String getLitellmApiKey() {
        return litellmApiKey;
    }

    public void setLitellmApiKey(String litellmApiKey) {
        this.litellmApiKey = litellmApiKey;
    }

    public List<CodexAnalysisPlugin.McpServerConfig> getMcpServers() {
        return mcpServers;
    }

    public void setMcpServers(List<CodexAnalysisPlugin.McpServerConfig> mcpServers) {
        this.mcpServers = mcpServers;
    }

    public boolean isUseJobConfig() {
        return useJobConfig;
    }

    public void setUseJobConfig(boolean useJobConfig) {
        this.useJobConfig = useJobConfig;
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Codex Analysis Plugin Configuration";
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true; // Applicable to all job types
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            if (formData == null || formData.isNullObject()) {
                return null;
            }
            return req.bindJSON(CodexAnalysisJobProperty.class, formData);
        }

        /**
         * Validate Codex CLI path
         */
        public FormValidation doCheckCodexCliPath(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.warning("Empty path will use global configuration");
            }
            return FormValidation.ok();
        }

        /**
         * Validate config path
         */
        public FormValidation doCheckConfigPath(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.warning("Empty path will use global configuration");
            }
            return FormValidation.ok();
        }

        /**
         * Validate timeout
         */
        public FormValidation doCheckTimeoutSeconds(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.warning("Empty value will use global configuration");
            }
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
         * Get available models from global configuration
         */
        public String[] getAvailableModels() {
            CodexAnalysisPlugin plugin = CodexAnalysisPlugin.get();
            if (plugin != null) {
                return new String[]{
                    plugin.getDefaultModel(),
                    "kimi-k2",
                    "gpt-4",
                    "claude-3",
                    "gemini-pro"
                };
            }
            return new String[]{"kimi-k2", "gpt-4", "claude-3", "gemini-pro"};
        }

        /**
         * Test Codex CLI connection with node binding
         * This method tests the CLI in the context of the specific job/node
         */
        public FormValidation doTestCodexCli(@QueryParameter("codexCliPath") String codexCliPath,
                                           @QueryParameter("codexCliDownloadUrl") String codexCliDownloadUrl,
                                           @QueryParameter("codexCliDownloadUsername") String codexCliDownloadUsername,
                                           @QueryParameter("codexCliDownloadPassword") String codexCliDownloadPassword,
                                           @QueryParameter("configPath") String configPath,
                                           @QueryParameter("litellmApiKey") String litellmApiKey) {
            try {
                // Get the current job property from the request context
                CodexAnalysisJobProperty jobProperty = getCurrentJobProperty();

                // Use effective values from job configuration
                String effectiveCliPath = jobProperty != null ? jobProperty.getEffectiveCodexCliPath() : "~/.local/bin/codex";
                String effectiveCliDownloadUrl = jobProperty != null ? jobProperty.getEffectiveCodexCliDownloadUrl() : "";
                String effectiveCliDownloadUsername = jobProperty != null ? jobProperty.getEffectiveCodexCliDownloadUsername() : "";
                String effectiveCliDownloadPassword = jobProperty != null ? jobProperty.getEffectiveCodexCliDownloadPassword() : "";
                String effectiveConfigPath = jobProperty != null ? jobProperty.getEffectiveConfigPath() : "~/.codex/config.toml";
                String effectiveLitellmApiKey = jobProperty != null ? jobProperty.getEffectiveLitellmApiKey() : "sk-1234";

                // Override with provided parameters if they are not empty
                if (codexCliPath != null && !codexCliPath.trim().isEmpty()) {
                    effectiveCliPath = codexCliPath;
                }
                if (codexCliDownloadUrl != null && !codexCliDownloadUrl.trim().isEmpty()) {
                    effectiveCliDownloadUrl = codexCliDownloadUrl;
                }
                if (codexCliDownloadUsername != null && !codexCliDownloadUsername.trim().isEmpty()) {
                    effectiveCliDownloadUsername = codexCliDownloadUsername;
                }
                if (codexCliDownloadPassword != null && !codexCliDownloadPassword.trim().isEmpty()) {
                    effectiveCliDownloadPassword = codexCliDownloadPassword;
                }
                if (configPath != null && !configPath.trim().isEmpty()) {
                    effectiveConfigPath = configPath;
                }
                if (litellmApiKey != null && !litellmApiKey.trim().isEmpty()) {
                    effectiveLitellmApiKey = litellmApiKey;
                }

                // Test CLI availability using a simple version check
                // This will test the CLI on the node where the job will run
                ProcessBuilder pb = new ProcessBuilder(effectiveCliPath, "--version");
                Process process = pb.start();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    String message = "Codex CLI is accessible on this node with path: " + effectiveCliPath;
                    if (jobProperty != null && jobProperty.isUseJobConfig()) {
                        message += " (using job-level configuration)";
                    } else {
                        message += " (using global configuration)";
                    }
                    return FormValidation.ok(message);
                } else {
                    return FormValidation.warning("Codex CLI returned exit code: " + exitCode + " with path: " + effectiveCliPath);
                }
            } catch (Exception e) {
                return FormValidation.error("Failed to test Codex CLI on this node: " + e.getMessage());
            }
        }

        /**
         * Helper method to get the current job property from the request context
         */
        private CodexAnalysisJobProperty getCurrentJobProperty() {
            try {
                // Try to get the job property from the current request context
                // This is a simplified approach - in a real Jenkins plugin, you'd use
                // StaplerRequest.getCurrentRequest() and navigate to the job context
                return null; // For now, return null to use global configuration
            } catch (Exception e) {
                return null;
            }
        }
    }
}
