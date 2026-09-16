package com.github.catatafishen.agentbridge.nativeagent.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItfTraceTest {
    @TempDir
    Path tempDir;
    @Test
    void readsGeneratedTraceAndDeduplicatesQuintVariableList() throws IOException {
        ItfTrace trace;
        try (InputStream stream = ItfTraceTest.class.getResourceAsStream(
            "/nativeagent/model/traces/agent_coarse_0.itf.json")) {
            if (stream == null) throw new AssertionError("missing trace resource");
            trace = ItfTrace.read(stream);
        }

        assertEquals(List.of("mbt::actionTaken", "mbt::nondetPicks", "prev", "s"), trace.variables());
        assertEquals(21, trace.states().size());
        assertEquals("stepCorpus", trace.states().get(0).action());
        assertEquals("newGeneration", trace.states().get(1).action());
    }

    @Test
    void rejectsUnknownActionsAndUnexpectedStateKeys() throws IOException {
        assertInvalid("""
            {"#meta":{},"vars":["mbt::actionTaken","mbt::nondetPicks","s"],"states":[
              {"#meta":{"index":0},"mbt::actionTaken":"stepCorpus","mbt::nondetPicks":{},"s":{},"extra":true}
            ]}
            """);
        assertInvalid("""
            {"#meta":{},"vars":["mbt::actionTaken","mbt::nondetPicks","s"],"states":[
              {"#meta":{"index":0},"mbt::actionTaken":"stepCorpus","mbt::nondetPicks":{},"s":{}},
              {"#meta":{"index":1},"mbt::actionTaken":"unknown","mbt::nondetPicks":{},"s":{}}
            ]}
            """);
    }

    @Test
    void rejectsUnknownWrappersAndNonIntegerNumbers() throws IOException {
        assertInvalid("""
            {"#meta":{},"vars":["mbt::actionTaken","mbt::nondetPicks","s"],"states":[
              {"#meta":{"index":0},"mbt::actionTaken":"stepCorpus","mbt::nondetPicks":{},"s":{"#unknown":[]}}
            ]}
            """);
        assertInvalid("""
            {"#meta":{},"vars":["mbt::actionTaken","mbt::nondetPicks","s"],"states":[
              {"#meta":{"index":0.0},"mbt::actionTaken":"stepCorpus","mbt::nondetPicks":{},"s":{}}
            ]}
            """);
    }
    @Test
    void rejectsNonJsonWhitespaceBetweenTokens() throws IOException {
        assertInvalid("{\"#meta\":{}," + ((char) 11)
            + "\"vars\":[],\"states\":[]}");
    }

    @Test
    void acceptsReadAdapterActionsOnlyWhenMetadataExcludesThemFromReplay() throws IOException {
        String json = "{\"#meta\":{\"read_adapter_only\":true},\"vars\":[\"mbt::actionTaken\",\"mbt::nondetPicks\",\"s\"],\"states\":["
            + "{\"#meta\":{\"index\":0},\"mbt::actionTaken\":\"stepCorpus\",\"mbt::nondetPicks\":{},\"s\":{}},"
            + "{\"#meta\":{\"index\":1},\"mbt::actionTaken\":\"startRead\",\"mbt::nondetPicks\":{},\"s\":{}}]}";
        Path file = Files.createTempFile(tempDir, "read-adapter-itf-", ".json");
        try {
            Files.writeString(file, json);
            ItfTrace trace = ItfTrace.read(file);
            assertFalse(trace.states().get(1).replayable());
        } finally {
            Files.deleteIfExists(file);
        }
        assertInvalid(json.replace("read_adapter_only\":true", "read_adapter_only\":false"));
    }

    @Test
    void acceptsZeroNegativeAndLargeBigintsAndRejectsMalformedDigits() throws IOException {
        for (String value : List.of("0", "-0", "-123456789012345678901234567890")) {
            assertDoesNotThrow(() -> readBigint(value));
        }
        for (String value : List.of("", "+1", "01", "1.0", "1e3")) {
            assertInvalid(bigintTrace(value));
        }
    }

    private ItfTrace readBigint(String value) throws IOException {
        Path file = Files.createTempFile(tempDir, "valid-itf-", ".json");
        try {
            Files.writeString(file, bigintTrace(value));
            return ItfTrace.read(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static String bigintTrace(String value) {
        return "{\"#meta\":{},\"vars\":[\"mbt::actionTaken\",\"mbt::nondetPicks\",\"s\"],\"states\":["
            + "{\"#meta\":{\"index\":0},\"mbt::actionTaken\":\"stepCorpus\","
            + "\"mbt::nondetPicks\":{},\"s\":{\"#bigint\":\"" + value + "\"}}]}";
    }


    private void assertInvalid(String json) throws IOException {
        Path file = Files.createTempFile(tempDir, "invalid-itf-", ".json");
        try {
            Files.writeString(file, json);
            assertThrows(IllegalArgumentException.class, () -> ItfTrace.read(file));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
