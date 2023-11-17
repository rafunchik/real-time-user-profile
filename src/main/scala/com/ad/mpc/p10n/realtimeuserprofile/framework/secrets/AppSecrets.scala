package com.ad.mpc.p10n.realtimeuserprofile.framework.secrets

import cats.Monad
import cats.data.EitherT
import cats.syntax.either._
import com.schibsted.mp.p10n.aws.secrets.program.SecretsManager
import com.schibsted.mp.p10n.kafka.config.KafkaCredentials


object AppSecrets {
  private val CredentialsRegex = "^(.*)?\\:(.*)$".r

  class CredentialsException(secretName: String, cause: Throwable)
    extends Exception(s"Error retrieving secrets for the secret '$secretName'", cause)

  object KafkaCredentialsFormatException extends Exception(s"Unexpected format for kafka credential")

  def apply[F[_] : Monad : SecretsManager](secretsConfig: SecretsConfig): AppSecrets[F] = new AppSecrets[F](secretsConfig)
}

class AppSecrets[F[_]: Monad : SecretsManager](secretsConfig: SecretsConfig) {

  import AppSecrets._

  def kafkaCredentials: F[Either[Throwable, KafkaCredentials]] =
    secretValue(secretsConfig.kafkaKey)(decodeKafkaCredentials)

  private def secretValue[T](key: String)(decode: String => Either[Throwable, T]): F[Either[Throwable, T]] =
    (for{
      secretValue  <- EitherT(implicitly[SecretsManager[F]].getSecret(key))
      decodedValue <- decodeSecretValue(decode, secretValue)
    } yield decodedValue).value


  private def decodeSecretValue[T](decode: String => Either[Throwable, T], secretValue: String): EitherT[F, Throwable, T] = {
    decode(secretValue)
      .leftMap[Throwable](cause => new CredentialsException(secretsConfig.kafkaKey, cause))
      .toEitherT[F]
  }

  private def decodeKafkaCredentials(encodedCredentials: String): Either[Throwable, KafkaCredentials] =
    CredentialsRegex.findFirstMatchIn(encodedCredentials)
      .map(m => KafkaCredentials(user = m.group(1), password = m.group(2)))
      .toRight(KafkaCredentialsFormatException)

}
