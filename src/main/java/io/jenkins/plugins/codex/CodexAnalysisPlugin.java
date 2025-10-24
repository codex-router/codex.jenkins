package io.jenkins.plugins.codex;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Global configuration for the Codex Analysis Plugin.
 * Manages Codex CLI settings and MCP server configurations.
 */
@Extension
@Symbol("codexAnalysis")
public class CodexAnalysisPlugin extends GlobalConfiguration {

    private String codexCliPath = "~/.local/bin/codex";
    private String configPath = "~/.codex/config.toml";
    private String mcpServersPath = "~/.codex/config.toml";
    private String defaultModel = "kimi-k2";
    private int timeoutSeconds = 120;
    private boolean enableMcpServers = true;
    private String litellmApiKey = "sk-1234";
    private List<McpServerConfig> mcpServers = new ArrayList<>();

    @DataBoundConstructor
    public CodexAnalysisPlugin() {
        load();
    }

    public static CodexAnalysisPlugin get() {
        return GlobalConfiguration.all().get(CodexAnalysisPlugin.class);
    }

    @Override
    public String getDisplayName() {
        return "Codex Analysis Plugin";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    // Getters and Setters
    public String getCodexCliPath() {
        return codexCliPath;
    }

    public void setCodexCliPath(String codexCliPath) {
        this.codexCliPath = codexCliPath;
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

    public List<McpServerConfig> getMcpServers() {
        return mcpServers;
    }

    public void setMcpServers(List<McpServerConfig> mcpServers) {
        this.mcpServers = mcpServers;
    }

    /**
     * MCP Server Configuration
     */
    public static class McpServerConfig implements Describable<McpServerConfig> {
        private String name;
        private String type; // "stdio" or "http"
        private String command;
        private String args;
        private String url;
        private String bearerTokenEnvVar;
        private int startupTimeoutSec = 10;
        private int toolTimeoutSec = 60;
        private boolean enabled = true;

        @DataBoundConstructor
        public McpServerConfig() {}

        @Override
        public Descriptor<McpServerConfig> getDescriptor() {
            return Jenkins.get().getDescriptorOrDie(McpServerConfig.class);
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<McpServerConfig> {
            @Override
            public String getDisplayName() {
                return "MCP Server Configuration";
            }
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }

        public String getArgs() { return args; }
        public void setArgs(String args) { this.args = args; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getBearerTokenEnvVar() { return bearerTokenEnvVar; }
        public void setBearerTokenEnvVar(String bearerTokenEnvVar) { this.bearerTokenEnvVar = bearerTokenEnvVar; }

        public int getStartupTimeoutSec() { return startupTimeoutSec; }
        public void setStartupTimeoutSec(int startupTimeoutSec) { this.startupTimeoutSec = startupTimeoutSec; }

        public int getToolTimeoutSec() { return toolTimeoutSec; }
        public void setToolTimeoutSec(int toolTimeoutSec) { this.toolTimeoutSec = toolTimeoutSec; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * Validation methods for configuration fields
     */

    /**
     * Validate Codex CLI path
     */
    public FormValidation doCheckCodexCliPath(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.warning("Codex CLI path is empty, will use 'codex'");
        }
        return FormValidation.ok();
    }

    /**
     * Validate config path
     */
    public FormValidation doCheckConfigPath(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.warning("Config path is empty, will use '~/.codex/config.toml'");
        }
        return FormValidation.ok();
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
     * Validate default model
     */
    public FormValidation doCheckDefaultModel(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.warning("Default model is empty, will use 'kimi-k2'");
        }
        return FormValidation.ok();
    }

    /**
     * Validate LiteLLM API key
     */
    public FormValidation doCheckLitellmApiKey(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.warning("LiteLLM API key is empty, will use default 'sk-1234'");
        }
        if (value.length() < 10) {
            return FormValidation.warning("API key seems too short");
        }
        return FormValidation.ok();
    }


    /**
     * Get available model options for the select dropdown
     */
    public List<String> getModelOptions() {
        List<String> models = new ArrayList<>();
        models.add("kimi-k2");
        models.add("gpt-4");
        models.add("gpt-4-turbo");
        models.add("gpt-3.5-turbo");
        models.add("claude-3-opus");
        models.add("claude-3-sonnet");
        models.add("claude-3-haiku");
        models.add("gemini-pro");
        models.add("gemini-pro-vision");
        return models;
    }

    /**
     * Fill default model items for the dropdown
     */
    public ListBoxModel doFillDefaultModelItems() {
        ListBoxModel items = new ListBoxModel();
        for (String model : getModelOptions()) {
            items.add(model, model);
        }
        return items;
    }
}
