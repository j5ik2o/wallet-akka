package wallet.adaptor.untyped.json

import java.time.Instant

import de.huxhorn.sulky.ulid.ULID
import monocle.Iso
import wallet.adaptor.json.MoneyJson
import wallet.adaptor.untyped.WalletProtocol.WalletCharged

final case class WalletChargedJson(chargeId: String, walletId: String, money: MoneyJson, occurredAt: Long)

object WalletChargedJson {

  import MoneyJson._

  implicit val walletChargedJsonIso = Iso[WalletCharged, WalletChargedJson] { event =>
    WalletChargedJson(
      chargeId = event.chargeId.toString,
      walletId = event.walletId.toString,
      money = moneyJsonIso.get(event.money),
      occurredAt = event.occurredAt.toEpochMilli
    )
  } { json =>
    WalletCharged(
      chargeId = ULID.parseULID(json.chargeId),
      walletId = ULID.parseULID(json.walletId),
      money = moneyJsonIso.reverseGet(json.money),
      occurredAt = Instant.ofEpochMilli(json.occurredAt)
    )
  }

}
