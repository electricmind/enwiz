package ru.wordmetrix.enwiz

import scala.concurrent.Promise

import scala.util.Try
import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.scalatra._
import scalate.ScalateSupport
import EnWizLookup._
import org.json4s.{ DefaultFormats, Formats }
import org.scalatra.json._
import scala.xml.Unparsed
/**
 * Servlet for testing purposes
 */
class EnWizSimple(system: ActorSystem, lookup: ActorRef) extends EnwizStack with FutureSupport with AuthenticationSupport { //with GZipSupport{

    protected implicit def executor: ExecutionContext = system.dispatcher
    implicit val defaultTimeout = Timeout(10000)

    before("*") {
        basicAuth
    }

    get("/") {
        <html lang="en">
            <head>
                <meta charset="utf-8"/>
                <title>EnWiz : </title>
                <link rel="stylesheet" href="http://code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css"/>
                <script src="http://code.jquery.com/jquery-1.9.1.js"></script>
                <script src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script>
                <script src="http://ajax.microsoft.com/ajax/jquery.templates/beta1/jquery.tmpl.min.js"></script>
                <script>
                    {
                        Unparsed("""
$(function() {
   $("#mainmenu").menu();
})
""")
                    }
                </script>
                <script id="dropmenu" type="text/x-jquery-tmpl">{
                    Unparsed("""
                    <ul class="words">{{each(i,wp) wps}}<li>
                                                            <a class="menuitem" data-word="${wp.word}" href="#${wp.word}">${ wp.word } : ${ wp.probability } </a>
                                                        </li>{{/each}}</ul>
            """)
                } </script>
                <script id="phrase" type="text/x-jquery-tmpl">
                    <div class="phrase">
                        <span class="word" data-word=""></span>
                    </div>
                </script>
                <script id="phraseitem" type="text/x-jquery-tmpl">
                    {
                        Unparsed("""

                    <span class="word" data-word="${word}">
                        ${ word }
                    </span> """)
                    }
                </script>
                <script>
                    {
                        Unparsed("""
$(function(){
                            $( ".words" ).menu();
                            
                            function handler(event) {
                            
                              a = $(event.target).parents(".words");
                            
                              var w1 = $(a).prev().data("word");
                              var w2 = $(event.target).data("word");
                            
                              $("#phraseitem").tmpl({
                                "word" : $(event.target).data("word")
                              }).appendTo(".phrase :last")
                            
                              $(a).remove();
                            
                            
                              if (w2 == ".") {
                                  $(".phrase :last").after($("#phrase").tmpl());
                                  first();
                              } else {
                                  var url = "";
                                  if (w1 == "") {
                                     url = "/json/words/" + w2;
                                  } else {
                                     url = "/json/words/" + w1 + "/" + w2;
                                  }
                                
                                  $.ajax({
                                     url: url
                                  }).done(function(data) {
                                     $("#dropmenu").tmpl({wps : data}).appendTo(".phrase :last");
                                     $(".words").menu();
                                     $(".menuitem").on("click", handler);
                                  });
                              }
                            }
                            
                            function first() {
                              $.ajax({
                                url: "/json/words/"
                              }).done(function(data) {
                                $("#dropmenu").tmpl({wps : data}).appendTo(".phrase :last");
                                $(".words").menu();
                                $(".menuitem").on("click", handler);
                              });
                            }
                            
                            first();
                    }) """)
                    }
                </script>
            </head>
            <body>
                <h1>English Wizard to generate phrases</h1>
                <div id="header">
                    <p>
                        This application helps to write English sentences. For an
        english sentence it prompts the range of words along with
        probabilities that could continue the sentence. You can chose a
        word of drop-list to do begin.
                    </p>
                </div>
                <div>
                    <div class="phrase">
                        <span class="word" data-word=""></span>
                    </div>
                </div>
                <hr/>
                <div>
                    <ul id="mainmenu">
                        <li><a href="/load">Load text</a></li>
                        <li><a href="/">Generate text</a></li>
                        <li><a href="/words">Explore words</a></li>
                    </ul>
                </div>
            </body>
        </html>
    }

    get("/hello") {
        <html><body>Hello, world!</body></html>
    }

    get("""^/words/(.*)/$""".r) {
        params("captures") match {
            case captures => <html><body> { captures.split("/").mkString(" ") }. </body></html>
        }
    }

    def result(prefix: String, path: String, word1: String, word2: String) = new AsyncResult() {
        val promise = Promise[scala.xml.Elem]()
        val is = promise.future

        lookup ? EnWizWords(word1, word2) onSuccess {
            case Some(words: List[(String, Double)]) =>
                promise.complete(Try(
                    <html><body>
                              <p><a href="/">Main</a></p>
                              {
                                  for (word <- path.split("/")) yield { word + " " }
                              }
                              {
                                  word1
                              }
                              {
                                  word2
                              }
                              <ul> {
                                  for ((w, p) <- words) yield {
                                      <li>
                                          <a href={ prefix + word2 + "/" + w }>
                                              { w }
                                              :{ p }
                                          </a>
                                      </li>
                                  }
                              } </ul>
                          </body></html>

                ))

            case None => NotFound(s"Sorry, unknown words")
        }
    }

    get("/words/?") {
        result("words", "", "", "")
    }

    get("/words/:word2") {
        result("", "", "", params("word2"))
    }

    get("/words/:word1/:word2") {
        result("", "", params("word1"), params("word2"))
    }

    get("""/words(/.+)+/([^/]+)/([^/]+)$""".r) {
        multiParams("captures") match {
            case Seq(path, word1, word2) => result("", path, word1, word2)
        }
    }
}
