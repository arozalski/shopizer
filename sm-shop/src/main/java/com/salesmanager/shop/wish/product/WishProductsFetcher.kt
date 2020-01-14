package com.salesmanager.shop.wish.product

import com.salesmanager.shop.wish.WishFetcher
import org.apache.http.message.BasicNameValuePair

object WishProductsFetcher : WishFetcher() {
    override val endpoint = "feed/get-filtered-feed"

    @JvmStatic
    fun fetch(count: Int, offset: Int): String = listOf(
        BasicNameValuePair("count", count.toString()),
        BasicNameValuePair("offset", offset.toString()),
        BasicNameValuePair("request_categories", "false"),
        BasicNameValuePair("request_id", "tabbed_feed_latest"),
        BasicNameValuePair("request_branded_filter", "false")
    ).let(this@WishProductsFetcher::fetch)
}