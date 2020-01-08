package com.salesmanager.shop.application

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.support.SpringBootServletInitializer

@SpringBootApplication
open class ShopApplication : SpringBootServletInitializer() {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ShopApplication::class.java, *args)
        }
    }
}
