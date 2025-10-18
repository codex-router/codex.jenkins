package io.jenkins.plugins.codex;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CodexAnalysisStepTest {

    @Mock
    private StepContext stepContext;

    @Mock
    private Launcher launcher;

    @Mock
    private TaskListener taskListener;

    @Mock
    private EnvVars envVars;

    @Mock
    private FilePath workspace;

    private CodexAnalysisStep step;

    @Before
    public void setUp() {
        step = new CodexAnalysisStep();
        step.setContent("Test content");
        step.setAnalysisType("general");
        step.setPrompt("Test prompt");
    }

    @Test
    public void testStepConfiguration() {
        assertEquals("Test content", step.getContent());
        assertEquals("general", step.getAnalysisType());
        assertEquals("Test prompt", step.getPrompt());
    }

    @Test
    public void testStepDescriptor() {
        CodexAnalysisStep.DescriptorImpl descriptor = step.getDescriptor();
        assertNotNull(descriptor);
        assertEquals("codexAnalysis", descriptor.getFunctionName());
        assertEquals("Codex Analysis", descriptor.getDisplayName());
        assertFalse(descriptor.takesImplicitBlockArgument());
        assertFalse(descriptor.isAdvanced());
    }

    @Test
    public void testStepSetters() {
        step.setModel("gpt-4");
        step.setTimeoutSeconds(180);
        step.setIncludeContext(true);

        assertEquals("gpt-4", step.getModel());
        assertEquals(180, step.getTimeoutSeconds());
        assertTrue(step.isIncludeContext());
    }

    @Test
    public void testAdditionalParams() {
        step.setAdditionalParams(null);
        assertNotNull(step.getAdditionalParams());
        assertTrue(step.getAdditionalParams().isEmpty());

        // Test with actual params
        // Note: This would require more complex setup for actual execution
    }
}
