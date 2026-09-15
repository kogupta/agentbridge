module read_execution {
  // Detailed read-effect model, one read lifecycle. read_execution owns registry
  // state, PSI modification count, document committed flag, wait clock, dumb-mode
  // state, document version and write-restart count. It refines the coarse
  // ToolEffect interface (agent_interface); startRead/refuseRead are the detailed
  // counterparts of the coarse admit(call, result) outcomes EXECUTED vs
  // LIMITED/REJECTED. Every action assigns every variable so all `any` branches
  // carry identical update effects.

  pure val INDEX_BOUND: int = 2  // abstract smart-wait ticks
  pure val MAX_RESTARTS: int = 3 // write-action restarts per read
  pure val MAX_EDITS: int = 2    // document edits per read (docVersion bound)
  pure val MAX_DUMB: int = 2     // dumb-mode entries per read (psiCount bound)

  var registry: str             // LIVE | DISPOSED
  var docCommitted: bool
  var docVersion: int
  var dumbMode: bool
  var psiCount: int             // PSI modification count
  var waitClock: int            // abstract wait ticks, restart time not counted
  var restarts: int
  var computation: str          // IDLE | RUNNING | DONE | ABORTED | TIMED_OUT
  var resultReady: bool
  var registered: bool
  var cancelled: bool
  var refused: bool             // admission was LIMITED or REJECTED for this read
  var platformAccessed: bool    // any platform (PSI/index) access occurred
  var blockedBeforeRegister: bool // cancel or dispose happened before registration
  var settled: bool             // the effect returned
  var computeWasCommitted: bool // documents were committed at compute time
  var resultVersion: int       // document version captured by the returned result
  var resultContent: int       // abstract deterministic content fingerprint
  var indexNotReady: bool       // dumb mode outlasted the bound
  var indexExceptionOccurred: bool // escaped IndexNotReadyException terminal cause

  action init = all {
    registry' = "LIVE",
    docCommitted' = true,
    docVersion' = 0,
    dumbMode' = false,
    psiCount' = 0,
    waitClock' = 0,
    restarts' = 0,
    computation' = "IDLE",
    resultReady' = false,
    registered' = false,
    cancelled' = false,
    refused' = false,
    platformAccessed' = false,
    blockedBeforeRegister' = false,
    settled' = false,
    computeWasCommitted' = false,
    indexNotReady' = false,
    resultVersion' = 0,
    resultContent' = 0,
    indexExceptionOccurred' = false,
  }

  action startRead = all {
    computation == "IDLE",
    not(refused),
    not(cancelled),
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    restarts' = 0,
    waitClock' = 0,
    computation' = "RUNNING",
    resultReady' = false,
    registered' = registered,
    cancelled' = false,
    refused' = refused,
    platformAccessed' = false,
    blockedBeforeRegister' = false,
    settled' = false,
    computeWasCommitted' = false,
    indexNotReady' = false,
    resultVersion' = 0,
    resultContent' = 0,
    indexExceptionOccurred' = false,
  }

  action refuseRead = all {
    computation == "IDLE",
    not(refused),
    refused' = true,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = computation,
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    platformAccessed' = platformAccessed,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action smartWaitTick = all {
    computation == "RUNNING",
    not(cancelled),
    waitClock < INDEX_BOUND,
    waitClock' = waitClock + 1,
    platformAccessed' = true,
    indexNotReady' = (dumbMode and (waitClock + 1 == INDEX_BOUND)),
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    restarts' = restarts,
    // Dumb mode outlasting the bound ends the read with INDEX_NOT_READY.
    computation' = if (dumbMode and waitClock + 1 == INDEX_BOUND) "TIMED_OUT" else computation,
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action dumbEnter = all {
    computation == "RUNNING",
    not(cancelled),
    psiCount < MAX_DUMB,
    dumbMode' = true,
    psiCount' = psiCount + 1,
    docCommitted' = false,
    platformAccessed' = true,
    registry' = registry,
    docVersion' = docVersion,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = computation,
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action dumbExit = all {
    dumbMode,
    dumbMode' = false,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = computation,
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    platformAccessed' = platformAccessed,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action editDocument = all {
    computation == "IDLE" or computation == "RUNNING",
    docVersion < MAX_EDITS,
    docCommitted' = false,
    docVersion' = docVersion + 1,
    registry' = registry,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = computation,
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    platformAccessed' = platformAccessed,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action commitDocuments = all {
    docCommitted' = true,
    registry' = registry,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = computation,
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    platformAccessed' = platformAccessed,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action writeAction = all {
    computation == "RUNNING",
    not(cancelled),
    restarts < MAX_RESTARTS,
    restarts' = restarts + 1,
    waitClock' = 0,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    computation' = "RUNNING",
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    platformAccessed' = platformAccessed,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action compute = all {
    computation == "RUNNING",
    docCommitted,
    not(cancelled),
    registry == "LIVE",
    not(indexNotReady),
    computation' = "DONE",
    resultReady' = false,
    computeWasCommitted' = true,
    platformAccessed' = true,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    indexNotReady' = indexNotReady,
    resultVersion' = docVersion,
    resultContent' = docVersion,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action register = all {
    computation == "DONE",
    not(cancelled),
    not(blockedBeforeRegister),
    registry == "LIVE",
    registered' = true,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = computation,
    resultReady' = true,
    cancelled' = cancelled,
    refused' = refused,
    platformAccessed' = platformAccessed,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action disposeRegistry = all {
    registry' = "DISPOSED",
    blockedBeforeRegister' = (blockedBeforeRegister or not(registered)),
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = if (not(settled) and (computation == "RUNNING" or computation == "DONE")) "ABORTED" else computation,
    resultReady' = if (not(settled)) false else resultReady,
    registered' = if (not(settled)) false else registered,
    cancelled' = if (not(settled) and computation != "TIMED_OUT") true else cancelled,
    refused' = refused,
    platformAccessed' = platformAccessed,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action cancel = all {
    settled == false,
    cancelled == false,
    computation != "TIMED_OUT",
    cancelled' = true,
    blockedBeforeRegister' = (blockedBeforeRegister or not(registered)),
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = if (computation == "RUNNING" or computation == "DONE") "ABORTED" else computation,
    resultReady' = false,
    registered' = false,
    refused' = refused,
    platformAccessed' = platformAccessed,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }


  // Cancel or registry disposal terminates a running computation without a result.
  action abortComputation = all {
    computation == "RUNNING",
    cancelled or registry == "DISPOSED",
    computation' = "ABORTED",
    resultReady' = false,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    platformAccessed' = platformAccessed,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  // IndexNotReadyException can escape the read computation before the wait
  // bound. It is terminal and never exposes a partial result.
  action indexException = all {
    computation == "RUNNING",
    not(cancelled),
    waitClock < INDEX_BOUND,
    computation' = "TIMED_OUT",
    resultReady' = false,
    registered' = false,
    indexNotReady' = true,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    cancelled' = cancelled,
    refused' = refused,
    platformAccessed' = true,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = true,
  }

  action settle = all {
    computation == "DONE" or computation == "ABORTED" or computation == "TIMED_OUT",
    not(settled),
    settled' = true,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = computation,
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    platformAccessed' = platformAccessed,
    blockedBeforeRegister' = blockedBeforeRegister,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action step = any {
    startRead,
    refuseRead,
    smartWaitTick,
    indexException,
    dumbEnter,
    dumbExit,
    editDocument,
    commitDocuments,
    writeAction,
    compute,
    abortComputation,
    register,
    disposeRegistry,
    cancel,
    settle,
  }

  // ---- Invariants ----

  val INV_R1 = not(registered and blockedBeforeRegister)
    and not(cancelled and resultReady)
  val INV_R2 = not(settled) or computation == "DONE" or computation == "ABORTED" or computation == "TIMED_OUT"
  val INV_R3 = not(refused and platformAccessed)
  // INDEX_NOT_READY is either the bounded wait result or an escaped
  // IndexNotReadyException before the bound.
  val INV_R4 =
    waitClock <= INDEX_BOUND
      and (indexNotReady implies
        (computation == "TIMED_OUT" and not(resultReady)
          and ((indexExceptionOccurred and waitClock < INDEX_BOUND)
            or (not(indexExceptionOccurred) and waitClock == INDEX_BOUND))))
  val INV_R5 =
    not(resultReady)
      or (computeWasCommitted and computation == "DONE" and docCommitted
          and not(cancelled) and resultVersion == docVersion and resultContent == docVersion)
  // ---- Witnesses ----

  val W_RESTART = restarts > 0 and settled and resultReady
  val W_REFUSED = refused
  val W_INDEX_TIMEOUT = indexNotReady
  // Stop during the smart wait gives FAILED_AFTER_START (settled, no result).
  val W_STOP_WAIT = cancelled and waitClock > 0 and settled and computation == "ABORTED"

  // ---- Mutant fault actions (used only by mutants/<INV>.qnt) ----

  action mut_registerIgnoringCancel = all {
    resultReady or cancelled,
    registered' = true,
    resultReady' = true,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = computation,
    cancelled' = cancelled,
    refused' = refused,
    platformAccessed' = platformAccessed,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action mut_settleEarly = all {
    computation == "RUNNING",
    settled' = true,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = computation,
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    platformAccessed' = platformAccessed,
    blockedBeforeRegister' = blockedBeforeRegister,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action mut_accessAfterRefusal = all {
    refused,
    platformAccessed' = true,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = computation,
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action mut_returnPastBound = all {
    computation == "RUNNING",
    not(cancelled),
    waitClock < INDEX_BOUND + 1,
    waitClock' = waitClock + 1,
    platformAccessed' = true,
    indexNotReady' = indexNotReady,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    restarts' = restarts,
    computation' = computation,
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    computeWasCommitted' = computeWasCommitted,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,

  }
  action mut_computeUncommitted = all {
    computation == "RUNNING",
    not(cancelled),
    registry == "LIVE",
    not(indexNotReady),
    computation' = "DONE",
    resultReady' = true,
    computeWasCommitted' = docCommitted,
    platformAccessed' = true,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    blockedBeforeRegister' = blockedBeforeRegister,
    settled' = settled,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  action mut_unsettle = all {
    settled,
    resultReady,
    settled' = false,
    registry' = registry,
    docCommitted' = docCommitted,
    docVersion' = docVersion,
    dumbMode' = dumbMode,
    psiCount' = psiCount,
    waitClock' = waitClock,
    restarts' = restarts,
    computation' = computation,
    resultReady' = resultReady,
    registered' = registered,
    cancelled' = cancelled,
    refused' = refused,
    platformAccessed' = platformAccessed,
    blockedBeforeRegister' = blockedBeforeRegister,
    computeWasCommitted' = computeWasCommitted,
    indexNotReady' = indexNotReady,
    resultVersion' = resultVersion,
    resultContent' = resultContent,
    indexExceptionOccurred' = indexExceptionOccurred,
  }

  // Mutant steps: check with --step=step_mut_<INV> --invariant=<INV>; a counterexample
  // is required. See agent_coarse for why they are not separate importing modules.
  action step_mut_INV_R1 = any { step, mut_registerIgnoringCancel }
  action step_mut_INV_R2 = any { step, mut_settleEarly }
  action step_mut_INV_R3 = any { step, mut_accessAfterRefusal }
  action step_mut_INV_R4 = any { step, mut_returnPastBound }
  action step_mut_INV_R5 = any { step, mut_computeUncommitted }
}
