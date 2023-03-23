package forex.services.rates

import cats.Applicative
import cats.effect.Async
import interpreters._
import forex.config.OneFrameConfig

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Async](oneFrameConfig: OneFrameConfig): Algebra[F] = new OneFrameLive[F](oneFrameConfig)
}
