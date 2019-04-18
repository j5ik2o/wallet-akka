package wallet.adaptor.typed

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior }
import wallet.WalletId
import wallet.adaptor.typed.WalletProtocol.CommandRequest

object WalletAggregates {

  val name = "wallets"

  def behavior(
      requestsLimit: Int = Int.MaxValue
  )(behaviorF: (WalletId, Int) => Behavior[CommandRequest]): Behavior[CommandRequest] = {
    def createAndSend(ctx: ActorContext[CommandRequest], walletId: WalletId): ActorRef[CommandRequest] = {
      ctx.child(WalletAggregate.name(walletId)) match {
        case None =>
          // 子アクター作成
          val childRef = ctx.spawn(
            behaviorF(walletId, requestsLimit),
            name = WalletAggregate.name(walletId)
          )
          ctx.watch(childRef)
          childRef
        case Some(ref) =>
          // 子アクターの参照取得
          ref.asInstanceOf[ActorRef[CommandRequest]]
      }
    }
    Behaviors.setup { ctx =>
      Behaviors.receiveMessage[CommandRequest] { msg =>
        createAndSend(ctx, msg.walletId) ! msg
        Behaviors.same
      }
    }
  }

}
