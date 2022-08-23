package io.blindnet.pce
package db.repositories.privacyscope

import java.time.Instant
import java.util.UUID

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.blindnet.pce.util.extension.*
import io.circe.*
import io.circe.parser.*
import db.DbUtil.*
import priv.privacyrequest.*
import priv.*
import priv.terms.*

private object codecs {}
