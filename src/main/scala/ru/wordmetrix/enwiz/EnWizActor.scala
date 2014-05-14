package ru.wordmetrix.enwiz

import EnWizLookup.{EnWizStatRequest, EnWizWords}
import EnWizParser.EnWizText
import akka.actor.{Actor, Props, actorRef2Scala}

/**
 * Dispatcher of requests that resends time-consuming request
 * into special queue. 
 */

object EnWizActor {
    abstract sealed trait EnWizMessage
    def props(lookupprop: Props, parserprop: Props): Props =
        Props(new EnWizActor(lookupprop, parserprop))
}

class EnWizActor(lookupprop: Props, parserprop: Props) extends Actor {
    val lookup = context.actorOf(lookupprop, "Lookup")
    val parser = context.actorOf(parserprop, "Parser")

    import EnWizParser._
    import EnWizLookup._

    def receive(): Receive = {
        case msg @ EnWizStatRequest() =>
            lookup forward msg

        case msg @ EnWizWords(word1, word2) =>
            lookup forward msg

        case msg @ EnWizText(text) =>
            parser forward msg
    }
}
