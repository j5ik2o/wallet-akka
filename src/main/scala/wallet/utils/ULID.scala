package wallet.utils

object ULID {

  val ENCODE_TABLE: Array[Char] = Array(
    '0',
    '1',
    '2',
    '3',
    '4',
    '5',
    '6',
    '7',
    '8',
    '9',
    'a',
    'b',
    'c',
    'd',
    'e',
    'f',
    'g',
    'h',
    'j',
    'k',
    'm',
    'n',
    'p',
    'q',
    'r',
    's',
    't',
    'v',
    'w',
    'x',
    'y',
    'z'
  )
  val TIME_LENGTH               = 10
  val RANDOM_LENGTH             = 16
  val RANDOM_MULTIPLIER: Double = Math.pow(2, 40)

  private def encode(buffer: StringBuilder, value: Long, length: Int): Unit = {
    def loop(index: Int, acc: StringBuilder, lvalue: Long): String = {
      index match {
        case 0 => acc.result()
        case n =>
          val remainder = (lvalue % ENCODE_TABLE.length).asInstanceOf[Int]
          acc.append(ENCODE_TABLE(remainder))
          loop(n - 1, acc, (value - remainder) / ENCODE_TABLE.length)
      }
    }
    loop(length, buffer, value)
  }

  private def encodeTime(buffer: StringBuilder): Unit = { encode(buffer, System.currentTimeMillis, TIME_LENGTH) }

  private def encodeRandom(buffer: StringBuilder): Unit = {
    encode(buffer, (Math.random * RANDOM_MULTIPLIER).asInstanceOf[Long], RANDOM_LENGTH / 2)
    encode(buffer, (Math.random * RANDOM_MULTIPLIER).asInstanceOf[Long], RANDOM_LENGTH / 2)
  }

  def generate: ULID = {
    val builder = new StringBuilder
    encodeRandom(builder)
    encodeTime(builder)
    ULID(builder.reverse.toString)
  }

}

final case class ULID(private val value: String) {
  def asString: String = value
}
