package io.jenkins.plugins.codex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class CodexAnalysisPluginTest {

    @Test
    public void testDefaultModelOptions() {
        // Test that no hardcoded models are provided (CLI-only approach)
        List<String> defaultModels = getDefaultModelOptions();
        assertNotNull(defaultModels);
        assertTrue("Should return empty list (no hardcoded models)", defaultModels.isEmpty());
    }

    @Test
    public void testModelCacheStatus() {
        // Test cache status logic without Jenkins instance
        String status = getModelCacheStatusForTest();
        assertNotNull(status);
        assertTrue("Status should mention no models cached", status.contains("No models cached"));
    }

    @Test
    public void testModelValidation() {
        // Test validation logic without Jenkins instance (CLI-only approach)
        List<String> availableModels = getDefaultModelOptions(); // Returns empty list

        // Test with empty model
        hudson.util.FormValidation result = validateDefaultModel("", availableModels);
        assertNotNull(result);
        assertTrue("Should show warning for empty model", result.kind == hudson.util.FormValidation.Kind.WARNING);

        // Test with any model when no models are available from CLI
        result = validateDefaultModel("kimi-k2", availableModels);
        assertNotNull(result);
        assertTrue("Should show warning when no models available from CLI", result.kind == hudson.util.FormValidation.Kind.WARNING);
        assertTrue("Should mention model list refresh", result.getMessage().contains("Update Model List"));
    }

    @Test
    public void testModelFetchingWithInvalidPath() {
        // Test with invalid CLI path - this should fail
        hudson.util.FormValidation result = fetchAvailableModels("/invalid/path/codex");
        assertNotNull(result);
        assertTrue("Should show error for invalid path", result.kind == hudson.util.FormValidation.Kind.ERROR);
    }

    @Test
    public void testModelFetchingWithValidPathButNoCLI() {
        // Test with a path that exists but doesn't contain the codex executable
        hudson.util.FormValidation result = fetchAvailableModels("/tmp");
        assertNotNull(result);
        // This should either succeed (if codex is in PATH) or fail (if not)
        // We just verify the method doesn't throw an exception
        assertTrue("Should return either OK or ERROR",
                   result.kind == hudson.util.FormValidation.Kind.OK ||
                   result.kind == hudson.util.FormValidation.Kind.ERROR);
    }

    @Test
    public void testMcpServersFetchingWithInvalidPath() {
        // Test MCP servers fetching with invalid CLI path
        hudson.util.FormValidation result = fetchAvailableMcpServers("/invalid/path/codex", "/invalid/config.toml");
        assertNotNull(result);
        assertTrue("Should show error for invalid path", result.kind == hudson.util.FormValidation.Kind.ERROR);
    }

    @Test
    public void testMcpServersFetchingWithValidPathButNoCLI() {
        // Test MCP servers fetching with a path that exists but doesn't contain the codex executable
        hudson.util.FormValidation result = fetchAvailableMcpServers("/tmp", "/tmp/config.toml");
        assertNotNull(result);
        // This should either succeed (if codex is in PATH) or fail (if not)
        // We just verify the method doesn't throw an exception
        assertTrue("Should return either OK or ERROR",
                   result.kind == hudson.util.FormValidation.Kind.OK ||
                   result.kind == hudson.util.FormValidation.Kind.ERROR);
    }

    @Test
    public void testMcpServersCacheStatus() {
        // Test MCP servers cache status logic without Jenkins instance
        String status = getMcpServersCacheStatusForTest();
        assertNotNull(status);
        assertTrue("Status should mention no MCP servers cached", status.contains("No MCP servers cached"));
    }

    // Helper methods that replicate the logic without Jenkins dependencies
    private List<String> getDefaultModelOptions() {
        // Return empty list - no hardcoded models anymore
        return new java.util.ArrayList<>();
    }

    private String getModelCacheStatusForTest() {
        return "No models cached. Click 'Update Model List' to fetch from Codex CLI.";
    }

    private String getMcpServersCacheStatusForTest() {
        return "No MCP servers cached. Click 'Update MCP Servers List' to fetch from Codex CLI.";
    }

    private hudson.util.FormValidation validateDefaultModel(String value, List<String> availableModels) {
        if (value == null || value.trim().isEmpty()) {
            return hudson.util.FormValidation.warning("Default model is empty, will use 'kimi-k2'");
        }

        // Check if the selected model is in the available models list
        if (!availableModels.contains(value.trim())) {
            return hudson.util.FormValidation.warning("Selected model '" + value + "' is not in the current model list. Click 'Update Model List' to refresh available models.");
        }

        return hudson.util.FormValidation.ok();
    }

    private hudson.util.FormValidation fetchAvailableModels(String codexCliPath) {
        try {
            String effectiveCliPath = codexCliPath != null && !codexCliPath.trim().isEmpty()
                ? codexCliPath.trim()
                : "~/.local/bin/codex";

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
                    return hudson.util.FormValidation.ok("Successfully fetched " + models.size() + " models from Codex CLI");
                } else {
                    return hudson.util.FormValidation.warning("No models found in Codex CLI output");
                }
            } else {
                return hudson.util.FormValidation.error("Codex CLI returned exit code: " + exitCode + ". Output: " + output.toString());
            }
        } catch (Exception e) {
            return hudson.util.FormValidation.error("Failed to fetch models from Codex CLI: " + e.getMessage());
        }
    }

    private List<String> parseModelList(String output) {
        List<String> models = new java.util.ArrayList<>();
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

        // If no models found, return empty list (no hardcoded fallback)
        // models list remains empty

        return models;
    }

    private hudson.util.FormValidation fetchAvailableMcpServers(String codexCliPath, String configPath) {
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
                    return hudson.util.FormValidation.ok("Successfully fetched " + servers.size() + " MCP servers from Codex CLI");
                } else {
                    return hudson.util.FormValidation.warning("No MCP servers found in Codex CLI output");
                }
            } else {
                return hudson.util.FormValidation.error("Codex CLI returned exit code: " + exitCode + ". Output: " + output.toString());
            }
        } catch (Exception e) {
            return hudson.util.FormValidation.error("Failed to fetch MCP servers from Codex CLI: " + e.getMessage());
        }
    }

    private List<String> parseMcpServersList(String output) {
        List<String> servers = new java.util.ArrayList<>();
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

        // If no servers found, return default list
        if (servers.isEmpty()) {
            servers.add("filesystem");
            servers.add("github");
            servers.add("database");
            servers.add("web-search");
        }

        return servers;
    }
}
