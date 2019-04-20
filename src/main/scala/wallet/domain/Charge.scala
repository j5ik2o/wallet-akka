package wallet.domain

import java.time.Instant

import wallet.{ ChargeId, WalletId }

final case class Charge(id: ChargeId, walletId: WalletId, money: Money, occurredAt: Instant)
