package com.salesmanager.shop.application

import com.salesmanager.shop.wish.WishProductsFetcher
import com.salesmanager.shop.wish.WishProductsParser
import org.apache.http.HttpEntity
import org.apache.http.HttpHeaders
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.support.SpringBootServletInitializer
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.util.ArrayList

@SpringBootApplication
open class ShopApplication : SpringBootServletInitializer() {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ShopApplication::class.java, *args)

            Thread {
                val products = WishProductsFetcher.fetch(10, 0).let(WishProductsParser::parse)
                println(products)
            }.start()
        }
    }
}
