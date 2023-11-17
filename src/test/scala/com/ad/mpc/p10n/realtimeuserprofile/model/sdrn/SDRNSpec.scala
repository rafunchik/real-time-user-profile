package com.ad.mpc.p10n.realtimeuserprofile.model.sdrn

import org.scalatest.{FlatSpec, Matchers}

class SDRNSpec extends FlatSpec with Matchers {
  val validSdrn = SDRN("sdrn:namespace:resource-type:identifier")
  val invalidSdrn = SDRN("one:two:three")

  "RichSDRN" should "extract values from a valid sdrn" in {
    validSdrn.namespace shouldBe "namespace"
    validSdrn.resourceType shouldBe "resource-type"
    validSdrn.identifier shouldBe "identifier"
  }

  it should "be valid for a valid SDRN" in {
    validSdrn.isValid shouldBe true
  }

  it should "be invalid for an invalid SDRN" in {
    invalidSdrn.isValid shouldBe false
  }

  "Valid.unapply" should "work with nulls" in {
    SDRN.Valid.unapply(null) shouldBe None
  }

  it should "return an SDRN from a valid SDRN string" in {
    SDRN.Valid.unapply("sdrn:ns:rt:id") shouldBe Some(SDRN("sdrn:ns:rt:id"))
  }

  it should "return none from an invalid sdrn string" in {
    SDRN.Valid.unapply("id") shouldBe None
  }
}
