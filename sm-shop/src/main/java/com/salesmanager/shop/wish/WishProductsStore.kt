package com.salesmanager.shop.wish

import com.salesmanager.core.business.services.catalog.category.CategoryService
import com.salesmanager.core.business.services.catalog.product.ProductService
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
import com.salesmanager.shop.init.data.InitData
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.net.URL
import javax.inject.Inject

@Component
class WishProductsStore : InitData {

    @Inject
    lateinit var merchantService: MerchantStoreService
    @Inject
    lateinit var categoryService: CategoryService
    @Inject
    lateinit var productService: ProductService
    @Inject
    lateinit var languageService: LanguageService

    override fun initInitialData() {
        val products = WishProductsFetcher.fetch(10, 0).let(WishProductsParser::parse)
        store(products)
    }

    private fun store(products: List<WishProductsParser.Product>) {
        val store = merchantService.getMerchantStore(MerchantStore.DEFAULT_STORE)
        val category = categoryService.getByCode(store, DEFAULT_CATEGORY)
        val language = languageService.defaultLanguage()
        products.forEach {
            productService.update(it.toDbProduct(store, category, language))
        }
    }

    private fun WishProductsParser.Product.toDbProduct(
        store: MerchantStore,
        category: Category,
        lan: Language
    ): Product {
        val currentProduct = Product().apply {
            sku = this@toDbProduct.id
            manufacturer = null
            type = null
            merchantStore = store
            isProductShipeable = true
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
            name = "Product description"
            language = lan
            seUrl = null
            product = currentProduct
        }
        URL(imageUrl).openStream().use {
        }
        createProductImage(currentProduct)?.let(currentProduct.images::add)
        currentProduct.descriptions.add(productDescription)
        currentProduct.categories.add(category)
        return currentProduct
    }

    private fun WishProductsParser.Product.createProductImage(newProduct: Product) = try {
        URL(imageUrl).openStream().use {
            ProductImage().apply {
                productImage = "${this@createProductImage.id}.jpeg"
                product = newProduct
                image = ByteArrayInputStream(IOUtils.toByteArray(it))
            }
        }
    } catch (e: Exception) {
        LOGGER.error("Error while reading product image", e)
        null
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(WishProductsStore::class.java)
    }
}