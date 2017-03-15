package com.github.avenderov;

import com.github.avenderov.leader.LeaderLatch;
import com.github.avenderov.leader.consul.ConsulLeaderLatch;
import com.github.avenderov.leader.consul.ConsulLeaderLatchProperties;
import com.orbitz.consul.Consul;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Launcher {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public Consul consul() {
        return Consul.builder().build();
    }

    @Bean
    public LeaderLatch leaderLatch() {
        final ConsulLeaderLatch leaderLatch = new ConsulLeaderLatch(
                new ConsulLeaderLatchProperties.Builder().applicationName(applicationName).build());
        leaderLatch.start();

        return leaderLatch;
    }

    public static void main(String[] args) {
        SpringApplication.run(Launcher.class, args);
    }

}
