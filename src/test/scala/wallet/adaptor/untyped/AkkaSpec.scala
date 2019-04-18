package wallet.adaptor.untyped

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.testkit.{ ImplicitSender, TestKit }
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Span }
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }

object AkkaSpec {
  val testConf: Config = ConfigFactory.parseString("""
      akka {
        loggers = ["akka.testkit.TestEventListener"]
        loglevel = "WARNING"
        stdout-loglevel = "WARNING"
        actor {
          default-dispatcher {
            executor = "fork-join-executor"
            fork-join-executor {
              parallelism-min = 8
              parallelism-factor = 2.0
              parallelism-max = 8
            }
          }
        }
      }
                                                    """)

  def mapToConfig(map: Map[String, Any]): Config = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseMap(map.asJava)
  }

  def getCallerName(clazz: Class[_]): String = {
    val s = Thread.currentThread.getStackTrace
      .map(_.getClassName)
      .drop(1)
      .dropWhile(_.matches("(java.lang.Thread|.*AkkaSpec.*|.*\\.StreamSpec.*|.*MultiNodeSpec.*|.*\\.Abstract.*)"))
    val reduced = s.lastIndexWhere(_ == clazz.getName) match {
      case -1 => s
      case z  => s.drop(z + 1)
    }
    reduced.head.replaceFirst(""".*\.""", "").replaceAll("[^a-zA-Z_0-9]", "_")
  }

}

abstract class AkkaSpec(_system: ActorSystem)
    extends TestKit(_system)
    with FreeSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender
    with ScalaFutures
    with ActorSpecSupport {

  implicit val patience = PatienceConfig(testKitSettings.DefaultTimeout.duration, Span(100, Millis))

  def this(config: Config) =
    this(ActorSystem(AkkaSpec.getCallerName(getClass), ConfigFactory.load(config.withFallback(AkkaSpec.testConf))))

  def this(s: String) = this(ConfigFactory.parseString(s))

  def this(configMap: Map[String, _]) = this(AkkaSpec.mapToConfig(configMap))

  def this() = this(ActorSystem(AkkaSpec.getCallerName(getClass), AkkaSpec.testConf))

  val log: LoggingAdapter = Logging(system, this.getClass)

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  protected def atStartup(): Unit = {}

  protected def beforeTermination(): Unit = {}

  protected def afterTermination(): Unit = {}

  final override def beforeAll: Unit = {
    atStartup()
  }

  final override def afterAll: Unit = {
    beforeTermination()
    shutdown()
    afterTermination()
  }

}
