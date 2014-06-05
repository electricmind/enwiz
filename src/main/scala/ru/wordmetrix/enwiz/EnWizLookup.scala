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

    case class EnWizMnemonicRequest(ns: List[Int]) extends EnWizMessage

    case class EnWizMnemonic(ns: Either[List[String], List[String]]) extends EnWizMessage

    case class EnWizAcronymRequest(ls: List[String]) extends EnWizMessage

    case class EnWizAcronym(ns: Either[List[String], List[String]]) extends EnWizMessage

    case class EnWizPhraseRequest(ls: List[String]) extends EnWizMessage

    case class EnWizPhrase(probability: Double) extends EnWizMessage

    case class EnWizGapRequest(ws1: List[String], ws2: List[String]) extends EnWizMessage

    case class EnWizGap(wps1: List[(String, Double)]) extends EnWizMessage

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

    def betterof(ws1: List[String], ws2: List[String]) =
        if (ws1.length > ws2.length) ws1 else ws2

    def findLeft(it: Iterator[Either[List[String], List[String]]], best: List[String]): Either[List[String], List[String]] = if (it.nonEmpty) {
        it.next() match {
            case Left(ws) =>
                Left(ws)
            case Right(ws) =>
                findLeft(it, betterof(ws, best))
        }
    } else {
        Right(best)
    }

    def ns2ws[A](ns: List[A], best: List[String], ws: List[String], t: Long)(
        f: (A, String) => Boolean //PartialFunction[(A,String), Boolean]
        ): Either[List[String], List[String]] =
        if (t < System.currentTimeMillis()) {
            Right(betterof(best, ws))
        } else ns match {
            case n :: ns =>
                ws match {
                    case word2 :: word1 :: _ =>
                        val better = betterof(best, ws)

                        findLeft(
                            coll.findOne($and(
                                "kind" $eq "bigram",
                                "word1" $eq word1,
                                "word2" $eq word2)) match {
                                case Some(bigram) =>
                                    val count: Double = bigram.getOrElse("probability", 0.0).toString.toDouble

                                    coll.find($and(
                                        "kind" $eq "trigram",
                                        "word1" $eq word1,
                                        "word2" $eq word2
                                    )).sort(MO("probability" -> -1)).
                                        map(x => {
                                            (
                                                x.get("word3").toString,
                                                x.get("probability").toString.toDouble / count
                                            )
                                        }) filter {
                                            case (word3, _) =>
                                                f(n, word3)
                                            case _ => false
                                        } map {
                                            case (word3, p) =>
                                                //                                    println(s" ok : $n : $word3 x $p : $ws")
                                                ns2ws(ns, better, word3 :: ws, t)(f)
                                        }
                                case None => Iterator.empty

                            },
                            better
                        )
                }
            case List() =>
                println(s"result: ${ws.reverse.mkString(" ")})")
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

    def receive(): Receive = {
        /**
         * Return sequence of words each has a length equal to according number
         */
        case EnWizMnemonicRequest(ns) =>
            Future {
                EnWizMnemonic(
                    ns2ws(ns.map(x => if (x == 0) 10 else x), List(), List("", ""), System.currentTimeMillis() + 25000) {
                        case (n, word3) => !Set("'", ",", "-")(word3) && word3.length == n
                    } match {
                        case Left(x)  => Left(x.reverse)
                        case Right(x) => Right(x.reverse)
                    }
                )
            } pipeTo sender

        /**
         * Return sequence of words each begins with a letter of an acronym
         */
        case EnWizAcronymRequest(ls) =>
            println(s"lookup $ls")
            Future {
                EnWizAcronym(
                    ns2ws(ls, List(), List("", ""), System.currentTimeMillis() + 25000) {
                        case (letter, word3) => letter.head.toLower == word3.head.toLower
                    } match {
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
                val trigram = coll.find("kind" $eq "trigram").count();
                val bigram = coll.find("kind" $eq "bigram").count();
                val unigram = coll.find("kind" $eq "unigram").count();
                val average = (coll.find("kind" $eq "trigram").map {
                    x =>
                        x.getOrElse("probability", "0").toString.toDouble
                } reduceOption (_ + _)) getOrElse (0.0) / trigram

                println(EnWizStat(unigram, bigram, trigram,
                    average))
                sender ! EnWizStat(unigram, bigram, trigram,
                    average)
            }

        case EnWizWords(word1, word2) =>
            coll.findOne($and(
                "kind" $eq "bigram",
                "word1" $eq word1,
                "word2" $eq word2)) match {
                case Some(bigram) =>
                    val count: Double = bigram.getOrElse("probability", 0.0).toString.toDouble
                    sender ! Some(
                        coll.find($and(
                            "kind" $eq "trigram",
                            "word1" $eq word1,
                            "word2" $eq word2
                        ))
                            .sort(MO("probability" -> -1))
                            .map(x =>
                                (x.get("word3"), x.get("probability").toString.toDouble / count)
                            ).toList
                    )
                case None => None
            }

        case EnWizGapRequest(ws1, w3 :: _) =>
            println(s"$ws1 x $w3")

            sender ! EnWizGap(coll.find($and(
                "kind" $eq "trigram",
                "word1" $eq ws1.last,
                "word3" $eq w3
            ))
                .sort(MO("probability" -> -1))
                .map(x =>
                    (x.get("word2").toString -> x.get("probability").toString.toDouble)
                ).toList.sortBy(-_._2))

        case EnWizGapRequest(ws1, ws2) =>
            val wps1: Map[String, Double] = ws1 match {
                case w11 :: w12 :: _ => coll.findOne($and(
                    "kind" $eq "bigram",
                    "word1" $eq w11,
                    "word2" $eq w12)) match {
                    case Some(bigram) =>
                        val count: Double = bigram.getOrElse("probability", 0.0).toString.toDouble
                        coll.find($and(
                            "kind" $eq "trigram",
                            "word1" $eq w11,
                            "word2" $eq w12
                        ))
                            .sort(MO("probability" -> -1))
                            .map(x =>
                                (x.get("word3").toString -> x.get("probability").toString.toDouble / count)
                            ).toMap

                    case None => Map()
                }

                case w11 :: _ => coll.findOne($and(
                    "kind" $eq "unigram",
                    "word1" $eq w11)) match {
                    case Some(uniram) =>
                        val count: Double = uniram.getOrElse("probability", 0.0).toString.toDouble
                        coll.find($and(
                            "kind" $eq "bigram",
                            "word1" $eq w11
                        ))
                            .sort(MO("probability" -> -1))
                            .map(x =>
                                (x.get("word2").toString -> x.get("probability").toString.toDouble / count)
                            ).toMap

                    case None => Map()
                }

            }

            val wps2: Map[String, Double] = ws2 match {
                case w22 :: w23 :: _ => coll.findOne($and(
                    "kind" $eq "bigram",
                    "word1" $eq w22,
                    "word2" $eq w23)) match {
                    case Some(bigram) =>
                        val count: Double = bigram.getOrElse("probability", 0.0).toString.toDouble
                        coll.find($and(
                            "kind" $eq "trigram",
                            "word2" $eq w22,
                            "word3" $eq w23
                        ))
                            .sort(MO("probability" -> -1))
                            .map(x =>
                                (x.get("word1").toString -> x.get("probability").toString.toDouble / count)
                            ).toMap
                    case None => Map()
                }

                case w22 :: _ =>
                    coll.findOne($and(
                        "kind" $eq "unigram",
                        "word1" $eq w22)) match {
                        case Some(unigram) =>
                            val count: Double = unigram.getOrElse("probability", 0.0).toString.toDouble
                            coll.find($and(
                                "kind" $eq "bigram",
                                "word2" $eq w22
                            ))
                                .sort(MO("probability" -> -1))
                                .map(x =>
                                    (x.get("word1").toString -> x.get("probability").toString.toDouble / count)
                                ).toMap
                        case None => Map()
                    }
            }
            println(ws1, ws2)
            //println(wps1)
            //println(wps2)
            sender ! EnWizGap((wps1.keySet & wps2.keySet).map(x => x -> wps1(x) * wps2(x)).toList.sortBy(-_._2))

        case EnWizPhraseRequest(words) =>
            sender ! EnWizPhrase(words.sliding(3).map({

                case List(w1, w2, w3) =>
                    val count = coll.find($and(
                        "kind" $eq "bigram",
                        "word1" $eq w1,
                        "word2" $eq w2)).map(x => x.getOrElse("probability", 0.0).
                        toString.toDouble).sum

                    coll.findOne($and(
                        "kind" $eq "trigram",
                        "word1" $eq w1,
                        "word2" $eq w2,
                        "word3" $eq w3
                    )) match {
                        case Some(trigram) =>
                            trigram.getOrElse("probability", 0.0).
                                toString.toDouble / count
                        case None => 0.0d
                    }

                case List(w1, w2, w3) =>
                    coll.findOne($and(
                        "kind" $eq "bigram",
                        "word1" $eq w1,
                        "word2" $eq w2)) match {
                        case Some(bigram) =>
                            val count: Double = bigram.getOrElse("probability", 0.0).toString.toDouble
                            coll.findOne($and(
                                "kind" $eq "trigram",
                                "word1" $eq w1,
                                "word2" $eq w2,
                                "word3" $eq w3
                            )) match {
                                case Some(trigram) =>
                                    trigram.getOrElse("probability", 0.0).
                                        toString.toDouble / count
                                case None => 0.0d
                            }
                        case None => 0.0d
                    }
            }).reduceOption(_ * _).getOrElse(0.0d))
    }
}
