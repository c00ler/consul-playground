package com.github.avenderov.job;

import com.github.avenderov.leader.annotation.RunIfLeader;
import com.google.common.base.Optional;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.kv.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LeaderInfoJob {

    private static final Logger LOG = LoggerFactory.getLogger(LeaderInfoJob.class);

    private final Consul consul;

    public LeaderInfoJob(final Consul consul) {
        this.consul = consul;
    }

    @RunIfLeader
    @Scheduled(initialDelay = 5_000, fixedRate = 5_000)
    public void run() {
        final Optional<Value> valueOpt = consul.keyValueClient().getValue("service/rabbit/leader");
        if (valueOpt.isPresent()) {
            LOG.info("Leader info: {}", valueOpt.get());
        }
    }

}
