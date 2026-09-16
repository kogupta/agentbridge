module agent_coarse {
  // Layered interaction model, coarse layer. One RunSession with its RunLifecycle,
  // the NativeRunDriver loop, the provider and the tool effects.
  //
  // Owners (Java): RunSession owns phase, active run, history, provisional, the
  // result cursor and per-call result counts; RunLifecycle owns lc, call statuses
  // and accepted call IDs; RunCancellation owns cancelled; the provider owns
  // request outcomes; ToolEffect owns effect completion; CacheGeneration owns
  // generation and request prefixes. read_execution owns registry and read state.
  //
  // State is one record `s`. `prev` is the pre-state of the last step, so
  // invariants can state transition properties over primary state. Every action,
  // mutants included, sets `prev' = s`.
  // Encoding for Apalache: a batch has at most three calls, held in three fixed
  // slots with plain fields. The model has no maps, no folds and no whole-record
  // comparisons; each of these made the SMT encoding grow too fast to reach 12 steps.

  // Finite bounds, recorded in evidence params.
  pure val MAX_TURNS: int = 2        // accepted provider responses per run (RunLimits)
  pure val MAX_RUNS: int = 2         // runs per session
  pure val MAX_GENERATIONS: int = 2  // cache generation replacements
  // Provider call batches as (first, second, third) local call IDs; zero means unused.
  // MAX_CALLS = 3. The provider may repeat an ID from an earlier turn of the same
  // run; RunLifecycle rejects it.
  pure val BATCHES: Set[(int, int, int)] =
    Set((1, 0, 0), (2, 0, 0), (3, 0, 0), (1, 2, 0), (1, 3, 0), (2, 3, 0), (1, 2, 3))
  // One retry per request (RetryPolicy): `retried` is the retry state.

  type Slot = {
    key: int,        // call key 10*run+localId, 0 when unused
    status: str,     // NONE | PENDING | EXECUTING | COMPLETED | FAILED_AFTER_START | CANCELLED_BEFORE_START
    starts: int,     // effect starts for this call
    results: int     // results appended for this call (RunSession run.results)
  }

  type State = {
    phase: str,             // RunSession.Phase: IDLE | REQUESTING | EXECUTING_TOOLS | STOPPING | DISPOSED
    lc: str,                // RunLifecycle phase: IDLE | RUNNING | STOPPING | CLOSING | CLOSED
    disposed: bool,
    registryDisposed: bool,     // content-owned read registry has been disposed
    active: bool,           // RunSession.current != null
    runId: int,             // runs started; also the run ID
    cancelled: bool,        // RunCancellation of the current run
    history: List[int],     // RunSession.accepted, encoded items (see below)
    provisional: bool,      // provisional text delivered and not cleared
    req: str,               // provider request: NONE | IN_FLIGHT
    retried: bool,
    turn: str,              // validated turn: NONE | TEXT | CALLS | LENGTH_CALLS
    turnId0: int,           // local call IDs of a CALLS turn; turnId1/2 0 = unused
    turnId1: int,
    turnId2: int,
    turns: int,             // accepted responses this run
    n: int,                 // calls in the current batch, 0..3
    slot0: Slot,
    slot1: Slot,
    slot2: Slot,
    nextIndex: int,         // RunSession nextResultIndex
    acceptedIds: Set[int],  // RunLifecycle acceptedCallIds
    startedIds: Set[int],   // call keys whose effect started this run
    cursor: int,            // slot returned by nextCall, -1 when none
    outcome: str,           // admission: NONE | EXECUTED | LIMITED | REJECTED
    inEffect: int,          // slot whose effect runs, -1 when none
    generation: int,
    requestHead: int,           // complete serialized instructions/tools fingerprint
    lastRequest: List[int],     // serialized request input sequence for the last attempt
    requestsInGen: int
  }

  // History items: user message 100*run, assistant turn 1000+10*run+turn,
  // tool result = call key 10*run+localId (always below 100).
  pure def userItem(r: int): int = 100 * r
  pure def assistantItem(r: int, turn: int): int = 1000 + 10 * r + turn
  pure def callKey(r: int, id: int): int = 10 * r + id

  pure val EMPTY_SLOT: Slot = { key: 0, status: "NONE", starts: 0, results: 0 }

  pure val INIT: State = {
    phase: "IDLE", lc: "IDLE", disposed: false, registryDisposed: false, active: false, runId: 0, cancelled: false,
    history: [], provisional: false, req: "NONE", retried: false, turn: "NONE",
    turnId0: 0, turnId1: 0, turnId2: 0, turns: 0, n: 0, slot0: EMPTY_SLOT,
    slot1: EMPTY_SLOT, slot2: EMPTY_SLOT,
    nextIndex: 0, acceptedIds: Set(), startedIds: Set(), cursor: -1, outcome: "NONE",
    inEffect: -1, generation: 0, requestHead: 0, lastRequest: [], requestsInGen: 0
  }

  var s: State
  var prev: State

  action init = all {
    s' = INIT,
    prev' = INIT,
  }

  // ---- Pure helpers ----

  pure def isUnsettled(st: str): bool = st == "PENDING" or st == "EXECUTING"
  pure def isPrefix(a: List[int], b: List[int]): bool =
    length(a) <= length(b) and a.indices().forall(i => a[i] == b[i])

  pure def slotAt(t: State, i: int): Slot =
    if (i == 0) t.slot0 else if (i == 1) t.slot1 else t.slot2

  pure def withSlot(t: State, i: int, sl: Slot): State =
    if (i == 0) { ...t, slot0: sl } else if (i == 1) { ...t, slot1: sl } else { ...t, slot2: sl }

  pure def batchSettled(t: State): bool =
    t.inEffect == -1
      and (t.n <= 0 or not(isUnsettled(t.slot0.status)))
      and (t.n <= 1 or not(isUnsettled(t.slot1.status)))
      and (t.n <= 2 or not(isUnsettled(t.slot2.status)))

  pure def batchRecorded(t: State): bool =
    t.n <= 0
      or (t.slot0.results == 1
          and (t.n <= 1 or (t.slot1.results == 1
              and (t.n <= 2 or t.slot2.results == 1))))

  pure def cancelSlot(sl: Slot): Slot =
    if (sl.status == "PENDING") { ...sl, status: "CANCELLED_BEFORE_START" } else sl

  // Batch.cancelPending: PENDING calls become CANCELLED_BEFORE_START.
  pure def cancelPending(t: State): State =
    { ...t, slot0: cancelSlot(t.slot0), slot1: cancelSlot(t.slot1), slot2: cancelSlot(t.slot2) }

  // RunSession.appendCancelledInOrder: the next call has a cancelled result to append.
  pure def drainable(t: State): bool =
    t.nextIndex < t.n and slotAt(t, t.nextIndex).status == "CANCELLED_BEFORE_START"

  // Append the result of the call at nextIndex and advance the cursor position.
  pure def appendResult(t: State): State = {
    val sl = slotAt(t, t.nextIndex)
    withSlot({ ...t, history: t.history.append(sl.key), nextIndex: t.nextIndex + 1 },
             t.nextIndex, { ...sl, results: sl.results + 1 })
  }

  pure def drainOne(t: State): State = if (drainable(t)) appendResult(t) else t

  pure def drain(t: State): State = drainOne(drainOne(drainOne(t)))
  // RunSession.stop: cancel the run, lifecycle RUNNING -> STOPPING, drain.
  pure def stopped(t: State): State = {
    val c = { ...t, cancelled: true, phase: "STOPPING", provisional: false,
              lc: if (t.lc == "RUNNING") "STOPPING" else t.lc }
    drain(cancelPending(c))
  }

  // RunSession.finish / finishLifecycle.
  pure def finished(t: State): State = {
    ...t,
    active: false,
    provisional: false,
    phase: if (t.disposed) "DISPOSED" else "IDLE",
    lc: if (t.lc == "CLOSING") "CLOSED" else "IDLE",
    acceptedIds: Set(),
    cursor: -1,
    outcome: "NONE",
  }

  // The assistant turn of RunSession.acceptTurn is appended.
  pure def acceptedTurn(t: State): State = {
    ...t, turn: "NONE", turnId0: 0, turnId1: 0, turnId2: 0, provisional: false, turns: t.turns + 1,
    history: t.history.append(assistantItem(t.runId, t.turns + 1)),
  }
  // LengthCalls is admitted and accounted without executing tool effects.
  pure def lengthCompleted(a: State, k0: int, k1: int, k2: int): State = {
    val b = begun(a, k0, k1, k2)
    val first = withSlot({ ...b, history: b.history.append(k0), nextIndex: 1 },
                         0, { ...b.slot0, status: "COMPLETED", results: 1 })
    if (b.n == 1) finished(first)
    else {
      val second = withSlot({ ...first, history: first.history.append(k1), nextIndex: 2 },
                            1, { ...first.slot1, status: "COMPLETED", results: 1 })
      if (b.n == 2) finished(second)
      else finished(withSlot({ ...second, history: second.history.append(k2), nextIndex: 3 },
                             2, { ...second.slot2, status: "COMPLETED", results: 1 }))
    }
  }

  // RunSession.beginBatch after the lifecycle accepted the call IDs.
  pure def begun(a: State, k0: int, k1: int, k2: int): State = {
    ...a,
    phase: "EXECUTING_TOOLS",
    n: if (k2 != 0) 3 else if (k1 == 0) 1 else 2,
    slot0: { key: k0, status: "PENDING", starts: 0, results: 0 },
    slot1: if (k1 == 0) EMPTY_SLOT else { key: k1, status: "PENDING", starts: 0, results: 0 },
    slot2: if (k2 == 0) EMPTY_SLOT else { key: k2, status: "PENDING", starts: 0, results: 0 },
    nextIndex: 0,
    acceptedIds: if (k2 != 0) a.acceptedIds.union(Set(k0, k1, k2))
                 else if (k1 == 0) a.acceptedIds.union(Set(k0))
                 else a.acceptedIds.union(Set(k0, k1)),
    cursor: -1,
    outcome: "NONE",
  }

  pure def key0(t: State): int = callKey(t.runId, t.turnId0)
  pure def key1(t: State): int = if (t.turnId1 == 0) 0 else callKey(t.runId, t.turnId1)
  pure def key2(t: State): int = if (t.turnId2 == 0) 0 else callKey(t.runId, t.turnId2)

  // RunSession.acceptTurn in REQUESTING.
  pure def accepted(t: State): State = {
    val reused = t.acceptedIds.contains(key0(t))
      or (key1(t) != 0 and t.acceptedIds.contains(key1(t)))
      or (key2(t) != 0 and t.acceptedIds.contains(key2(t)))
    if (reused) stopped({ ...t, turn: "NONE", turnId0: 0, turnId1: 0, turnId2: 0 })
    else {
      val a = acceptedTurn(t)
      if (t.turn == "TEXT") finished(a)
      else if (t.turn == "LENGTH_CALLS") lengthCompleted(a, key0(t), key1(t), key2(t))
      else begun(a, key0(t), key1(t), key2(t))
    }
  }

  // Effect start for the cursor call.
  pure def started(t: State): State = {
    val sl = slotAt(t, t.cursor)
    withSlot({ ...t, outcome: "EXECUTED", inEffect: t.cursor, startedIds: t.startedIds.union(Set(sl.key)) },
             t.cursor, { ...sl, status: "EXECUTING", starts: sl.starts + 1 })
  }

  // RunSession.recordToolResult plus the driver follow-up in the same thread.
  pure def recorded(t: State): State = {
    val failed = slotAt(t, t.cursor).status == "FAILED_AFTER_START"
    val r = { ...appendResult(t), cursor: -1, outcome: "NONE" }
    if (t.phase == "STOPPING") drain(r)
    else if (t.outcome == "LIMITED" or failed) stopped(r)
    else r
  }

  // ---- Actions ----

  // User: RunSession.start.
  action send = all {
    s.phase == "IDLE",
    s.lc == "IDLE",
    s.runId < MAX_RUNS,
    s' = { ...s, phase: "REQUESTING", lc: "RUNNING", active: true, runId: s.runId + 1,
           cancelled: false, history: s.history.append(userItem(s.runId + 1)),
           provisional: false, req: "NONE", retried: false, turn: "NONE", turnId0: 0,
           turnId1: 0, turnId2: 0, turns: 0, n: 0, slot0: EMPTY_SLOT,
           slot1: EMPTY_SLOT, slot2: EMPTY_SLOT, nextIndex: 0,
           acceptedIds: Set(), startedIds: Set(), cursor: -1, outcome: "NONE", inEffect: -1 },
    prev' = s,
  }

  // Driver: beginRequest admitted, cache.requestFor(history), transport.request.
  action beginRequest = all {
    s.phase == "REQUESTING",
    s.active,
    not(s.cancelled),
    s.req == "NONE",
    s.turn == "NONE",
    s.turns < MAX_TURNS,
    s' = { ...s, req: "IN_FLIGHT", retried: false, lastRequest: s.history,
           requestsInGen: s.requestsInGen + 1 },
    prev' = s,
  }

  // Driver: beginRequest not admitted (turn budget), then RunSession.stop.
  action limitStop = all {
    s.phase == "REQUESTING",
    s.active,
    not(s.cancelled),
    s.req == "NONE",
    s.turn == "NONE",
    s.turns >= MAX_TURNS,
    s' = stopped(s),
    prev' = s,
  }

  // Provider: one attempt of the in-flight request.
  action providerAttempt = {
    nondet outcome = oneOf(Set("TERMINAL_TEXT", "TERMINAL_CALLS", "TERMINAL_LENGTH_CALLS",
                               "RETRYABLE", "FAILED"))
    nondet ids = oneOf(BATCHES)
    all {
      s.req == "IN_FLIGHT",
      s' = if (s.phase != "REQUESTING") { ...s, req: "NONE" }
           else if (outcome == "TERMINAL_TEXT") { ...s, req: "NONE", turn: "TEXT" }
           else if (outcome == "TERMINAL_LENGTH_CALLS")
             { ...s, req: "NONE", turn: "LENGTH_CALLS", turnId0: ids._1,
               turnId1: ids._2, turnId2: ids._3 }
           else if (outcome == "TERMINAL_CALLS")
             { ...s, req: "NONE", turn: "CALLS", turnId0: ids._1,
               turnId1: ids._2, turnId2: ids._3 }
           else if (outcome == "RETRYABLE" and not(s.retried)) { ...s, retried: true, provisional: false }
           else stopped({ ...s, req: "NONE" }),
      prev' = s,
    }
  }

  // Provider stream: RunSession.tryUpdateProvisional.
  action deliverProvisional = all {
    s.phase == "REQUESTING",
    s.req == "IN_FLIGHT",
    not(s.disposed),
    not(s.provisional),
    s' = { ...s, provisional: true },
    prev' = s,
  }

  // Driver: RunSession.acceptTurn. Outside REQUESTING it throws; the driver drops the turn.
  action acceptTurn = all {
    s.turn != "NONE",
    s' = if (s.phase != "REQUESTING")
           { ...s, turn: "NONE", turnId0: 0, turnId1: 0, turnId2: 0 }
         else accepted(s),
    prev' = s,
  }

  // Driver: RunSession.nextCall. Complete returns to REQUESTING (the tool loop).
  action nextCall = all {
    s.phase == "EXECUTING_TOOLS",
    s.cursor == -1,
    s.inEffect == -1,
    s' = if (s.nextIndex < s.n) { ...s, cursor: s.nextIndex, outcome: "NONE" }
         else { ...s, phase: "REQUESTING" },
    prev' = s,
  }

  // Driver: CallAdmission.execute entry, atomic with the lifecycle phase check.
  action admit = {
    nondet result = oneOf(Set("EXECUTED", "LIMITED", "REJECTED"))
    all {
      s.cursor != -1,
      s.outcome == "NONE",
      s.inEffect == -1,
      s' = if (s.lc != "RUNNING" or slotAt(s, s.cursor).status != "PENDING") { ...s, cursor: -1 }
           else if (result == "LIMITED" or result == "REJECTED")
             withSlot({ ...s, outcome: result }, s.cursor,
                      { ...slotAt(s, s.cursor), status: "COMPLETED" })
           else started(s),
      prev' = s,
    }
  }

  // ToolEffect: the effect returns or throws.
  action effectFinish = {
    nondet ok = oneOf(Set(true, false))
    all {
      s.inEffect != -1,
      s' = withSlot({ ...s, inEffect: -1 }, s.inEffect,
                    { ...slotAt(s, s.inEffect), status: if (ok) "COMPLETED" else "FAILED_AFTER_START" }),
      prev' = s,
    }
  }

  // Driver: RunSession.recordToolResult for the admitted cursor call.
  action recordResult = all {
    s.cursor != -1,
    s.inEffect == -1,
    s.outcome != "NONE",
    s.phase == "EXECUTING_TOOLS" or s.phase == "STOPPING",
    s' = recorded(s),
    prev' = s,
  }

  // User or driver: RunSession.stop. In STOPPING only when it drains a result.
  action stop = all {
    s.active,
    s.phase == "REQUESTING" or s.phase == "EXECUTING_TOOLS" or (s.phase == "STOPPING" and drainable(s)),
    s' = stopped(s),
    prev' = s,
  }

  // Content: RunSession.dispose. Closing an active run cancels and drains all
  // calls that have not started, just as RunSession.stop does.
  action close = all {
    not(s.disposed),
    s' = if (not(s.active)) { ...s, disposed: true, registryDisposed: true,
                               provisional: false, phase: "DISPOSED", lc: "CLOSED" }
         else if (s.lc == "RUNNING" or s.lc == "STOPPING")
           { ...stopped(s), disposed: true, registryDisposed: true, lc: "CLOSING" }
         else { ...s, disposed: true, registryDisposed: true, provisional: false,
                cancelled: true, phase: "STOPPING" },
    prev' = s,
  }

  // Driver: finishStopped -> RunSession.finish once the driver is idle, the
  // batch is settled, and every accepted call occurrence has a result.
  action finish = all {
    s.active,
    s.phase == "STOPPING",
    s.req == "NONE",
    s.turn == "NONE",
    s.cursor == -1,
    batchSettled(s),
    batchRecorded(s),
    s' = finished(s),
    prev' = s,
  }

  // CacheGeneration.replacement from an idle session. A replacement changes
  // the complete stable request head and resets the append-only input prefix.
  action newGeneration = all {
    s.phase == "IDLE",
    s.generation < MAX_GENERATIONS,
    s' = { ...s, generation: s.generation + 1, requestHead: s.requestHead + 1,
           lastRequest: [], requestsInGen: 0 },
    prev' = s,
  }

  action step = any {
    send,
    beginRequest,
    limitStop,
    providerAttempt,
    deliverProvisional,
    acceptTurn,
    nextCall,
    admit,
    effectFinish,
    recordResult,
    stop,
    close,
    finish,
    newGeneration,
  }

  // Replay-corpus scheduler only: a subset of `step` behaviors. close is enabled in
  // almost every state and DISPOSED is final, so under `step` the simulator ends
  // most traces early. Here close waits for a tool batch or the last run.
  // Invariants, witnesses and mutants use `step`.
  action stepCorpus = any {
    send,
    beginRequest,
    limitStop,
    providerAttempt,
    deliverProvisional,
    acceptTurn,
    nextCall,
    admit,
    effectFinish,
    recordResult,
    stop,
    all { s.phase == "EXECUTING_TOOLS" or s.runId == MAX_RUNS, close },
    finish,
    newGeneration,
  }

  // ---- Invariants ----

  // I4: every call has at most one result, and before the next provider request
  // every call of the previous batch has exactly one result.
  val INV_C1 =
    s.slot0.results <= 1 and s.slot1.results <= 1 and s.slot2.results <= 1
      and ((s.phase == "IDLE" or s.phase == "DISPOSED") implies batchRecorded(s))
      and (s.req == "IN_FLIGHT" implies
             ((s.n <= 0 or s.slot0.results == 1)
              and (s.n <= 1 or s.slot1.results == 1)
              and (s.n <= 2 or s.slot2.results == 1)))

  val INV_C2 =
    (s.lc != "RUNNING" and s.runId == prev.runId)
      implies (s.startedIds == prev.startedIds
               and (s.slot0.key != prev.slot0.key or s.slot0.starts == prev.slot0.starts)
               and (s.slot1.key != prev.slot1.key or s.slot1.starts == prev.slot1.starts)
               and (s.slot2.key != prev.slot2.key or s.slot2.starts == prev.slot2.starts))

  // I8, I9: an idle or disposed session has no active run and no unsettled call.
  val INV_C3 = (s.phase == "IDLE" or s.phase == "DISPOSED") implies (not(s.active) and batchSettled(s))

  // I3: no call effect starts twice, and a started call never becomes admissible again.
  // A begun batch's call IDs are disjoint from every previously accepted occurrence of
  // the same run, including occurrences that were only admitted (LIMITED/REJECTED).
  val INV_C4 =
    s.slot0.starts <= 1 and s.slot1.starts <= 1 and s.slot2.starts <= 1
      and (s.n <= 0 or s.slot0.status != "PENDING" or not(s.startedIds.contains(s.slot0.key)))
      and (s.n <= 1 or s.slot1.status != "PENDING" or not(s.startedIds.contains(s.slot1.key)))
      and (s.n <= 2 or s.slot2.status != "PENDING" or not(s.startedIds.contains(s.slot2.key)))
      and ((s.phase == "EXECUTING_TOOLS" and prev.phase == "REQUESTING" and prev.turnId0 != 0)
           implies (not(prev.acceptedIds.contains(callKey(prev.runId, prev.turnId0)))
                    and (prev.turnId1 == 0 or not(prev.acceptedIds.contains(callKey(prev.runId, prev.turnId1))))
                    and (prev.turnId2 == 0 or not(prev.acceptedIds.contains(callKey(prev.runId, prev.turnId2))))))

  // I13: within one generation, the stable serialized request head is constant and
  // the serialized input sequence is append-only.
  val INV_C5 =
    (s.generation == prev.generation implies s.requestHead == prev.requestHead)
      and ((s.generation == prev.generation and s.requestsInGen == prev.requestsInGen + 1)
           implies isPrefix(prev.lastRequest, s.lastRequest))

  // I11: disposed content receives no provisional delivery, a DISPOSED
  // session's history does not change, and its read registry is disposed.
  val INV_C6 =
    (s.disposed implies (not(s.provisional) and s.registryDisposed))
      and (prev.phase == "DISPOSED" implies s.history == prev.history)

  // I3/I4: rejecting a reused call ID happens before the assistant item is
  // committed: no REQUESTING -> STOPPING step with a dropped CALLS or LENGTH_CALLS turn
  // appends history.
  val INV_C7 =
    (s.phase == "STOPPING" and prev.phase == "REQUESTING"
       and (prev.turn == "CALLS" or prev.turn == "LENGTH_CALLS") and s.turn == "NONE")
      implies s.history == prev.history

  // ---- Consistency lemmas (checked like invariants; they tie cursor and effect
  // bookkeeping to lifecycle state; no mutant or witness) ----

  val LEM_EFFECT =
    (s.inEffect == -1 or (s.inEffect < s.n and slotAt(s, s.inEffect).status == "EXECUTING"))
      and (s.n <= 0 or s.slot0.status != "EXECUTING" or s.inEffect == 0)
      and (s.n <= 1 or s.slot1.status != "EXECUTING" or s.inEffect == 1)
      and (s.n <= 2 or s.slot2.status != "EXECUTING" or s.inEffect == 2)

  val LEM_CURSOR = s.outcome == "NONE" or (s.cursor == s.nextIndex and s.cursor < s.n)

  val LEM_PHASE =
    ((s.phase == "IDLE") iff (s.lc == "IDLE" and not(s.active) and not(s.disposed)))
      and ((s.phase == "DISPOSED") iff (s.lc == "CLOSED"))
      and (s.active iff (s.lc == "RUNNING" or s.lc == "STOPPING" or s.lc == "CLOSING"))
      and (s.disposed implies (s.phase == "DISPOSED" or (s.phase == "STOPPING" and s.lc == "CLOSING")))
  val LEM_BOUNDS =
    s.turns <= MAX_TURNS and s.runId <= MAX_RUNS and s.generation <= MAX_GENERATIONS
      and s.n <= 3 and s.nextIndex <= s.n

  // ---- Witnesses ----

  // A tool turn completes, its result is recorded, and a text turn ends the run.
  val W_SUCCESS =
    s.phase == "IDLE" and s.turns == MAX_TURNS and s.n > 0
      and s.slot0.status == "COMPLETED" and s.slot0.results == 1
      and (s.n <= 1 or (s.slot1.status == "COMPLETED" and s.slot1.results == 1))
      and (s.n <= 2 or (s.slot2.status == "COMPLETED" and s.slot2.results == 1))

  // Stop during a running effect, then the effect fails after start.
  val W_STOP_WAIT =
    prev.cancelled and prev.inEffect != -1 and s.inEffect == -1
      and slotAt(s, prev.inEffect).status == "FAILED_AFTER_START"

  // A retry, then a terminal response, on the second or later request of a generation.
  val W_RETRY = prev.retried and prev.req == "IN_FLIGHT" and s.turn != "NONE" and s.requestsInGen >= 2

  // Close during a tool batch.
  val W_CLOSE = s.disposed and not(prev.disposed) and prev.phase == "EXECUTING_TOOLS"

  // A reused call ID is rejected and the run stops without committing the assistant item.
  val W_ID_REUSE =
    prev.phase == "REQUESTING" and prev.turn == "CALLS" and s.phase == "STOPPING" and s.turn == "NONE"
      and s.history == prev.history

  // ---- Mutant fault actions (used only by mutants/<INV>.qnt) ----

  // INV_C1: nextCall returns Complete before every result is recorded.
  action mut_completeEarly = all {
    s.phase == "EXECUTING_TOOLS",
    s.cursor == -1,
    s.inEffect == -1,
    s.nextIndex < s.n,
    s' = { ...s, phase: "REQUESTING" },
    prev' = s,
  }

  // INV_C2: admission ignores the lifecycle phase, so an effect starts after Stop.
  action mut_admitAfterStop = all {
    s.cursor != -1,
    s.outcome == "NONE",
    s.inEffect == -1,
    s.lc != "RUNNING",
    s' = started(s),
    prev' = s,
  }

  // INV_C3: finish while an effect still runs.
  action mut_finishDuringEffect = all {
    s.active,
    s.phase == "STOPPING",
    s.inEffect != -1,
    s' = finished(s),
    prev' = s,
  }

  action mut_acceptReusedIds = all {
    s.phase == "REQUESTING",
    s.turn == "CALLS",
    s' = begun(acceptedTurn(s), key0(s), key1(s), key2(s)),
    prev' = s,
  }

  // INV_C5: history compaction without a new cache generation.
  action mut_compactHistory = all {
    s.phase == "IDLE",
    length(s.history) > 0,
    s' = { ...s, history: s.history.tail() },
    prev' = s,
  }

  // INV_C6: provisional delivery ignores disposal and phase.
  action mut_provisionalAfterDispose = all {
    s.req == "IN_FLIGHT",
    not(s.provisional),
    s' = { ...s, provisional: true },
    prev' = s,
  }

  // INV_C4: reuse is rejected only against started occurrences, so a call that was
  // merely admitted (LIMITED/REJECTED) can be accepted again.
  action mut_reuseStartedOnly = all {
    s.phase == "REQUESTING",
    s.turn == "CALLS",
    s.acceptedIds.contains(key0(s)),
    not(s.startedIds.contains(key0(s))),
    s' = begun(acceptedTurn(s), key0(s), key1(s), key2(s)),
    prev' = s,
  }

  // INV_C7: the assistant item is committed before a reused batch is rejected.
  action mut_appendBeforeReject = all {
    s.phase == "REQUESTING",
    s.turn == "CALLS",
    s.acceptedIds.contains(key0(s)),
    s' = stopped(acceptedTurn(s)),
    prev' = s,
  }

  // INV_C7: the assistant item of a LENGTH_CALLS turn is committed before a reused
  // batch is rejected.
  action mut_appendBeforeRejectLength = all {
    s.phase == "REQUESTING",
    s.turn == "LENGTH_CALLS",
    s.acceptedIds.contains(key0(s)),
    s' = stopped(acceptedTurn(s)),
    prev' = s,
  }

  // Mutant steps: check with --step=step_mut_<INV>[__<variant>] --invariant=<INV>; a
  // counterexample is required. One mutant per step, so each fault is detected on its own. They live here, not in importing modules, because Quint 0.32.0
  // fails to flatten `import ... as B` names for TLC and Apalache (QNT404).
  action step_mut_INV_C1 = any { step, mut_completeEarly }
  action step_mut_INV_C2 = any { step, mut_admitAfterStop }
  action step_mut_INV_C3 = any { step, mut_finishDuringEffect }
  action step_mut_INV_C4 = any { step, mut_acceptReusedIds }
  action step_mut_INV_C4__reuseStartedOnly = any { step, mut_reuseStartedOnly }
  action step_mut_INV_C5 = any { step, mut_compactHistory }
  action step_mut_INV_C6 = any { step, mut_provisionalAfterDispose }
  action step_mut_INV_C7 = any { step, mut_appendBeforeReject }
  action step_mut_INV_C7__length = any { step, mut_appendBeforeRejectLength }
}
