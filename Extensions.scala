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

case class Failure(attempt: Any, t: Throwable)

object FlowExtensions {

    implicit final class SafeFuture[In, Out](private val f: Future[Out]) extends AnyVal {
        def safe(attempt: In) =
            f.map(Right.apply)
            .recoverWith {
               case t: Throwable => Future.successful(Left(Failure(attempt, t))) 
            }
    }

    implicit final class FlowEx[In, Out](val flow: Flow[In, Either[Failure, Out], NotUsed]) extends AnyVal {
        def divertErrors(sinkError: Sink[Left[Failure, Out], Future[Done]]) = {
            val sink = Flow[Either[Failure, Out]]
              .collect({ case l@Left(failure) => l })
              .to(sinkError)

            flow
              .divertTo(sink, when = _.isLeft)
              .collect({ case Right(value) => value })
        }
    }

    implicit final class RetryFlowEx[In, Out](private val f: Flow[In, Either[Failure, Out], NotUsed]) extends AnyVal {
        def retrying(times: Int) =
            RetryFlow.withBackoff(
                minBackoff = 10.millis,
                maxBackoff = 5.seconds,
                randomFactor = 1d,
                maxRetries = times,
                flow = f)({
                    case (in, Left(_)) =>
                        println(s"retry item:[$in]")
                        Some(in)
                    case (in, Right(_)) =>
                        Some(in)
                })
    }

    //https://doc.akka.io/docs/alpakka/current/patterns.html
    object PassThrough {
        import akka.NotUsed
        import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, ZipWith}
        import akka.stream.{FlowShape, Graph}

        def apply[A, T](processingFlow: Flow[A, T, NotUsed]): Graph[FlowShape[A, (T, A)], NotUsed] =
            apply[A, T, (T, A)](processingFlow, Keep.both)

        
  def apply[A, T, O](processingFlow: Flow[A, T, NotUsed], output: (T, A) => O): Graph[FlowShape[A, O], NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit builder => {
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[A](2))
      val zip = builder.add(ZipWith[T, A, O]((left, right) => output(left, right)))

      // format: off
      broadcast.out(0) ~> processingFlow ~> zip.in0
      broadcast.out(1) ~> zip.in1
      // format: on

      FlowShape(broadcast.in, zip.out)
    }
    })
    }
}