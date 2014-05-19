package ru.wordmetrix.enwiz

import scala.concurrent.{ ExecutionContext, Promise }

import scala.concurrent.duration.DurationInt
import scala.util.Try
import org.scalatra.{ AsyncResult, FutureSupport, BadRequest }
import org.scalatra.servlet.{ FileUploadSupport, MultipartConfig }
import EnWizLookup._
import EnWizAccessLog._
import EnWizParser.{ EnWizText, EnWizTaskId }
import akka.actor.{ ActorRef, ActorSystem, actorRef2Scala }
import akka.pattern.ask
import akka.util.Timeout
import scala.util.Success
import scala.util.Failure

/**
 * Servlet that provides UI
 */
class EnWizServlet(system: ActorSystem, lookup: ActorRef, log: ActorRef) extends EnwizStack
        with FutureSupport with FileUploadSupport with AuthenticationSupport { //with GZipSupport{

    configureMultipartHandling(MultipartConfig(maxFileSize = Some(100 * 1024 * 1024)))

    protected implicit def executor: ExecutionContext = system.dispatcher
    implicit val defaultTimeout = Timeout(100 second)

    /**
     * Page with phrase generator
     */
    get("/") {
        contentType = "text/html"
        ssp("/main.ssp")
    }

    get("/readme") {
        contentType = "text/html"
        ssp("/readme.ssp")
    }
    
    get("/generate") {
        contentType = "text/html"
        ssp("/generate.ssp")
    }
    
    get("/mnemonic") {
        contentType = "text/html"
        ssp("/mnemonic.ssp")
    }
    
    get("/acronym") {
        contentType = "text/html"
        ssp("/acronym.ssp")
    }
    
    get("/upload") {
        contentType = "text/html"
        basicAuth
        ssp("/upload.ssp")
    }

    
    
    get("/admin") {
        contentType = "text/html"
        basicAuth
        ssp("/generator.ssp", "restricted" -> true)
    }

    
    
    /**
     * Page with statistic
     */
    get("/stat") {
        contentType = "text/html"
        new AsyncResult() {
            val promise = Promise[String]()
            val is = promise.future

            lookup ? EnWizStatRequest() onComplete {
                case Success(EnWizStat(count1, count2, count3, average)) =>
                    promise.complete(Try(
                        ssp("/stat.ssp",
                            "count1" -> count1,
                            "count2" -> count2,
                            "count3" -> count3,
                            "average" -> average
                        )
                    ))
                case Failure(f) =>
                    promise.complete(Try(
                        BadRequest().toString
                    ))
            }
        }

    }
    
    
    get("/memento/?") {
        contentType = "text/html"
        new AsyncResult() {
            val promise = Promise[String]
            val is = promise.future

            def page(ok: Boolean, words: List[String]) =
                ssp("/memento.ssp",
                    "layout" -> "WEB-INF/layouts/light.ssp",
                    "ok" -> ok,
                    "words" -> words.mkString(" ")
                )

            val query = params.getOrElse("numbers", "")
            val figures = query.split("").map(
                x => Try(x.toInt).toOption
            ).flatten.toList

            def writeLog(words: List[String]) = log ! EnWizAccessLogMnemonic(
                request.getRemoteAddr(), query, figures.mkString,
                words.mkString(" ")
            )

            lookup ? EnWizPi2WordsRequest(
                figures
            ) onSuccess {
                    case EnWizPi2Words(Left(words)) =>
                        writeLog(words)
                        promise.complete(Try(page(true, words)))
                    case EnWizPi2Words(Right(words)) =>
                        writeLog(words)
                        promise.complete(Try(page(false, words)))
                }
        }
    }

    /**
     * Page that posts new text
     */
    val taskids = Iterator.from(1)
    def load = {
        contentType = "text/html"
        basicAuth

        params.get("text") match {
            case None =>
            case Some(text) =>
                lookup ! EnWizText(EnWizTaskId(taskids.next, ""), text)
        }

        for {
            files <- fileMultiParams.get("text")
            file <- files
        } {
            lookup ! EnWizText(
                EnWizTaskId(taskids.next, file.getName),
                io.Source.fromInputStream(file.getInputStream).
                    getLines.mkString("\n")
            )

        }

        ssp("/load.ssp", "layout" -> "WEB-INF/layouts/light.ssp", "text" -> "")
    }

    get("/load")(load)

    post("/load")(load)
}
