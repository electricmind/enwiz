package ru.wordmetrix.enwiz

import scala.concurrent.{ ExecutionContext, Promise }

import scala.concurrent.duration.DurationInt
import scala.util.Try

import org.json4s.{ DefaultFormats, Formats }
import org.scalatra.{ AsyncResult, FutureSupport, NotFound, ScalatraServlet }
import org.scalatra.json.JacksonJsonSupport

import EnWizLookup._
import EnWizParser.{ EnWizStatusRequest, EnWizStatus, EnWizTaskId }
import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern.ask
import akka.util.Timeout

/**
 * A servlet that provides access to API with JSON
 */
object Probability {
    def apply(pair: (String, Double)): Probability = pair match {
        case (w, p) => Probability(w, p)
    }
}

case class Probability(word: String, probability: Double)

class EnWizJSON(system: ActorSystem, lookup: ActorRef)
        extends ScalatraServlet with FutureSupport with JacksonJsonSupport { //with GZipSupport{
    protected implicit def executor: ExecutionContext = system.dispatcher
    implicit val defaultTimeout = Timeout(10 second)
    protected implicit val jsonFormats: Formats = DefaultFormats

    before() {
        contentType = formats("json")
    }

    /**
     * Return a list of words along with conditional probabilities to follow
     * after a bigram (word1, word2)
     */
    def result(prefix: String, path: String, word1: String, word2: String) =
        new AsyncResult() {
            val promise = Promise[List[Probability]]()
            val is = promise.future
            lookup ? EnWizWords(word1, word2) onSuccess {
                case Some(words: List[(String, Double)]) =>
                    promise.complete(Try(
                        words.map(x => Probability(x))
                    ))

                case None => NotFound(s"Sorry, unknown words")
            }
        }

    get("/words/?") {
        result("words", "", "", "")
    }

    get("/words/:word2") {
        result("", "", "", params("word2"))
    }

    get("/words/:word1/:word2") {
        result("", "", params("word1"), params("word2"))
    }
    /**
     * Return a progress of texts' parsing
     */
    get("/progress") { 
        new AsyncResult() {
            val promise = Promise[List[(EnWizTaskId,String)]]()
            val is = promise.future
            lookup ? EnWizStatusRequest() onSuccess {
                case EnWizStatus(tasks) =>
                    promise.complete(Try(
                        tasks map {
                            case (tid, part) => (tid, f"${part}%4.2f")
                        }
                    ))

                case None => NotFound(s"Sorry, unknown words")
            }
        }
    }
    /**
     * Return mnemonic for a sequence of figures. 
     */
    
    def memento = new AsyncResult() {
            val promise = Promise[(Boolean,List[String],String,String)]
            val is = promise.future
            val key = params("figures")
            
            lookup ? EnWizPi2WordsRequest(
                    key.split("").map(x => Try(x.toInt).toOption).flatten.toList.take(15)
                    ) onSuccess {
                case EnWizPi2Words(Left(words)) => 
                    promise.complete( Try(true,words,words.mkString(" "),key)  )
                case EnWizPi2Words(Right(words)) => 
                    promise.complete( Try(false,words,words.mkString(" "),key)  )
            }
        }

    
    get("/memento/:figures") {
        memento
    } 
    
    get("/memento/?") {
        memento
    }

}
