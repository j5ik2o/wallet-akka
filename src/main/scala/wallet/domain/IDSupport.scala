package wallet.domain

import wallet.ULID

trait IDSupport {

  def value: ULID

  /**
    * Returns the most significant 64 bits of this ULID's 128 bit value.
    *
    * @return The most significant 64 bits of this ULID's 128 bit value
    */
  def getMostSignificantBits: Long = value.getMostSignificantBits

  /**
    * Returns the least significant 64 bits of this ULID's 128 bit value.
    *
    * @return The least significant 64 bits of this ULID's 128 bit value
    */
  def getLeastSignificantBits: Long = value.getLeastSignificantBits
}
