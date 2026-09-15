module mut_INV_C6 {
  import agent_coarse as B from "../agent_coarse"
  action init = B::init
  action step = any {
    B::send,
    B::beginRequest,
    B::limitStop,
    B::providerAttempt,
    B::deliverProvisional,
    B::acceptTurn,
    B::nextCall,
    B::admit,
    B::effectFinish,
    B::recordResult,
    B::stop,
    B::close,
    B::finish,
    B::newGeneration,
    B::mut_provisionalAfterDispose,
  }
  val INV_C6 = B::INV_C6
}
