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
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;
import hudson.model.AbstractProject;
import hudson.Util;

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
         * Fetch available models from Codex CLI with node binding
         * This method fetches models from the CLI on the node where the job will run
         */
        @POST
        public FormValidation doFetchAvailableModels(@AncestorInPath Job<?, ?> job, @QueryParameter String codexCliPath) {
            try {
                Jenkins j = Jenkins.get();
                Node target = null;
                String path = Util.fixEmptyAndTrim(codexCliPath);

                // Only try labeled agents for freestyle projects
                if (job != null && job instanceof AbstractProject) {
                    AbstractProject<?, ?> project = (AbstractProject<?, ?>) job;
                    Label assigned = project.getAssignedLabel();
                    if (assigned != null) {
                        for (Node n : assigned.getNodes()) {
                            if (n != null && n.toComputer() != null && n.toComputer().isOnline()) {
                                target = n;
                                break;
                            }
                        }
                        if (target == null) {
                            return FormValidation.error("No online agents match the job's label: '" + assigned.getExpression() + "'.");
                        }
                    }
                }

                // For pipeline jobs or freestyle jobs without labels, use controller
                if (target == null) {
                    target = j; // fallback to controller
                }

                // If job-level path is not provided, use global configuration
                if (path == null) {
                    CodexAnalysisJobProperty jobProperty = job != null ? job.getProperty(CodexAnalysisJobProperty.class) : null;
                    if (jobProperty != null) {
                        path = Util.fixEmptyAndTrim(jobProperty.getEffectiveCodexCliPath());
                    }
                    if (path == null) {
                        CodexAnalysisPlugin cfg = CodexAnalysisPlugin.get();
                        String globalPath = (cfg != null) ? Util.fixEmptyAndTrim(cfg.getCodexCliPath()) : null;
                        path = (globalPath != null) ? globalPath : "~/.local/bin/codex";
                    }
                }

                FilePath root = target.getRootPath();
                if (root == null) {
                    return FormValidation.error("Unable to access workspace on target node.");
                }

                String output = root.act(new ModelsListCallable(path));
                List<String> models = parseModelList(output);
                if (!models.isEmpty()) {
                    String where = (target == j) ? "controller" : target.getNodeName();
                    return FormValidation.ok("Successfully fetched " + models.size() + " models from Codex CLI on " + where);
                } else {
                    return FormValidation.warning("No models found in Codex CLI output");
                }
            } catch (Exception e) {
                return FormValidation.error("Failed to fetch models from Codex CLI: " + e.getMessage());
            }
        }

        /**
         * Fetch available MCP servers from Codex CLI with node binding
         * This method fetches MCP servers from the CLI on the node where the job will run
         */
        @POST
        public FormValidation doFetchAvailableMcpServers(@AncestorInPath Job<?, ?> job, @QueryParameter String codexCliPath, @QueryParameter String configPath) {
            try {
                Jenkins j = Jenkins.get();
                Node target = null;
                String path = Util.fixEmptyAndTrim(codexCliPath);
                String cfgPath = Util.fixEmptyAndTrim(configPath);

                // Only try labeled agents for freestyle projects
                if (job != null && job instanceof AbstractProject) {
                    AbstractProject<?, ?> project = (AbstractProject<?, ?>) job;
                    Label assigned = project.getAssignedLabel();
                    if (assigned != null) {
                        for (Node n : assigned.getNodes()) {
                            if (n != null && n.toComputer() != null && n.toComputer().isOnline()) {
                                target = n;
                                break;
                            }
                        }
                        if (target == null) {
                            return FormValidation.error("No online agents match the job's label: '" + assigned.getExpression() + "'.");
                        }
                    }
                }

                // For pipeline jobs or freestyle jobs without labels, use controller
                if (target == null) {
                    target = j; // fallback to controller
                }

                // If job-level path is not provided, use global configuration
                if (path == null) {
                    CodexAnalysisJobProperty jobProperty = job != null ? job.getProperty(CodexAnalysisJobProperty.class) : null;
                    if (jobProperty != null) {
                        path = Util.fixEmptyAndTrim(jobProperty.getEffectiveCodexCliPath());
                    }
                    if (path == null) {
                        CodexAnalysisPlugin cfg = CodexAnalysisPlugin.get();
                        String globalPath = (cfg != null) ? Util.fixEmptyAndTrim(cfg.getCodexCliPath()) : null;
                        path = (globalPath != null) ? globalPath : "~/.local/bin/codex";
                    }
                }

                // If config path is not provided, use job or global configuration
                if (cfgPath == null) {
                    CodexAnalysisJobProperty jobProperty = job != null ? job.getProperty(CodexAnalysisJobProperty.class) : null;
                    if (jobProperty != null) {
                        cfgPath = Util.fixEmptyAndTrim(jobProperty.getEffectiveConfigPath());
                    }
                    if (cfgPath == null) {
                        CodexAnalysisPlugin cfg = CodexAnalysisPlugin.get();
                        String globalConfigPath = (cfg != null) ? Util.fixEmptyAndTrim(cfg.getConfigPath()) : null;
                        cfgPath = (globalConfigPath != null) ? globalConfigPath : "~/.codex/config.toml";
                    }
                }

                FilePath root = target.getRootPath();
                if (root == null) {
                    return FormValidation.error("Unable to access workspace on target node.");
                }

                String output = root.act(new McpServersListCallable(path, cfgPath));
                List<String> servers = parseMcpServersList(output);
                if (!servers.isEmpty()) {
                    String where = (target == j) ? "controller" : target.getNodeName();
                    return FormValidation.ok("Successfully fetched " + servers.size() + " MCP servers from Codex CLI on " + where);
                } else {
                    return FormValidation.warning("No MCP servers found in Codex CLI output");
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

            // Return empty list if no servers found (no default examples)
            return serverNames;
        }

        /**
         * Fill the selected MCP servers dropdown list
         * This method is required by Jenkins when using f:select in Jelly files
         */
        public ListBoxModel doFillSelectedMcpServersItems(@QueryParameter String codexCliPath, @QueryParameter String configPath) {
            ListBoxModel model = new ListBoxModel();

            // Add empty option as default
            model.add("");

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

            // Add empty option as default
            model.add("");

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

            // Fall back to local method
            String[] availableModels = getAvailableModels();
            if (availableModels.length > 0) {
                for (String modelName : availableModels) {
                    model.add(modelName);
                }
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
         * Manually update Codex CLI from download URL with node binding
         * This method downloads and updates the CLI on the node where the job will run
         */
        @POST
        public FormValidation doUpdateCodexCli(@AncestorInPath Job<?, ?> job, @QueryParameter("codexCliPath") String codexCliPath,
                                             @QueryParameter("codexCliDownloadUrl") String codexCliDownloadUrl,
                                             @QueryParameter("codexCliDownloadUsername") String codexCliDownloadUsername,
                                             @QueryParameter("codexCliDownloadPassword") String codexCliDownloadPassword) {
            try {
                Jenkins j = Jenkins.get();
                Node target = null;

                // Get the current job property from the job parameter
                CodexAnalysisJobProperty jobProperty = job != null ? job.getProperty(CodexAnalysisJobProperty.class) : null;

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

                // Only try labeled agents for freestyle projects
                if (job != null && job instanceof AbstractProject) {
                    AbstractProject<?, ?> project = (AbstractProject<?, ?>) job;
                    Label assigned = project.getAssignedLabel();
                    if (assigned != null) {
                        for (Node n : assigned.getNodes()) {
                            if (n != null && n.toComputer() != null && n.toComputer().isOnline()) {
                                target = n;
                                break;
                            }
                        }
                        if (target == null) {
                            return FormValidation.error("No online agents match the job's label: '" + assigned.getExpression() + "'.");
                        }
                    }
                }

                // For pipeline jobs or freestyle jobs without labels, use controller
                if (target == null) {
                    target = j; // fallback to controller
                }

                // If job-level path is not provided, use global configuration
                String path = Util.fixEmptyAndTrim(effectiveCliPath);
                if (path == null) {
                    if (jobProperty != null) {
                        path = Util.fixEmptyAndTrim(jobProperty.getEffectiveCodexCliPath());
                    }
                    if (path == null) {
                        CodexAnalysisPlugin cfg = CodexAnalysisPlugin.get();
                        String globalPath = (cfg != null) ? Util.fixEmptyAndTrim(cfg.getCodexCliPath()) : null;
                        path = (globalPath != null) ? globalPath : "~/.local/bin/codex";
                    }
                }

                FilePath root = target.getRootPath();
                if (root == null) {
                    return FormValidation.error("Unable to access workspace on target node.");
                }

                // Perform the download and update on the target node
                boolean success = root.act(new DownloadCliCallable(effectiveCliDownloadUrl, effectiveCliDownloadUsername,
                                                                   effectiveCliDownloadPassword, path));

                if (success) {
                    String where = (target == j) ? "controller" : target.getNodeName();
                    return FormValidation.ok("Codex CLI successfully updated from: " + effectiveCliDownloadUrl + " on " + where);
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
        @POST
        public FormValidation doTestCodexCli(@AncestorInPath Job<?, ?> job, @QueryParameter String codexCliPath) {
            try {
                Jenkins j = Jenkins.get();
                Node target = null;
                String path = Util.fixEmptyAndTrim(codexCliPath);

                // Only try labeled agents for freestyle projects
                if (job != null && job instanceof AbstractProject) {
                    AbstractProject<?, ?> project = (AbstractProject<?, ?>) job;
                    Label assigned = project.getAssignedLabel();
                    if (assigned != null) {
                        for (Node n : assigned.getNodes()) {
                            if (n != null && n.toComputer() != null && n.toComputer().isOnline()) {
                                target = n;
                                break;
                            }
                        }
                        if (target == null) {
                            return FormValidation.error("No online agents match the job's label: '" + assigned.getExpression() + "'.");
                        }
                    }
                }

                // For pipeline jobs or freestyle jobs without labels, use controller
                if (target == null) {
                    target = j; // fallback to controller
                }

                // If job-level path is not provided, use global configuration
                if (path == null) {
                    CodexAnalysisJobProperty jobProperty = job != null ? job.getProperty(CodexAnalysisJobProperty.class) : null;
                    if (jobProperty != null) {
                        path = Util.fixEmptyAndTrim(jobProperty.getEffectiveCodexCliPath());
                    }
                    if (path == null) {
                        CodexAnalysisPlugin cfg = CodexAnalysisPlugin.get();
                        String globalPath = (cfg != null) ? Util.fixEmptyAndTrim(cfg.getCodexCliPath()) : null;
                        path = (globalPath != null) ? globalPath : "~/.local/bin/codex";
                    }
                }

                FilePath root = target.getRootPath();
                if (root == null) {
                    return FormValidation.error("Unable to access workspace on target node.");
                }

                String version = root.act(new CliVersionCallable(path));
                String where = (target == j) ? "controller" : target.getNodeName();
                return FormValidation.ok("Codex CLI is working on " + where + "! Version: " + version);
            } catch (Exception e) {
                return FormValidation.error("Failed to execute Codex CLI: " + e.getMessage());
            }
        }

        // Callable executed on a remote node to get Codex CLI version
        private static class CliVersionCallable implements FilePath.FileCallable<String> {
            private final String rawPath;

            CliVersionCallable(String rawPath) {
                this.rawPath = rawPath;
            }

            @Override
            public String invoke(java.io.File f, hudson.remoting.VirtualChannel channel) throws java.io.IOException, InterruptedException {
                String path = rawPath;
                if (path == null || path.isEmpty()) {
                    path = "codex";
                }
                // Expand leading ~ on the agent
                if (path.startsWith("~/")) {
                    String home = System.getProperty("user.home");
                    if (home != null && !home.isEmpty()) {
                        path = home + path.substring(1);
                    }
                }

                java.lang.ProcessBuilder pb = new java.lang.ProcessBuilder(path, "--version");
                java.lang.Process proc = pb.start();
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                try (java.io.InputStream is = proc.getInputStream()) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = is.read(buf)) >= 0) {
                        baos.write(buf, 0, r);
                    }
                }
                int code = proc.waitFor();
                if (code != 0) {
                    // Try to capture stderr for better diagnostics
                    java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
                    try (java.io.InputStream es = proc.getErrorStream()) {
                        byte[] buf = new byte[4096];
                        int r;
                        while ((r = es.read(buf)) >= 0) {
                            err.write(buf, 0, r);
                        }
                    }
                    throw new java.io.IOException("Codex CLI --version failed with exit code " + code + ": " + err.toString(java.nio.charset.StandardCharsets.UTF_8));
                }
                return baos.toString(java.nio.charset.StandardCharsets.UTF_8).trim();
            }

            @Override
            public void checkRoles(org.jenkinsci.remoting.RoleChecker checker) throws SecurityException {
                // Accept default; no special roles required
            }
        }

        // Callable executed on a remote node to get Codex CLI models list
        private static class ModelsListCallable implements FilePath.FileCallable<String> {
            private final String rawPath;

            ModelsListCallable(String rawPath) {
                this.rawPath = rawPath;
            }

            @Override
            public String invoke(java.io.File f, hudson.remoting.VirtualChannel channel) throws java.io.IOException, InterruptedException {
                String path = rawPath;
                if (path == null || path.isEmpty()) {
                    path = "codex";
                }
                // Expand leading ~ on the agent
                if (path.startsWith("~/")) {
                    String home = System.getProperty("user.home");
                    if (home != null && !home.isEmpty()) {
                        path = home + path.substring(1);
                    }
                }

                java.lang.ProcessBuilder pb = new java.lang.ProcessBuilder(path, "models", "list");
                java.lang.Process proc = pb.start();
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                try (java.io.InputStream is = proc.getInputStream()) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = is.read(buf)) >= 0) {
                        baos.write(buf, 0, r);
                    }
                }
                int code = proc.waitFor();
                if (code != 0) {
                    // Try to capture stderr for better diagnostics
                    java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
                    try (java.io.InputStream es = proc.getErrorStream()) {
                        byte[] buf = new byte[4096];
                        int r;
                        while ((r = es.read(buf)) >= 0) {
                            err.write(buf, 0, r);
                        }
                    }
                    throw new java.io.IOException("Codex CLI models list failed with exit code " + code + ": " + err.toString(java.nio.charset.StandardCharsets.UTF_8));
                }
                return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
            }

            @Override
            public void checkRoles(org.jenkinsci.remoting.RoleChecker checker) throws SecurityException {
                // Accept default; no special roles required
            }
        }

        // Callable executed on a remote node to get Codex CLI MCP servers list
        private static class McpServersListCallable implements FilePath.FileCallable<String> {
            private final String rawPath;
            private final String rawConfigPath;

            McpServersListCallable(String rawPath, String rawConfigPath) {
                this.rawPath = rawPath;
                this.rawConfigPath = rawConfigPath;
            }

            @Override
            public String invoke(java.io.File f, hudson.remoting.VirtualChannel channel) throws java.io.IOException, InterruptedException {
                String path = rawPath;
                if (path == null || path.isEmpty()) {
                    path = "codex";
                }
                // Expand leading ~ on the agent
                if (path.startsWith("~/")) {
                    String home = System.getProperty("user.home");
                    if (home != null && !home.isEmpty()) {
                        path = home + path.substring(1);
                    }
                }

                String configPath = rawConfigPath;
                if (configPath != null && !configPath.isEmpty()) {
                    // Expand leading ~ on the agent
                    if (configPath.startsWith("~/")) {
                        String home = System.getProperty("user.home");
                        if (home != null && !home.isEmpty()) {
                            configPath = home + configPath.substring(1);
                        }
                    }
                }

                java.lang.ProcessBuilder pb;
                if (configPath != null && !configPath.isEmpty()) {
                    pb = new java.lang.ProcessBuilder(path, "mcp", "list", "--config", configPath);
                } else {
                    pb = new java.lang.ProcessBuilder(path, "mcp", "list");
                }
                java.lang.Process proc = pb.start();
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                try (java.io.InputStream is = proc.getInputStream()) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = is.read(buf)) >= 0) {
                        baos.write(buf, 0, r);
                    }
                }
                int code = proc.waitFor();
                if (code != 0) {
                    // Try to capture stderr for better diagnostics
                    java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
                    try (java.io.InputStream es = proc.getErrorStream()) {
                        byte[] buf = new byte[4096];
                        int r;
                        while ((r = es.read(buf)) >= 0) {
                            err.write(buf, 0, r);
                        }
                    }
                    throw new java.io.IOException("Codex CLI mcp list failed with exit code " + code + ": " + err.toString(java.nio.charset.StandardCharsets.UTF_8));
                }
                return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
            }

            @Override
            public void checkRoles(org.jenkinsci.remoting.RoleChecker checker) throws SecurityException {
                // Accept default; no special roles required
            }
        }

        // Callable executed on a remote node to download and update Codex CLI
        private static class DownloadCliCallable implements FilePath.FileCallable<Boolean> {
            private final String downloadUrl;
            private final String username;
            private final String password;
            private final String rawPath;

            DownloadCliCallable(String downloadUrl, String username, String password, String rawPath) {
                this.downloadUrl = downloadUrl;
                this.username = username;
                this.password = password;
                this.rawPath = rawPath;
            }

            @Override
            public Boolean invoke(java.io.File f, hudson.remoting.VirtualChannel channel) throws java.io.IOException, InterruptedException {
                try {
                    String path = rawPath;
                    if (path == null || path.isEmpty()) {
                        path = "~/.local/bin/codex";
                    }
                    // Expand leading ~ on the agent
                    if (path.startsWith("~/")) {
                        String home = System.getProperty("user.home");
                        if (home != null && !home.isEmpty()) {
                            path = home + path.substring(1);
                        }
                    }

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
                        // Create parent directory if it doesn't exist
                        java.io.File cliFile = new java.io.File(path);
                        java.io.File parentDir = cliFile.getParentFile();
                        if (parentDir != null && !parentDir.exists()) {
                            parentDir.mkdirs();
                        }

                        // Download the file
                        try (java.io.InputStream inputStream = connection.getInputStream();
                             java.io.FileOutputStream outputStream = new java.io.FileOutputStream(path)) {

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }

                        // Make the file executable
                        cliFile.setExecutable(true);

                        return true;
                    } else {
                        return false; // Download failed
                    }
                } catch (Exception e) {
                    throw new java.io.IOException("Failed to download and update CLI: " + e.getMessage(), e);
                }
            }

            @Override
            public void checkRoles(org.jenkinsci.remoting.RoleChecker checker) throws SecurityException {
                // Accept default; no special roles required
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
