package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.errors._
import forex.config.OneFrameConfig
import forex.domain.Currency
import cats.effect.Async
import org.http4s.Uri


class OneFrameLive[F[_]: Async](oneFrameConfig: OneFrameConfig)extends Algebra[F] {

  val pairsArray = Currency.allPairs.map { case (c1, c2) =>
    s"${c1}${c2}"
  }
  val pairParams = pairsArray.map(v => s"pair=$v").mkString("&")

  val uri = Uri.fromString(oneFrameConfig.host+oneFrameConfig.path+"?"+pairParams) //http4s does not seem to support addParams()

  println(uri)
  // var lastCallTime = Timestamp(OffsetDateTime.now().minus(1, ChronoUnit.DAYS))
  // var dict = Map(
  //   "XXXYYY" -> BigDecimal("2.50")
  // )

  // override def lookupPairPrice(pair: Rate.Pair): F[Error Either Price] = {
  //   Price(BigDecimal(100))
  // }
  override def get(pair: Rate.Pair): F[Error Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[Error].pure[F]    
}
