package wallet.adaptor.untyped.json

import java.time.Instant

import de.huxhorn.sulky.ulid.ULID
import monocle.Iso
import wallet.adaptor.json.MoneyJson
import wallet.adaptor.untyped.WalletProtocol.WalletCharged
import wallet.domain.{ ChargeId, WalletId }

final case class WalletChargedJson(walletId: String, chargeId: String, money: MoneyJson, occurredAt: Long)

object WalletChargedJson {

  import MoneyJson._

  implicit val walletChargedJsonIso = Iso[WalletCharged, WalletChargedJson] { event =>
    WalletChargedJson(
      walletId = event.walletId.value.toString,
      chargeId = event.chargeId.value.toString,
      money = moneyJsonIso.get(event.money),
      occurredAt = event.occurredAt.toEpochMilli
    )
  } { json =>
    WalletCharged(
      walletId = WalletId(ULID.parseULID(json.walletId)),
      chargeId = ChargeId(ULID.parseULID(json.chargeId)),
      money = moneyJsonIso.reverseGet(json.money),
      occurredAt = Instant.ofEpochMilli(json.occurredAt)
    )
  }

}
