package wallet.adaptor.json

import monocle.Iso
import wallet.domain.Money

final case class MoneyJson(amount: String)

object MoneyJson {

  implicit val moneyJsonIso = Iso[Money, MoneyJson] { model =>
    MoneyJson(
      model.breachEncapsulationOfAmount.toString()
    )
  } { json =>
    Money(
      BigDecimal(json.amount)
    )
  }

}
