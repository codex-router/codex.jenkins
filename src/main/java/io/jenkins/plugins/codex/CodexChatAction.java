package io.jenkins.plugins.codex;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Action for Codex Chat functionality.
 * Provides interactive chat with Codex CLI from the build dropdown menu.
 */
public class CodexChatAction implements Action {

    private final Run<?, ?> run;

    public CodexChatAction(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public String getIconFileName() {
        // For Action icons, Jenkins resolves paths differently than regular resources
        // The SVG file is packaged at: target/codex-analysis/images/codex-chat.svg
        // And accessible at: /plugin/codex-analysis/images/codex-chat.svg
        // However, Action.getIconFileName() for dropdown menu items may need the full path
        // Try using the plugin path format
        return "/plugin/codex-analysis/images/codex-chat.svg";
    }

    @Override
    public String getDisplayName() {
        return "Codex Chat";
    }

    @Override
    public String getUrlName() {
        return "codex-chat";
    }

    public Run<?, ?> getRun() {
        return run;
    }

    /**
     * Get the effective Codex CLI path from job or global configuration
     */
    public String getEffectiveCodexCliPath() {
        CodexAnalysisJobProperty jobProperty = run.getParent().getProperty(CodexAnalysisJobProperty.class);
        if (jobProperty != null && jobProperty.isUseJobConfig()) {
            String jobPath = jobProperty.getCodexCliPath();
            if (jobPath != null && !jobPath.trim().isEmpty()) {
                return jobPath;
            }
        }
        CodexAnalysisPlugin global = CodexAnalysisPlugin.get();
        return global != null ? global.getCodexCliPath() : "~/.local/bin/codex";
    }

    /**
     * Check if Codex CLI is available
     */
    public boolean isCodexAvailable() {
        try {
            String cliPath = getEffectiveCodexCliPath();
            return cliPath != null && !cliPath.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Handle the chat page request
     */
    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        req.getView(this, "index").forward(req, rsp);
    }

    /**
     * Start an interactive chat session
     * This will execute the codex CLI chat command in interactive mode
     * Note: For true interactivity, this redirects to show instructions on how to use
     * the chat feature, as Jenkins web UI has limitations for interactive terminal sessions.
     */
    @RequirePOST
    public void doStartChat(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        String initialMessage = req.getParameter("initialMessage");
        String context = req.getParameter("context");

        // Store the chat parameters in the request for the view
        req.setAttribute("initialMessage", initialMessage);
        req.setAttribute("context", context);

        // Forward to a page that will execute the chat
        req.getView(this, "chat").forward(req, rsp);
    }

    /**
     * Execute a chat query and return the response
     * This method can be called via AJAX for interactive chat
     */
    @RequirePOST
    public void doChatQuery(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        String message = req.getParameter("message");
        String context = req.getParameter("context");

        if (message == null || message.trim().isEmpty()) {
            rsp.sendError(400, "Message is required");
            return;
        }

        // Get the node where the build ran
        Node node = null;

        // Try to get from AbstractBuild if available
        if (run instanceof AbstractBuild) {
            try {
                AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
                node = build.getBuiltOn();
            } catch (Exception e) {
                // Ignore
            }
        }

        // Fallback: try to get node from executor
        if (node == null) {
            try {
                if (run.getExecutor() != null && run.getExecutor().getOwner() != null) {
                    node = run.getExecutor().getOwner().getNode();
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        // Final fallback: use Jenkins instance
        if (node == null) {
            node = Jenkins.get();
        }

        Computer computer = node.toComputer();
        if (computer == null || computer.isOffline()) {
            rsp.sendError(500, "Node is offline or unavailable");
            return;
        }

        try {
            // Get workspace from the build
            FilePath workspace = null;

            // Try to get workspace from AbstractBuild if available
            if (run instanceof AbstractBuild) {
                try {
                    AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
                    workspace = build.getWorkspace();
                } catch (Exception e) {
                    // Ignore
                }
            }

            // Fallback: use node root path
            if (workspace == null) {
                workspace = node.getRootPath();
            }

            // Create a launcher for the node
            Launcher launcher = node.createLauncher(TaskListener.NULL);

            // Create a task listener that captures output
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(outputStream, true);
            TaskListener listener = new TaskListener() {
                @Override
                public PrintStream getLogger() {
                    return printStream;
                }
            };

            // Get environment variables
            EnvVars envVars = new EnvVars();
            try {
                envVars = run.getEnvironment(TaskListener.NULL);
            } catch (Exception e) {
                // Use default environment if we can't get build environment
            }

            // Get job property for configuration
            CodexAnalysisJobProperty jobProperty = run.getParent().getProperty(CodexAnalysisJobProperty.class);

            // Create executor and execute chat
            CodexCliExecutor executor = new CodexCliExecutor(launcher, listener, envVars, workspace, jobProperty);

            // Prepare additional parameters
            Map<String, String> additionalParams = new HashMap<>();

            // Execute interactive chat
            executor.executeInteractiveChat(message, context, additionalParams);

            // Get the output
            String output = outputStream.toString();
            printStream.close();

            // Return JSON response
            rsp.setContentType("application/json;charset=UTF-8");
            String escapedOutput = output.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            rsp.getWriter().write("{\"success\": true, \"response\": \"" + escapedOutput + "\"}");

        } catch (Exception e) {
            rsp.setContentType("application/json;charset=UTF-8");
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            String escapedError = errorMsg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            rsp.getWriter().write("{\"success\": false, \"error\": \"" + escapedError + "\"}");
        }
    }
}
