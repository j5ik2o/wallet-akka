package wallet.adaptor.typed.json

import java.time.Instant

import de.huxhorn.sulky.ulid.ULID
import monocle.Iso
import wallet.adaptor.json.MoneyJson
import wallet.adaptor.typed.WalletProtocol.WalletPayed

final case class WalletPayedJson(
    walletId: String,
    toWalletId: String,
    money: MoneyJson,
    chargeId: Option[String],
    occurredAt: Long
)

object WalletPayedJson {
  import MoneyJson._

  implicit val walletPayedJsonIso: Iso[WalletPayed, WalletPayedJson] = Iso[WalletPayed, WalletPayedJson] { event =>
    WalletPayedJson(
      walletId = event.walletId.toString,
      toWalletId = event.toWalletId.toString,
      money = moneyJsonIso.get(event.money),
      chargeId = event.chargeId.map(_.toString),
      occurredAt = event.occurredAt.toEpochMilli
    )
  } { json =>
    WalletPayed(
      walletId = ULID.parseULID(json.walletId),
      toWalletId = ULID.parseULID(json.toWalletId),
      money = moneyJsonIso.reverseGet(json.money),
      chargeId = json.chargeId.map(ULID.parseULID),
      occurredAt = Instant.ofEpochMilli(json.occurredAt)
    )
  }

}
