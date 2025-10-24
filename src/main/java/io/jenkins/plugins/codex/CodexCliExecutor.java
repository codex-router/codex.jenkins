package io.jenkins.plugins.codex;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * Utility class for executing Codex CLI commands.
 * Handles command building, execution, and result processing.
 */
public class CodexCliExecutor {

    private final Launcher launcher;
    private final TaskListener listener;
    private final EnvVars environment;
    private final FilePath workspace;
    private final CodexAnalysisJobProperty jobConfig;

    public CodexCliExecutor(Launcher launcher, TaskListener listener, EnvVars environment, FilePath workspace) {
        this(launcher, listener, environment, workspace, null);
    }

    public CodexCliExecutor(Launcher launcher, TaskListener listener, EnvVars environment, FilePath workspace, CodexAnalysisJobProperty jobConfig) {
        this.launcher = launcher;
        this.listener = listener;
        this.environment = environment;
        this.workspace = workspace;
        this.jobConfig = jobConfig;
    }

    /**
     * Execute a Codex analysis command
     */
    public CodexAnalysisResult executeAnalysis(String content, String analysisType, String customPrompt,
                                             Map<String, String> additionalParams) throws IOException, InterruptedException {
        CodexAnalysisPlugin globalConfig = CodexAnalysisPlugin.get();
        if (globalConfig == null) {
            throw new IOException("Codex Analysis Plugin configuration not found");
        }

        ArgumentListBuilder args = new ArgumentListBuilder();

        // Use job-level CLI path if available, otherwise use global
        String cliPath = jobConfig != null ? jobConfig.getEffectiveCodexCliPath() : globalConfig.getCodexCliPath();
        args.add(cliPath);
        args.add("analyze");

        // Add content
        args.add("--content", content);

        // Add analysis type
        if (StringUtils.isNotBlank(analysisType)) {
            args.add("--type", analysisType);
        }

        // Add custom prompt
        if (StringUtils.isNotBlank(customPrompt)) {
            args.add("--prompt", customPrompt);
        }

        // Add model - use additional params first, then job config, then global
        String model = additionalParams != null ? additionalParams.get("model") : null;
        if (StringUtils.isBlank(model)) {
            model = jobConfig != null ? jobConfig.getEffectiveDefaultModel() : "kimi-k2";
        }
        args.add("--model", model);

        // Add timeout - use additional params first, then job config, then global
        String timeout = additionalParams != null ? additionalParams.get("timeout") : null;
        if (StringUtils.isBlank(timeout)) {
            timeout = String.valueOf(jobConfig != null ? jobConfig.getEffectiveTimeoutSeconds() : globalConfig.getTimeoutSeconds());
        }
        args.add("--timeout", timeout);

        // Add MCP servers if enabled
        boolean enableMcp = jobConfig != null ? jobConfig.getEffectiveEnableMcpServers() : false;
        if (enableMcp) {
            String mcpPath = jobConfig != null ? jobConfig.getEffectiveConfigPath() : globalConfig.getConfigPath();
            args.add("--mcp-config", mcpPath);
        }

        // Add additional parameters
        if (additionalParams != null) {
            for (Map.Entry<String, String> entry : additionalParams.entrySet()) {
                args.add("--" + entry.getKey(), entry.getValue());
            }
        }

        // Execute command
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        PrintStream errorPrintStream = new PrintStream(errorStream);

        try {
            Launcher.ProcStarter procStarter = launcher.launch()
                    .cmds(args)
                    .envs(environment)
                    .stdout(printStream)
                    .stderr(errorPrintStream)
                    .pwd(workspace);

            int exitCode = procStarter.start().join();

            String output = outputStream.toString();
            String error = errorStream.toString();

            if (exitCode != 0) {
                listener.error("Codex CLI execution failed with exit code " + exitCode);
                listener.error("Error output: " + error);
                throw new IOException("Codex CLI execution failed: " + error);
            }

            return new CodexAnalysisResult(output, error, exitCode == 0);

        } finally {
            printStream.close();
            errorPrintStream.close();
        }
    }

    /**
     * Execute a simple Codex query
     */
    public String executeQuery(String query, String context) throws IOException, InterruptedException {
        CodexAnalysisPlugin globalConfig = CodexAnalysisPlugin.get();
        if (globalConfig == null) {
            throw new IOException("Codex Analysis Plugin configuration not found");
        }

        ArgumentListBuilder args = new ArgumentListBuilder();

        // Use job-level CLI path if available, otherwise use global
        String cliPath = jobConfig != null ? jobConfig.getEffectiveCodexCliPath() : globalConfig.getCodexCliPath();
        args.add(cliPath);
        args.add("query");

        args.add("--query", query);
        if (StringUtils.isNotBlank(context)) {
            args.add("--context", context);
        }

        // Use job-level model and timeout if available, otherwise use default
        String model = jobConfig != null ? jobConfig.getEffectiveDefaultModel() : "kimi-k2";
        int timeout = jobConfig != null ? jobConfig.getEffectiveTimeoutSeconds() : globalConfig.getTimeoutSeconds();
        args.add("--model", model);
        args.add("--timeout", String.valueOf(timeout));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        try {
            Launcher.ProcStarter procStarter = launcher.launch()
                    .cmds(args)
                    .envs(environment)
                    .stdout(printStream)
                    .pwd(workspace);

            int exitCode = procStarter.start().join();
            String output = outputStream.toString();

            if (exitCode != 0) {
                throw new IOException("Codex query failed with exit code " + exitCode);
            }

            return output;

        } finally {
            printStream.close();
        }
    }

    /**
     * Check if Codex CLI is available and properly configured
     */
    public boolean isCodexAvailable() throws IOException, InterruptedException {
        CodexAnalysisPlugin globalConfig = CodexAnalysisPlugin.get();
        if (globalConfig == null) {
            return false;
        }

        ArgumentListBuilder args = new ArgumentListBuilder();

        // Use job-level CLI path if available, otherwise use global
        String cliPath = jobConfig != null ? jobConfig.getEffectiveCodexCliPath() : globalConfig.getCodexCliPath();
        args.add(cliPath);
        args.add("--version");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        try {
            Launcher.ProcStarter procStarter = launcher.launch()
                    .cmds(args)
                    .envs(environment)
                    .stdout(printStream)
                    .pwd(workspace);

            int exitCode = procStarter.start().join();
            return exitCode == 0;

        } catch (Exception e) {
            listener.getLogger().println("Codex CLI not available: " + e.getMessage());
            return false;
        } finally {
            printStream.close();
        }
    }

    /**
     * Result of Codex analysis
     */
    public static class CodexAnalysisResult {
        private final String output;
        private final String error;
        private final boolean success;

        public CodexAnalysisResult(String output, String error, boolean success) {
            this.output = output;
            this.error = error;
            this.success = success;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
