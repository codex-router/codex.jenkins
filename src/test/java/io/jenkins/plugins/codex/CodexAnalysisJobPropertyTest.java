package io.jenkins.plugins.codex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class CodexAnalysisJobPropertyTest {

    private CodexAnalysisJobProperty jobProperty;

    @Test
    public void testJobPropertyCreation() {
        jobProperty = new CodexAnalysisJobProperty(
            "job-codex", "https://example.com/codex-download", "testuser", "testpass", "job-config.toml", "job-mcp.toml",
            "job-model", 180, false, "job-litellm-key", new ArrayList<>(), true
        );

        assertEquals("job-codex", jobProperty.getCodexCliPath());
        assertEquals("https://example.com/codex-download", jobProperty.getCodexCliDownloadUrl());
        assertEquals("testuser", jobProperty.getCodexCliDownloadUsername());
        assertEquals("testpass", jobProperty.getCodexCliDownloadPassword());
        assertEquals("job-config.toml", jobProperty.getConfigPath());
        assertEquals("job-mcp.toml", jobProperty.getMcpServersPath());
        assertEquals("job-model", jobProperty.getDefaultModel());
        assertEquals(180, jobProperty.getTimeoutSeconds());
        assertFalse(jobProperty.isEnableMcpServers());
        assertEquals("job-litellm-key", jobProperty.getLitellmApiKey());
        assertTrue(jobProperty.isUseJobConfig());
    }

    @Test
    public void testEffectiveValuesWithJobConfig() {
        jobProperty = new CodexAnalysisJobProperty(
            "job-codex", "https://example.com/codex-download", "testuser", "testpass", "job-config.toml", "job-mcp.toml",
            "job-model", 180, false, "job-litellm-key", new ArrayList<>(), true
        );

        // When useJobConfig is true, job values should be returned
        assertEquals("job-codex", jobProperty.getEffectiveCodexCliPath());
        assertEquals("job-config.toml", jobProperty.getEffectiveConfigPath());
        assertEquals("job-mcp.toml", jobProperty.getEffectiveMcpServersPath());
        assertEquals("job-model", jobProperty.getEffectiveDefaultModel());
        assertEquals(180, jobProperty.getEffectiveTimeoutSeconds());
        assertFalse(jobProperty.getEffectiveEnableMcpServers());
    }

    @Test
    public void testEffectiveValuesWithoutJobConfig() {
        jobProperty = new CodexAnalysisJobProperty(
            "job-codex", "https://example.com/codex-download", "testuser", "testpass", "job-config.toml", "job-mcp.toml",
            "job-model", 180, false, "job-litellm-key", new ArrayList<>(), false
        );

        // When useJobConfig is false, the raw values should still be set
        // but the effective methods would use global values in real usage
        assertEquals("job-codex", jobProperty.getCodexCliPath());
        assertEquals("job-config.toml", jobProperty.getConfigPath());
        assertEquals("job-mcp.toml", jobProperty.getMcpServersPath());
        assertEquals("job-model", jobProperty.getDefaultModel());
        assertEquals(180, jobProperty.getTimeoutSeconds());
        assertFalse(jobProperty.isUseJobConfig());
    }

    @Test
    public void testEffectiveValuesWithEmptyJobConfig() {
        jobProperty = new CodexAnalysisJobProperty(
            "", "", "", "", "", "", "", 0, false, "", new ArrayList<>(), true
        );

        // When job config is enabled but values are empty, the raw values should be empty
        // The effective methods would fall back to global in real usage, but in tests
        // we can't access Jenkins instance, so we test the raw values
        assertEquals("", jobProperty.getCodexCliPath());
        assertEquals("", jobProperty.getConfigPath());
        assertEquals("", jobProperty.getMcpServersPath());
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
            "initial-codex", "https://example.com/initial-download", "initialuser", "initialpass", "initial-config.toml", "initial-mcp.toml",
            "initial-model", 120, true, "initial-litellm-key", new ArrayList<>(), false
        );

        // Test setters
        jobProperty.setCodexCliPath("new-codex");
        jobProperty.setCodexCliDownloadUrl("https://example.com/new-download");
        jobProperty.setCodexCliDownloadUsername("newuser");
        jobProperty.setCodexCliDownloadPassword("newpass");
        jobProperty.setConfigPath("new-config.toml");
        jobProperty.setMcpServersPath("new-mcp.toml");
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
        assertEquals("new-mcp.toml", jobProperty.getMcpServersPath());
        assertEquals("new-model", jobProperty.getDefaultModel());
        assertEquals(240, jobProperty.getTimeoutSeconds());
        assertFalse(jobProperty.isEnableMcpServers());
        assertEquals("new-litellm-key", jobProperty.getLitellmApiKey());
        assertTrue(jobProperty.isUseJobConfig());
    }
}
