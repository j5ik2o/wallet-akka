package wallet.domain

final case class Balance(breachEncapsulationOfValue: Money) extends Ordered[Balance] {

  def sub(other: Money): Balance = add(other multi -1)

  def add(other: Money): Balance =
    copy(breachEncapsulationOfValue = breachEncapsulationOfValue add other)

  override def compare(that: Balance): Int =
    breachEncapsulationOfValue compare that.breachEncapsulationOfValue
}

object Balance {

  val zero = Balance(Money.zero)

}
