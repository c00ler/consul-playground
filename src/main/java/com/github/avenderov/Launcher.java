package com.github.avenderov;

import com.orbitz.consul.Consul;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Launcher {

    @Bean
    public Consul consul() {
        return Consul.builder().build();
    }

	public static void main(String[] args) {
		SpringApplication.run(Launcher.class, args);
	}

}
