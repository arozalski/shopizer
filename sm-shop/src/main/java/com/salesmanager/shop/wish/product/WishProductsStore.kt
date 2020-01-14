package com.salesmanager.shop.wish.product

import com.salesmanager.core.business.services.catalog.category.CategoryService
import com.salesmanager.core.business.services.catalog.product.ProductService
import com.salesmanager.core.business.services.catalog.product.review.ProductReviewService
import com.salesmanager.core.business.services.customer.CustomerService
import com.salesmanager.core.business.services.merchant.MerchantStoreService
import com.salesmanager.core.business.services.reference.language.LanguageService
import com.salesmanager.core.model.catalog.category.Category
import com.salesmanager.core.model.catalog.category.Category.DEFAULT_CATEGORY
import com.salesmanager.core.model.catalog.product.Product
import com.salesmanager.core.model.catalog.product.availability.ProductAvailability
import com.salesmanager.core.model.catalog.product.description.ProductDescription
import com.salesmanager.core.model.catalog.product.image.ProductImage
import com.salesmanager.core.model.catalog.product.price.ProductPrice
import com.salesmanager.core.model.catalog.product.price.ProductPriceDescription
import com.salesmanager.core.model.merchant.MerchantStore
import com.salesmanager.core.model.reference.language.Language
import com.salesmanager.shop.wish.review.WishProductReviewGenerator
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.net.URL
import javax.inject.Inject
import kotlin.random.Random
import kotlin.random.nextLong

@Component
class WishProductsStore {

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

    @Scheduled(fixedRate = ONE_HOUR_IN_MS)
    fun run() {
        Thread.sleep(calculateSleepTime())
        val products = WishProductsFetcher.fetch(
            PRODUCT_COUNT,
            PRODUCT_OFFSET
        ).let(WishProductsParser::parse)
        store(products)
    }

    private fun store(products: List<WishProductsParser.Product>) {
        val store = merchantService.getMerchantStore(MerchantStore.DEFAULT_STORE)
        val category = categoryService.getByCode(store, DEFAULT_CATEGORY)
        val language = languageService.defaultLanguage()
        val customers = customerService.getListByStore(store)
        products.forEach {
            it.toDbProduct(store, category, language).let { product ->
                val isNewProduct = product.isNewProduct()
                saveProduct(product)
                if (isNewProduct) {
                    customers.forEach { customer ->
                        WishProductReviewGenerator.generate(product, customer, language).let(reviewService::update)
                    }
                }
            }
        }
    }

    private fun Product.isNewProduct() = id == null || id == 0L

    private fun saveProduct(product: Product) = productService.update(product)

    private fun calculateSleepTime() = Random.nextLong(SLEEP_TIME_RANGE)

    private fun WishProductsParser.Product.toDbProduct(
        store: MerchantStore,
        category: Category,
        lan: Language
    ): Product {
        val product = productService.getByCode(id, lan)
        return if (product != null) {
            updateDbProduct(lan, product)
        } else {
            createDbProduct(store, category, lan)
        }
    }

    private fun WishProductsParser.Product.createDbProduct(
        store: MerchantStore,
        category: Category,
        lan: Language
    ): Product {
        val currentProduct = Product().apply {
            sku = this@createDbProduct.id
            manufacturer = null
            type = null
            merchantStore = store
            isProductShipeable = true
            productReviewAvg = rating
            productReviewCount = ratingCount
        }
        val availability = ProductAvailability().apply {
            productDateAvailable = null
            productQuantity = numberOfBought
            region = "*"
            product = currentProduct
        }
        val price = ProductPrice().apply {
            isDefaultPrice = true
            productPriceAmount = retailPrice
            productPriceSpecialAmount = promoPrice
            productAvailability = availability
        }
        val priceDescription = ProductPriceDescription().apply {
            name = "Base price"
            productPrice = price
            language = lan
        }
        price.descriptions.add(priceDescription)
        availability.prices.add(price)
        currentProduct.availabilities.add(availability)
        val productDescription = ProductDescription().apply {
            name = this@createDbProduct.name.take(200)
            language = lan
            seUrl = null
            product = currentProduct
        }
        createProductImage(currentProduct)?.let(currentProduct.images::add)
        currentProduct.descriptions.add(productDescription)
        currentProduct.categories.add(category)
        return currentProduct
    }

    private fun WishProductsParser.Product.updateDbProduct(lan: Language, product: Product): Product {
        val availability = product.availabilities.last().apply {
            productQuantity = numberOfBought
        }
        val lastPrice = availability.prices.last()
        if (lastPrice.productPriceSpecialAmount != promoPrice) {
            val newPrice = ProductPrice().apply {
                isDefaultPrice = true
                productPriceAmount = retailPrice
                productPriceSpecialAmount = promoPrice
                productAvailability = availability
            }
            val priceDescription = ProductPriceDescription().apply {
                name = "Base price"
                productPrice = newPrice
                language = lan
            }
            newPrice.descriptions.add(priceDescription)
            availability.prices.add(newPrice)
        }
        product.availabilities.clear()
        product.availabilities.add(availability)
        return product
    }

    private fun WishProductsParser.Product.createProductImage(newProduct: Product) = try {
        URL(imageUrl).openStream().use {
            ProductImage().apply {
                productImage = "${this@createProductImage.id}.jpg"
                product = newProduct
                image = ByteArrayInputStream(IOUtils.toByteArray(it))
            }
        }
    } catch (e: Exception) {
        LOGGER.error("Error while reading product image", e)
        null
    }

    companion object {
        private const val HALF_HOUR_IN_MS = 1800000L
        private const val ONE_HOUR_IN_MS = HALF_HOUR_IN_MS * 2
        private val SLEEP_TIME_RANGE = LongRange(0, 0)
        private const val PRODUCT_COUNT = 70
        private const val PRODUCT_OFFSET = 0
        private val LOGGER = LoggerFactory.getLogger(WishProductsStore::class.java)
    }
}