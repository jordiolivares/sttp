package sttp.client.httpclient.zio

import sttp.client._
import sttp.client.httpclient.WebSocketHandler
import sttp.client.impl.zio.RIOMonadAsyncError
import sttp.client.monad.MonadError
import sttp.client.testing.ConvertToFuture
import sttp.client.testing.HttpTest.wsEndpoint
import sttp.client.testing.websocket.HighLevelWebsocketTest
import sttp.client.ws.WebSocket
import zio.blocking.Blocking
import zio.stream.ZStream
import sttp.client.impl.zio._
import zio.clock.Clock
import zio.{Schedule, Task, ZIO}
import zio.duration._

import scala.concurrent.duration._

class HttpClientHighLevelZioWebsocketTest extends HighLevelWebsocketTest[BlockingTask, WebSocketHandler] {
  implicit val backend: SttpBackend[BlockingTask, ZStream[Blocking, Throwable, Byte], WebSocketHandler] =
    runtime.unsafeRun(HttpClientZioBackend())
  implicit val convertToFuture: ConvertToFuture[BlockingTask] = convertZioBlockingTaskToFuture
  implicit val monad: MonadError[BlockingTask] = new RIOMonadAsyncError

  def createHandler: Option[Int] => BlockingTask[WebSocketHandler[WebSocket[BlockingTask]]] = bufferCapacity => ZioWebSocketHandler(bufferCapacity)

  it should "handle backpressure correctly" in {
    basicRequest
      .get(uri"$wsEndpoint/ws/echo")
      .openWebsocketF(createHandler(Some(3)))
      .flatMap { response =>
        val ws = response.result
        send(ws, 1000).andThen(eventually(10.millis, 500)) { ws.isOpen.map(_ shouldBe true) }
      }
      .toFuture()
  }

  override def eventually[T](interval: FiniteDuration, attempts: Int)(f: => Task[T]): Task[T] = {
    ZIO.sleep(interval.toMillis.millis).andThen(f).retry(Schedule.recurs(attempts)).provideLayer(Clock.live)
  }
//  override def eventually[T](interval: FiniteDuration, attempts: Int)(f: => BlockingTask[T]): BlockingTask[T] = {
//    (ZIO.sleep(interval) >> f).onErrorRestart(attempts.toLong)
//  }
}
