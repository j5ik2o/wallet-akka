package wallet.domain

import java.time.Instant

import wallet._

final case class ChargeId(value: ULID)
final case class Charge(id: ChargeId, fromWalletId: WalletId, toWalletId: WalletId, money: Money, occurredAt: Instant)
