package ru.wordmetrix.nlp
/**
 * Text tokenizer
 * (acquired from an old HMM NLP project of my own)
 */
object NLP {
    type Phrase = String
    type Word = String

    implicit def string2NLP(s: String) = new NLP(s)
    implicit def list2NLP(l: List[String]) = new NLP(l)
    implicit def list2String(l: List[String]) = l.rectify()
    implicit def nlp2String(nlp: NLP) = nlp.phrase.rectify()

    val rPhrase = """((?<=\.\s{0,4})|^)\p{Lu}+[\p{L}\s,.’'\-]+?\p{Ll}{2}[?!.]($|(?=(\s*\p{Lu})))""".r
}

class NLP(val phrase: List[NLP.Phrase]) {

    def this(phrase: String) = this(phrase.split("\\b").map(
        x => x.replaceAll("\\s+", "")).filter(_ != "").map(
            x => "^\\p{Punct}+$".r.findFirstMatchIn(x) match {
                case None => List(x);
                case _    => x.split("").toList
            }).flatten.filter(_ != "").toList)

    import NLP._

    def phrases() = rPhrase.findAllIn("\\s+".r.replaceAllIn(phrase, " "))

    def tokenize(): List[NLP.Word] = phrase

    def tokenizeGap(): List[NLP.Word] = List("", "") ++ phrase

    def hidewords(ws: List[NLP.Word]): Phrase = hidewords_(ws)

    def hidewords_(ws: List[NLP.Word]): List[Word] = {
        def filter(ws1: List[Word], ws2: List[Word],
                   wout: List[Word] = List()): Option[List[Word]] =
            (ws1, ws2) match {
                case (w1 :: ws1, w2 :: ws2) if (w1.equalsIgnoreCase(w2)) =>
                    filter(ws1, ws2, "*" * w1.length :: wout)
                case (ws1, List())    => Some(wout.reverse ++ ws1)
                case (w1 :: ws1, ws2) => filter(ws1, ws2, w1 :: wout)
                case (List(), ws2)    => None
            }

        filter(phrase.tokenize(), ws) match {
            case Some(ws) => ws
            case None     => phrase
        }
    }

    def hidephrases(ps: List[NLP.Phrase]): Phrase = ps.
        map(_.tokenize).
        foldLeft(phrase)({
            case (p, ws) => p.hidewords_(ws.filter(_ != "*"))
        })

    def rectify(): String = {
        """\s+(\p{Punct})""".r.replaceAllIn(
            phrase.mkString(" "),
            m => scala.util.matching.Regex.quoteReplacement(m.group(1) match {
                    case "*" => " *";
                    case x   => x
                })
        ).replace("' ", "'")

    }

}
