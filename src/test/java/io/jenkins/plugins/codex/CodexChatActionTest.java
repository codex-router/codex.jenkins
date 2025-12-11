package io.jenkins.plugins.codex;

import hudson.model.Job;
import hudson.model.Run;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CodexChatActionTest {

    @Mock
    private Run<?, ?> run;

    @Mock
    private hudson.model.Job<?, ?> job;

    @Mock
    private CodexAnalysisJobProperty jobProperty;

    @Mock
    private CodexAnalysisPlugin globalPlugin;

    private CodexChatAction action;

    @Before
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setUp() {
        when(run.getParent()).thenReturn((Job) job);
        action = new CodexChatAction(run);
    }

    @Test
    public void testActionCreation() {
        assertNotNull(action);
        assertEquals(run, action.getRun());
    }

    @Test
    public void testGetIconFileName() {
        String iconFileName = action.getIconFileName();
        assertNotNull(iconFileName);
        assertEquals("/plugin/codex-analysis/images/codex-chat.svg", iconFileName);
    }

    @Test
    public void testGetDisplayName() {
        String displayName = action.getDisplayName();
        assertNotNull(displayName);
        assertEquals("Codex Chat", displayName);
    }

    @Test
    public void testGetUrlName() {
        String urlName = action.getUrlName();
        assertNotNull(urlName);
        assertEquals("codex-chat", urlName);
    }

    @Test
    public void testGetEffectiveCodexCliPathWithJobConfig() {
        // Test with job-level configuration enabled
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenReturn(jobProperty);
        when(jobProperty.isUseJobConfig()).thenReturn(true);
        when(jobProperty.getCodexCliPath()).thenReturn("/job/path/codex");

        String cliPath = action.getEffectiveCodexCliPath();
        assertEquals("/job/path/codex", cliPath);
    }

    @Test
    public void testGetEffectiveCodexCliPathWithJobConfigButEmptyPath() {
        // Test with job-level configuration enabled but empty path
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenReturn(jobProperty);
        when(jobProperty.isUseJobConfig()).thenReturn(true);
        when(jobProperty.getCodexCliPath()).thenReturn("");

        // Mock global plugin using static mock
        try (MockedStatic<CodexAnalysisPlugin> mockedPlugin = mockStatic(CodexAnalysisPlugin.class)) {
            mockedPlugin.when(CodexAnalysisPlugin::get).thenReturn(globalPlugin);
            when(globalPlugin.getCodexCliPath()).thenReturn("/global/path/codex");

            String cliPath = action.getEffectiveCodexCliPath();
            assertEquals("/global/path/codex", cliPath);
        }
    }

    @Test
    public void testGetEffectiveCodexCliPathWithJobConfigButNullPath() {
        // Test with job-level configuration enabled but null path
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenReturn(jobProperty);
        when(jobProperty.isUseJobConfig()).thenReturn(true);
        when(jobProperty.getCodexCliPath()).thenReturn(null);

        // Mock global plugin using static mock
        try (MockedStatic<CodexAnalysisPlugin> mockedPlugin = mockStatic(CodexAnalysisPlugin.class)) {
            mockedPlugin.when(CodexAnalysisPlugin::get).thenReturn(globalPlugin);
            when(globalPlugin.getCodexCliPath()).thenReturn("/global/path/codex");

            String cliPath = action.getEffectiveCodexCliPath();
            assertEquals("/global/path/codex", cliPath);
        }
    }

    @Test
    public void testGetEffectiveCodexCliPathWithoutJobConfig() {
        // Test without job-level configuration
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenReturn(null);

        // Mock global plugin using static mock
        try (MockedStatic<CodexAnalysisPlugin> mockedPlugin = mockStatic(CodexAnalysisPlugin.class)) {
            mockedPlugin.when(CodexAnalysisPlugin::get).thenReturn(globalPlugin);
            when(globalPlugin.getCodexCliPath()).thenReturn("/global/path/codex");

            String cliPath = action.getEffectiveCodexCliPath();
            assertEquals("/global/path/codex", cliPath);
        }
    }

    @Test
    public void testGetEffectiveCodexCliPathWithJobConfigDisabled() {
        // Test with job-level configuration disabled
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenReturn(jobProperty);
        when(jobProperty.isUseJobConfig()).thenReturn(false);

        // Mock global plugin using static mock
        try (MockedStatic<CodexAnalysisPlugin> mockedPlugin = mockStatic(CodexAnalysisPlugin.class)) {
            mockedPlugin.when(CodexAnalysisPlugin::get).thenReturn(globalPlugin);
            when(globalPlugin.getCodexCliPath()).thenReturn("/global/path/codex");

            String cliPath = action.getEffectiveCodexCliPath();
            assertEquals("/global/path/codex", cliPath);
        }
    }

    @Test
    public void testGetEffectiveCodexCliPathWithNoGlobalPlugin() {
        // Test when global plugin is not available
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenReturn(null);

        // Mock global plugin to return null
        try (MockedStatic<CodexAnalysisPlugin> mockedPlugin = mockStatic(CodexAnalysisPlugin.class)) {
            mockedPlugin.when(CodexAnalysisPlugin::get).thenReturn(null);

            String cliPath = action.getEffectiveCodexCliPath();
            assertEquals("~/.local/bin/codex", cliPath); // Default fallback
        }
    }

    @Test
    public void testIsCodexAvailableWithValidPath() {
        // Test when CLI path is available
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenReturn(jobProperty);
        when(jobProperty.isUseJobConfig()).thenReturn(true);
        when(jobProperty.getCodexCliPath()).thenReturn("/valid/path/codex");

        boolean available = action.isCodexAvailable();
        assertTrue(available);
    }

    @Test
    public void testIsCodexAvailableWithEmptyPath() {
        // Test when CLI path is empty
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenReturn(null);

        // Mock global plugin using static mock
        try (MockedStatic<CodexAnalysisPlugin> mockedPlugin = mockStatic(CodexAnalysisPlugin.class)) {
            mockedPlugin.when(CodexAnalysisPlugin::get).thenReturn(globalPlugin);
            when(globalPlugin.getCodexCliPath()).thenReturn("");

            boolean available = action.isCodexAvailable();
            assertFalse(available);
        }
    }

    @Test
    public void testIsCodexAvailableWithNullPath() {
        // Test when CLI path is null
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenReturn(null);

        // Mock global plugin using static mock
        try (MockedStatic<CodexAnalysisPlugin> mockedPlugin = mockStatic(CodexAnalysisPlugin.class)) {
            mockedPlugin.when(CodexAnalysisPlugin::get).thenReturn(globalPlugin);
            when(globalPlugin.getCodexCliPath()).thenReturn(null);

            boolean available = action.isCodexAvailable();
            assertFalse(available);
        }
    }

    @Test
    public void testIsCodexAvailableWithException() {
        // Test when exception occurs
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenThrow(new RuntimeException("Test exception"));

        boolean available = action.isCodexAvailable();
        assertFalse(available);
    }

    @Test
    public void testGetEffectiveCodexCliPathWithWhitespacePath() {
        // Test with whitespace in path (should be trimmed by getter)
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenReturn(jobProperty);
        when(jobProperty.isUseJobConfig()).thenReturn(true);
        when(jobProperty.getCodexCliPath()).thenReturn("  /path/with/whitespace/codex  ");

        String cliPath = action.getEffectiveCodexCliPath();
        // The path should be returned as-is (trimming happens in the getter if needed)
        assertNotNull(cliPath);
    }

    @Test
    public void testGetEffectiveCodexCliPathFallbackChain() {
        // Test the complete fallback chain: job config -> global config -> default
        when(job.getProperty(CodexAnalysisJobProperty.class)).thenReturn(null);

        // Mock global plugin using static mock
        try (MockedStatic<CodexAnalysisPlugin> mockedPlugin = mockStatic(CodexAnalysisPlugin.class)) {
            // First, test with no global plugin
            mockedPlugin.when(CodexAnalysisPlugin::get).thenReturn(null);

            String cliPath = action.getEffectiveCodexCliPath();
            assertEquals("~/.local/bin/codex", cliPath);

            // Then test with global plugin
            mockedPlugin.when(CodexAnalysisPlugin::get).thenReturn(globalPlugin);
            when(globalPlugin.getCodexCliPath()).thenReturn("/global/codex");

            cliPath = action.getEffectiveCodexCliPath();
            assertEquals("/global/codex", cliPath);
        }
    }

    @Test
    public void testActionImplementsActionInterface() {
        // Verify that CodexChatAction implements Action interface
        assertTrue(action instanceof hudson.model.Action);
    }

    @Test
    public void testActionUrlNameFormat() {
        // Verify URL name format is valid
        String urlName = action.getUrlName();
        assertNotNull(urlName);
        assertFalse(urlName.isEmpty());
        // URL name should not contain spaces or special characters
        assertTrue("URL name should be valid", urlName.matches("^[a-z0-9-]+$"));
    }

    @Test
    public void testActionIconFileNameFormat() {
        // Verify icon file name format
        String iconFileName = action.getIconFileName();
        assertNotNull(iconFileName);
        // Icon file name can be null (to hide the action) or a valid string
        // In our case, it should be the full plugin path to the SVG icon
        assertEquals("/plugin/codex-analysis/images/codex-chat.svg", iconFileName);
    }
}

