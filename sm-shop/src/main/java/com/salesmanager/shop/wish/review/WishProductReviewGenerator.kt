package com.salesmanager.shop.wish.review

import com.salesmanager.core.model.catalog.product.Product
import com.salesmanager.core.model.catalog.product.review.ProductReview
import com.salesmanager.core.model.catalog.product.review.ProductReviewDescription
import com.salesmanager.core.model.customer.Customer
import com.salesmanager.core.model.reference.language.Language
import java.util.*

object WishProductReviewGenerator {
    private const val REVIEW_TEXT = "Bardzo dobry produkt. Gorąco polecam!!!"

    fun generate(product: Product, customer: Customer, language: Language): ProductReview {
        val review = ProductReview().apply {
            reviewRating = product.productReviewAvg.toDouble()
            reviewDate = Date()
            this.customer = customer
            this.product = product
        }
        val description = ProductReviewDescription().apply {
            productReview = review
            name = "review-name"
            description = REVIEW_TEXT
            this.language = language
        }
        review.descriptions.add(description)
        return review
    }
}