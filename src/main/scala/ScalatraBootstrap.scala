import _root_.akka.actor.{ActorSystem, Props}
import ru.wordmetrix.enwiz._
import org.scalatra._
import javax.servlet.ServletContext


class ScalatraBootstrap extends LifeCycle {
    
  val system = ActorSystem()
  val actor = system.actorOf(EnWizActor.props(Props[EnWizLookup], Props[EnWizParser]))

  override def init(context: ServletContext) {
    context.mount(new EnWizServlet(system, actor), "/*")
    context.mount(new EnWizSimple(system, actor), "/simple/*")
    context.mount(new EnWizJSON(system, actor), "/json/*")
  }
  
  override def destroy(context:ServletContext) {
    system.shutdown()
  }
}
