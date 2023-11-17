package com.ad.mpc.p10n.realtimeuserprofile.model.sdrn

import scala.util.matching.Regex

private[realtimeuserprofile] case class SDRN(value: String) extends AnyVal

object SDRN {
  val SdrnRegex: Regex = "sdrn:([^:]+):([^:]+):([^:]+)".r

  object Valid {
    def unapply(arg: String): Option[SDRN] = Option(arg)
      .map(SdrnRegex.pattern.matcher)
      .filter(_.matches())
      .map(_ => SDRN(arg))
  }

  /**
    * The implicit class [[RichSDRN]] exposes three methods to
    * access the elements of an [[SDRN]]:
    * <ul>
    *   <li>namespace</li>
    *   <li>resourceType</li>
    *   <li>identifier</li>
    * </ul>
    *
    * This methods will raise an exception if the value of this class is
    * not a valid SDRN, matching the [[SdrnRegex]], so it's better
    * to validate an SDRN before use with the [[isValid]] method
    *
    * @param sdrn a SDRN instance
    */
  implicit class RichSDRN(sdrn: SDRN) {
    lazy val SdrnRegex(namespace, resourceType, identifier) = sdrn.value

    def isValid: Boolean = SdrnRegex.pattern.matcher(sdrn.value).matches()
  }
}
