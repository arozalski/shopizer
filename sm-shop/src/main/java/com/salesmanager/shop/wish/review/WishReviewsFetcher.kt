package com.salesmanager.shop.wish.review

import com.salesmanager.shop.wish.WishFetcher
import org.apache.http.message.BasicNameValuePair

object WishReviewsFetcher : WishFetcher() {
    override val endpoint = "product-ratings/get"

    @JvmStatic
    fun fetch(productId: String, start: Int, count: Int): String = listOf(
        BasicNameValuePair("product_id", productId),
        BasicNameValuePair("start", start.toString()),
        BasicNameValuePair("count", count.toString()),
        BasicNameValuePair("request_count", "1")
    ).let(this@WishReviewsFetcher::fetch)
}