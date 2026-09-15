module read_refinement {
  // Refinement of the detailed read_execution module against the coarse ToolEffect
  // interface (agent_interface). prevView and view are the history-variable
  // projection of read state onto interface views; every detailed step must
  // respect legalEffectTransition (stuttering on the interface is allowed).
  import read_execution as B from "read_execution"
  import agent_interface as I from "agent_interface"

  var prevView: str
  var view: str

  action init = all {
    B::init,
    prevView' = "not_requested",
    view' = "not_requested",
  }

  action step = all {
    B::step,
    prevView' = view,
    view' = projectView,
  }

  val projectView: str =
    if (B::refused or (B::cancelled and B::computation == "IDLE")) "cancelled_before_start"
    else if (B::settled)
      (if (B::resultReady or B::computation == "TIMED_OUT") "completed" else "failed_after_start")
    else if (B::computation != "IDLE") "executing"
    else "not_requested"

  val REF_R = I::legalEffectTransition(prevView, view)

  val W_SUCCESS = view == "completed"

  // Mutant fault action (used only by mutants/REF_R.qnt): an illegal detailed
  // transition whose projection is not a legal interface transition.
  action mut_unsettleAndProject = all {
    B::mut_unsettle,
    prevView' = view,
    view' = projectView,
  }
}
