package forex

import scala.concurrent.ExecutionContext

import cats.effect._
import forex.config._
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder
import sttp.client3.armeria.fs2.ArmeriaFs2Backend

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      sttpBackend = ArmeriaFs2Backend.usingDefaultClient[F]()
      module = new Module[F](config, sttpBackend)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
