package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.ActorRef
import wallet.domain.{ Balance, Money }
import wallet.{ CommandId, RequestId, WalletId }

object WalletProtocol {

  sealed trait Message
  sealed trait Event   extends Message
  sealed trait Command extends Message
  sealed trait CommandRequest extends Command {
    val id: CommandId
    val walletId: WalletId
  }
  sealed trait CommandResponse extends Command

  // 作成
  case class CreateWalletRequest(id: CommandId, walletId: WalletId) extends CommandRequest

  sealed trait CreateWalletResponse extends CommandResponse

  case object CreateWalletSucceeded extends CreateWalletResponse

  case class CreateWalletFailed(message: String) extends CreateWalletResponse

  case class WalletCreated(walletId: WalletId) extends Event

  // 入金
  case class DepositRequest(id: CommandId, walletId: WalletId, money: Money, createdAt: Instant) extends CommandRequest

  sealed trait DepositResponse extends CommandResponse

  case object DepositSucceeded extends DepositResponse

  case class DepositFailed(message: String) extends DepositResponse

  case class WalletDeposited(walletId: WalletId, money: Money, createdAt: Instant) extends Event

  // 支払い
  case class PayRequest(
      id: CommandId,
      walletId: WalletId,
      money: Money,
      requestId: Option[RequestId],
      createdAt: Instant
  ) extends CommandRequest

  sealed trait PayResponse extends CommandResponse

  case object PaySucceeded extends PayResponse

  case class PayFailed(message: String) extends PayResponse

  case class WalletPayed(walletId: WalletId, money: Money, requestId: Option[RequestId], createdAt: Instant)
      extends Event

  // 請求
  case class RequestRequest(id: CommandId, requestId: RequestId, walletId: WalletId, money: Money, createdAt: Instant)
      extends CommandRequest

  sealed trait RequestResponse extends CommandResponse

  case object RequestSucceeded extends RequestResponse

  case class RequestFailed(message: String) extends RequestResponse

  case class WalletRequested(requestId: RequestId, walletId: WalletId, money: Money, createdAt: Instant) extends Event

  // 残高確認
  case class GetBalanceRequest(id: CommandId, walletId: WalletId) extends CommandRequest

  case class GetBalanceResponse(balance: Balance) extends CommandResponse

  // 購読者
  case class AddSubscribers(id: CommandId, walletId: WalletId, subscribers: Vector[ActorRef]) extends CommandRequest

  // シャットダウン
  case class Shutdown(id: CommandId, walletId: WalletId) extends CommandRequest

}
