package ru.wordmetrix.enwiz

import scala.annotation.implicitNotFound
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.DurationInt

import com.mongodb.casbah.Imports.{
    $and,
    $set,
    CoreOperatorResultObjOk,
    MongoDBObject,
    mongoQueryStatements,
    wrapDBObj
}

import akka.actor.{ Actor, ActorRef, Props, actorRef2Scala }
import akka.util.Timeout

/**
 * Actor that serves short-time requests that requires an answer
 */
object EnWizLookup {
    abstract sealed trait EnWizMessage

    case class EnWizWords(word1: String, word2: String) extends EnWizMessage
    case class EnWizStatRequest() extends EnWizMessage
    case class EnWizStat(count1: Int, count2: Int, count3: Int,
                         average: Double) extends EnWizMessage

    def props(): Props = Props(new EnWizLookup())
}

class EnWizLookup() extends Actor with EnWizMongo {

    import EnWizLookup._

    protected implicit def executor: ExecutionContext =
        scala.concurrent.ExecutionContext.global
    implicit val defaultTimeout = Timeout(100 second)

    /**
     * Return a statistic for database recalculating in needs.
     *
     * @param: name name of statistic
     * @param: f procedure that computes statistic
     * @return last known statistic
     */
    def stat(name: String, f: => MongoDBObject) = {
        val time = System.currentTimeMillis()

        val mo = coll.findOne($and("stat" $eq name)) match {
            case None =>
                MO("stat" -> name, "time" -> 0, "value" -> 0.0)
            case Some(mo) =>
                mo

        }
        println(s"stat = $mo")

        if (time - mo.getOrElse("time", 0).toString.toLong > 3600000) {
            coll.update(MO("stat" -> name), $set("time" -> time), upsert = true)

            Future({
                println(s"schedule to update $mo")
                f
            }).onSuccess {
                case stat: MongoDBObject =>
                    println(s"stat $stat mo = $mo")
                    coll.update(
                        MO("stat" -> name),
                        $set(
                            "time" -> System.currentTimeMillis(),
                            "value" -> stat.getOrElse("value", 0.0)
                        ),
                        upsert = true
                    )
            }
        } else {
            println("no update needed")
        }

        mo
    }

    def receive(): Receive = {
        /**
         * Return a set of statistics of vocabulary
         */
        case (EnWizStatRequest(), sender: ActorRef) =>
            if (host != "localhost") {
                val average = stat("average", {
                    println("calc ave")
                    coll.aggregate(List(
                        MO("$match" -> MO()),
                        MO("$group" ->
                            MO("_id" -> MO(
                                "word1" -> "$word1", "word2" -> "$word2",
                                "word3" -> "$word3"),
                                "p" -> MO("$sum" -> "$probability"))),
                        MO("$group" ->
                            MO("_id" -> 1, "value" -> MO("$sum" -> "$p"))))
                    ).results.head
                }).get("value").toString().toDouble

                println(s"return average $average")

                val unigram = stat("unigram", {
                    coll.aggregate(List(
                        MO("$match" -> MO()),
                        MO("$group" ->
                            MO("_id" -> MO(
                                "word1" -> "$word1"),
                                "p" -> MO("$sum" -> "$probability"))),
                        MO("$group" ->
                            MO("_id" -> 1, "value" -> MO("$sum" -> 1))))
                    ).results.head
                }).get("value").toString().toDouble

                println(s"return unigram $unigram")

                val bigram = stat("bigram", {
                    coll.aggregate(List(
                        MO("$match" -> MO()),
                        MO("$group" ->
                            MO("_id" -> MO(
                                "word1" -> "$word1", "word2" -> "$word2"),
                                "p" -> MO("$sum" -> "$probability"))),
                        MO("$group" ->
                            MO("_id" -> 1, "value" -> MO("$sum" -> 1))))
                    ).results.head
                }).get("value").toString().toDouble

                println(s"return bigram $bigram")

                val trigram = stat("trigram", {
                    coll.aggregate(List(
                        MO("$match" -> MO()),
                        MO("$group" ->
                            MO("_id" -> MO(
                                "word1" -> "$word1", "word2" -> "$word2",
                                "word3" -> "$word3"),
                                "p" -> MO("$sum" -> "$probability"))),
                        MO("$group" ->
                            MO("_id" -> 1, "value" -> MO("$sum" -> 1))))
                    ).results.head
                }).get("value").toString().toDouble

                println(s"return trigram $trigram")

                sender ! EnWizStat(unigram.toInt, bigram.toInt, trigram.toInt,
                    average / trigram)
            } else {
                val records = coll.find($and(
                    "word1" $exists true,
                    "word2" $exists true,
                    "word3" $exists true,
                    "probability" $exists true
                )).map(x => (x.get("word1"), x.get("word2"),
                    x.get("word3"), x.get("probability"))
                ).toSet

                val average = records.iterator.map({
                    case (w1, w2, w3, p) =>
                        p.toString.toDouble
                }).reduce(_ + _) / records.size

                val trigram = records.map({
                    case (w1, w2, w3, _) => (w1, w2, w3)
                })

                val bigram = trigram.map({ case (w1, w2, _) => (w1, w2) })

                val unigram = bigram.map({ case (w1, _) => (w1) })
                sender ! EnWizStat(unigram.size, bigram.size, trigram.size,
                    average)
            }

        case (EnWizWords(word1, word2), sender: ActorRef) =>
            sender ! Some(
                coll.find($and(
                    "word1" $eq word1,
                    "word2" $eq word2,
                    "word3" $exists true,
                    "probability" $exists true)).
                    sort(MO("probability" -> -1)).
                    limit(100).map(x =>
                        (x.get("word3"), x.get("probability"))
                    ).toList
            )
    }

}
