package com.github.catatafishen.agentbridge.nativeagent.run.session;

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call;
import com.github.catatafishen.agentbridge.nativeagent.run.policy.RunLimits;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdmissionPropertyTest {
    private static final Instant NOW = Instant.parse("2026-09-14T00:00:00Z");

    enum Operation { START, STOP, ADMIT, FINISH, DISPOSE }

    @Property(tries = 100)
    void admissionSequencesPreserveC1C2AndC3(
        @ForAll @Size(min = 1, max = 30) List<Operation> operations) {
        RunSession session = new RunSession();
        AtomicInteger effects = new AtomicInteger();
        RunSession.ActiveRun run = startWithCall(session, 0);
        CallAdmission admission = nextAdmission(session, run);
        int armedCalls = 1;
        int effectsAtStop = Integer.MAX_VALUE;

        for (Operation operation : operations) {
            RunSession.Phase before = session.phase();
            switch (operation) {
                case START -> {
                    if (session.phase() == RunSession.Phase.IDLE) {
                        run = startWithCall(session, armedCalls);
                        admission = nextAdmission(session, run);
                        armedCalls++;
                    }
                }
                case STOP -> {
                    if (session.phase() == RunSession.Phase.REQUESTING
                        || session.phase() == RunSession.Phase.EXECUTING_TOOLS) {
                        session.stop(run);
                    }
                }
                case ADMIT -> {
                    if (admission != null) {
                        int beforeEffects = effects.get();
                        CallAdmission.Result result = admission.execute(effects::incrementAndGet);
                        if (result instanceof CallAdmission.Result.Executed) {
                            session.recordToolResult(run, new RunMessage.ToolResult(
                                admission.call(), new ToolOutcome.Completed("ok", List.of())));
                            if (session.phase() == RunSession.Phase.EXECUTING_TOOLS) session.nextCall(run);
                        } else if (session.phase() == RunSession.Phase.STOPPING
                            || session.phase() == RunSession.Phase.DISPOSED) {
                            assertTrue(effects.get() == beforeEffects,
                                "C2: stopped/disposed admission started an effect");
                            assertTrue(admission.status().isTerminal(),
                                "C2: rejected admission did not settle its call");
                        }
                        admission = null;
                    }
                }
                case FINISH -> {
                    if (session.phase() == RunSession.Phase.STOPPING) {
                        session.finish(run);
                        admission = null;
                    }
                }
                case DISPOSE -> session.dispose();
            }
            if (before != RunSession.Phase.STOPPING && session.phase() == RunSession.Phase.STOPPING) {
                effectsAtStop = effects.get();
            }
            if (effectsAtStop != Integer.MAX_VALUE) {
                assertTrue(effects.get() >= effectsAtStop,
                    "effect count must not decrease after stop");
            }

            List<RunMessage.ToolResult> results = session.history().messages().stream()
                .filter(RunMessage.ToolResult.class::isInstance)
                .map(RunMessage.ToolResult.class::cast).toList();
            assertTrue(results.stream().map(RunMessage.ToolResult::callId).distinct().count() == results.size(),
                "C1: duplicate result for one call");
            assertTrue(results.size() <= armedCalls, "C1: more results than armed calls");
            if (session.phase() == RunSession.Phase.IDLE || session.phase() == RunSession.Phase.DISPOSED) {
                assertTrue(admission == null, "C3: terminal session retains a pending admission");
                assertTrue(results.size() == armedCalls,
                    "C3: terminal session has unsettled calls");
            }
        }
    }

    private static RunSession.ActiveRun startWithCall(RunSession session, int callNumber) {
        RunSession.StartResult.Started started = (RunSession.StartResult.Started) session.start(
            new RunMessage.User("property"), RunLimits.mvp(), NOW, () -> NOW);
        RunSession.ActiveRun run = started.run();
        session.acceptTurn(run, new ValidatedAssistantTurn.Complete(new RunMessage.Assistant(
            "", RunMessage.Completion.COMPLETE, List.of(call("property-call-" + callNumber)))));
        return run;
    }

    private static CallAdmission nextAdmission(RunSession session, RunSession.ActiveRun run) {
        return ((RunSession.CallStep.Execute) session.nextCall(run)).admission();
    }

    private static PlannedCall.Executable call(String id) {
        return new PlannedCall.Executable(new Call.Id(id), new PlannedCall.ToolName("tool"),
            new PlannedCall.ToolOperation() { });
    }
}
