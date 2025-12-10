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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CodexChatStepTest {

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

    private CodexChatStep step;

    @Before
    public void setUp() {
        step = new CodexChatStep();
        step.setInitialMessage("Hello, Codex!");
        step.setContext("Test context");
        step.setModel("gpt-4");
        step.setTimeoutSeconds(300);
    }

    @Test
    public void testStepConfiguration() {
        assertEquals("Hello, Codex!", step.getInitialMessage());
        assertEquals("Test context", step.getContext());
        assertEquals("gpt-4", step.getModel());
        assertEquals(300, step.getTimeoutSeconds());
    }

    @Test
    public void testStepDescriptor() {
        CodexChatStep.DescriptorImpl descriptor = new CodexChatStep.DescriptorImpl();
        assertNotNull(descriptor);
        assertEquals("codexChat", descriptor.getFunctionName());
        assertEquals("Codex Interactive Chat", descriptor.getDisplayName());
        assertFalse(descriptor.takesImplicitBlockArgument());
        assertFalse(descriptor.isAdvanced());
    }

    @Test
    public void testStepSetters() {
        step.setInitialMessage("New message");
        step.setContext("New context");
        step.setModel("claude-3-opus");
        step.setTimeoutSeconds(600);

        assertEquals("New message", step.getInitialMessage());
        assertEquals("New context", step.getContext());
        assertEquals("claude-3-opus", step.getModel());
        assertEquals(600, step.getTimeoutSeconds());
    }

    @Test
    public void testAdditionalParams() {
        step.setAdditionalParams(null);
        assertNotNull(step.getAdditionalParams());
        assertTrue(step.getAdditionalParams().isEmpty());

        Map<String, String> params = new HashMap<>();
        params.put("temperature", "0.7");
        params.put("max_tokens", "2000");
        step.setAdditionalParams(params);

        assertEquals(2, step.getAdditionalParams().size());
        assertEquals("0.7", step.getAdditionalParams().get("temperature"));
        assertEquals("2000", step.getAdditionalParams().get("max_tokens"));
    }

    @Test
    public void testStepWithEmptyValues() {
        CodexChatStep emptyStep = new CodexChatStep();
        assertNull(emptyStep.getInitialMessage());
        assertNull(emptyStep.getContext());
        assertNull(emptyStep.getModel());
        assertEquals(120, emptyStep.getTimeoutSeconds()); // Default value
        assertNotNull(emptyStep.getAdditionalParams());
    }

    @Test
    public void testStepDescriptorRequiredContext() {
        CodexChatStep.DescriptorImpl descriptor = new CodexChatStep.DescriptorImpl();
        Set<? extends Class<?>> requiredContext = descriptor.getRequiredContext();

        assertNotNull(requiredContext);
        assertTrue(requiredContext.contains(hudson.model.Run.class));
        assertTrue(requiredContext.contains(TaskListener.class));
        assertTrue(requiredContext.contains(Launcher.class));
        assertTrue(requiredContext.contains(EnvVars.class));
        assertTrue(requiredContext.contains(FilePath.class));
    }
}
