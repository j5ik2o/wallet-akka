package wallet.adaptor.typed

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior }
import wallet.WalletId
import wallet.adaptor.typed.WalletProtocol.CommandRequest

import scala.concurrent.duration.FiniteDuration

object WalletAggregates {

  def behavior(receiveTimeout: FiniteDuration, requestsLimit: Int = Int.MaxValue): Behavior[CommandRequest] = {
    def createAndSend(ctx: ActorContext[CommandRequest], walletId: WalletId): ActorRef[CommandRequest] = {
      ctx.child(WalletAggregate.name(walletId)) match {
        case None =>
          // 子アクター作成
          ctx.spawn(
            WalletAggregate.behavior(walletId, receiveTimeout, requestsLimit),
            name = WalletAggregate.name(walletId)
          )
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
