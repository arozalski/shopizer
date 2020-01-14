package com.salesmanager.shop.application

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.support.SpringBootServletInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.util.concurrent.Executor
import java.util.concurrent.Executors



@Configuration
@SpringBootApplication
@EnableScheduling
open class ShopApplication : SpringBootServletInitializer(), SchedulingConfigurer {

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor())
    }

    @Bean(destroyMethod = "shutdown")
    open fun taskExecutor(): Executor = Executors.newScheduledThreadPool(2)

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ShopApplication::class.java, *args)
        }
    }
}
