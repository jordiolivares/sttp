package sttp.client.httpclient

import sttp.client._
import _root_.zio._
import _root_.zio.blocking.Blocking
import _root_.zio.stream.ZStream

package object zio {

  type BlockingTask[A] = ZIO[Blocking, Throwable, A]

  /**
    * Experimental! ZIO-environment service definition, which is an SttpBackend.
    */
  type SttpClient = Has[SttpBackend[BlockingTask, ZStream[Blocking, Throwable, Byte], NothingT]]

  /**
    * Experimental!
    */
  object SttpClient {

    /**
      * Sends the request. Only requests for which the method & URI are specified can be sent.
      *
      * @return An effect resulting in a [[Response]], containing the body, deserialized as specified by the request
      *         (see [[RequestT.response]]), if the request was successful (1xx, 2xx, 3xx response codes), or if there
      *         was a protocol-level failure (4xx, 5xx response codes).
      *
      *         A failed effect, if an exception occurred when connecting to the target host, writing the request or
      *         reading the response.
      *
      *         Known exceptions are converted to one of [[SttpClientException]]. Other exceptions are kept unchanged.
      */
    def send[T](request: Request[T, Nothing]): ZIO[SttpClient with Blocking, Throwable, Response[T]] =
      ZIO.accessM(env => env.get[SttpBackend[BlockingTask, ZStream[Blocking, Throwable, Byte], NothingT]].send(request))

  }
}
