//> using jvm "8"
//> using dep "com.typesafe.akka::akka-stream:2.6.20"
//> using file "Extensions.scala"
//> using scala 2.13

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.NotUsed
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps._
import FlowExtensions._
import akka.Done

object Main extends App {
    implicit val system: ActorSystem = ActorSystem("QuickStart")
    implicit val mat: Materializer = ActorMaterializer()

    val Parallelism = 3
    val MaxRetries = 2

    val source = Source(1 to 10)

    // A Business Logic that fails for not even input
    // otherwise: 2 -> "number: 2"
    val myBusinessLogicFlow = Flow[Int].mapAsync(Parallelism)(n =>
        Future {
            if (n % 2 == 0) s"number: $n"
            else throw new RuntimeException("it failed")
        }.safe(n)
    )
    // print results
    val sink = Sink.foreach {
        item: Any => println(s"result - item:[$item]")
    }
    
    // it could be a sqs deadletter queue
    val deadletterSink =
        Sink.foreach[Left[Failure, _]] {
            case Left(item) => println(s"so sad we give up on item:[${item}]")
        }

    // the graph
    source
      .via(myBusinessLogicFlow retrying MaxRetries divertErrors(deadletterSink))
      .to(sink)
      .run()
}
