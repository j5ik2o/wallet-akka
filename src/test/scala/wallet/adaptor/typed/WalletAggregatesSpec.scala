package wallet.adaptor.typed

import akka.actor.typed.ActorRef
import wallet._
import wallet.adaptor.typed.WalletProtocol._

class WalletAggregatesSpec extends WalletAggregateSpec {

  override def newWalletRef(id: WalletId): ActorRef[CommandRequest] =
    spawn(WalletAggregates.behavior(WalletAggregate.name)(WalletAggregate.behavior))

}
