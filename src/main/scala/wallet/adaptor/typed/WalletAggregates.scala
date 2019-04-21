package wallet.adaptor.typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import wallet.WalletId
import wallet.adaptor.typed.WalletProtocol.CommandRequest

object WalletAggregates {

  val name = "wallets"

  def behavior(
      name: WalletId => String,
      chargesLimit: Int = Int.MaxValue
  )(behaviorF: (WalletId, Int) => Behavior[CommandRequest]): Behavior[CommandRequest] = {
    Behaviors.setup { ctx =>
      def createAndSend(walletId: WalletId): ActorRef[CommandRequest] = {
        ctx.child(WalletAggregate.name(walletId)) match {
          case None =>
            // 子アクター作成
            ctx.spawn(behaviorF(walletId, chargesLimit), name = name(walletId))
          case Some(ref) =>
            // 子アクターの参照取得
            ref.asInstanceOf[ActorRef[CommandRequest]]
        }
      }
      Behaviors.receiveMessage[CommandRequest] { msg =>
        createAndSend(msg.walletId) ! msg
        Behaviors.same
      }
    }
  }

}
