package com.pawar.item.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(scanBasePackages = {"com.pawar.sop.http","com.pawar.item.integration" })
public class ItemIntegration {

	public static void main(String[] args) {
		SpringApplication.run(ItemIntegration.class, args);
	}

}
