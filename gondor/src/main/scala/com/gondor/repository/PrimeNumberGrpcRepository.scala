package com.gondor.repository

import cats.ApplicativeError
import cats.effect.kernel.Sync
import com.gondor.model.{
  ApplicationError,
  GeneralError,
  GondorNumberResponse,
  PrimeNumberDomain
}
import com.service.prime.{PrimeNumberRequest, PrimeNumberServiceFs2Grpc}
import io.grpc.Metadata

class PrimeNumberGrpcRepository[F[_]](
    grpcService: PrimeNumberServiceFs2Grpc[F, Metadata]
)(implicit F: ApplicativeError[F, Throwable])
    extends PrimeNumberRepository[F] {

  private def buildGenerelError(error: Throwable) = GeneralError(
    s"There seems to be a problem due to : ${error.getMessage}"
  )

  private def callGrpcService(primeNumberRequest: PrimeNumberRequest) = {
    grpcService.generatePrimeNumber(primeNumberRequest, new Metadata()).attempt
  }

  def eitherGondorResponseOrApplicationError(
      primeNumberRequest: PrimeNumberRequest
  ): fs2.Stream[F, Either[ApplicationError, GondorNumberResponse]] =
    callGrpcService(primeNumberRequest).flatMap {
      case Right(value) => fs2.Stream(Right(GondorNumberResponse(value.value)))
      case Left(error)  => fs2.Stream(Left(buildGenerelError(error)))
    }

  def maybeGondorResponseOrGeneralError(
      primeNumberRequest: PrimeNumberRequest
  ): fs2.Stream[F, PrimeNumberDomain] =
    callGrpcService(primeNumberRequest).rethrow
      .map(v => GondorNumberResponse(v.value))
      .handleErrorWith { case error =>
        fs2.Stream(buildGenerelError(error))
      }
}

object PrimeNumberGrpcRepository {
  def apply[F[_]: Sync](grpcService: PrimeNumberServiceFs2Grpc[F, Metadata]) =
    new PrimeNumberGrpcRepository(grpcService)
}

trait PrimeNumberRepository[F[_]] {
  def eitherGondorResponseOrApplicationError(
      primeNumberRequest: PrimeNumberRequest
  ): fs2.Stream[F, Either[ApplicationError, GondorNumberResponse]]
  def maybeGondorResponseOrGeneralError(
      primeNumberRequest: PrimeNumberRequest
  ): fs2.Stream[F, PrimeNumberDomain]
}
