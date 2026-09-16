package com.github.catatafishen.agentbridge.nativeagent.run.session;

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call;
import com.github.catatafishen.agentbridge.nativeagent.model.ItfTrace;
import com.github.catatafishen.agentbridge.nativeagent.lifecycle.RunLifecycle;
import com.github.catatafishen.agentbridge.nativeagent.run.policy.RunLimits;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RunSessionReplayTest {
    private static final Instant NOW = Instant.parse("2026-09-14T00:00:00Z");

    @Test
    void replaysEverySelectedTraceWithPerStepOracle() throws IOException {
        for (int i = 0; i < 20; i++) {
            String resource = "/nativeagent/model/traces/agent_coarse_" + i + ".itf.json";
            ItfTrace trace;
            try (InputStream stream = RunSessionReplayTest.class.getResourceAsStream(resource)) {
                if (stream == null) throw new AssertionError("missing trace resource " + resource);
                trace = ItfTrace.read(stream);
            }
            Replay replay = new Replay(resource);
            for (ItfTrace.State state : trace.states()) {
                replay.apply(state);
                replay.assertState(state);
            }
        }
    }

    @Test
    void limitStopRefusesTheNextRequestAndStopsTheRun() {
        RunSession session = new RunSession();
        RunSession.StartResult.Started started = assertInstanceOf(RunSession.StartResult.Started.class,
            session.start(new RunMessage.User("request"),
                new RunLimits(1, 1, java.time.Duration.ofMinutes(1)), NOW, () -> NOW));
        RunSession.ActiveRun run = started.run();
        assertEquals(RunLimits.Admission.ADMITTED, session.beginRequest(run, NOW));
        PlannedCall.Executable call = new PlannedCall.Executable(new Call.Id("limit-call"),
            new PlannedCall.ToolName("tool"), new PlannedCall.ToolOperation() { });
        session.acceptTurn(run, new ValidatedAssistantTurn.Complete(
            new RunMessage.Assistant("", RunMessage.Completion.COMPLETE, List.of(call))));
        RunSession.CallStep.Execute step = assertInstanceOf(RunSession.CallStep.Execute.class, session.nextCall(run));
        assertInstanceOf(CallAdmission.Result.Executed.class, step.admission().execute(() -> { }));
        session.recordToolResult(run, new RunMessage.ToolResult(call.id(), new ToolOutcome.Completed("ok", List.of())));
        assertInstanceOf(RunSession.CallStep.Complete.class, session.nextCall(run));
        assertEquals(RunLimits.Admission.RESPONSE_LIMIT_REACHED, session.beginRequest(run, NOW));
        session.stopForLimit(run);
        assertEquals(RunSession.Phase.STOPPING, session.phase());
    }

    private static final class Replay {
        private final String file;
        private final RunSession session = new RunSession();
        private RunSession.ActiveRun run;
        private CallAdmission admission;
        private CallAdmission.Result admissionResult;
        private CountDownLatch effectEntered;
        private CountDownLatch effectRelease;
        private Thread effectThread;
        private boolean cancelled;
        private int runNumber;
        private String turn = "NONE";
        private List<PlannedCall> calls = List.of();

        private Replay(String file) { this.file = file; }

        private void apply(ItfTrace.State state) {
            Map<?, ?> current = stateObject(state, "s");
            switch (state.action()) {
                case "send" -> start();
                case "beginRequest" -> beginRequest();
                case "providerAttempt" -> providerAttempt(current);
                case "deliverProvisional" -> deliverProvisional();
                case "acceptTurn" -> acceptTurn();
                case "nextCall" -> nextCall();
                case "admit" -> admit(current);
                case "effectFinish" -> finishEffect();
                case "recordResult" -> recordResult();
                case "stop", "limitStop" -> stop(state.action());
                case "close" -> {
                    session.dispose();
                    cancelled = true;
                }
                case "finish" -> finish();
                case "newGeneration", "init", "step", "stepCorpus" -> { }
                default -> throw new AssertionError("unhandled action " + state.action());
            }
        }

        private void start() {
            if (session.phase() != RunSession.Phase.IDLE) return;
            RunSession.StartResult result = session.start(
                new RunMessage.User("user"), RunLimits.mvp(), NOW, () -> NOW);
            if (result instanceof RunSession.StartResult.Started started) {
                run = started.run();
                runNumber++;
                admission = null;
                admissionResult = null;
                cancelled = false;
                turn = "NONE";
                calls = List.of();
            }
        }

        private void beginRequest() {
            if (run != null && session.phase() == RunSession.Phase.REQUESTING) session.beginRequest(run, NOW);
        }

        private void providerAttempt(Map<?, ?> current) {
            Object turnValue = current.get("turn");
            turn = turnValue instanceof String value ? value : "NONE";
            if (turn.equals("NONE")) {
                if (run != null && session.phase() == RunSession.Phase.REQUESTING
                    && session.provisional() instanceof RunSession.ProvisionalObservation.Present) {
                    session.clearProvisional(run);
                }
                calls = List.of();
                return;
            }
            List<PlannedCall.Executable> nextCalls = new java.util.ArrayList<>();
            for (String name : List.of("turnId0", "turnId1", "turnId2")) {
                BigInteger id = bigint(current.get(name));
                if (id.signum() > 0) {
                    nextCalls.add(new PlannedCall.Executable(new Call.Id(id.toString()),
                        new PlannedCall.ToolName("tool"), new PlannedCall.ToolOperation() { }));
                }
            }
            calls = List.copyOf(nextCalls);
        }

        private void deliverProvisional() {
            if (run != null && session.phase() == RunSession.Phase.REQUESTING) session.tryUpdateProvisional(run, "provisional");
        }

        private void acceptTurn() {
            if (run == null || session.phase() != RunSession.Phase.REQUESTING) return;
            if (turn.equals("CALLS") && calls.isEmpty()) {
                throw new AssertionError(file + " CALLS provider attempt produced no calls");
            }
            String text = turn.equals("TEXT") ? "text" : "";
            session.acceptTurn(run, new ValidatedAssistantTurn.Complete(
                new RunMessage.Assistant(text, RunMessage.Completion.COMPLETE, calls)));
            turn = "NONE";
        }

        private void nextCall() {
            if (run == null || session.phase() != RunSession.Phase.EXECUTING_TOOLS) return;
            RunSession.CallStep step = session.nextCall(run);
            if (step instanceof RunSession.CallStep.Execute execute) admission = execute.admission();
        }

        private void admit(Map<?, ?> model) {
            if (admission == null) return;
            if (!expectsExecuting(model)) {
                admissionResult = admission.execute(() -> { });
                return;
            }
            CallAdmission pending = admission;
            effectEntered = new CountDownLatch(1);
            effectRelease = new CountDownLatch(1);
            effectThread = new Thread(() -> {
                admissionResult = pending.execute(() -> {
                    effectEntered.countDown();
                    try {
                        if (!effectRelease.await(5, TimeUnit.SECONDS)) {
                            throw new AssertionError("effect release timed out");
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("effect interrupted", interrupted);
                    }
                });
            }, "itf-replay-effect");
            effectThread.start();
            try {
                if (!effectEntered.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("effect did not enter");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("effect entry interrupted", interrupted);
            }
        }

        private boolean expectsExecuting(Map<?, ?> model) {
            for (int i = 0; i < 3; i++) {
                Map<?, ?> slot = (Map<?, ?>) model.get("slot" + i);
                if ("EXECUTING".equals(slot.get("status"))) return true;
            }
            return false;
        }

        private void finishEffect() {
            if (effectThread == null) return;
            effectRelease.countDown();
            try {
                effectThread.join(5_000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("effect completion interrupted", interrupted);
            }
            if (effectThread.isAlive()) throw new AssertionError("effect did not finish");
            effectThread = null;
            effectEntered = null;
            effectRelease = null;
        }

        private void recordResult() {
            if (run == null || admission == null
                || (session.phase() != RunSession.Phase.EXECUTING_TOOLS
                    && session.phase() != RunSession.Phase.STOPPING)
                || !(admissionResult instanceof CallAdmission.Result.Executed)) return;
            session.recordToolResult(run, new RunMessage.ToolResult(admission.call(),
                new ToolOutcome.Completed("ok", List.of())));
            admission = null;
            admissionResult = null;
        }

        private void stop(String action) {
            if (run == null) return;
            if (session.phase() == RunSession.Phase.REQUESTING
                || session.phase() == RunSession.Phase.EXECUTING_TOOLS) {
                if (action.equals("limitStop")) session.stopForLimit(run);
                else session.stop(run);
                cancelled = true;
            }
        }

        private void finish() {
            if (run != null && session.phase() == RunSession.Phase.STOPPING) {
                session.finish(run);
                admission = null;
                admissionResult = null;
            }
        }

        private void assertState(ItfTrace.State state) {
            Map<?, ?> model = stateObject(state, "s");
            String prefix = file + ":" + state.index() + ":" + state.action();
            assertEquals(model.get("phase"), session.phase().name(), prefix + " phase");
            assertEquals(model.get("lc"), lifecyclePhase(), prefix + " lifecycle phase");
            assertEquals(model.get("active"), session.phase() != RunSession.Phase.IDLE
                && session.phase() != RunSession.Phase.DISPOSED, prefix + " active");
            assertEquals(model.get("disposed"), field(session, "disposed"), prefix + " disposed");
            assertEquals(model.get("provisional"), session.provisional() instanceof RunSession.ProvisionalObservation.Present,
                prefix + " provisional");
            assertEquals(model.get("cancelled"), cancelled, prefix + " cancelled");
            assertEquals(bigint(model.get("runId")).intValueExact(), runNumber, prefix + " runId");
            assertEquals(((List<?>) model.get("history")).size(), session.history().messages().size(),
                prefix + " history size");
            assertSlots(model, prefix);
        }

        private String lifecyclePhase() {
            RunLifecycle lifecycle = (RunLifecycle) field(session, "lifecycle");
            return lifecycle.snapshot().phase().name();
        }


        private void assertSlots(Map<?, ?> model, String prefix) {
            if (run == null) return;
            Object batch = field(run, "batch");
            if (batch == null) {
                for (int i = 0; i < 3; i++) assertEquals("NONE", slot(model, i).get("status"), prefix + " slot" + i);
                return;
            }
            RunLifecycle lifecycle = (RunLifecycle) field(session, "lifecycle");
            RunLifecycle.BatchSnapshotResult result = lifecycle.batchSnapshot((com.github.catatafishen.agentbridge.nativeagent.lifecycle.Batch.Handle) batch);
            if (!(result instanceof RunLifecycle.BatchSnapshotResult.Available available)) return;
            Map<?, ?> results = (Map<?, ?>) field(run, "results");
            for (int i = 0; i < 3; i++) {
                Map<?, ?> expected = slot(model, i);
                BigInteger key = bigint(expected.get("key"));
                if (key.signum() == 0) {
                    assertEquals("NONE", expected.get("status"), prefix + " unused slot" + i);
                    continue;
                }
                String callId = key.subtract(BigInteger.TEN.multiply(BigInteger.valueOf(runNumber))).toString();
                Call.Snapshot actual = available.snapshot().calls().stream()
                    .filter(call -> call.call().value().equals(callId))
                    .findFirst().orElseThrow(() -> new AssertionError(
                        prefix + " missing Java call for model key " + key));
                assertEquals(expected.get("status"), actual.status().name(), prefix + " slot" + i + " status");
                assertEquals(bigint(expected.get("results")).intValueExact(),
                    results.containsKey(actual.call()) ? 1 : 0, prefix + " slot" + i + " results");
            }
            assertEquals(bigint(model.get("nextIndex")).intValueExact(),
                ((Integer) field(run, "nextResultIndex")).intValue(), prefix + " nextIndex");
        }

        private static Map<?, ?> slot(Map<?, ?> model, int index) {
            return (Map<?, ?>) model.get("slot" + index);
        }

        private static Object field(Object target, String name) {
            try {
                var field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Unable to inspect " + name, exception);
            }
        }
    }

    private static Map<?, ?> stateObject(ItfTrace.State state, String name) {
        return (Map<?, ?>) state.values().get(name);
    }

    private static BigInteger bigint(Object value) {
        Map<?, ?> wrapper = (Map<?, ?>) value;
        return new BigInteger((String) wrapper.get("#bigint"));
    }
}
