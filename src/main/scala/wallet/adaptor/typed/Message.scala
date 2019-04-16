package wallet.adaptor.typed

import java.time.Instant

import akka.actor.typed.ActorRef
import wallet.domain.{ Balance, Money }
import wallet.{ RequestId, WalletId }

sealed trait Message
sealed trait Event extends Message

case class CreateWalletRequest(walletId: WalletId, replyTo: ActorRef[CreateWalletResponse]) extends Message
sealed trait CreateWalletResponse                                                           extends Message
case object CreateWalletSucceeded                                                           extends CreateWalletResponse
case class CreateWalletFailed(message: String)                                              extends CreateWalletResponse
case class WalletCreated(walletId: WalletId)                                                extends Event

case class DepositRequest(walletId: WalletId, money: Money, createdAt: Instant, replyTo: ActorRef[DepositResponse])
    extends Message
sealed trait DepositResponse                                                     extends Message
case object DepositSucceeded                                                     extends DepositResponse
case class DepositFailed(message: String)                                        extends DepositResponse
case class WalletDeposited(walletId: WalletId, money: Money, createdAt: Instant) extends Event

case class PayRequest(walletId: WalletId,
                      money: Money,
                      requestId: Option[RequestId],
                      createdAt: Instant,
                      replyTo: ActorRef[PayResponse])
    extends Message
sealed trait PayResponse                                                                                   extends Message
case object PaySucceeded                                                                                   extends PayResponse
case class PayFailed(message: String)                                                                      extends PayResponse
case class WalletPayed(walletId: WalletId, money: Money, requestId: Option[RequestId], createdAt: Instant) extends Event

case class RequestRequest(id: RequestId,
                          walletId: WalletId,
                          money: Money,
                          createdAt: Instant,
                          replyTo: ActorRef[RequestResponse])
    extends Message
sealed trait RequestResponse                                                                    extends Message
case object RequestSucceeded                                                                    extends RequestResponse
case class RequestFailed(message: String)                                                       extends RequestResponse
case class WalletRequested(id: RequestId, walletId: WalletId, moeny: Money, createdAt: Instant) extends Event

case class GetBalanceRequest(walletId: WalletId, replyTo: ActorRef[GetBalanceResponse]) extends Message
case class GetBalanceResponse(balance: Balance)                                         extends Message

case class AddSubscribers(subscribers: Vector[ActorRef[Event]]) extends Message

case object Shutdown extends Message
