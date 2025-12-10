package io.jenkins.plugins.codex;

import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class CodexChatBuilderTest {

    @Test
    public void testBuilderCreation() {
        CodexChatBuilder builder = new CodexChatBuilder(
            "Hello, Codex!",
            "Test context",
            "gpt-4",
            300,
            "temperature=0.7\nmax_tokens=2000"
        );

        assertEquals("Hello, Codex!", builder.getInitialMessage());
        assertEquals("Test context", builder.getContext());
        assertEquals("gpt-4", builder.getModel());
        assertEquals(300, builder.getTimeoutSeconds());
        assertEquals("temperature=0.7\nmax_tokens=2000", builder.getAdditionalParams());
    }

    @Test
    public void testBuilderWithDefaultTimeout() {
        CodexChatBuilder builder = new CodexChatBuilder(
            "Test message",
            null,
            null,
            0, // Should default to 120
            null
        );

        assertEquals(120, builder.getTimeoutSeconds());
    }

    @Test
    public void testBuilderWithEmptyValues() {
        CodexChatBuilder builder = new CodexChatBuilder(
            null,
            null,
            null,
            0,
            null
        );

        assertNull(builder.getInitialMessage());
        assertNull(builder.getContext());
        assertNull(builder.getModel());
        assertEquals(120, builder.getTimeoutSeconds());
        assertNull(builder.getAdditionalParams());
    }

    @Test
    public void testBuilderWithCustomTimeout() {
        CodexChatBuilder builder = new CodexChatBuilder(
            "Message",
            "Context",
            "model",
            600,
            ""
        );

        assertEquals(600, builder.getTimeoutSeconds());
    }

    @Test
    public void testDescriptor() {
        CodexChatBuilder.DescriptorImpl descriptor = new CodexChatBuilder.DescriptorImpl();
        assertNotNull(descriptor);
        assertEquals("Codex Interactive Chat", descriptor.getDisplayName());
        assertTrue(descriptor.isApplicable(AbstractProject.class));
    }

    @Test
    public void testTimeoutValidation() {
        CodexChatBuilder.DescriptorImpl descriptor = new CodexChatBuilder.DescriptorImpl();

        // Test valid timeout
        FormValidation result = descriptor.doCheckTimeoutSeconds("300");
        assertNotNull(result);
        assertEquals(FormValidation.Kind.OK, result.kind);

        // Test invalid timeout (negative)
        result = descriptor.doCheckTimeoutSeconds("-10");
        assertNotNull(result);
        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertTrue(result.getMessage().contains("must be positive"));

        // Test invalid timeout (zero)
        result = descriptor.doCheckTimeoutSeconds("0");
        assertNotNull(result);
        assertEquals(FormValidation.Kind.ERROR, result.kind);

        // Test invalid timeout (too large)
        result = descriptor.doCheckTimeoutSeconds("5000");
        assertNotNull(result);
        assertEquals(FormValidation.Kind.WARNING, result.kind);
        assertTrue(result.getMessage().contains("Very long timeout"));

        // Test invalid format
        result = descriptor.doCheckTimeoutSeconds("not-a-number");
        assertNotNull(result);
        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertTrue(result.getMessage().contains("Invalid timeout value"));
    }

    @Test
    public void testAdditionalParamsParsing() {
        // This tests the private parseAdditionalParams method indirectly
        // by creating a builder and verifying it stores the params correctly
        CodexChatBuilder builder = new CodexChatBuilder(
            "Message",
            "Context",
            "model",
            120,
            "key1=value1\nkey2=value2;key3=value3"
        );

        assertNotNull(builder.getAdditionalParams());
        // The parsing happens in performChat, but we can verify the string is stored
        assertTrue(builder.getAdditionalParams().contains("key1=value1"));
    }

    @Test
    public void testBuilderWithNullAdditionalParams() {
        CodexChatBuilder builder = new CodexChatBuilder(
            "Message",
            "Context",
            "model",
            120,
            null
        );

        assertNull(builder.getAdditionalParams());
    }
}
