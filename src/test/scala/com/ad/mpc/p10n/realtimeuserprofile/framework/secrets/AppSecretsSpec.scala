package com.ad.mpc.p10n.realtimeuserprofile.framework.secrets

import cats.Id
import com.ad.mpc.p10n.realtimeuserprofile.framework.secrets.AppSecrets.CredentialsException
import com.schibsted.mp.p10n.aws.secrets.program.SecretsManager
import com.schibsted.mp.p10n.kafka.config.KafkaCredentials
import org.mockito.Mockito.when
import org.scalatest.{FlatSpec, Inside, Matchers}
import org.scalatestplus.mockito.MockitoSugar

class AppSecretsSpec extends FlatSpec with Matchers with Inside with MockitoSugar {

  behavior of classOf[AppSecrets[Id]].getSimpleName

  import AppSecretsSpec._

  it should "get kafka credentials" in {

    implicit val store = mock[SecretsManager[Id]]

    val secretsStore = new AppSecrets(DefaultSecretsConfig)

    when(store.getSecret(KafkaName)).thenReturn(Right(ValidEncodedCredentials))

    inside(secretsStore.kafkaCredentials) {
      case Right(expected) => expected shouldBe ExpectedKafkaCredentials
    }

  }

  it should "fail to get kafka credentials when the secret store fails" in {
    implicit val store = mock[SecretsManager[Id]]
    val secretsStore = new AppSecrets(DefaultSecretsConfig)

    when(store.getSecret(KafkaName)).thenReturn(Left(KafkaExpectedException))

    inside(secretsStore.kafkaCredentials) {
      case Left(_: CredentialsException) => ()
    }

  }

  it should "fail to get kafka credentials when the secret is not encoded properly" in {

    implicit val store = mock[SecretsManager[Id]]
    val secretsStore = new AppSecrets(DefaultSecretsConfig)

    when(store.getSecret(KafkaName)).thenReturn(Right(InvalidEncodedCredentials))

    inside(secretsStore.kafkaCredentials) {
      case Left(_: CredentialsException) =>
    }
  }
}


object AppSecretsSpec {

  private val KafkaName = "kafka-credentials"

  private val DefaultSecretsConfig = SecretsConfig(kafkaKey = KafkaName)

  private val ValidEncodedCredentials: String = "user:password"
  private val InvalidEncodedCredentials: String = "user/password"

  private val ExpectedKafkaCredentials: KafkaCredentials = KafkaCredentials("user", "password")

  private val KafkaExpectedException = new CredentialsException(KafkaName, new RuntimeException)
}
