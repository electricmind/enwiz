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


class EnWizSettingsAuthimpl extends Extension {
    private val cfg = ConfigFactory.parseFile(
        new File(System.getProperty("user.home"), "enwizdb.cfg")
    ).getObject("mongo").toConfig()

    val username: String = cfg.getString("username")
    val password: String = cfg.getString("password")
}

 
object EnWizSettingsAuth extends ExtensionId[EnWizSettingsAuthimpl] with ExtensionIdProvider {
    override def lookup = EnWizSettingsDB

    override def createExtension(system: ExtendedActorSystem) = new EnWizSettingsAuthimpl

    override def get(system : ActorSystem) = super.get(system)
}