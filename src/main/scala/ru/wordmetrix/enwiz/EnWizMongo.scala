package ru.wordmetrix.enwiz
import com.mongodb.casbah.Imports._
import com.typesafe.config.ConfigFactory
import java.util.Properties
import scala.collection.JavaConverters._
import java.io.FileInputStream
import java.io.File

/**
 * Bind a collection to MongoDB
 * MongoDB access data comes from ~/enwizdb.cfg
 */

object EnWizMongo {
    
}

trait EnWizMongo {
    val MO = MongoDBObject

    val cfg = ConfigFactory.parseFile(
        new File(System.getProperty("user.home"), "enwizdb.cfg")
    ).getObject("mongo").toConfig()

    val host: String = cfg.getString("host")
    val dbname: String = cfg.getString("dbname")
    val user: String = cfg.getString("user")
    val password: String = cfg.getString("password")
    val port: String = cfg.getString("port")

    lazy val coll = MongoClient(MongoClientURI(
        s"mongodb://$user:$password@$host:$port/$dbname")
    )(dbname)("words")

    def version() = {
        coll.findOne("stat" $eq "version") match {
            case Some(stat) => stat.getOrElse("value", 0)
            case None => 0
        }
    }

    def version(version: Integer) = {
        coll.update(
            "stat" $eq "version",
            $set("value" -> version),
            upsert = true
        )
    }

    def update : Unit = EnWizMongo.synchronized  {
        println(s"Update # ${version()} $dbname")
        version() match {
            case 0 =>
                coll.ensureIndex(MO("word1" -> 1))
                coll.ensureIndex(MO("word2" -> 1))
                coll.ensureIndex(MO("word3" -> 1))
                coll.ensureIndex(MO("word1" -> 1, "word2" -> 1))
                coll.ensureIndex(MO("word1" -> 1, "word2" -> 1, "word3" -> 1))
                coll.ensureIndex(MO("stat" -> 1))
                version(1)
                update
                
            case _ =>
                println(s"Update completed")
        }
    }
    
    update
}