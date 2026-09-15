module mut_INV_R2 {
  import read_execution as B from "../read_execution"
  action init = B::init
  action step = any {
    B::startRead, B::refuseRead, B::smartWaitTick, B::dumbEnter, B::dumbExit,
    B::editDocument, B::commitDocuments, B::writeAction, B::compute, B::register,
    B::disposeRegistry, B::cancel, B::settle,
    B::mut_settleEarly,
  }
  val INV_R2 = B::INV_R2
}
