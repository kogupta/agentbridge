module mut_REF_R {
  import read_refinement as R from "../read_refinement"
  action init = R::init
  action step = any {
    R::step,
    R::mut_unsettleAndProject,
  }
  val REF_R = R::REF_R
}
