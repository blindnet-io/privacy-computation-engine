package io.blindnet.pce
package api.endpoints

import java.util.UUID

import cats.effect.IO
import io.blindnet.identityclient.auth.*
import io.circe.generic.auto.*
import org.http4s.server.Router
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*
import services.*
import api.endpoints.messages.privacyrequest.*
import api.endpoints.BaseEndpoint.*
import io.blindnet.pce.model.error.*
import cats.implicits.*

trait EndpointsUtil {
  val unprocessable =
    oneOfVariant(
      statusCode(StatusCode.UnprocessableEntity).and(stringBody.mapTo[BadRequestException])
    )

  val notFound = oneOfVariant(
    statusCode(StatusCode.NotFound).and(stringBody.mapTo[NotFoundException])
  )

  def runLogicNoPrincipal[Req, Resp](f: Req => IO[Resp])(r: Req) =
    f(r).map(Right(_)).handleError {
      case e: Exception => Left(e)
    }

  def runLogic[T, Req, Resp](f: T => Req => IO[Resp])(t: T)(r: Req) =
    f(t)(r).map(Right(_)).handleError {
      case e: Exception => Left(e)
    }

  def runLogicOnlyAuth[T, Req, Resp](f: T => Req => IO[Resp])(t: T)(r: Req) =
    f(t)(r).map(Right(_)).handleError {
      case e: AuthException => Left(e)
    }

}

object util extends EndpointsUtil
