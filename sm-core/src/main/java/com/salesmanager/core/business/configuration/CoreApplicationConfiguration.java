package com.salesmanager.core.business.configuration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableAutoConfiguration
/**
 * added in shopizer-core-config.xml
 * 
 * @author c.samson
 *
 */
@ComponentScan({"com.salesmanager.core.business"})
@ImportResource("classpath:/spring/shopizer-core-context.xml")
//@Import({DroolsConfiguration.class,VaultConfiguration.class})
@Import({DroolsConfiguration.class})
public class CoreApplicationConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
