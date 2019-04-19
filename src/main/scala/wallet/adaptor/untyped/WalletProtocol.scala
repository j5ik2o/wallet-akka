package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.ActorRef
import wallet.domain.{ Balance, Money }
import wallet.{ ChargeId, CommandId, WalletId }

object WalletProtocol {

  sealed trait Message
  sealed trait Event extends Message {
    def occurredAt: Instant
  }
  sealed trait CommandMessage extends Message
  sealed trait CommandRequest extends CommandMessage {
    def id: CommandId
    def walletId: WalletId
  }
  sealed trait CommandResponse extends CommandMessage

  // 作成
  case class CreateWalletRequest(id: CommandId, walletId: WalletId, createdAt: Instant) extends CommandRequest

  sealed trait CreateWalletResponse extends CommandResponse

  case object CreateWalletSucceeded extends CreateWalletResponse

  case class CreateWalletFailed(message: String) extends CreateWalletResponse

  case class WalletCreated(walletId: WalletId, occurredAt: Instant) extends Event

  // 入金
  case class DepositRequest(id: CommandId, walletId: WalletId, money: Money, createdAt: Instant) extends CommandRequest

  sealed trait DepositResponse extends CommandResponse

  case object DepositSucceeded extends DepositResponse

  case class DepositFailed(message: String) extends DepositResponse

  case class WalletDeposited(walletId: WalletId, money: Money, occurredAt: Instant) extends Event

  // 支払い
  case class PayRequest(
      id: CommandId,
      walletId: WalletId,
      toWalletId: WalletId,
      money: Money,
      chargeId: Option[ChargeId],
      createdAt: Instant
  ) extends CommandRequest

  sealed trait PayResponse extends CommandResponse

  case object PaySucceeded extends PayResponse

  case class PayFailed(message: String) extends PayResponse

  case class WalletPayed(
      walletId: WalletId,
      toWalletId: WalletId,
      money: Money,
      chargeId: Option[ChargeId],
      occurredAt: Instant
  ) extends Event

  // 請求
  case class ChargeRequest(id: CommandId, chargeId: ChargeId, walletId: WalletId, money: Money, createdAt: Instant)
      extends CommandRequest

  sealed trait ChargeResponse extends CommandResponse

  case object ChargeSucceeded extends ChargeResponse

  case class ChargeFailed(message: String) extends ChargeResponse

  case class WalletCharged(chargeId: ChargeId, walletId: WalletId, money: Money, occurredAt: Instant) extends Event

  // 残高確認
  case class GetBalanceRequest(id: CommandId, walletId: WalletId) extends CommandRequest

  case class GetBalanceResponse(balance: Balance) extends CommandResponse

  // 購読者
  case class AddSubscribers(id: CommandId, walletId: WalletId, subscribers: Vector[ActorRef]) extends CommandRequest

  // シャットダウン
  case class Shutdown(id: CommandId, walletId: WalletId) extends CommandRequest

}
