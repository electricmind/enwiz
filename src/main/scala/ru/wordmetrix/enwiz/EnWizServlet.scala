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
import org.scalatra.GZipSupport

/**
 * Servlet that provides UI
 */
class EnWizServlet(system: ActorSystem, lookup: ActorRef, log: ActorRef) extends EnwizStack
        with FutureSupport with FileUploadSupport with AuthenticationSupport {

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
        ssp("/main.ssp", "restricted" -> true)
    }

    
    
    /**
     * Page with statistic
     */
    get("/stat") {
        contentType = "text/html"
        basicAuth
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

        ssp("/upload.ssp", "layout" -> "WEB-INF/layouts/light.ssp", "text" -> "")
    }

    get("/load")(load)

    post("/load")(load)
}
