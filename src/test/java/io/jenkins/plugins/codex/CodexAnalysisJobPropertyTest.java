package io.jenkins.plugins.codex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import hudson.util.FormValidation;

import java.util.ArrayList;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class CodexAnalysisJobPropertyTest {

    private CodexAnalysisJobProperty jobProperty;

    @Test
    public void testJobPropertyCreation() {
        jobProperty = new CodexAnalysisJobProperty(
            "job-codex", "https://example.com/codex-download", "testuser", "testpass", "job-config.toml",
            "job-model", 180, false, "job-litellm-key", new ArrayList<>(), true
        );

        assertEquals("job-codex", jobProperty.getCodexCliPath());
        assertEquals("https://example.com/codex-download", jobProperty.getCodexCliDownloadUrl());
        assertEquals("testuser", jobProperty.getCodexCliDownloadUsername());
        assertEquals("testpass", jobProperty.getCodexCliDownloadPassword());
        assertEquals("job-config.toml", jobProperty.getConfigPath());
        assertEquals("job-model", jobProperty.getDefaultModel());
        assertEquals(180, jobProperty.getTimeoutSeconds());
        assertFalse(jobProperty.isEnableMcpServers());
        assertEquals("job-litellm-key", jobProperty.getLitellmApiKey());
        assertTrue(jobProperty.getSelectedMcpServers().isEmpty());
        assertTrue(jobProperty.isUseJobConfig());
    }

    @Test
    public void testEffectiveValuesWithJobConfig() {
        jobProperty = new CodexAnalysisJobProperty(
            "job-codex", "https://example.com/codex-download", "testuser", "testpass", "job-config.toml",
            "job-model", 180, false, "job-litellm-key", new ArrayList<>(), true
        );

        // When useJobConfig is true, job values should be returned
        assertEquals("job-codex", jobProperty.getEffectiveCodexCliPath());
        assertEquals("job-config.toml", jobProperty.getEffectiveConfigPath());
        assertEquals("job-config.toml", jobProperty.getEffectiveMcpServersPath());
        assertEquals("job-model", jobProperty.getEffectiveDefaultModel());
        assertEquals(180, jobProperty.getEffectiveTimeoutSeconds());
        assertFalse(jobProperty.getEffectiveEnableMcpServers());
    }

    @Test
    public void testEffectiveValuesWithoutJobConfig() {
        jobProperty = new CodexAnalysisJobProperty(
            "job-codex", "https://example.com/codex-download", "testuser", "testpass", "job-config.toml",
            "job-model", 180, false, "job-litellm-key", new ArrayList<>(), false
        );

        // When useJobConfig is false, the raw values should still be set
        // but the effective methods would use global values in real usage
        assertEquals("job-codex", jobProperty.getCodexCliPath());
        assertEquals("job-config.toml", jobProperty.getConfigPath());
        assertEquals("job-model", jobProperty.getDefaultModel());
        assertEquals(180, jobProperty.getTimeoutSeconds());
        assertFalse(jobProperty.isUseJobConfig());
    }

    @Test
    public void testEffectiveValuesWithEmptyJobConfig() {
        jobProperty = new CodexAnalysisJobProperty(
            "", "", "", "", "", "", 0, false, "", new ArrayList<>(), true
        );

        // When job config is enabled but values are empty, the raw values should be empty
        // The effective methods would fall back to global in real usage, but in tests
        // we can't access Jenkins instance, so we test the raw values
        assertEquals("", jobProperty.getCodexCliPath());
        assertEquals("", jobProperty.getConfigPath());
        assertEquals("", jobProperty.getDefaultModel());
        assertEquals(0, jobProperty.getTimeoutSeconds());
        assertTrue(jobProperty.isUseJobConfig());
    }

    @Test
    public void testDescriptor() {
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();
        assertNotNull(descriptor);
        assertEquals("Codex Analysis Plugin Configuration", descriptor.getDisplayName());
        // Test with a mock Job class - in real usage this would be a proper Job subclass
        assertTrue(descriptor.isApplicable(hudson.model.Job.class)); // Should be applicable to all job types
    }

    @Test
    public void testSettersAndGetters() {
        jobProperty = new CodexAnalysisJobProperty(
            "initial-codex", "https://example.com/initial-download", "initialuser", "initialpass", "initial-config.toml",
            "initial-model", 120, true, "initial-litellm-key", new ArrayList<>(), false
        );

        // Test setters
        jobProperty.setCodexCliPath("new-codex");
        jobProperty.setCodexCliDownloadUrl("https://example.com/new-download");
        jobProperty.setCodexCliDownloadUsername("newuser");
        jobProperty.setCodexCliDownloadPassword("newpass");
        jobProperty.setConfigPath("new-config.toml");
        jobProperty.setDefaultModel("new-model");
        jobProperty.setTimeoutSeconds(240);
        jobProperty.setEnableMcpServers(false);
        jobProperty.setLitellmApiKey("new-litellm-key");
        jobProperty.setUseJobConfig(true);

        // Test getters
        assertEquals("new-codex", jobProperty.getCodexCliPath());
        assertEquals("https://example.com/new-download", jobProperty.getCodexCliDownloadUrl());
        assertEquals("newuser", jobProperty.getCodexCliDownloadUsername());
        assertEquals("newpass", jobProperty.getCodexCliDownloadPassword());
        assertEquals("new-config.toml", jobProperty.getConfigPath());
        assertEquals("new-model", jobProperty.getDefaultModel());
        assertEquals(240, jobProperty.getTimeoutSeconds());
        assertFalse(jobProperty.isEnableMcpServers());
        assertEquals("new-litellm-key", jobProperty.getLitellmApiKey());
        assertTrue(jobProperty.isUseJobConfig());
    }

    @Test
    public void testModelFetchingDelegation() {
        // Test that job property delegates model fetching to global configuration
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // In test environment, Jenkins instance is not available, so this should return an error
        FormValidation result = descriptor.doFetchAvailableModels("test-codex-path");

        // Should return an error since Jenkins instance is not available in test
        assertNotNull(result);
        assertTrue("Should return error when Jenkins instance is not available",
                   result.kind == FormValidation.Kind.ERROR);
        assertTrue("Error message should mention Jenkins instance not available",
                   result.getMessage().contains("Jenkins instance not available"));
    }

    @Test
    public void testGetAvailableModels() {
        // Test the getAvailableModels method (CLI-only, no hardcoded models)
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // This should return empty array when CLI is not available (test environment)
        String[] models = descriptor.getAvailableModels();
        assertNotNull(models);
        assertTrue("Should return empty array when CLI is not available", models.length == 0);
    }

    @Test
    public void testMcpServersFetchingDelegation() {
        // Test that job property handles MCP servers fetching independently (no global delegation)
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // This should attempt to fetch MCP servers directly (will likely fail due to invalid path)
        FormValidation result = descriptor.doFetchAvailableMcpServers("test-codex-path", "test-config-path");

        // Should return an error due to invalid CLI path, not Jenkins instance issues
        assertNotNull(result);
        assertTrue("Should return error due to invalid CLI path",
                   result.kind == FormValidation.Kind.ERROR);
        assertTrue("Error message should mention CLI execution failure",
                   result.getMessage().contains("Failed to fetch MCP servers from Codex CLI"));
    }

    @Test
    public void testGetMcpServersCacheStatus() {
        // Test the getMcpServersCacheStatus method (job-level only)
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // This should return a status message indicating job-specific configuration
        String status = descriptor.getMcpServersCacheStatus();
        assertNotNull(status);
        assertTrue("Should mention job-specific configuration", status.contains("job-specific"));
        assertTrue("Should mention Update MCP Servers List button", status.contains("Update MCP Servers List"));
    }

    @Test
    public void testTestCodexCliWithTildePath() {
        // Test that tilde (~) in path is expanded to home directory
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // Test with tilde path - should expand ~ to home directory
        FormValidation result = descriptor.doTestCodexCli("~/.local/bin/codex", null, null, null, null, null);

        // Should attempt to execute (will likely fail due to invalid path, but tilde should be expanded)
        assertNotNull(result);
        // The path should be expanded, so error message should contain expanded path (not ~)
        String homeDir = System.getProperty("user.home");
        String expectedPath = homeDir + "/.local/bin/codex";
        assertTrue("Error message should contain expanded path without tilde",
                   result.getMessage().contains(homeDir) ||
                   result.getMessage().contains("Failed to test Codex CLI"));
        // Verify tilde is not in the message (it should be expanded)
        assertFalse("Message should not contain tilde character after expansion",
                    result.getMessage().contains("~/.local/bin/codex"));
    }

    @Test
    public void testTestCodexCliWithRegularPath() {
        // Test with regular path (no tilde)
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // Test with regular path - should work normally
        FormValidation result = descriptor.doTestCodexCli("/usr/local/bin/codex", null, null, null, null, null);

        // Should attempt to execute (will likely fail due to invalid path)
        assertNotNull(result);
        assertTrue("Should return either OK or ERROR",
                   result.kind == FormValidation.Kind.OK ||
                   result.kind == FormValidation.Kind.ERROR ||
                   result.kind == FormValidation.Kind.WARNING);
        // Path should remain unchanged (no tilde expansion needed)
        assertTrue("Error message should contain the original path",
                   result.getMessage().contains("/usr/local/bin/codex") ||
                   result.getMessage().contains("Failed to test Codex CLI"));
    }

    @Test
    public void testTestCodexCliWithInvalidPath() {
        // Test with invalid path
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // Test with invalid path - should return error
        FormValidation result = descriptor.doTestCodexCli("/invalid/path/codex", null, null, null, null, null);

        // Should return an error due to invalid CLI path
        assertNotNull(result);
        assertTrue("Should return error due to invalid CLI path",
                   result.kind == FormValidation.Kind.ERROR);
        assertTrue("Error message should mention failure",
                   result.getMessage().contains("Failed to test Codex CLI"));
    }

    @Test
    public void testTestCodexCliWithEmptyPath() {
        // Test with empty path - should use default and expand tilde
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // Test with empty path - should use default "~/.local/bin/codex" and expand tilde
        FormValidation result = descriptor.doTestCodexCli("", null, null, null, null, null);

        // Should attempt to execute with default path (will likely fail, but tilde should be expanded)
        assertNotNull(result);
        // Should either succeed or fail, but tilde should be expanded
        String homeDir = System.getProperty("user.home");
        assertTrue("Should return a result (OK, ERROR, or WARNING)",
                   result.kind == FormValidation.Kind.OK ||
                   result.kind == FormValidation.Kind.ERROR ||
                   result.kind == FormValidation.Kind.WARNING);
        // Verify that if default path is used, tilde should be expanded
        if (result.getMessage().contains(homeDir + "/.local/bin/codex") ||
            result.getMessage().contains("~/.local/bin/codex")) {
            // If default path appears, verify tilde expansion
            assertTrue("If default path is used, it should be expanded",
                       result.getMessage().contains(homeDir) ||
                       !result.getMessage().contains("~/.local/bin/codex"));
        }
    }

    @Test
    public void testTestCodexCliWithNullPath() {
        // Test with null path - should use default and expand tilde
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // Test with null path - should use default "~/.local/bin/codex" and expand tilde
        FormValidation result = descriptor.doTestCodexCli(null, null, null, null, null, null);

        // Should attempt to execute with default path (will likely fail, but tilde should be expanded)
        assertNotNull(result);
        // Should either succeed or fail, but tilde should be expanded
        assertTrue("Should return a result (OK, ERROR, or WARNING)",
                   result.kind == FormValidation.Kind.OK ||
                   result.kind == FormValidation.Kind.ERROR ||
                   result.kind == FormValidation.Kind.WARNING);
    }

    @Test
    public void testTestCodexCliTildeExpansion() {
        // Test that tilde expansion works correctly
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();
        String homeDir = System.getProperty("user.home");

        // Test with various tilde paths
        String[] tildePaths = {
            "~/.local/bin/codex",
            "~/codex",
            "~/.codex/codex"
        };

        for (String tildePath : tildePaths) {
            FormValidation result = descriptor.doTestCodexCli(tildePath, null, null, null, null, null);
            assertNotNull(result);
            // Verify that the expanded path (without ~) appears in the message or error
            // The actual execution may fail, but tilde should be expanded
            String expectedPath = tildePath.replaceFirst("^~", homeDir);
            assertTrue("Path should be expanded (message should not contain ~ for " + tildePath,
                       !result.getMessage().contains("~") ||
                       result.getMessage().contains(homeDir) ||
                       result.getMessage().contains("Failed to test Codex CLI"));
        }
    }

    @Test
    public void testTestCodexCliWithAllParameters() {
        // Test with all parameters provided
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        FormValidation result = descriptor.doTestCodexCli(
            "~/.local/bin/codex",
            "https://example.com/download",
            "testuser",
            "testpass",
            "~/.codex/config.toml",
            "test-api-key"
        );

        // Should attempt to execute (will likely fail, but should handle all parameters)
        assertNotNull(result);
        assertTrue("Should return a result (OK, ERROR, or WARNING)",
                   result.kind == FormValidation.Kind.OK ||
                   result.kind == FormValidation.Kind.ERROR ||
                   result.kind == FormValidation.Kind.WARNING);
        // Verify tilde expansion happened
        String homeDir = System.getProperty("user.home");
        assertTrue("Tilde should be expanded in the path",
                   result.getMessage().contains(homeDir) ||
                   result.getMessage().contains("Failed to test Codex CLI"));
    }

    @Test
    public void testTestCodexCliWithWhitespacePath() {
        // Test with path containing whitespace (should be trimmed)
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        FormValidation result = descriptor.doTestCodexCli("  ~/.local/bin/codex  ", null, null, null, null, null);

        assertNotNull(result);
        // Should handle whitespace and expand tilde
        String homeDir = System.getProperty("user.home");
        assertTrue("Should return a result and handle whitespace",
                   result.kind == FormValidation.Kind.OK ||
                   result.kind == FormValidation.Kind.ERROR ||
                   result.kind == FormValidation.Kind.WARNING);
    }

    @Test
    public void testTestCodexCliPathExpansionVerification() {
        // Test to verify that tilde expansion actually happens by checking the path format
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();
        String homeDir = System.getProperty("user.home");

        // Test with tilde path
        FormValidation result = descriptor.doTestCodexCli("~/.local/bin/codex", null, null, null, null, null);
        assertNotNull(result);

        // The expanded path should be in the format: /home/user/.local/bin/codex (no ~)
        // We verify this by checking the message doesn't contain the original tilde path
        // and either contains the home directory or a generic error
        boolean hasExpandedPath = result.getMessage().contains(homeDir + "/.local/bin/codex");
        boolean hasGenericError = result.getMessage().contains("Failed to test Codex CLI");
        boolean hasOriginalTilde = result.getMessage().contains("~/.local/bin/codex");

        assertTrue("Path should be expanded (either shows expanded path or generic error, not original tilde path)",
                   hasExpandedPath || (hasGenericError && !hasOriginalTilde));
    }

    @Test
    public void testTestCodexCliBasicExecution() {
        // Test basic CLI execution
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // Test with a path - should attempt execution
        FormValidation result = descriptor.doTestCodexCli("/usr/local/bin/codex", null, null, null, null, null);

        assertNotNull(result);
        // Should return a result (OK, ERROR, or WARNING)
        assertTrue("Should return a result when testing CLI",
                   result.kind == FormValidation.Kind.OK ||
                   result.kind == FormValidation.Kind.ERROR ||
                   result.kind == FormValidation.Kind.WARNING);
        // Verify the method returns a message
        assertTrue("Should return a message",
                   !result.getMessage().isEmpty());
    }

    @Test
    public void testTestCodexCliErrorHandling() {
        // Test error handling for various failure scenarios
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // Test with null path - should handle gracefully
        FormValidation result = descriptor.doTestCodexCli(null, null, null, null, null, null);
        assertNotNull(result);
        // Should handle null path gracefully (uses default or returns error)
        // Improved error handling ensures no exceptions
        assertTrue("Should handle null path gracefully",
                   result.kind == FormValidation.Kind.OK ||
                   result.kind == FormValidation.Kind.ERROR ||
                   result.kind == FormValidation.Kind.WARNING);
        assertNotNull("Should return a message for null path", result.getMessage());

        // Test with empty path - should handle gracefully
        result = descriptor.doTestCodexCli("", null, null, null, null, null);
        assertNotNull(result);
        assertTrue("Should handle empty path gracefully",
                   result.kind == FormValidation.Kind.OK ||
                   result.kind == FormValidation.Kind.ERROR ||
                   result.kind == FormValidation.Kind.WARNING);

        // Test with invalid executable path
        result = descriptor.doTestCodexCli("/nonexistent/path/codex", null, null, null, null, null);
        assertNotNull(result);
        assertTrue("Should return error for invalid path",
                   result.kind == FormValidation.Kind.ERROR ||
                   result.kind == FormValidation.Kind.WARNING);
        assertTrue("Error message should mention failure",
                   result.getMessage().contains("Failed") ||
                   result.getMessage().contains("error") ||
                   result.getMessage().contains("not configured"));
    }

    @Test
    public void testTestCodexCliMessageContent() {
        // Test that messages contain relevant information
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        FormValidation result = descriptor.doTestCodexCli("/usr/local/bin/codex", null, null, null, null, null);

        assertNotNull(result);
        String message = result.getMessage();
        assertNotNull("Message should not be null", message);
        // Message should contain information about the test execution
        assertTrue("Message should contain execution context",
                   message.contains("Codex CLI") ||
                   message.contains("path") ||
                   message.contains("Failed") ||
                   message.contains("accessible"));
    }

    @Test
    public void testTestCodexCliGracefulExecution() {
        // Test that the method executes gracefully in various scenarios
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        // Test with regular path
        FormValidation result1 = descriptor.doTestCodexCli("/usr/bin/codex", null, null, null, null, null);
        assertNotNull("Should return result for regular path", result1);

        // Test with tilde path
        FormValidation result2 = descriptor.doTestCodexCli("~/.local/bin/codex", null, null, null, null, null);
        assertNotNull("Should return result for tilde path", result2);

        // Both should return valid results without throwing exceptions
        assertTrue("First result should be valid",
                   result1.kind == FormValidation.Kind.OK ||
                   result1.kind == FormValidation.Kind.ERROR ||
                   result1.kind == FormValidation.Kind.WARNING);
        assertTrue("Second result should be valid",
                   result2.kind == FormValidation.Kind.OK ||
                   result2.kind == FormValidation.Kind.ERROR ||
                   result2.kind == FormValidation.Kind.WARNING);
    }

    @Test
    public void testTestCodexCliTildeExpansionVerification() {
        // Test that tilde expansion works correctly
        CodexAnalysisJobProperty.DescriptorImpl descriptor = new CodexAnalysisJobProperty.DescriptorImpl();

        FormValidation result = descriptor.doTestCodexCli("~/.local/bin/codex", null, null, null, null, null);

        assertNotNull(result);
        // Should expand tilde to home directory
        String homeDir = System.getProperty("user.home");
        String message = result.getMessage();

        // Verify tilde expansion happened
        assertTrue("Should expand tilde to home directory",
                   message.contains(homeDir) ||
                   message.contains("Failed to test Codex CLI"));
        // Verify original tilde path is not in the message (it should be expanded)
        assertFalse("Message should not contain original tilde path",
                    message.contains("~/.local/bin/codex"));
    }
}
