package wallet.adaptor.typed.json

import java.time.Instant

import de.huxhorn.sulky.ulid.ULID
import monocle.Iso
import wallet.adaptor.json.MoneyJson
import wallet.adaptor.typed.WalletProtocol.WalletCharged
import wallet.domain.{ ChargeId, WalletId }

final case class WalletChargedJson(chargeId: String, walletId: String, money: MoneyJson, occurredAt: Long)

object WalletChargedJson {

  import MoneyJson._

  implicit val walletChargedJsonIso = Iso[WalletCharged, WalletChargedJson] { event =>
    WalletChargedJson(
      chargeId = event.chargeId.value.toString,
      walletId = event.walletId.value.toString,
      money = moneyJsonIso.get(event.money),
      occurredAt = event.occurredAt.toEpochMilli
    )
  } { json =>
    WalletCharged(
      chargeId = ChargeId(ULID.parseULID(json.chargeId)),
      walletId = WalletId(ULID.parseULID(json.walletId)),
      money = moneyJsonIso.reverseGet(json.money),
      occurredAt = Instant.ofEpochMilli(json.occurredAt)
    )
  }

}
