package forex.services.rates.interpreters

import forex.services.rates.Algebra
import forex.domain.{ Price, Rate, Timestamp, Currency }
import sttp.client3.SttpBackend
import sttp.client3.basicRequest
import sttp.client3.circe.asJson
import sttp.client3.UriContext
import io.circe.generic.auto._
import java.time.OffsetDateTime
import forex.services.rates.errors.Error
import forex.config.OneFrameConfig
import cats.implicits._
import cats.effect.Async
import java.time.Duration

case class OneFrameResponsePayload(
    from: String,
    to: String,
    bid: BigDecimal,
    ask: BigDecimal,
    price: BigDecimal,
    time_stamp: OffsetDateTime) {

  def toRate: Either[Error, Rate] =
    (Currency.fromString(from), Currency.fromString(to)) match {
      case (Right(f), Right(t)) =>
        Right(
          Rate(Rate.Pair(f, t), Price(price), Timestamp(time_stamp))
        )
      case _ => Left(Error.OneFrameUnrecognizedResponse(f"Currency pair ${from}${to} is not supported."))
    }
}

class OneFrameLive[F[_]: Async](oneFrameConfig: OneFrameConfig, backend: SttpBackend[F, _])extends Algebra[F] {

  var pairPriceDic: Map[Rate.Pair, Price] = Map.empty[Rate.Pair, Price]

  var lastSyncTime = Timestamp(OffsetDateTime.now().minusDays(1))

  val pairsArray = Currency.allPairs.map { case (c1, c2) =>
    s"${c1}${c2}"
  }
  val pairParams = pairsArray.map(p => ("pair", p.toString()))

  val uri = uri"${oneFrameConfig.host}${oneFrameConfig.path}".addParams(pairParams: _*)

  val oneFrameRequest = {
    basicRequest
      .header("Token", oneFrameConfig.token)
      .get(uri)
      .response(asJson[List[OneFrameResponsePayload]])
  }

  private def getLiveRates(): F[Error Either Map[Rate.Pair, Price]] = for {
    response <- backend.send(oneFrameRequest)
    body = response.body.leftMap(e => Error.OneFrameLookupFailed(e.toString()))
    allRates = body.flatMap { rate => {
                 val (left, right) = rate.map(_.toRate).partitionMap(identity)
                 left.headOption.toLeft(right.map(r => (r.pair, r.price)).toMap)
                 }
               }
  } yield allRates

  private def shouldGetLiveRate(): Boolean = {
    (Duration.between(OffsetDateTime.now(), lastSyncTime.value).compareTo(Duration.ofMillis(oneFrameConfig.secondsBetweenCall.toMillis)) <= 0) 
    //Using ofMillis to convert between Duration and FiniteDuration
  }

  private def getPrice(pair: Rate.Pair): BigDecimal = {
    if(shouldGetLiveRate()){
      val result = getLiveRates()
      result.map {
      case Right(latestRatesMap) => {
      pairPriceDic = latestRatesMap
      lastSyncTime = Timestamp.now // lastSyncTime is only updated when we actually make an API call
      }
      case Left(error) =>
        println(s"Error: $error")
      }
    }
    pairPriceDic(pair).value
  }

  private def getRateTime(): Timestamp = {
    (if (shouldGetLiveRate()) Timestamp.now else lastSyncTime)
  }

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    Rate(pair, Price(getPrice(pair)), getRateTime()).asRight[Error].pure[F]    
}
