package ru.wordmetrix.enwiz

import akka.actor.ActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.ExtendedActorSystem
import scala.concurrent.duration.Duration
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import com.typesafe.config.ConfigFactory
import java.io.File


class EnWizSettingsDBimpl extends Extension {
    private val cfg = ConfigFactory.parseFile(
        new File(System.getProperty("user.home"), "enwizdb.cfg")
    ).getObject("mongo").toConfig()

    val host: String = cfg.getString("host")
    val dbname: String = cfg.getString("dbname")
    val user: String = cfg.getString("user")
    val password: String = cfg.getString("password")
    val port: String = cfg.getString("port")
    val url = s"mongodb://$user:$password@$host:$port/$dbname"
}

 
object EnWizSettingsDB extends ExtensionId[EnWizSettingsDBimpl] with ExtensionIdProvider {
    override def lookup = EnWizSettingsDB

    override def createExtension(system: ExtendedActorSystem) = new EnWizSettingsDBimpl

    override def get(system : ActorSystem) = super.get(system)
}