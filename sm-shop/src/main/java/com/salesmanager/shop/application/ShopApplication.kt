package com.salesmanager.shop.application

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.support.SpringBootServletInitializer
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
open class ShopApplication : SpringBootServletInitializer() {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ShopApplication::class.java, *args)
        }
    }
}
