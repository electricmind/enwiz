package ru.wordmetrix.enwiz
import com.mongodb.casbah.Imports._

import java.util.Properties
import scala.collection.JavaConverters._
import java.io.FileInputStream
/**
 * Bind a collection to MongoDB
 * MongoDB access data comes from ~/enwizdb.cfg
 */

trait EnWizMongo {
    val MO = MongoDBObject
    val prop = new Properties() match {
        case prop =>
            prop.load(new FileInputStream(
                System.getProperty("user.home") + "/enwizdb.cfg"));
            prop.asScala
    }

    val host: String = prop("host")
    val dbname: String = prop("dbname")
    val user: String = prop("user")
    val password: String = prop("password")
    val port: String = prop("port")

    //TODO: ensure indexes
    
    lazy val coll = MongoClient(MongoClientURI(
        s"mongodb://$user:$password@$host:$port/$dbname")
    )(dbname)("words")
    
    ensureIndexes
    
    def ensureIndexes = {
        coll.ensureIndex(MO("word1" -> 1))
        coll.ensureIndex(MO("word2" -> 1))
        coll.ensureIndex(MO("word3" -> 1))
        coll.ensureIndex(MO("word1" -> 1, "word2" -> 1))
        coll.ensureIndex(MO("word1" -> 1, "word2" -> 1, "word3" -> 1))
        coll.ensureIndex(MO("staat" -> 1))
    }
}