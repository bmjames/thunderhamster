package thunderhamster

import blueeyes.core.service.HttpClient
import java.net.URLEncoder._
import blueeyes.json.JsonAST
import akka.dispatch.Future


class ContentApi(client: HttpClient[JsonAST.JValue]) {

  def getContent(searchParams: (String, String)*): Future[Option[JsonAST.JValue]] = {
    val params = ("user-tier" -> "internal") +: ("show-fields" -> "wordcount") +: searchParams
    val url = "search.json?" +
      (params map { case (k, v) => encode(k, "utf-8") + "=" + encode(v, "utf-8") } mkString "&")
    client.get[JsonAST.JValue](url) map (_.content map (_ \\ "results"))
  }

  def getArticles(limit: Int, maxWordcount: Int, minWordcount: Int = 0): Future[Option[JsonAST.JValue]] =
    getContent("tag" -> "type/article", "min-wordcount" -> minWordcount.toString,
      "max-wordcount" -> maxWordcount.toString, "page-size" -> limit.toString)

}
