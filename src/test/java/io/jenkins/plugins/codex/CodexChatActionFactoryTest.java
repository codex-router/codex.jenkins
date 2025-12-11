package io.jenkins.plugins.codex;

import hudson.model.Run;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CodexChatActionFactoryTest {

    @Mock
    private Run<?, ?> run;

    private CodexChatActionFactory factory;

    @Before
    public void setUp() {
        factory = new CodexChatActionFactory();
    }

    @Test
    public void testFactoryCreation() {
        assertNotNull(factory);
    }

    @Test
    public void testType() {
        Class<Run> type = factory.type();
        assertNotNull(type);
        assertEquals(Run.class, type);
    }

    @Test
    public void testCreateFor() {
        Collection<? extends CodexChatAction> actions = factory.createFor(run);

        assertNotNull(actions);
        assertEquals(1, actions.size());

        CodexChatAction action = actions.iterator().next();
        assertNotNull(action);
        assertEquals(run, action.getRun());
    }

    @Test
    public void testCreateForMultipleCalls() {
        // Test that factory creates new action instances for each call
        Collection<? extends CodexChatAction> actions1 = factory.createFor(run);
        Collection<? extends CodexChatAction> actions2 = factory.createFor(run);

        assertNotNull(actions1);
        assertNotNull(actions2);
        assertEquals(1, actions1.size());
        assertEquals(1, actions2.size());

        // Actions should be different instances
        CodexChatAction action1 = actions1.iterator().next();
        CodexChatAction action2 = actions2.iterator().next();
        assertNotSame("Actions should be different instances", action1, action2);
        // But they should have the same run
        assertEquals(action1.getRun(), action2.getRun());
    }

    @Test
    public void testCreateForWithNullRun() {
        // Test that factory handles null run gracefully
        try {
            Collection<? extends CodexChatAction> actions = factory.createFor(null);
            // Should either return empty collection or throw exception
            // In our implementation, it creates an action with null run
            assertNotNull(actions);
        } catch (Exception e) {
            // If exception is thrown, that's also acceptable behavior
            assertNotNull(e);
        }
    }

    @Test
    public void testFactoryIsExtension() {
        // Verify that factory is annotated as Extension
        assertTrue("Factory should be an Extension",
                   factory.getClass().isAnnotationPresent(hudson.Extension.class));
    }

    @Test
    public void testFactoryImplementsTransientActionFactory() {
        // Verify that factory implements TransientActionFactory
        assertTrue("Factory should implement TransientActionFactory",
                   factory instanceof jenkins.model.TransientActionFactory);
    }

    @Test
    public void testCreateForReturnsSingletonCollection() {
        // Test that createFor returns a singleton collection
        Collection<? extends CodexChatAction> actions = factory.createFor(run);

        assertNotNull(actions);
        assertEquals(1, actions.size());

        // Verify it's a singleton (only one element)
        int count = 0;
        for (CodexChatAction action : actions) {
            count++;
            assertNotNull(action);
        }
        assertEquals(1, count);
    }

    @Test
    public void testCreateForActionProperties() {
        // Test that created action has correct properties
        Collection<? extends CodexChatAction> actions = factory.createFor(run);
        CodexChatAction action = actions.iterator().next();

        assertEquals(run, action.getRun());
        assertEquals("Codex Chat", action.getDisplayName());
        assertEquals("codex-chat", action.getUrlName());
        assertEquals("/plugin/codex-analysis/images/codex-chat.svg", action.getIconFileName());
    }

    @Test
    public void testFactoryTypeMatchesRun() {
        // Test that factory type matches Run class
        Class<Run> type = factory.type();
        assertEquals(Run.class, type);

        // Verify that the run mock is an instance of the type
        assertTrue("Run should be instance of factory type", type.isInstance(run));
    }

    @Test
    public void testCreateForWithDifferentRuns() {
        // Test that factory creates different actions for different runs
        Run<?, ?> run1 = mock(Run.class);
        Run<?, ?> run2 = mock(Run.class);

        Collection<? extends CodexChatAction> actions1 = factory.createFor(run1);
        Collection<? extends CodexChatAction> actions2 = factory.createFor(run2);

        CodexChatAction action1 = actions1.iterator().next();
        CodexChatAction action2 = actions2.iterator().next();

        assertNotSame("Actions should be different instances", action1, action2);
        assertNotSame("Runs should be different", action1.getRun(), action2.getRun());
        assertEquals(run1, action1.getRun());
        assertEquals(run2, action2.getRun());
    }

    @Test
    public void testFactoryExtensionAnnotation() {
        // Verify the Extension annotation is present
        hudson.Extension extension = factory.getClass().getAnnotation(hudson.Extension.class);
        assertNotNull("Factory should have @Extension annotation", extension);
    }
}

