package ru.wordmetrix.enwiz

import scala.concurrent.{ ExecutionContext, Promise }
import scala.concurrent.duration.DurationInt
import scala.util.Try

import org.scalatra.{ AsyncResult, FutureSupport }
import org.scalatra.servlet.{ FileUploadSupport, MultipartConfig }

import EnWizLookup.{ EnWizStat, EnWizStatRequest }
import EnWizParser.EnWizText
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

    /**
     * Page that posts new text
     */
    def load = {
        contentType = "text/html"
        basicAuth

        params.get("text") match {
            case None =>
            case Some(text) =>
                lookup ! EnWizText(text)
        }

        fileParams.get("text") match {
            case Some(file) =>
                lookup ! EnWizText(
                    io.Source.fromInputStream(file.getInputStream).
                        getLines.mkString("\n")
                )
            case None =>
        }

        ssp("/load.ssp", "text" -> "")
    }

    get("/load")(load)

    post("/load")(load)
}
