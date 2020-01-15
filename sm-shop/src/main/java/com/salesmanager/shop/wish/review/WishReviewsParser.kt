package com.salesmanager.shop.wish.review

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object WishReviewsParser {
    private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")

    fun parse(response: String) = JSONObject(response).getReviews()

    private fun JSONObject.getReviews(): List<Review> {
        val jsonReviews = getJSONObject("data").getJSONArray("results")
        return (0 until jsonReviews.length()).map {
            val jsonReview = jsonReviews.getJSONObject(it)
            val jsonUser = jsonReview.getJSONObject("user")
            val fullName = jsonUser.getString("name")
            Review(
                comment = jsonReview.getString("comment"),
                rating = jsonReview.getInt("rating"),
                user = User(
                    id = jsonUser.getString("id"),
                    firstName = fullName.substringBefore(" "),
                    lastName = fullName.substringAfter(" ")
                ),
                createdAt = format.parse(jsonReview.getString("time"))
            )
        }
    }

    data class Review(val comment: String, val rating: Int, val user: User, val createdAt: Date)

    data class User(val id: String, val firstName: String, val lastName: String)
}