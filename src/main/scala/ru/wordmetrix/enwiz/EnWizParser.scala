package ru.wordmetrix.enwiz

import scala.annotation.implicitNotFound
import com.mongodb.casbah.Imports.{ $inc, DoubleOk, MongoDBObject }
import akka.actor.{ Actor, Props }
import akka.pattern.{ ask, pipe }
import ru.wordmetrix.nlp.NLP.string2NLP
import scala.collection.immutable.Queue
import scala.concurrent.{ ExecutionContext, Future }
//import akka.actor.`

/**
 * Actor that parses a long text calculating probabilities
 */
object EnWizParser {
    abstract sealed trait EnWizMessage

    case class EnWizTaskId(id: Int, title: String)

    case class EnWizText(task: EnWizTaskId, text: String) extends EnWizMessage
    case class EnWizCompleted(msg: EnWizTaskId) extends EnWizMessage
    case class EnWizProgress(msg: EnWizTaskId, progress: Double)
        extends EnWizMessage
    case class EnWizStatusRequest() extends EnWizMessage
    case class EnWizStatus(tasks: List[(EnWizTaskId, Double)])
        extends EnWizMessage

    def props(): Props = Props(new EnWizParser())
}

class EnWizParser() extends Actor with EnWizMongo {
    import EnWizParser._
    implicit val ec = context.dispatcher

    val queue = Queue[(EnWizText, Double)]();

    def runTask(msg: EnWizText) = Future {
        val trigrams = (for {
            sentence <- msg.text.phrases
            Vector(word1, word2, word3) <- {
                //   println(sentence)
                ("" +: "" +:
                    sentence.tokenize.toVector :+ ".") sliding 3
            }
        } yield (word1, word2, word3)) toList

        val size = trigrams.length

        for {
            ((word1, word2, word3), id) <- trigrams.zipWithIndex
        } {
            coll.update(
                MongoDBObject("word1" -> word1, "word2" -> word2,
                    "word3" -> word3),
                $inc("probability" -> 1.0),
                upsert = true
            );
            self ! EnWizProgress(msg.task, id.toDouble / size)
        }

        self ! EnWizCompleted(msg.task)

    } pipeTo self

    def idle(): Receive = {
        case msg @ EnWizText(task, text: String) =>
            context.become(running(
                Queue[EnWizText](), Map(task -> 0.0))
            )
            runTask(msg)

        case EnWizCompleted(task) =>
            println(s"I have no idea whose $task you are completed")

        case EnWizProgress(task, amount) =>
            println("I have no idea what $task that progress is about")

        case EnWizStatusRequest() =>
            sender ! EnWizStatus(List())
    }

    def running(queue: Queue[EnWizText],
                progress: Map[EnWizTaskId, Double]): Receive = {
        case msg @ EnWizText(task, text) =>
            context.become(running(queue.enqueue(msg),
                progress + (task -> 0.0)))

        case EnWizCompleted(task) =>
            if (queue.isEmpty) {
                context.become(idle)
            } else {
                queue.dequeue match {
                    case (msg, queue) =>
                        context.become(
                            running(queue, progress - task)
                        )
                        runTask(msg)
                }
            }

        case EnWizProgress(msg, amount) =>
            context.become(
                running(queue, progress + (msg -> amount))
            )

        case EnWizStatusRequest() =>
            sender ! EnWizStatus(progress.toList.sortBy(x => x._1.id)/*queue.toList.map(
                msg => msg.task -> progress(msg.task)
            )*/)
    }

    def receive(): Receive = idle
}
