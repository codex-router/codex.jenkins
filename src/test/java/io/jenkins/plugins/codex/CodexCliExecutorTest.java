package io.jenkins.plugins.codex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class CodexCliExecutorTest {

    @Test
    public void testCodexAnalysisResultSuccess() {
        CodexCliExecutor.CodexAnalysisResult result = new CodexCliExecutor.CodexAnalysisResult(
            "Test output",
            "",
            true
        );

        assertTrue(result.isSuccess());
        assertEquals("Test output", result.getOutput());
        assertEquals("", result.getError());
    }

    @Test
    public void testCodexAnalysisResultFailure() {
        CodexCliExecutor.CodexAnalysisResult result = new CodexCliExecutor.CodexAnalysisResult(
            "",
            "Test error",
            false
        );

        assertFalse(result.isSuccess());
        assertEquals("", result.getOutput());
        assertEquals("Test error", result.getError());
    }

    @Test
    public void testCodexAnalysisResultWithBothOutputs() {
        CodexCliExecutor.CodexAnalysisResult result = new CodexCliExecutor.CodexAnalysisResult(
            "Standard output",
            "Error output",
            true
        );

        assertTrue(result.isSuccess());
        assertEquals("Standard output", result.getOutput());
        assertEquals("Error output", result.getError());
    }

    @Test
    public void testCodexAnalysisResultNullValues() {
        CodexCliExecutor.CodexAnalysisResult result = new CodexCliExecutor.CodexAnalysisResult(
            null,
            null,
            false
        );

        assertFalse(result.isSuccess());
        assertNull(result.getOutput());
        assertNull(result.getError());
    }

    @Test
    public void testCodexAnalysisResultEmptyStrings() {
        CodexCliExecutor.CodexAnalysisResult result = new CodexCliExecutor.CodexAnalysisResult(
            "",
            "",
            true
        );

        assertTrue(result.isSuccess());
        assertEquals("", result.getOutput());
        assertEquals("", result.getError());
    }

    @Test
    public void testCodexAnalysisResultMultilineOutput() {
        String multilineOutput = "Line 1\nLine 2\nLine 3";
        CodexCliExecutor.CodexAnalysisResult result = new CodexCliExecutor.CodexAnalysisResult(
            multilineOutput,
            "",
            true
        );

        assertTrue(result.isSuccess());
        assertEquals(multilineOutput, result.getOutput());
        assertTrue(result.getOutput().contains("Line 1"));
        assertTrue(result.getOutput().contains("Line 2"));
        assertTrue(result.getOutput().contains("Line 3"));
    }
}
