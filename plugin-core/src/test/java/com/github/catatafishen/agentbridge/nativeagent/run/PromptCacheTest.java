package com.github.catatafishen.agentbridge.nativeagent.run;

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class PromptCacheTest {
    private static final ModelItemRenderer RENDERER = (message, index) -> List.of(
        new CacheField("item", ("|" + index + ":" + message).getBytes(StandardCharsets.UTF_8))
    );

    @Test
    void cacheGenerationRequiresExplicitReset() {
        CacheGeneration initial = generation("initial");
        assertEquals(CacheGeneration.CacheReset.SessionStart.INSTANCE, initial.reset());
        RunSession prior = new RunSession();
        RunSession.ActiveRun active = ((RunSession.StartResult.Started) prior.start(
            new RunMessage.User("request"), RunLimits.mvp(), Instant.EPOCH, Instant::now)).run();

        assertThrows(IllegalStateException.class, () -> CacheGeneration.replacement(prior,
            CacheGeneration.CacheReset.ExplicitContextReset.INSTANCE,
            new CacheGeneration.Id("blocked"), head()));

        prior.acceptTurn(active, new ValidatedAssistantTurn.Complete(
            new RunMessage.Assistant("done", RunMessage.Completion.COMPLETE, List.of())));
        CacheGeneration modelChange = CacheGeneration.replacement(prior,
            new CacheGeneration.CacheReset.ModelChange("model-a", "model-b"),
            new CacheGeneration.Id("model-change"), head());
        CacheGeneration explicit = CacheGeneration.replacement(prior,
            CacheGeneration.CacheReset.ExplicitContextReset.INSTANCE,
            new CacheGeneration.Id("explicit"), head());
        assertInstanceOf(CacheGeneration.CacheReset.ModelChange.class, modelChange.reset());
        assertEquals(CacheGeneration.CacheReset.ExplicitContextReset.INSTANCE, explicit.reset());
        assertNotEquals(initial.id(), modelChange.id());
        assertThrows(IllegalArgumentException.class, () -> CacheGeneration.replacement(prior,
            CacheGeneration.CacheReset.SessionStart.INSTANCE,
            new CacheGeneration.Id("invalid"), head()));
    }

    @Test
    void dispatchRejectsNonPrefixAndRetryReusesBytes() {
        CacheGeneration generation = generation("same");
        List<Integer> renderedIndexes = new ArrayList<>();
        ModelItemRenderer counting = (message, index) -> {
            renderedIndexes.add(index);
            return RENDERER.render(message, index);
        };
        RunMessage.User user = new RunMessage.User("request");
        CacheRequest first = generation.requestFor(new RunSession.HistorySnapshot(List.of(user)), counting);
        CacheRequest retry = generation.requestFor(new RunSession.HistorySnapshot(List.of(user)), counting);
        assertSame(first, retry, "retry must reuse the immutable request object");

        RunSession session = new RunSession();
        RunSession.ActiveRun run = start(session, user);
        session.updateProvisional(run, "rejected partial");
        assertSame(first, generation.requestFor(session.history(), counting),
            "provisional output must not change cache bytes");
        session.acceptTurn(run, new ValidatedAssistantTurn.Rejected(
            ValidatedAssistantTurn.ProtocolFailure.MALFORMED_JSON, "rejected partial"));
        assertSame(first, generation.requestFor(session.history(), counting),
            "rejected output must not change cache bytes");

        RunSession.ActiveRun truncated = start(session, new RunMessage.User("truncate"));
        session.acceptTurn(truncated, new ValidatedAssistantTurn.LengthCalls(new RunMessage.Assistant(
            "cut", RunMessage.Completion.INCOMPLETE, List.of(executable("truncated")))));
        CacheRequest afterTruncation = assertExtends(generation, first, session, counting);

        RunSession.ActiveRun toolRound = start(session, new RunMessage.User("tools"));
        session.acceptTurn(toolRound, new ValidatedAssistantTurn.Complete(new RunMessage.Assistant(
            "", RunMessage.Completion.COMPLETE, List.of(executable("executed")))));
        RunSession.CallStep.Execute step = (RunSession.CallStep.Execute) session.nextCall(toolRound);
        assertInstanceOf(CallAdmission.Result.Executed.class, step.admission().execute(() -> { }));
        session.recordToolResult(toolRound, new RunMessage.ToolResult(step.call().id(),
            new ToolOutcome.Completed("ok", List.of())));
        assertInstanceOf(RunSession.CallStep.Complete.class, session.nextCall(toolRound));
        CacheRequest afterToolRound = assertExtends(generation, afterTruncation, session, counting);
        session.acceptTurn(toolRound, new ValidatedAssistantTurn.Complete(
            new RunMessage.Assistant("done", RunMessage.Completion.COMPLETE, List.of())));

        RunSession.ActiveRun stopped = start(session, new RunMessage.User("stop"));
        session.acceptTurn(stopped, new ValidatedAssistantTurn.Complete(new RunMessage.Assistant(
            "", RunMessage.Completion.COMPLETE, List.of(executable("cancelled")))));
        session.stop(stopped);
        assertInstanceOf(RunSession.FinishResult.Finished.class, session.finish(stopped));
        CacheRequest afterStop = assertExtends(generation, afterToolRound, session, counting);

        List<Integer> expectedIndexes = new ArrayList<>();
        for (int index = 0; index < session.history().messages().size(); index++) expectedIndexes.add(index);
        assertEquals(expectedIndexes, renderedIndexes, "each accepted item must render exactly once");

        CacheRequest divergent = new CacheRequest(generation.id(), List.of(
            new CacheRequest.Part(CacheRequest.Component.HEAD,
                new CacheField("model", "different".getBytes(StandardCharsets.UTF_8)), OptionalInt.empty())
        ));
        assertThrows(CachePrefixViolation.class, () -> new CachePrefixGuard().verify(first, divergent));

        CacheGeneration reset = CacheGeneration.replacement(session,
            CacheGeneration.CacheReset.ExplicitContextReset.INSTANCE, new CacheGeneration.Id("reset"), head());
        CacheRequest fresh = reset.requestFor(
            new RunSession.HistorySnapshot(List.of(new RunMessage.User("fresh"))), RENDERER);
        assertEquals(headLength(), reset.lastObservation().orElseThrow().previousBytes(),
            "reset must compare against its own head, not the previous generation");
        assertThrows(IllegalArgumentException.class, () -> new CachePrefixGuard().verify(afterStop, fresh));
    }

    @Test
    void prefixViolationReportsFirstDivergence() {
        CacheGeneration.Id id = new CacheGeneration.Id("diagnostic");
        CacheRequest previous = new CacheRequest(id, List.of(
            new CacheRequest.Part(CacheRequest.Component.HEAD,
                new CacheField("instructions", "abcdef".getBytes(StandardCharsets.UTF_8)), OptionalInt.empty())
        ));
        CacheRequest candidate = new CacheRequest(id, List.of(
            new CacheRequest.Part(CacheRequest.Component.HEAD,
                new CacheField("instructions", "abcxef".getBytes(StandardCharsets.UTF_8)), OptionalInt.empty())
        ));

        CachePrefixViolation violation = assertThrows(CachePrefixViolation.class,
            () -> new CachePrefixGuard().verify(previous, candidate));
        assertEquals(CacheRequest.Component.HEAD, violation.component());
        assertEquals("instructions", violation.field());
        assertTrue(violation.itemIndex().isEmpty());
        assertEquals(3, violation.byteOffset());
        assertEquals(6, violation.previousBytes());
        assertEquals(6, violation.candidateBytes());
        assertEquals(3, violation.reusedBytes());
        assertFalse(violation.getMessage().contains("abcdef"));
        assertFalse(violation.getMessage().contains("abcxef"));

        CacheField stableHead = new CacheField("instructions", "abc".getBytes(StandardCharsets.UTF_8));
        CacheRequest storedItem = new CacheRequest(id, List.of(
            new CacheRequest.Part(CacheRequest.Component.HEAD, stableHead, OptionalInt.empty()),
            new CacheRequest.Part(CacheRequest.Component.INPUT,
                new CacheField("item", "|1:x".getBytes(StandardCharsets.UTF_8)), OptionalInt.of(1))
        ));
        CacheRequest changedItem = new CacheRequest(id, List.of(
            new CacheRequest.Part(CacheRequest.Component.HEAD, stableHead, OptionalInt.empty()),
            new CacheRequest.Part(CacheRequest.Component.INPUT,
                new CacheField("item", "|1:y".getBytes(StandardCharsets.UTF_8)), OptionalInt.of(1))
        ));
        CachePrefixViolation itemViolation = assertThrows(CachePrefixViolation.class,
            () -> new CachePrefixGuard().verify(storedItem, changedItem));
        assertEquals(CacheRequest.Component.INPUT, itemViolation.component());
        assertEquals("item", itemViolation.field());
        assertEquals(OptionalInt.of(1), itemViolation.itemIndex());
        assertEquals(6, itemViolation.byteOffset());
        assertEquals(7, itemViolation.previousBytes());
        assertEquals(7, itemViolation.candidateBytes());
        assertEquals(6, itemViolation.reusedBytes());
        assertFalse(itemViolation.getMessage().contains("|1:x"));
        assertFalse(itemViolation.getMessage().contains("|1:y"));
    }

    @Test
    void rendererFailureAdmitsNoPartialTail() {
        CacheGeneration generation = generation("partial");
        RunSession.HistorySnapshot history = new RunSession.HistorySnapshot(List.of(
            new RunMessage.User("u0"),
            new RunMessage.Assistant("a1", RunMessage.Completion.COMPLETE, List.of()),
            new RunMessage.User("u2")));
        ModelItemRenderer failsOnLastItem = (message, index) -> {
            if (index == 2) throw new IllegalStateException("renderer failed");
            return RENDERER.render(message, index);
        };

        assertThrows(IllegalStateException.class, () -> generation.requestFor(history, failsOnLastItem));
        CacheRequest recovered = generation.requestFor(history, RENDERER);

        CacheRequest expected = generation("partial").requestFor(history, RENDERER);
        assertArrayEquals(expected.bytes(), recovered.bytes(), "a failed render must not drop earlier tail items");
    }

    @Test
    void scriptedSessionExceedsNinetyPercentReuse() {
        CacheGeneration generation = generation("benchmark");
        CachePrefixGuard guard = new CachePrefixGuard();
        List<RunMessage> history = new ArrayList<>();
        List<Integer> renderedIndexes = new ArrayList<>();
        ModelItemRenderer counting = (message, index) -> {
            renderedIndexes.add(index);
            return RENDERER.render(message, index);
        };
        CacheRequest previous = null;
        long totalPrevious = 0;
        long totalReused = 0;
        for (int dispatch = 0; dispatch < 20; dispatch++) {
            if (dispatch != 7 && dispatch != 13) {
                history.add(scriptedMessage(dispatch));
            }
            CacheRequest candidate = generation.requestFor(new RunSession.HistorySnapshot(history), counting);
            if (previous != null) {
                CachePrefixGuard.Observation observation = guard.verify(previous, candidate);
                if (dispatch >= 2) {
                    totalPrevious += observation.previousBytes();
                    totalReused += observation.reusedBytes();
                }
            }
            previous = candidate;
        }
        double aggregateReuse = (double) totalReused / totalPrevious;
        assertTrue(aggregateReuse > 0.90, "aggregate structural reuse=" + aggregateReuse);
        assertEquals(1.0, aggregateReuse);
        List<Integer> expectedIndexes = new ArrayList<>();
        for (int index = 0; index < history.size(); index++) expectedIndexes.add(index);
        assertEquals(expectedIndexes, renderedIndexes, "each accepted item must render exactly once");
    }

    private static RunMessage scriptedMessage(int index) {
        return switch (index % 6) {
            case 0 -> new RunMessage.User("user-" + index);
            case 1 -> new RunMessage.Assistant("assistant-" + index, RunMessage.Completion.COMPLETE, List.of());
            case 2 -> new RunMessage.ToolResult(new Call.Id("tool-" + index),
                new ToolOutcome.Completed("result", List.of()));
            case 3 -> new RunMessage.Assistant("truncated", RunMessage.Completion.INCOMPLETE, List.of());
            case 4 -> new RunMessage.ToolResult(new Call.Id("truncated-" + index),
                new ToolOutcome.NotStarted(ToolOutcome.Reason.TRUNCATED_NOT_EXECUTED, "not executed"));
            default -> new RunMessage.ToolResult(new Call.Id("stopped-" + index),
                new ToolOutcome.NotStarted(ToolOutcome.Reason.CANCELLED_NOT_STARTED, "stopped"));
        };
    }

    private static RunSession.ActiveRun start(RunSession session, RunMessage.User user) {
        return ((RunSession.StartResult.Started) session.start(
            user, RunLimits.mvp(), Instant.EPOCH, () -> Instant.EPOCH)).run();
    }

    private static CacheRequest assertExtends(CacheGeneration generation, CacheRequest previous,
                                              RunSession session, ModelItemRenderer renderer) {
        CacheRequest candidate = generation.requestFor(session.history(), renderer);
        assertTrue(candidate.length() > previous.length(), "accepted output must append cache bytes");
        assertArrayEquals(previous.bytes(), Arrays.copyOf(candidate.bytes(), previous.length()));
        return candidate;
    }

    private static PlannedCall.Executable executable(String id) {
        return new PlannedCall.Executable(new Call.Id(id), new PlannedCall.ToolName("tool-" + id),
            new PlannedCall.ToolOperation() { });
    }

    private static int headLength() {
        return head().stream().mapToInt(field -> field.bytes().length).sum();
    }

    private static CacheGeneration generation(String id) {
        return CacheGeneration.initial(new CacheGeneration.Id(id), head());
    }

    private static List<CacheField> head() {
        return List.of(
            new CacheField("model", "model-a".getBytes(StandardCharsets.UTF_8)),
            new CacheField("instructions", new byte[4096])
        );
    }
}
