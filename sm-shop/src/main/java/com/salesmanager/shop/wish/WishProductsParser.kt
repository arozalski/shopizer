package com.salesmanager.shop.wish

import org.json.JSONObject
import java.math.BigDecimal

object WishProductsParser {

    fun parse(response: String) = JSONObject(response).getProducts()

    private fun JSONObject.getProducts(): List<Product> {
        val jsonProducts = getJSONObject("data").getJSONArray("products")
        return (0 until jsonProducts.length()).map {
            val jsonProduct = jsonProducts.getJSONObject(it)
            val jsonVariant = jsonProduct.getJSONObject("commerce_product_info").getJSONArray("variations").getJSONObject(0)
            Product(
                id = jsonProduct.getString("id"),
                name = jsonProduct.getString("name"),
                description = "",
                retailPrice = jsonVariant.getJSONObject("localized_retail_price").getDouble("localized_value").toBigDecimal(),
                promoPrice = jsonVariant.getJSONObject("localized_price").getDouble("localized_value").toBigDecimal(),
                rating = jsonProduct.getJSONObject("product_rating").getDouble("rating"),
                numberOfBought = jsonProduct.getInt("num_bought"),
                imageUrl = jsonProduct.getString("small_picture")
            )
        }
    }

    data class Product(
        val id: String,
        val name: String,
        val description: String,
        val retailPrice: BigDecimal,
        val promoPrice: BigDecimal,
        val rating: Double,
        val numberOfBought: Int,
        val imageUrl: String
    )
}