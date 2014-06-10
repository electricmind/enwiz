package ru.wordmetrix.enwiz

import scala.annotation.implicitNotFound
import com.mongodb.casbah.Imports._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.DurationInt

import com.mongodb.casbah.Imports.{
    $and,
    $set,
    CoreOperatorResultObjOk,
    MongoDBObject,
    mongoQueryStatements,
    wrapDBObj
}

import akka.actor.{ Actor, ActorRef, Props, actorRef2Scala }
import akka.util.Timeout
import akka.pattern.pipe

/**
 * Actor that serves short-time requests that requires an answer
 */
object EnWizAccessLog {
    abstract sealed trait EnWizMessage

    case class EnWizAccessLogWords(ip: String, word1: String, word2: String) extends EnWizMessage
    case class EnWizAccessLogMnemonic(ip: String, request: String, figures: String, response: String) extends EnWizMessage
    case class EnWizAccessLogAcronym(ip: String, request: String, acronym: String, response: String) extends EnWizMessage

    def props(): Props = Props(new EnWizAccessLog())
}

class EnWizAccessLog() extends Actor with EnWizMongo {

    import EnWizAccessLog._

    override lazy val coll = MongoClient(MongoClientURI(
        settings.url
    ))(settings.dbname)("logs")

    coll.ensureIndex(MO("ip" -> 1))
    coll.ensureIndex(MO("time" -> -1))


    def receive(): Receive = {
        /**
         * Add Record into access log for words lookup
         */
        case EnWizAccessLogWords(ip, word1, word2) =>
            coll.insert(MO(
                "ip" -> ip,
                "time" -> System.currentTimeMillis(),
                "action" -> "words",

                "word1" -> word1,
                "word2" -> word2
            ))
            
        case msg @ EnWizAccessLogMnemonic(ip, request, figures, response) =>
                        coll.insert(MO(
                "ip" -> ip,
                "time" -> System.currentTimeMillis(),
                "action" -> "mnemonic",
                "request" -> request,
                "digits" -> figures,
                "response" -> response
            ))
            
            
        case msg @ EnWizAccessLogAcronym(ip, request, figures, response) =>
            coll.insert(MO(
                "ip" -> ip,
                "time" -> System.currentTimeMillis(),
                "action" -> "acronym",
                "request" -> request,
                "acronym" -> figures,
                "response" -> response
            ))

    }
}
