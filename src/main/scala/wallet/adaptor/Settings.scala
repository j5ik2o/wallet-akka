package wallet.adaptor

import scala.concurrent.duration._

import akka.actor._

import com.typesafe.config.Config

object Settings extends ExtensionId[Settings] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): Settings = new Settings(system.settings.config)

  override def lookup(): ExtensionId[_ <: Extension] = Settings
}

class Settings(config: Config) extends Extension {
  def this(system: ExtendedActorSystem) = this(system.settings.config)

  val passivateTimeout = Duration(config.getString("passivate-timeout"))

//  object http {
//    val host = config.getString("http.host")
//    val port = config.getInt("http.port")
//  }
}
