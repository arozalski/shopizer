package com.salesmanager.shop.wish.review

import com.salesmanager.core.business.services.catalog.category.CategoryService
import com.salesmanager.core.business.services.catalog.product.ProductService
import com.salesmanager.core.business.services.catalog.product.review.ProductReviewService
import com.salesmanager.core.business.services.customer.CustomerService
import com.salesmanager.core.business.services.merchant.MerchantStoreService
import com.salesmanager.core.business.services.reference.country.CountryService
import com.salesmanager.core.business.services.reference.language.LanguageService
import com.salesmanager.core.model.catalog.product.Product
import com.salesmanager.core.model.catalog.product.review.ProductReview
import com.salesmanager.core.model.catalog.product.review.ProductReviewDescription
import com.salesmanager.core.model.common.Billing
import com.salesmanager.core.model.common.Delivery
import com.salesmanager.core.model.customer.Customer
import com.salesmanager.core.model.merchant.MerchantStore
import com.salesmanager.core.model.reference.language.Language
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.inject.Inject

@Component
class WishReviewsStore {

    @Inject
    lateinit var merchantService: MerchantStoreService
    @Inject
    lateinit var categoryService: CategoryService
    @Inject
    lateinit var productService: ProductService
    @Inject
    lateinit var languageService: LanguageService
    @Inject
    lateinit var reviewService: ProductReviewService
    @Inject
    lateinit var customerService: CustomerService
    @Inject
    lateinit var countryService: CountryService
    private lateinit var reviewedProductIds: MutableSet<Long>

    @Scheduled(fixedRate = SCHEDULER_RATE)
    fun run() {
        if (!::reviewedProductIds.isInitialized) {
            reviewedProductIds = reviewService.list().map { it.product.id }.toMutableSet()
        }
        val language = languageService.defaultLanguage()
        val store = merchantService.getMerchantStore(MerchantStore.DEFAULT_STORE)
        findProductWithoutReviews()?.let { product ->
            val reviews = WishReviewsFetcher.fetch(product.sku, 0, REVIEW_COUNT).let(WishReviewsParser::parse)
            store(reviews, product, language, store)
        }
    }

    private fun findProductWithoutReviews(): Product? {
        val allProducts = productService.list()
        return allProducts.firstOrNull { !reviewedProductIds.contains(it.id) }
    }

    private fun store(reviews: List<WishReviewsParser.Review>, product: Product, language: Language, store: MerchantStore) {
        reviews.forEach {
            try {
                reviewService.update(it.toDbReview(product, language, store))
            } catch (e: Exception) {
                LOGGER.error("Error while saving product review: ${e.printStackTrace()}")
            }
        }
        reviewedProductIds.add(product.id)
    }

    private fun WishReviewsParser.Review.toDbReview(product: Product, language: Language, store: MerchantStore): ProductReview {
        val review = ProductReview().apply {
            reviewRating = rating.toDouble()
            reviewDate = createdAt
            customer = provideCustomer(language, store)
            this.product = product
        }
        val description = ProductReviewDescription().apply {
            productReview = review
            name = "review-name"
            description = comment
            this.language = language
        }
        review.descriptions.add(description)
        return review
    }

    private fun WishReviewsParser.Review.provideCustomer(language: Language, store: MerchantStore): Customer {
        var customer = customerService.getByNick(user.id)
        if (customer == null) {
            customer = Customer().apply {
                merchantStore = store
                emailAddress = "shopizer@shopizer.com"
                isAnonymous = false
                defaultLanguage = language
                nick = user.id
                password = "password"
                delivery = Delivery()
                billing = Billing().apply {
                    firstName = user.firstName.valueOrDefault("John")
                    lastName = user.lastName.valueOrDefault("Smith")
                    country = countryService.getByCode("PL")
                }
            }
            customerService.update(customer)
        }
        return customer
    }

    private fun String.valueOrDefault(default: String) = if (isNullOrBlank()) default else this

    companion object {
        private const val REVIEW_COUNT = 30
        private const val SCHEDULER_RATE = 60000L
        private val LOGGER = LoggerFactory.getLogger(WishReviewsStore::class.java)
    }
}