package com.gondor.repository

import cats.MonadError
import cats.effect.kernel.Sync
import com.gondor.service.Validator.GeneralError
import com.service.prime.{PrimeNumberRequest, PrimeNumberResponse, PrimeNumberServiceFs2Grpc}
import io.grpc.Metadata

class PrimeNumberGrpcRepository[F[_]: Sync](grpcService: PrimeNumberServiceFs2Grpc[F, Metadata])(implicit F: MonadError[F, Throwable]) extends PrimeNumberRepository[F] {
  def requestForPrimeNumbers(primeNumberRequest: PrimeNumberRequest): fs2.Stream[F, PrimeNumberResponse] = {
    requestToService(primeNumberRequest)
  }

  private def requestToService(primeNumberRequest: PrimeNumberRequest): fs2.Stream[F, PrimeNumberResponse] = {
    grpcService.generatePrimeNumber(primeNumberRequest, new Metadata()).handleErrorWith {
      case error => fs2.Stream.raiseError(GeneralError(error.getMessage))
    }
  }
}

object PrimeNumberGrpcRepository {
  def apply[F[_]: Sync](grpcService: PrimeNumberServiceFs2Grpc[F, Metadata]) = new PrimeNumberGrpcRepository(grpcService)
}

trait PrimeNumberRepository[F[_]] {
  def requestForPrimeNumbers(primeNumberRequest: PrimeNumberRequest): fs2.Stream[F, PrimeNumberResponse]
}