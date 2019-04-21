package wallet.adaptor.untyped.json

import java.time.Instant

import de.huxhorn.sulky.ulid.ULID
import monocle.Iso
import wallet.adaptor.json.MoneyJson
import wallet.adaptor.untyped.WalletProtocol.WalletPayed
import wallet.domain.{ ChargeId, WalletId }

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
      walletId = event.walletId.value.toString,
      toWalletId = event.toWalletId.value.toString,
      money = moneyJsonIso.get(event.money),
      chargeId = event.chargeId.map(_.value.toString),
      occurredAt = event.occurredAt.toEpochMilli
    )
  } { json =>
    WalletPayed(
      walletId = WalletId(ULID.parseULID(json.walletId)),
      toWalletId = WalletId(ULID.parseULID(json.toWalletId)),
      money = moneyJsonIso.reverseGet(json.money),
      chargeId = json.chargeId.map(v => ChargeId(ULID.parseULID(v))),
      occurredAt = Instant.ofEpochMilli(json.occurredAt)
    )
  }

}
