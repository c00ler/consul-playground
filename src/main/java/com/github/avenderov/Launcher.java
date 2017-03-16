package com.github.avenderov;

import com.github.avenderov.leader.aspect.RunIfLeaderAspect;
import com.github.avenderov.leader.consul.ConsulInfoContributor;
import com.github.avenderov.leader.consul.ConsulLeaderLatch;
import com.github.avenderov.leader.consul.ConsulLeaderLatchProperties;
import com.orbitz.consul.Consul;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Launcher {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public Consul consul() {
        return Consul.builder().build();
    }

    @Bean(initMethod = "start")
    public ConsulLeaderLatch leaderLatch() {
        return new ConsulLeaderLatch(
                new ConsulLeaderLatchProperties.Builder().applicationName(applicationName).build());
    }

    @Bean
    public InfoContributor consulInfoContributor(final ConsulLeaderLatch leaderLatch) {
        return new ConsulInfoContributor(leaderLatch);
    }

    @Bean
    public RunIfLeaderAspect runIfLeaderAspect(final ConsulLeaderLatch leaderLatch) {
        return new RunIfLeaderAspect(leaderLatch);
    }

    public static void main(String[] args) {
        SpringApplication.run(Launcher.class, args);
    }

}
