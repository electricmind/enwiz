package ru.wordmetrix.enwiz

import scala.concurrent.{ ExecutionContext, Promise }
import scala.concurrent.duration.DurationInt
import scala.util.Try

import org.json4s.{ DefaultFormats, Formats }
import org.scalatra.{ AsyncResult, FutureSupport, NotFound, ScalatraServlet }
import org.scalatra.json.JacksonJsonSupport

import EnWizLookup.EnWizWords
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
                    println(2)
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

}
