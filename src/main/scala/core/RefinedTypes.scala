package core

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.MatchesRegex

object RefinedTypes {
  type DatabaseUrl = String Refined MatchesRegex["^(.+):/{2}(.+):(.+)@(.+):(.+)/(.+)$"]
  type Host = String Refined MatchesRegex["^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9])(.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]))*$"]
  type Email = String Refined MatchesRegex["""^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$"""]
  type Password = String Refined NonEmpty
}
