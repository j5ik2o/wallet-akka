package wallet.adaptor.untyped

import akka.actor.ActorRef
import wallet._

class WalletAggregatesSpec extends WalletAggregateSpec {
  override def newWalletRef(id: WalletId): ActorRef = system.actorOf(WalletAggregates.props()(WalletAggregate.props))
}
