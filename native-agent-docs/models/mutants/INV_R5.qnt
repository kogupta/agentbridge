module mut_INV_R5 {
  import read_execution as B from "../read_execution"
  action init = B::init
  action step = any {
    B::startRead, B::refuseRead, B::smartWaitTick, B::dumbEnter, B::dumbExit,
    B::editDocument, B::commitDocuments, B::writeAction, B::compute, B::register,
    B::disposeRegistry, B::cancel, B::settle,
    B::mut_computeUncommitted,
  }
  val INV_R5 = B::INV_R5
}
