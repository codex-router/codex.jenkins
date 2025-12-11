package io.jenkins.plugins.codex;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
    private String defaultModel;
    private int timeoutSeconds;
    private boolean enableMcpServers;
    private String litellmApiKey;
    private List<String> selectedMcpServers;
    private boolean useJobConfig;

    @DataBoundConstructor
    public CodexAnalysisJobProperty(String codexCliPath, String codexCliDownloadUrl, String codexCliDownloadUsername, String codexCliDownloadPassword, String configPath,
                                   String defaultModel, int timeoutSeconds, boolean enableMcpServers,
                                   String litellmApiKey, List<String> selectedMcpServers, boolean useJobConfig) {
        this.codexCliPath = codexCliPath;
        this.codexCliDownloadUrl = codexCliDownloadUrl;
        this.codexCliDownloadUsername = codexCliDownloadUsername;
        this.codexCliDownloadPassword = codexCliDownloadPassword;
        this.configPath = configPath;
        this.defaultModel = defaultModel;
        this.timeoutSeconds = timeoutSeconds;
        this.enableMcpServers = enableMcpServers;
        this.litellmApiKey = litellmApiKey;
        this.selectedMcpServers = selectedMcpServers != null ? selectedMcpServers : new ArrayList<>();
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
     * Now uses the same config path as the main configuration
     */
    public String getEffectiveMcpServersPath() {
        return getEffectiveConfigPath();
    }

    /**
     * Get the effective default model (job config only, no global fallback)
     */
    public String getEffectiveDefaultModel() {
        if (useJobConfig && defaultModel != null && !defaultModel.trim().isEmpty()) {
            return defaultModel;
        }
        // No global fallback - return empty string to indicate no default
        return "";
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
     * Get the effective MCP servers enable flag (job config only, no global fallback)
     */
    public boolean getEffectiveEnableMcpServers() {
        if (useJobConfig) {
            return enableMcpServers;
        }
        // No global fallback - return default value
        return false;
    }

    /**
     * Get the effective LiteLLM API key (job config or global fallback)
     */
    public String getEffectiveLitellmApiKey() {
        if (useJobConfig && litellmApiKey != null && !litellmApiKey.trim().isEmpty()) {
            return litellmApiKey;
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getLitellmApiKey() : "";
    }

    /**
     * Get the effective MCP servers list (job config only, no global fallback)
     */
    public List<String> getEffectiveMcpServers() {
        if (useJobConfig && selectedMcpServers != null && !selectedMcpServers.isEmpty()) {
            return selectedMcpServers;
        }
        // No global fallback - return empty list
        return new ArrayList<>();
    }

    /**
     * Download and update Codex CLI from the configured URL with authentication
     * This method is only available for job-level configuration
     */
    public boolean updateCodexCli() {
        if (!useJobConfig) {
            return false; // Only available for job-level configuration
        }

        String downloadUrl = getEffectiveCodexCliDownloadUrl();
        String username = getEffectiveCodexCliDownloadUsername();
        String password = getEffectiveCodexCliDownloadPassword();
        String cliPath = getEffectiveCodexCliPath();

        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            return false; // No download URL configured
        }

        try {
            // Create HTTP connection with authentication if provided
            java.net.URL url = new java.net.URL(downloadUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

            // Set authentication if username and password are provided
            if (username != null && !username.trim().isEmpty() &&
                password != null && !password.trim().isEmpty()) {
                String auth = username + ":" + password;
                String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
            }

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000); // 30 seconds timeout
            connection.setReadTimeout(60000); // 60 seconds timeout

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // Download the file
                try (java.io.InputStream inputStream = connection.getInputStream();
                     java.io.FileOutputStream outputStream = new java.io.FileOutputStream(cliPath)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                // Make the file executable
                java.io.File cliFile = new java.io.File(cliPath);
                cliFile.setExecutable(true);

                return true;
            } else {
                return false; // Download failed
            }
        } catch (Exception e) {
            return false; // Download failed with exception
        }
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
         * Fetch available models from Codex CLI (delegates to global configuration)
         */
        public FormValidation doFetchAvailableModels(@QueryParameter String codexCliPath) {
            try {
                CodexAnalysisPlugin plugin = CodexAnalysisPlugin.get();
                if (plugin != null) {
                    return plugin.doFetchAvailableModels(codexCliPath);
                }
            } catch (IllegalStateException e) {
                // Jenkins instance not available (e.g., in test environment)
                return FormValidation.error("Global Codex Analysis Plugin configuration not found - Jenkins instance not available");
            }
            return FormValidation.error("Global Codex Analysis Plugin configuration not found");
        }

        /**
         * Fetch available MCP servers from Codex CLI (job-level only)
         */
        public FormValidation doFetchAvailableMcpServers(@QueryParameter String codexCliPath, @QueryParameter String configPath) {
            try {
                String effectiveCliPath = codexCliPath != null && !codexCliPath.trim().isEmpty()
                    ? codexCliPath.trim()
                    : "~/.local/bin/codex";

                String effectiveConfigPath = configPath != null && !configPath.trim().isEmpty()
                    ? configPath.trim()
                    : "~/.codex/config.toml";

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
            return parseMcpServersFromConfigFile("~/.codex/config.toml");
        }

        /**
         * Parse MCP servers from configuration file with specified path
         */
        private List<String> parseMcpServersFromConfigFile(String configPath) {
            List<String> serverNames = new ArrayList<>();
            try {
                String effectiveConfigPath = configPath.replaceFirst("^~", System.getProperty("user.home"));
                java.io.File configFile = new java.io.File(effectiveConfigPath);

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
         * Fill the selected MCP servers dropdown list
         * This method is required by Jenkins when using f:select in Jelly files
         */
        public ListBoxModel doFillSelectedMcpServersItems(@QueryParameter String codexCliPath, @QueryParameter String configPath) {
            ListBoxModel model = new ListBoxModel();

            // Try to get MCP servers from Codex CLI if paths are provided
            if (codexCliPath != null && !codexCliPath.trim().isEmpty() &&
                configPath != null && !configPath.trim().isEmpty()) {
                try {
                    String effectiveCliPath = codexCliPath.trim().replaceFirst("^~", System.getProperty("user.home"));
                    String effectiveConfigPath = configPath.trim().replaceFirst("^~", System.getProperty("user.home"));

                    // Try to execute codex CLI to get MCP servers
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
                        List<String> servers = parseMcpServersList(output.toString());
                        if (!servers.isEmpty()) {
                            for (String serverName : servers) {
                                model.add(serverName);
                            }
                            return model;
                        }
                    }
                } catch (Exception e) {
                    // If CLI execution fails, fall through to config file parsing
                }
            }

            // Fall back to parsing from config file
            // Use provided configPath if available, otherwise use default
            String effectiveConfigPath = (configPath != null && !configPath.trim().isEmpty())
                ? configPath.trim().replaceFirst("^~", System.getProperty("user.home"))
                : "~/.codex/config.toml".replaceFirst("^~", System.getProperty("user.home"));

            List<String> servers = parseMcpServersFromConfigFile(effectiveConfigPath);
            for (String serverName : servers) {
                model.add(serverName);
            }

            return model;
        }

        /**
         * Fill the default model dropdown list
         * This method is required by Jenkins when using f:select in Jelly files
         */
        public ListBoxModel doFillDefaultModelItems() {
            ListBoxModel model = new ListBoxModel();

            // Try to get models from global plugin first (has caching)
            try {
                CodexAnalysisPlugin plugin = CodexAnalysisPlugin.get();
                if (plugin != null) {
                    List<String> models = plugin.getModelOptions();
                    for (String modelName : models) {
                        model.add(modelName);
                    }
                    if (!models.isEmpty()) {
                        return model;
                    }
                }
            } catch (Exception e) {
                // If global plugin is not available, fall through to local method
            }

            // Fall back to local method or default models
            String[] availableModels = getAvailableModels();
            if (availableModels.length > 0) {
                for (String modelName : availableModels) {
                    model.add(modelName);
                }
            } else {
                // If no models available, provide default options
                model.add("kimi-k2");
                model.add("gpt-4");
                model.add("gpt-4-turbo");
                model.add("gpt-3.5-turbo");
                model.add("claude-3-opus");
                model.add("claude-3-sonnet");
                model.add("claude-3-haiku");
                model.add("gemini-pro");
                model.add("gemini-pro-vision");
            }

            return model;
        }

        /**
         * Get available models from Codex CLI (job-level only)
         */
        public String[] getAvailableModels() {
            try {
                // Try to fetch models from Codex CLI directly
                String effectiveCliPath = "~/.local/bin/codex".replaceFirst("^~", System.getProperty("user.home"));

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
                        return models.toArray(new String[0]);
                    }
                }
            } catch (Exception e) {
                // If CLI execution fails, return empty array
            }

            // Return empty array if CLI execution fails
            return new String[0];
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
                if (!line.isEmpty() && !line.contains(" ") && (line.contains("-") || line.contains("/") || line.matches("^[a-zA-Z0-9_]+$"))) {
                    models.add(line);
                }
            }

            return models;
        }

        /**
         * Get MCP servers cache status (job-level only)
         */
        public String getMcpServersCacheStatus() {
            return "MCP servers configuration is job-specific. Use the 'Update MCP Servers List' button to fetch available servers.";
        }

        /**
         * Manually update Codex CLI from download URL
         * This method downloads and updates the CLI in the context of the specific job/node
         */
        public FormValidation doUpdateCodexCli(@QueryParameter("codexCliPath") String codexCliPath,
                                             @QueryParameter("codexCliDownloadUrl") String codexCliDownloadUrl,
                                             @QueryParameter("codexCliDownloadUsername") String codexCliDownloadUsername,
                                             @QueryParameter("codexCliDownloadPassword") String codexCliDownloadPassword) {
            try {
                // Get the current job property from the request context
                CodexAnalysisJobProperty jobProperty = getCurrentJobProperty();

                // Use effective values from job configuration
                String effectiveCliPath = jobProperty != null ? jobProperty.getEffectiveCodexCliPath() : "~/.local/bin/codex";
                String effectiveCliDownloadUrl = jobProperty != null ? jobProperty.getEffectiveCodexCliDownloadUrl() : "";
                String effectiveCliDownloadUsername = jobProperty != null ? jobProperty.getEffectiveCodexCliDownloadUsername() : "";
                String effectiveCliDownloadPassword = jobProperty != null ? jobProperty.getEffectiveCodexCliDownloadPassword() : "";

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

                // Check if download URL is configured
                if (effectiveCliDownloadUrl == null || effectiveCliDownloadUrl.trim().isEmpty()) {
                    return FormValidation.error("Codex CLI Download URL is required for updating CLI. Please configure the download URL to use this feature.");
                }

                // Perform the download and update
                boolean success = downloadAndUpdateCli(effectiveCliDownloadUrl, effectiveCliDownloadUsername,
                                                     effectiveCliDownloadPassword, effectiveCliPath);

                if (success) {
                    return FormValidation.ok("Codex CLI successfully updated from: " + effectiveCliDownloadUrl);
                } else {
                    return FormValidation.error("Failed to update Codex CLI from: " + effectiveCliDownloadUrl);
                }

            } catch (Exception e) {
                return FormValidation.error("Error updating Codex CLI: " + e.getMessage());
            }
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

                if (effectiveCliPath == null || effectiveCliPath.trim().isEmpty()) {
                    return FormValidation.error("Codex CLI path is not configured");
                }

                // Expand ~ to home directory for Ubuntu/Unix systems
                effectiveCliPath = effectiveCliPath.replaceFirst("^~", System.getProperty("user.home"));

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
         * Download and update Codex CLI from the specified URL with authentication
         */
        private boolean downloadAndUpdateCli(String downloadUrl, String username, String password, String cliPath) {
            try {
                // Create HTTP connection with authentication if provided
                java.net.URL url = new java.net.URL(downloadUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

                // Set authentication if username and password are provided
                if (username != null && !username.trim().isEmpty() &&
                    password != null && !password.trim().isEmpty()) {
                    String auth = username + ":" + password;
                    String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                    connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                }

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000); // 30 seconds timeout
                connection.setReadTimeout(60000); // 60 seconds timeout

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // Download the file
                    try (java.io.InputStream inputStream = connection.getInputStream();
                         java.io.FileOutputStream outputStream = new java.io.FileOutputStream(cliPath)) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }

                    // Make the file executable
                    java.io.File cliFile = new java.io.File(cliPath);
                    cliFile.setExecutable(true);

                    return true;
                } else {
                    return false; // Download failed
                }
            } catch (Exception e) {
                return false; // Download failed with exception
            }
        }

        /**
         * Helper method to get the current job property from the request context
         */
        private CodexAnalysisJobProperty getCurrentJobProperty() {
            try {
                StaplerRequest req = Stapler.getCurrentRequest();
                if (req != null) {
                    // Navigate to the job from the request
                    // The URL pattern is typically /job/JobName/configure
                    Object job = req.findAncestor(Job.class);
                    if (job instanceof Job) {
                        return ((Job<?, ?>) job).getProperty(CodexAnalysisJobProperty.class);
                    }
                }
            } catch (Exception e) {
                // Ignore and return null
            }
            return null;
        }

        /**
         * Helper method to get the current job from the request context
         */
        private Job<?, ?> getCurrentJob() {
            try {
                StaplerRequest req = Stapler.getCurrentRequest();
                if (req != null) {
                    Object job = req.findAncestor(Job.class);
                    if (job instanceof Job) {
                        return (Job<?, ?>) job;
                    }
                }
            } catch (Exception e) {
                // Ignore and return null
            }
            return null;
        }
    }
}
