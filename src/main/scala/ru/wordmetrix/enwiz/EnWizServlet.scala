package ru.wordmetrix.enwiz

import scala.concurrent.{ ExecutionContext, Promise }

import scala.concurrent.duration.DurationInt
import scala.util.Try
import org.scalatra.{ AsyncResult, FutureSupport }
import org.scalatra.servlet.{ FileUploadSupport, MultipartConfig }
import EnWizLookup._
import EnWizParser.{ EnWizText, EnWizTaskId }
import akka.actor.{ ActorRef, ActorSystem, actorRef2Scala }
import akka.pattern.ask
import akka.util.Timeout

/**
 * Servlet that provides UI
 */
class EnWizServlet(system: ActorSystem, lookup: ActorRef) extends EnwizStack
        with FutureSupport with FileUploadSupport with AuthenticationSupport { //with GZipSupport{

    configureMultipartHandling(MultipartConfig(maxFileSize = Some(10 * 1024 * 1024)))

    protected implicit def executor: ExecutionContext = system.dispatcher
    implicit val defaultTimeout = Timeout(100 second)

    /**
     * Page with phrase generator
     */
    get("/") {
        contentType = "text/html"
        ssp("/generator.ssp")
    }

    /**
     * Page with statistic
     */
    get("/stat") {
        contentType = "text/html"
        new AsyncResult() {
            val promise = Promise[String]()
            val is = promise.future

            lookup ? EnWizStatRequest() onSuccess {
                case EnWizStat(count1, count2, count3, average) =>
                    promise.complete(Try(
                        ssp("/stat.ssp",
                            "count1" -> count1,
                            "count2" -> count2,
                            "count3" -> count3,
                            "average" -> average
                        )
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

            lookup ? EnWizPi2WordsRequest(
                params.getOrElse("numbers", "").split("").map(
                    x => Try(x.toInt).toOption
                ).flatten.toList
            ) onSuccess {
                    case EnWizPi2Words(Left(words)) =>
                        promise.complete(Try(page(true, words)))
                    case EnWizPi2Words(Right(words)) =>
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

        fileParams.get("text") match {
            case Some(file) =>
                lookup ! EnWizText(
                    EnWizTaskId(taskids.next, file.getName),
                    io.Source.fromInputStream(file.getInputStream).
                        getLines.mkString("\n")
                )
            case None =>
        }

        ssp("/load.ssp", "layout" -> "WEB-INF/layouts/light.ssp", "text" -> "")
    }

    get("/load")(load)

    post("/load")(load)
}
