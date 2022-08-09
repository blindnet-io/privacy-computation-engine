package io.blindnet.pce
package config

import ciris.*
import cats.Show

enum AppEnvironment {
  case Development
  case Staging
  case Production
}

given Show[AppEnvironment] =
  Show.show {
    case AppEnvironment.Development => "development"
    case AppEnvironment.Staging     => "staging"
    case AppEnvironment.Production  => "production"
  }

given ConfigDecoder[String, AppEnvironment] =
  ConfigDecoder[String].mapOption("io.blindnet.pce.config.AppEnvironment") {
    case "development" => Some(AppEnvironment.Development)
    case "staging"     => Some(AppEnvironment.Staging)
    case "production"  => Some(AppEnvironment.Production)
    case _             => None
  }
