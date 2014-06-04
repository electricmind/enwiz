package ru.wordmetrix.enwiz

import EnWizLookup.{ EnWizStatRequest, EnWizMnemonicRequest, EnWizWords }

//import akka.routing.ActorRefRoutee
import akka.routing.Router
import akka.routing.RoundRobinRouter
//import akka.routing.
//import akka.routing.RoundRobinRoutingLogic

import EnWizParser.EnWizText
import akka.actor.{ Actor, Props, actorRef2Scala }

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
    val lookup = context.actorOf(lookupprop.withRouter(
        RoundRobinRouter(nrOfInstances = 5)
    ))
    
    val parser = context.actorOf(parserprop, "Parser")

    import EnWizParser._
    import EnWizLookup._

    def receive(): Receive = {
        case msg @ EnWizStatRequest() =>
            lookup forward msg

        case msg @ EnWizWords(word1, word2) =>
            lookup forward msg

        case msg @ EnWizText(_,_) =>
            parser forward msg
            
        case msg @ EnWizStatusRequest() =>
            parser forward msg
            
        case msg @ EnWizMnemonicRequest(_) =>
            lookup forward msg
            
        case msg @ EnWizAcronymRequest(_) =>
            lookup forward msg

        case msg @ EnWizPhraseRequest(_) =>
            lookup forward msg

        case msg @ EnWizGapRequest(_,_) =>
            println(msg)
            lookup forward msg
    }
}
