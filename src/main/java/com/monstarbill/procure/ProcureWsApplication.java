package com.monstarbill.procure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import feign.Logger;

@SpringBootApplication
@EnableJpaAuditing
@EnableEurekaClient
@EnableAutoConfiguration
@EnableFeignClients
public class ProcureWsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProcureWsApplication.class, args);
	}

	@Bean
	Logger.Level fiegnLoggerLevel() {
		return Logger.Level.FULL;
	}
	
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedOrigins("http://localhost:8082");
			}
		};
	}
}
