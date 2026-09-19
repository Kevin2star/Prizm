package com.prizm.config;

import com.prizm.domain.JoinCodeGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public JoinCodeGenerator joinCodeGenerator() {
        return new JoinCodeGenerator();
    }
}
