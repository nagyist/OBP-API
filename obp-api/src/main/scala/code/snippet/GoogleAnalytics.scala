package code.snippet

import code.api.util.APIUtil
import net.liftweb.util.Helpers._

object GoogleAnalytics {
  private val analyticsIdOpt = APIUtil.getPropsValue("google_analytics_id").toOption

  def set = analyticsIdOpt match {
    case Some(analyticsId) =>
      val script =
        s"""
          window.dataLayer = window.dataLayer || [];
          function gtag(){dataLayer.push(arguments);}
          gtag('js', new Date());
          gtag('config', '$analyticsId');
        """
      "#google_analytics_1 [src]" #> s"https://www.googletagmanager.com/gtag/js?id=$analyticsId" &
        "#google_analytics_2 *" #> script

    case None =>
      "#google_analytics_1" #> "" & "#google_analytics_2" #> ""
  }
}
