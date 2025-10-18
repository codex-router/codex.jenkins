package io.jenkins.plugins.codex;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.DescribableList;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Global configuration for the Codex Analysis Plugin.
 * Manages Codex CLI settings and MCP server configurations.
 */
@Extension
public class CodexAnalysisPlugin extends GlobalConfiguration {

    private String codexCliPath = "codex";
    private String configPath = "~/.codex/config.toml";
    private String mcpServersPath = "~/.codex/mcp_servers.toml";
    private String defaultModel = "kimi-k2";
    private int timeoutSeconds = 120;
    private boolean enableMcpServers = true;
    private List<McpServerConfig> mcpServers = new ArrayList<>();

    @DataBoundConstructor
    public CodexAnalysisPlugin() {
        load();
    }

    public static CodexAnalysisPlugin get() {
        return GlobalConfiguration.all().get(CodexAnalysisPlugin.class);
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

    public List<McpServerConfig> getMcpServers() {
        return mcpServers;
    }

    public void setMcpServers(List<McpServerConfig> mcpServers) {
        this.mcpServers = mcpServers;
    }

    /**
     * MCP Server Configuration
     */
    public static class McpServerConfig {
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
}
