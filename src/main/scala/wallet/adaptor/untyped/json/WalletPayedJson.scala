package wallet.adaptor.untyped.json

import java.time.Instant

import de.huxhorn.sulky.ulid.ULID
import monocle.Iso
import wallet.adaptor.json.MoneyJson
import wallet.adaptor.untyped.WalletProtocol.WalletPayed

final case class WalletPayedJson(walletId: String, money: MoneyJson, requestId: Option[String], occurredAt: Long)

object WalletPayedJson {
  import MoneyJson._

  implicit val walletPayedJsonIso = Iso[WalletPayed, WalletPayedJson] { event =>
    WalletPayedJson(
      walletId = event.walletId.toString,
      money = moneyJsonIso.get(event.money),
      requestId = event.requestId.map(_.toString),
      occurredAt = event.occurredAt.toEpochMilli
    )
  } { json =>
    WalletPayed(
      walletId = ULID.parseULID(json.walletId),
      money = moneyJsonIso.reverseGet(json.money),
      requestId = json.requestId.map(ULID.parseULID),
      occurredAt = Instant.ofEpochMilli(json.occurredAt)
    )
  }

}
