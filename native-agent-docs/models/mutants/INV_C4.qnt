module mut_INV_C4 {
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
    B::mut_acceptReusedIds,
  }
  val INV_C4 = B::INV_C4
}
