module agent_interface {
  // Shared ToolEffect interface: pure legal-transition relations over effect views.
  // A view is the projection of detailed tool-effect state onto the coarse model:
  //   not_requested -> executing -> completed | failed_after_start
  //   not_requested -> cancelled_before_start (terminal)
  // Refinement (read_refinement.qnt) projects read_execution state onto these views
  // and checks every transition with legalEffectTransition.

  pure def terminalView(v: str): bool =
    v == "completed" or v == "failed_after_start" or v == "cancelled_before_start"

  pure def iAdmit(prev: str, after: str): bool =
    prev == "not_requested" and (after == "executing" or after == "cancelled_before_start")

  pure def iStart(prev: str, after: str): bool =
    prev == "not_requested" and after == "executing"

  pure def iFinish(prev: str, after: str): bool =
    prev == "executing" and after == "completed"

  pure def iFail(prev: str, after: str): bool =
    prev == "executing" and after == "failed_after_start"

  pure def iCancel(prev: str, after: str): bool =
    (prev == "not_requested" and after == "cancelled_before_start")
      or (prev == "executing" and after == "failed_after_start")

  pure def legalEffectTransition(prev: str, after: str): bool =
    prev == after
      or iAdmit(prev, after)
      or iStart(prev, after)
      or iFinish(prev, after)
      or iFail(prev, after)
      or iCancel(prev, after)
}
