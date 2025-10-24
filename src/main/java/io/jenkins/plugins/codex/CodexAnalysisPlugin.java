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
    private String codexCliDownloadUrl = "";
    private String codexCliDownloadUsername = "";
    private String codexCliDownloadPassword = "";
    private String configPath = "~/.codex/config.toml";
    private String defaultModel = "kimi-k2";
    private int timeoutSeconds = 120;
    private boolean enableMcpServers = true;
    private String litellmApiKey = "sk-1234";
    private List<String> selectedMcpServers = new ArrayList<>();

    // Cached model list from Codex CLI
    private List<String> cachedModels = new ArrayList<>();
    private long modelCacheTimestamp = 0;
    private static final long MODEL_CACHE_DURATION = 300000; // 5 minutes in milliseconds

    // Cached MCP servers list from Codex CLI
    private List<String> cachedMcpServers = new ArrayList<>();
    private long mcpServersCacheTimestamp = 0;
    private static final long MCP_SERVERS_CACHE_DURATION = 300000; // 5 minutes in milliseconds

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

    public List<String> getSelectedMcpServers() {
        return selectedMcpServers;
    }

    public void setSelectedMcpServers(List<String> selectedMcpServers) {
        this.selectedMcpServers = selectedMcpServers;
    }

    /**
     * Fetch available MCP servers from Codex CLI
     */
    public FormValidation doFetchAvailableMcpServers(@QueryParameter String codexCliPath, @QueryParameter String configPath) {
        try {
            String effectiveCliPath = codexCliPath != null && !codexCliPath.trim().isEmpty()
                ? codexCliPath.trim()
                : this.codexCliPath;

            String effectiveConfigPath = configPath != null && !configPath.trim().isEmpty()
                ? configPath.trim()
                : this.configPath;

            // Expand ~ to home directory
            effectiveCliPath = effectiveCliPath.replaceFirst("^~", System.getProperty("user.home"));
            effectiveConfigPath = effectiveConfigPath.replaceFirst("^~", System.getProperty("user.home"));

            // Execute codex CLI to get MCP servers information
            ProcessBuilder pb = new ProcessBuilder(effectiveCliPath, "mcp", "list", "--config", effectiveConfigPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // Parse the output to extract MCP server names
                List<String> servers = parseMcpServersList(output.toString());
                if (!servers.isEmpty()) {
                    // Update cache
                    this.cachedMcpServers = servers;
                    this.mcpServersCacheTimestamp = System.currentTimeMillis();
                    save(); // Save the updated cache
                    return FormValidation.ok("Successfully fetched " + servers.size() + " MCP servers from Codex CLI");
                } else {
                    return FormValidation.warning("No MCP servers found in Codex CLI output");
                }
            } else {
                return FormValidation.error("Codex CLI returned exit code: " + exitCode + ". Output: " + output.toString());
            }
        } catch (Exception e) {
            return FormValidation.error("Failed to fetch MCP servers from Codex CLI: " + e.getMessage());
        }
    }

    /**
     * Parse MCP servers list from Codex CLI output
     */
    private List<String> parseMcpServersList(String output) {
        List<String> servers = new ArrayList<>();
        String[] lines = output.split("\n");

        for (String line : lines) {
            line = line.trim();
            // Skip empty lines and headers
            if (line.isEmpty() || line.startsWith("Available MCP servers:") || line.startsWith("Server") || line.startsWith("-")) {
                continue;
            }
            // Extract server name (assuming format like "server-name" or "provider/server-name")
            if (!line.isEmpty() && !line.contains(" ") && (line.contains("-") || line.contains("/") || line.matches("^[a-zA-Z0-9_]+$"))) {
                servers.add(line);
            }
        }

        // If no servers found, try to parse from config file as fallback
        if (servers.isEmpty()) {
            servers.addAll(parseMcpServersFromConfigFile());
        }

        return servers;
    }

    /**
     * Parse MCP servers from configuration file (fallback method)
     */
    private List<String> parseMcpServersFromConfigFile() {
        List<String> serverNames = new ArrayList<>();
        try {
            String configPath = this.configPath.replaceFirst("^~", System.getProperty("user.home"));
            java.io.File configFile = new java.io.File(configPath);

            if (configFile.exists()) {
                // Read the TOML file and extract MCP server names
                String content = new String(java.nio.file.Files.readAllBytes(configFile.toPath()));

                // Simple parsing to extract server names from TOML format
                // Look for patterns like [mcp.servers."server-name"]
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "\\[mcp\\.servers\\.\"([^\"]+)\"\\]|\\[mcp\\.servers\\.([^\\]]+)\\]"
                );
                java.util.regex.Matcher matcher = pattern.matcher(content);

                while (matcher.find()) {
                    String serverName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                    if (serverName != null && !serverName.trim().isEmpty()) {
                        serverNames.add(serverName.trim());
                    }
                }
            }
        } catch (Exception e) {
            // If we can't read the config file, return empty list
            // This is not critical as the user can still configure manually
        }

        // If no servers found in config file, provide some common examples
        if (serverNames.isEmpty()) {
            serverNames.add("filesystem");
            serverNames.add("github");
            serverNames.add("database");
            serverNames.add("web-search");
        }

        return serverNames;
    }

    /**
     * Get available MCP server names from the configuration file
     */
    public List<String> getAvailableMcpServers() {
        // Check if cache is valid
        long currentTime = System.currentTimeMillis();
        if (cachedMcpServers.isEmpty() || (currentTime - mcpServersCacheTimestamp) > MCP_SERVERS_CACHE_DURATION) {
            // Return servers from config file if cache is empty or expired
            return parseMcpServersFromConfigFile();
        }
        return new ArrayList<>(cachedMcpServers);
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
     * Validate Codex CLI download URL
     */
    public FormValidation doCheckCodexCliDownloadUrl(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.warning("Codex CLI download URL is optional. Leave empty if CLI is already installed or will be installed manually.");
        }
        // Basic URL validation
        String trimmedValue = value.trim();
        if (!trimmedValue.startsWith("http://") && !trimmedValue.startsWith("https://")) {
            return FormValidation.warning("Download URL should start with http:// or https://");
        }
        return FormValidation.ok();
    }

    /**
     * Validate Codex CLI download username
     */
    public FormValidation doCheckCodexCliDownloadUsername(@QueryParameter String value) {
        if (value != null && !value.trim().isEmpty()) {
            if (value.trim().length() < 2) {
                return FormValidation.warning("Username seems too short");
            }
        }
        return FormValidation.ok();
    }

    /**
     * Validate Codex CLI download password
     */
    public FormValidation doCheckCodexCliDownloadPassword(@QueryParameter String value) {
        if (value != null && !value.trim().isEmpty()) {
            if (value.trim().length() < 4) {
                return FormValidation.warning("Password seems too short");
            }
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

        // Check if the selected model is in the available models list
        List<String> availableModels = getModelOptions();
        if (!availableModels.contains(value.trim())) {
            return FormValidation.warning("Selected model '" + value + "' is not in the current model list. Click 'Update Model List' to refresh available models.");
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
     * Fetch available models from Codex CLI
     */
    public FormValidation doFetchAvailableModels(@QueryParameter String codexCliPath) {
        try {
            String effectiveCliPath = codexCliPath != null && !codexCliPath.trim().isEmpty()
                ? codexCliPath.trim()
                : this.codexCliPath;

            // Expand ~ to home directory
            effectiveCliPath = effectiveCliPath.replaceFirst("^~", System.getProperty("user.home"));

            // Execute codex CLI to get available models
            ProcessBuilder pb = new ProcessBuilder(effectiveCliPath, "models", "list");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // Parse the output to extract model names
                List<String> models = parseModelList(output.toString());
                if (!models.isEmpty()) {
                    // Update cache
                    this.cachedModels = models;
                    this.modelCacheTimestamp = System.currentTimeMillis();
                    save(); // Save the updated cache
                    return FormValidation.ok("Successfully fetched " + models.size() + " models from Codex CLI");
                } else {
                    return FormValidation.warning("No models found in Codex CLI output");
                }
            } else {
                return FormValidation.error("Codex CLI returned exit code: " + exitCode + ". Output: " + output.toString());
            }
        } catch (Exception e) {
            return FormValidation.error("Failed to fetch models from Codex CLI: " + e.getMessage());
        }
    }

    /**
     * Parse model list from Codex CLI output
     */
    private List<String> parseModelList(String output) {
        List<String> models = new ArrayList<>();
        String[] lines = output.split("\n");

        for (String line : lines) {
            line = line.trim();
            // Skip empty lines and headers
            if (line.isEmpty() || line.startsWith("Available models:") || line.startsWith("Model") || line.startsWith("-")) {
                continue;
            }
            // Extract model name (assuming format like "model-name" or "provider/model-name")
            if (!line.isEmpty() && !line.contains(" ") && (line.contains("-") || line.contains("/"))) {
                models.add(line);
            }
        }

        // If no models found, return default list
        if (models.isEmpty()) {
            models.addAll(getDefaultModelOptions());
        }

        return models;
    }

    /**
     * Get default model options (fallback)
     */
    private List<String> getDefaultModelOptions() {
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
     * Get available model options for the select dropdown
     */
    public List<String> getModelOptions() {
        // Check if cache is valid
        long currentTime = System.currentTimeMillis();
        if (cachedModels.isEmpty() || (currentTime - modelCacheTimestamp) > MODEL_CACHE_DURATION) {
            // Return default models if cache is empty or expired
            return getDefaultModelOptions();
        }
        return new ArrayList<>(cachedModels);
    }

    /**
     * Fill default model items for the dropdown
     */
    public ListBoxModel doFillDefaultModelItems() {
        ListBoxModel items = new ListBoxModel();
        List<String> models = getModelOptions();

        // Add current default model first if it's not in the list
        if (defaultModel != null && !defaultModel.trim().isEmpty() && !models.contains(defaultModel)) {
            items.add(defaultModel, defaultModel);
        }

        // Add all available models
        for (String model : models) {
            items.add(model, model);
        }

        return items;
    }

    /**
     * Get model cache status information
     */
    public String getModelCacheStatus() {
        if (cachedModels.isEmpty()) {
            return "No models cached. Click 'Update Model List' to fetch from Codex CLI.";
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceUpdate = currentTime - modelCacheTimestamp;
        long minutesSinceUpdate = timeSinceUpdate / 60000;

        if (timeSinceUpdate > MODEL_CACHE_DURATION) {
            return "Model cache expired (" + minutesSinceUpdate + " minutes old). Click 'Update Model List' to refresh.";
        } else {
            return "Model cache is current (" + minutesSinceUpdate + " minutes old, " + cachedModels.size() + " models cached).";
        }
    }

    /**
     * Get MCP servers cache status information
     */
    public String getMcpServersCacheStatus() {
        if (cachedMcpServers.isEmpty()) {
            return "No MCP servers cached. Click 'Update MCP Servers List' to fetch from Codex CLI.";
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceUpdate = currentTime - mcpServersCacheTimestamp;
        long minutesSinceUpdate = timeSinceUpdate / 60000;

        if (timeSinceUpdate > MCP_SERVERS_CACHE_DURATION) {
            return "MCP servers cache expired (" + minutesSinceUpdate + " minutes old). Click 'Update MCP Servers List' to refresh.";
        } else {
            return "MCP servers cache is current (" + minutesSinceUpdate + " minutes old, " + cachedMcpServers.size() + " servers cached).";
        }
    }
}
