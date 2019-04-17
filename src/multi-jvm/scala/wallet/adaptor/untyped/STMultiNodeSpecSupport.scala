package wallet.adaptor.untyped

import akka.remote.testkit.MultiNodeSpecCallbacks
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }

trait STMultiNodeSpecSupport extends MultiNodeSpecCallbacks with FreeSpecLike with Matchers with BeforeAndAfterAll {

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

}
