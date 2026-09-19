package com.prizm;

import com.prizm.config.GeminiProperties;
import com.prizm.config.PrizmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({PrizmProperties.class, GeminiProperties.class})
public class PrizmApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrizmApplication.class, args);
    }
}
