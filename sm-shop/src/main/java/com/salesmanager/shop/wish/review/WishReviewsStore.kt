package com.salesmanager.shop.wish.review

import com.salesmanager.core.business.services.catalog.category.CategoryService
import com.salesmanager.core.business.services.catalog.product.ProductService
import com.salesmanager.core.business.services.catalog.product.review.ProductReviewService
import com.salesmanager.core.business.services.customer.CustomerService
import com.salesmanager.core.business.services.customer.attribute.CustomerOptionService
import com.salesmanager.core.business.services.customer.attribute.CustomerOptionValueService
import com.salesmanager.core.business.services.merchant.MerchantStoreService
import com.salesmanager.core.business.services.reference.country.CountryService
import com.salesmanager.core.business.services.reference.language.LanguageService
import com.salesmanager.core.model.catalog.product.Product
import com.salesmanager.core.model.catalog.product.review.ProductReview
import com.salesmanager.core.model.catalog.product.review.ProductReviewDescription
import com.salesmanager.core.model.common.Billing
import com.salesmanager.core.model.common.Delivery
import com.salesmanager.core.model.customer.Customer
import com.salesmanager.core.model.customer.attribute.CustomerAttribute
import com.salesmanager.core.model.customer.attribute.CustomerOption
import com.salesmanager.core.model.customer.attribute.CustomerOptionValue
import com.salesmanager.core.model.customer.attribute.status.CustomerStatus
import com.salesmanager.core.model.merchant.MerchantStore
import com.salesmanager.core.model.reference.language.Language
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.inject.Inject
import kotlin.random.Random

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
    @Inject
    lateinit var customerOptionService: CustomerOptionService
    @Inject
    lateinit var customerOptionValueService: CustomerOptionValueService
    private lateinit var reviewedProductIds: MutableSet<Long>
    private lateinit var currentLanguage: Language
    private lateinit var currentStore: MerchantStore
    private lateinit var customerStatusOption: CustomerOption
    private lateinit var customerStatusOptionValues: List<CustomerOptionValue>

    @Scheduled(fixedRate = SCHEDULER_RATE)
    fun run() {
        if (!::reviewedProductIds.isInitialized) {
            currentLanguage = languageService.defaultLanguage()
            currentStore = merchantService.getMerchantStore(MerchantStore.DEFAULT_STORE)
            customerStatusOption = customerOptionService.getByCode(currentStore, CustomerOption.CUSTOMER_STATUS_CODE)
            customerStatusOptionValues = CustomerStatus.values().map { customerOptionValueService.getByCode(currentStore, it.value) }
            reviewedProductIds = reviewService.list().map { it.product.id }.toMutableSet()
        }
        findProductWithoutReviews()?.let { product ->
            val reviews = WishReviewsFetcher.fetch(product.sku, 0, REVIEW_COUNT).let(WishReviewsParser::parse)
            store(reviews, product)
        }
    }

    private fun findProductWithoutReviews(): Product? {
        val allProducts = productService.list()
        return allProducts.firstOrNull { !reviewedProductIds.contains(it.id) }
    }

    private fun store(reviews: List<WishReviewsParser.Review>, product: Product) {
        reviews.forEach {
            try {
                reviewService.update(it.toDbReview(product))
            } catch (e: Exception) {
                LOGGER.error("Error while saving product review: ${e.printStackTrace()}")
            }
        }
        reviewedProductIds.add(product.id)
    }

    private fun WishReviewsParser.Review.toDbReview(product: Product): ProductReview {
        val review = ProductReview().apply {
            reviewRating = rating.toDouble()
            reviewDate = createdAt
            customer = provideCustomer()
            this.product = product
        }
        val description = ProductReviewDescription().apply {
            productReview = review
            name = "review-name"
            description = comment
            this.language = currentLanguage
        }
        review.descriptions.add(description)
        return review
    }

    private fun WishReviewsParser.Review.provideCustomer(): Customer {
        var customer = customerService.getByNick(user.id)
        if (customer == null) {
            customer = Customer().apply {
                merchantStore = currentStore
                emailAddress = "shopizer@shopizer.com"
                isAnonymous = false
                defaultLanguage = currentLanguage
                nick = user.id
                password = "password"
                delivery = Delivery()
                billing = Billing().apply {
                    firstName = user.firstName.valueOrDefault("John")
                    lastName = user.lastName.valueOrDefault("Smith")
                    country = countryService.getByCode("PL")
                }
            }
            val attribute = CustomerAttribute().apply {
                this.customerOption = customerStatusOption
                this.customerOptionValue = customerStatusOptionValues[Random.nextInt(0, CustomerStatus.values().size)]
                this.customer = customer
            }
            customer.attributes.add(attribute)
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