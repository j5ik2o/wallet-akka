package wallet.domain

final case class Money(breachEncapsulationOfAmount: BigDecimal) extends Ordered[Money] {

  def add(other: Money): Money = {
    copy(breachEncapsulationOfAmount = breachEncapsulationOfAmount + other.breachEncapsulationOfAmount)
  }

  def multi(other: Double): Money = {
    copy(breachEncapsulationOfAmount = breachEncapsulationOfAmount * other)
  }

  override def compare(that: Money): Int =
    breachEncapsulationOfAmount compare that.breachEncapsulationOfAmount

}

object Money {

  val zero = Money(BigDecimal(0))

}
