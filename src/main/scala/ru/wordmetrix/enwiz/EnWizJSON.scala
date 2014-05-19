package ru.wordmetrix.enwiz

import scala.concurrent.{ ExecutionContext, Promise }

import scala.concurrent.duration.DurationInt
import scala.util.Try
import org.json4s.{ DefaultFormats, Formats }
import org.scalatra.{ AsyncResult, FutureSupport, NotFound, ScalatraServlet }
import org.scalatra.json.JacksonJsonSupport
import EnWizLookup._
import EnWizAccessLog._
import EnWizParser.{ EnWizStatusRequest, EnWizStatus, EnWizTaskId }
import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern.ask
import akka.util.Timeout
import scala.util.Success
import scala.util.Failure
import org.scalatra.BadRequest

/**
 * A servlet that provides access to API with JSON
 */
object Probability {
    def apply(pair: (String, Double)): Probability = pair match {
        case (w, p) => Probability(w, p)
    }
}

case class Probability(word: String, probability: Double)

class EnWizJSON(system: ActorSystem, lookup: ActorRef, log: ActorRef)
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
            log ! EnWizAccessLogWords(
                ip = request.getRemoteAddr(), word1, word2)

            lookup ? EnWizWords(word1, word2) onComplete {
                case Success(Some(words: List[(String, Double)])) =>
                    promise.complete(Try(
                        words.map(x => Probability(x))
                    ))

                case Success(None) => NotFound(s"Sorry, unknown words")
                case Failure(f) =>
                    status = 502
                    promise.complete(Failure(f))

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
            val promise = Promise[List[(EnWizTaskId, String)]]()
            val is = promise.future
            lookup ? EnWizStatusRequest() onComplete {
                case Success(EnWizStatus(tasks)) =>
                    promise.complete(Try(
                        tasks map {
                            case (tid, part) => (tid, f"${part * 100}%4.2f")
                        }
                    ))

                case Success(None) => NotFound(s"Sorry, unknown words")
                case Failure(f) =>
                    status = 502
                    promise.complete(Failure(f))
            }
        }
    }
    
    /**
     * Return mnemonic for a sequence of figures.
     */

    def memento = new AsyncResult() {
        val promise = Promise[(Boolean, List[String], String, String)]
        val is = promise.future
        val query = params.getOrElse("figures", "")
        val figures = query.split("").map(
            x => Try(x.toInt).toOption).flatten.toList.take(25)

        def complete(success: Boolean, words: List[String]) = {
            val phrase = words.mkString(" ")
            log ! EnWizAccessLogMnemonic(

                request.getRemoteAddr(), query, figures.mkString,
                phrase
            )

            promise.complete(Try(success, words, phrase, query))
        }

        lookup ? EnWizPi2WordsRequest(
            figures
        ) onComplete {
                case Success(EnWizPi2Words(Left(words))) =>
                    complete(true, words)

                case Success(EnWizPi2Words(Right(words))) =>
                    complete(false, words)

                case Failure(f) =>
                    status = 502
                    promise.complete(Failure(f))
            }
    }

    get("/memento/:figures") {
        memento
    }

    get("/memento/?") {
        memento
    }

    /**
     * Return acronym for a sequence of chars.
     */

    def acronym = new AsyncResult() {
        val promise = Promise[(Boolean, List[String], String, String)]
        val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("").tail.toSet
        val is = promise.future
        val query = params.getOrElse("acronym", "")
        val letters = query.split("").filter(x=>allowed.contains(x.toUpperCase())).take(25).toList

        def complete(success: Boolean, words: List[String]) = {
            
            val phrase = words.map({
                case ""=> ""
                case s => s.head.toUpper + s.tail.toLowerCase
            }).mkString(" ")
            
            log ! EnWizAccessLogAcronym(
                request.getRemoteAddr(), query, letters.mkString,
                phrase
            )

            promise.complete(Try(success, words, phrase, letters.map(_ + ".").mkString.toUpperCase()))
        }
        lookup ? EnWizAcronymRequest(
            letters
        ) onComplete {
                case Success(EnWizAcronym(Left(words))) =>
                    complete(true, words)

                case Success(EnWizAcronym(Right(words))) =>
                    complete(false, words)

                case Failure(f) =>
                    status = 502
                    promise.complete(Failure(f))
            }
    }

    get("/acronym/:acronym") {
        acronym
    }

    get("/acronym/?") {
        acronym
    }
    
}
