package thunderhamster

import blueeyes.core.service.HttpClient
import java.net.URLEncoder.{encode => urlEncode}
import blueeyes.json.JsonAST
import akka.dispatch.Future


class ContentApi(client: HttpClient[JsonAST.JValue]) {

  def getContent(searchParams: (String, String)*): Future[Option[JsonAST.JValue]] = {
    val params = ("user-tier" -> "internal") +: ("show-fields" -> "wordcount") +: searchParams
    val url = "search.json?" +
      (params map { case (k, v) => urlEncode(k, "utf-8") + "=" + urlEncode(v, "utf-8") } mkString "&")
    client.get[JsonAST.JValue](url) map (_.content map (_ \\ "results"))
  }

}
