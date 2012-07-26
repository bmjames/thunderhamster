package thunderhamster

import akka.dispatch.{ExecutionContext, Future}
import blueeyes.BlueEyesServiceBuilder
import blueeyes.core.data.{BijectionsChunkJson, BijectionsChunkString}
import blueeyes.core.service.engines.HttpClientXLightWeb
import blueeyes.core.http.MimeTypes.{json, application}
import blueeyes.core.data.ByteChunk
import blueeyes.core.http._
import blueeyes.json.JsonAST.JValue
import blueeyes.core.http.HttpStatusCodes.Found
import blueeyes.core.http.HttpHeaders.Location
import scalaz._, Scalaz._
import ApplicativeInstances._, MonoidInstances._


trait CatchUpService extends BlueEyesServiceBuilder
    with BijectionsChunkString with BijectionsChunkJson {

  type Req = HttpRequest[ByteChunk]

  val catchUp = service("catchup", "1.0.0") { help { context =>

    startup {
      val contentApiUrl = context.config[String]("contentApiUrl")
      val client = (new HttpClientXLightWeb).path(contentApiUrl).contentType[JValue](application/json)
      Future(new ContentApi(client))
    } ->
    request { api =>
      produce(application/json) {
        path("/") {
          get { _: Req =>
            FoundResponse("/blueeyes/services/catchup/v1/docs/api")
          }
        } ~
        describe("Quick news catch-up") {
          path("/quick/news") {
            get { _: Req =>

              val shortStories = api.getContent(
                "page-size" -> "3",
                "tag" -> "tone/news",
                "min-wordcount" -> "50",
                "max-wordcount" -> "500"
              )
              val longStories = api.getContent(
                "page-size" -> "2",
                "tag" -> "tone/news",
                "min-wordcount" -> "501",
                "max-wordcount" -> "2000"
              )

              BundleResponse(shortStories, longStories)
            }
          }
        }~
        describe("A good long read") {
          path("/long/features") {
            get { _: Req =>

              val longStories = api.getContent(
                "page-size" -> "5",
                "tag" -> "tone/features",
                "min-wordcount" -> "1001",
                "max-wordcount" -> "2000"
              )

              BundleResponse(longStories)
            }
          }
        }
      }
    } ->
    shutdown { _ => Future(()) }
  }}
}

object BundleResponse {
  def apply(responses: Future[Option[JValue]]*)(implicit ex: ExecutionContext): Future[HttpResponse[JValue]] =
    responses.toList.sequence map (stories => HttpResponse(content = stories.suml))
}

object FoundResponse {
  def apply[T](location: T)(implicit ex: ExecutionContext, ev: T => URI): Future[HttpResponse[JValue]] =
    Future(HttpResponse[JValue](status=HttpStatus(Found), headers=HttpHeaders(Seq(Location(location)))))
}
