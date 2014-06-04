package ru.wordmetrix.enwiz

import java.io.FileInputStream
import java.util.Properties
import scala.collection.JavaConverters.propertiesAsScalaMapConverter
import org.scalatra.ScalatraBase
import org.scalatra.auth.{ ScentryConfig, ScentrySupport }
import org.scalatra.auth.strategy.{ BasicAuthStrategy, BasicAuthSupport }
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import com.typesafe.config.ConfigFactory
import java.io.File
import scala.util.Try
/**
 * Authentication strategy that saves identity in "~/enwizauth.cfg"
 */

class OurBasicAuthStrategy(protected override val app: ScalatraBase, realm: String)
        extends BasicAuthStrategy[User](app, realm) {

    protected def validate(
        userName: String,
        password: String)(
            implicit request: HttpServletRequest,
            response: HttpServletResponse): Option[User] = {

        val cfg = ConfigFactory.parseFile(
            new File(System.getProperty("user.home"), "enwizauth.cfg")
        ).getObject("auth").toConfig()

        val identity: String = cfg.getString("username")

        cfg.getString("password") match {
            case password if password == password &&
                userName == identity => Some(User(identity))
        }
    }

    protected def getUserId(
        user: User)(
            implicit request: HttpServletRequest,
            response: HttpServletResponse): String = user.id
}

trait AuthenticationSupport extends ScentrySupport[User] with BasicAuthSupport[User] {
    self: ScalatraBase =>

    val realm = "EnWiz Administrator"

    protected def fromSession = { case id: String => User(id) }
    protected def toSession = { case usr: User => usr.id }

    protected val scentryConfig = (new ScentryConfig {}).asInstanceOf[ScentryConfiguration]

    override protected def configureScentry = {
        scentry.unauthenticated {
            scentry.strategies("Basic").unauthenticated()
        }
    }

    override protected def registerAuthStrategies = {
        scentry.register("Basic", app => new OurBasicAuthStrategy(app, realm))
    }

}

case class User(id: String)