package ru.wordmetrix.enwiz

import scala.annotation.implicitNotFound

import com.mongodb.casbah.Imports.{ $inc, DoubleOk, MongoDBObject }

import akka.actor.{ Actor, Props }
import ru.wordmetrix.nlp.NLP.string2NLP
/**
 * Actor that parses a long text calculating probabilities
 */
object EnWizParser {
    abstract sealed trait EnWizMessage

    case class EnWizText(text: String) extends EnWizMessage
    def props(): Props = Props(new EnWizParser())
}

class EnWizParser() extends Actor with EnWizMongo {
    import EnWizParser._

    def receive(): Receive = {
        case EnWizText(text) =>
            for {
                sentence <- text.phrases
                Vector(word1, word2, word3) <- {
                    println(sentence)
                    ("" +: "" +:
                        sentence.tokenize.toVector :+ ".") sliding 3
                }
            } coll.update(
                MongoDBObject("word1" -> word1, "word2" -> word2,
                    "word3" -> word3),
                $inc("probability" -> 1.0),
                upsert = true
            )
    }
}
