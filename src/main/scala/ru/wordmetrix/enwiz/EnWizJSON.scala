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
import org.scalatra.GZipSupport
import ru.wordmetrix.nlp.NLP._

/**
 * A servlet that provides access to API with JSON
 */
object Probability {
    def apply(pair: (String, Double)): Probability = pair match {
        case (w, p) => Probability(w, p)
    }
}

case class Probability(word: String, probability: Double)

object Status extends Enumeration("OK", "Timeout", "Best", "Error") {
    type Status = Value
    val OK, Timeout, Best, Error = Value
}

object Result {
    def apply[F](status: Status.Status) = ResultFail[F](status)
    def apply[F](status: Status.Status, data: F) = ResultOK[F](status, data)
}

class Result[F](status: Status.Status)

case class ResultOK[F](status: Status.Status, data: F) extends Result[F](status)

case class ResultFail[F](status: Status.Status) extends Result[F](status)

case class ResultDataMnemonic(words: List[String], phrase: String, query: String)

case class ResultDataAcronym(words: List[String], phrase: String, query: String)

class EnWizJSON(system: ActorSystem, lookup: ActorRef, log: ActorRef)
        extends ScalatraServlet with FutureSupport with JacksonJsonSupport { //with GZipSupport{
    protected implicit def executor: ExecutionContext = system.dispatcher
    implicit val defaultTimeout = Timeout(30 second)
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
            val promise = Promise[Result[List[Probability]]]()
            val is = promise.future
            log ! EnWizAccessLogWords(
                ip = request.getRemoteAddr(), word1, word2)

            lookup ? EnWizWords(word1, word2) onComplete {
                case Success(Some(words: List[(String, Double)])) =>
                    promise.complete(Try(
                        ResultOK(Status.OK, words.map(x => Probability(x)))
                    ))

                case Success(None) => NotFound(s"Sorry, unknown words")
                case Failure(f) =>
                    promise.complete(Try(ResultFail(Status.Error)))

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
            val promise = Promise[Result[List[(EnWizTaskId, String)]]]()
            val is = promise.future

            lookup ? EnWizStatusRequest() onComplete {
                case Success(EnWizStatus(tasks)) =>
                    promise.complete(Try(ResultOK(Status.OK,
                        tasks map {
                            case (tid, part) => (tid, f"${part * 100}%4.2f")
                        }
                    )))

                case Success(None) => NotFound(s"Sorry, unknown words")

                case Failure(f) =>
                    promise.complete(Try(ResultFail(Status.Error)))
            }
        }
    }

    /**
     * Return mnemonic for a sequence of figures.
     */

    def mnemonic = new AsyncResult() {
        val promise = Promise[Result[ResultDataMnemonic]]
        val is = promise.future
        val query = params.getOrElse("digits", "")
        val digits = query.split("").map(
            x => Try(x.toInt).toOption).flatten.toList.take(25)

        lookup ? EnWizMnemonicRequest(
            digits
        ) onComplete {
                case Success(EnWizMnemonic(r)) =>
                    val (status, words) = r match {
                        case Left(words)  => (Status.OK, words)
                        case Right(words) => (Status.Best, words)
                    }
                    val phrase = words.mkString(" ")

                    promise.complete(Try(
                        Result(Status.OK, ResultDataMnemonic(words, phrase, query))
                    ))

                case Failure(f: akka.pattern.AskTimeoutException) =>
                    println(f)
                    promise.complete(Try(Result(Status.Timeout)))

                case Failure(f) =>
                    println(f)
                    promise.complete(Try(Result(Status.Error)))
            }
    }

    get("/mnemonic/:digits") {
        mnemonic
    }

    get("/mnemonic/?") {
        mnemonic
    }

    /**
     * Return acronym for a sequence of chars.
     */

    def acronym = new AsyncResult() {
        val promise = Promise[Result[ResultDataAcronym]]
        val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("").tail.toSet
        val is = promise.future
        val query = params.getOrElse("acronym", "")
        val letters = query.split("").filter(x => allowed.contains(x.toUpperCase())).take(25).toList

        lookup ? EnWizAcronymRequest(
            letters
        ) onComplete {
                case Success(EnWizAcronym(r)) =>
                    val (status, words) = r match {
                        case Left(words)  => (Status.OK, words)
                        case Right(words) => (Status.Best, words)
                    }
                    val phrase = words.map({
                        case "" => ""
                        case s  => s.head.toUpper + s.tail.toLowerCase
                    }).mkString(" ")
                    log ! EnWizAccessLogAcronym(
                        request.getRemoteAddr(), query, letters.mkString,
                        phrase
                    )

                    promise.complete(Try(
                        ResultOK(status, ResultDataAcronym(words, phrase, letters.map(_ + ".").mkString.toUpperCase()))
                    ))

                case Failure(f: akka.pattern.AskTimeoutException) =>
                    println(f)
                    promise.complete(Try(Result(Status.Timeout)))

                case Failure(f) =>
                    println(f)
                    promise.complete(Try(Result(Status.Error)))
            }
    }

    get("/acronym/:acronym") {
        acronym
    }

    get("/acronym/?") {
        acronym
    }

    get("/phrase/:phrase") {
        new AsyncResult() {
            val promise = Promise[Result[(List[String], String, Double)]]
            val is = promise.future
            val phrase = params.getOrElse("phrase", "")
            val query = phrase.tokenize

            lookup ? EnWizPhraseRequest(
                query
            ) onComplete {
                    case Success(EnWizPhrase(probability)) =>
                        promise.complete(Try(
                            Result(Status.OK, (query, phrase, probability))
                        ))

                    case Failure(f: akka.pattern.AskTimeoutException) =>
                        promise.complete(Try(Result(Status.Timeout)))

                    case Failure(f) =>
                        promise.complete(Try(Result(Status.Error)))
                }
        }
    }
}
