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
import akka.pattern.pipe

/**
 * Actor that serves short-time requests that requires an answer
 */
object EnWizLookup {
    abstract sealed trait EnWizMessage

    case class EnWizWords(word1: String, word2: String) extends EnWizMessage
    case class EnWizStatRequest() extends EnWizMessage
    case class EnWizStat(count1: Int, count2: Int, count3: Int,
                         average: Double) extends EnWizMessage

    case class EnWizPi2WordsRequest(ns: List[Int]) extends EnWizMessage

    case class EnWizPi2Words(ns: Either[List[String], List[String]]) extends EnWizMessage

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
         * Return sequence of words each has a length equal to according number
         */
        case EnWizPi2WordsRequest(ns) =>
            def betterof(ws1: List[String], ws2: List[String]) =
                if (ws1.length > ws2.length) ws1 else ws2

            def findLeft(it: Iterator[Either[List[String], List[String]]], best: List[String]): Either[List[String], List[String]] = if (it.nonEmpty) {
                it.next() match {
                    case Left(ws)  => Left(ws)
                    case Right(ws) => findLeft(it, betterof(ws, best))
                }
            } else {
                Right(best)
            }

            def ns2ws(ns: List[Int], best: List[String], ws: List[String], t: Long): Either[List[String], List[String]] = if (t < System.currentTimeMillis()) {
                Right(betterof(best, ws))
            } else ns match {
                case n :: ns =>
                    ws match {
                        case word2 :: word1 :: _ =>
                            val better = betterof(best, ws)

                            findLeft(coll.find($and(
                                "word1" $eq word1,
                                "word2" $eq word2,
                                "word3" $exists true,
                                "probability" $exists true)).
                                sort(MO("probability" -> -1)).
                                limit(100).map(x => {
                                    (x.get("word3").toString, x.get("probability").toString.toDouble)
                                }) filter {
                                    case (word3, _) => !Set("'", ",", "-")(word3) && word3.length == n
                                    case _          => false
                                } map {
                                    case (",", _) =>
                                        ns2ws(ns, better, "," :: ws, t)
                                    case (word3, p) =>
                                        //println(s" ok : $n : $word3 x $p : $ws")
                                        ns2ws(ns, better, word3 :: ws, t)
                                },
                                better
                            )
                    }
                case List() =>
                    ws match {
                        case word2 :: word1 :: _ =>
                            coll.findOne($and(
                                "word1" $eq word1,
                                "word2" $eq word2,
                                "word3" $eq ","
                            )) match {
                                case Some(_) => Left("." :: ws)
                                case None    => Right(ws)
                            }
                    }
            }

            Future {
                EnWizPi2Words(
                    ns2ws(ns.map(x => if (x == 0) 10 else x), List(), List("", ""), System.currentTimeMillis() + 10000) match {
                        case Left(x)  => Left(x.reverse)
                        case Right(x) => Right(x.reverse)
                    }
                )
            } pipeTo sender

        /**
         * Return a set of statistics of vocabulary
         */
        case EnWizStatRequest() =>
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
                }).getOrElse("value", 0.0).toString().toDouble

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
                }).getOrElse("value", 0.0).toString().toDouble

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
                }).getOrElse("value", 0.0).toString().toDouble

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
                }).getOrElse("value", 0.0).toString().toDouble

                println(s"return trigram $trigram")

                sender ! EnWizStat(unigram.toInt, bigram.toInt, trigram.toInt,
                    average / trigram)
            } else {
                val records = coll.find($and(
                    "word1" $exists true,
                    "word2" $exists true,
                    "word3" $exists true,
                    "probability" $exists true
                )).map(x => (
                    x.get("word1").toString,
                    x.get("word2").toString,
                    x.get("word3").toString,
                    x.get("probability").toString.toDouble)
                ).toSet

                val average = records.iterator.map({
                    case (w1, w2, w3, p) =>
                        p.toString.toDouble
                }).reduceOption(_ + _).getOrElse(0.0) / records.size

                val trigram: Set[(String, String, String)] = records.map({
                    case (w1, w2, w3, _) => (w1, w2, w3)
                })

                val bigram: Set[(String, String)] = trigram.map({ case (w1, w2, _) => (w1, w2) })

                val unigram: Set[String] = bigram.map({ case (w1, _) => (w1) })
                println(EnWizStat(unigram.size, bigram.size, trigram.size,
                    average))
                sender ! EnWizStat(unigram.size, bigram.size, trigram.size,
                    average)
            }

        case EnWizWords(word1, word2) =>
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
