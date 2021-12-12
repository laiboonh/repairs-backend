package core

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

object RefinedTypes {
  type DatabaseUrl = String Refined MatchesRegex["^(.+):/{2}(.+):(.+)@(.+):(.+)/(.+)$"]
  type Host = String Refined MatchesRegex["^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9])(.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]))*$"]
}
