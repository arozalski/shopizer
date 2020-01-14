package com.salesmanager.shop.wish.product

import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import java.net.URI

object WishProductsFetcher {
    private const val GET_FEED_URL = "https://www.wish.com/api/feed/get-filtered-feed"
    private const val USER_AGENT_HEADER_VALUE = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36"
    private const val XSRF_TOKEN_HEADER_VALUE = "2|dfbd3072|50668cf1ad1be654a6270b783a7d2612|1578389869"
    private const val COOKIE_HEADER_VALUE = "_is_desktop=true; _timezone=1; last_error_log_time=1578046825427; G_ENABLED_IDPS=google; _fbp=fb.1.1575975007248.2096902237; cto_lwid=60fa6bbc-013c-40df-8506-6ed0625b7167; __stripe_mid=0ccbe421-b586-4696-a060-3e4e8eec506c; notice_preferences=2:; notice_gdpr_prefs=0,1,2:; _ga=GA1.2.392095516.1575977009; __zlcmid=vgikaweuei02FM; G_AUTHUSER_H=1; __utmc=96128154; __utmz=96128154.1577978307.1.1.utmcsr=PASSWORD_RESET_CONFIRMATION|utmccn=2019-12-30_PASSWORD_RESET_CONFIRMATION_135ad29a7c8242a784b17147b527eb3a|utmcmd=email; __utma=96128154.392095516.1575977009.1577978307.1578046716.2; logged_out_locale=pl; bsid=babe527e977a41938bd94e60b40ccc72; _xsrf=2|dfbd3072|50668cf1ad1be654a6270b783a7d2612|1578389869; _derived_epik=dj0yJnU9SHdYRU9XZGR4b0t0X3ZMUGdPOF9ZTXA2aDRDSFVyNUgmbj1RZlp4OFpoa2g0eHVvc0pRU0M0bUJnJm09NyZ0PUFBQUFBRjRVVVc0; _timezone=1; _is_desktop=true; sweeper_uuid=2e36b80df1c8493eaec3f2572510fb6f; vendor_user_tracker=7abb6be0e304bfd22bd961e08682163924ff6293f802dafb2b7624dc9b612e4f; sweeper_session=\"2|1:0|10:1578389904|15:sweeper_session|84:MTY4NGI3NTQtNzE4NC00NjZmLWJhZjItNmM0NjBkZGQ3YTFiMjAyMC0wMS0wNyAwOTozODoyMS4zNzAzMzg=|ec72add83369556964b88a50e99ea09f74e2234dd641d62d609546e9eab2be16\"; sessionRefreshed_5e14518c50fa25044fa1a237=true; isDailyLoginBonusModalLoaded=true; __stripe_sid=72abb56e-2e67-437f-9f42-be4431faab8c"

    @JvmStatic
    fun fetch(count: Int, offset: Int): String {
        val urlParameters = listOf(
            BasicNameValuePair("count", count.toString()),
            BasicNameValuePair("offset", offset.toString()),
            BasicNameValuePair("request_categories", "false"),
            BasicNameValuePair("request_id", "tabbed_feed_latest"),
            BasicNameValuePair("request_branded_filter", "false")
        )
        return HttpPost().apply {
            uri = URI(GET_FEED_URL)
            entity = UrlEncodedFormEntity(urlParameters)
            addHeader("User-Agent", USER_AGENT_HEADER_VALUE)
            addHeader("x-xsrftoken", XSRF_TOKEN_HEADER_VALUE)
            addHeader("Cookie", COOKIE_HEADER_VALUE)
        }.let { request ->
            HttpClients.createDefault().use {
                val response = it.execute(request)
                EntityUtils.toString(response.entity)
            }
        }
    }
}