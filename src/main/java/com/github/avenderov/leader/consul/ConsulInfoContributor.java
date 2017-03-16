package com.github.avenderov.leader.consul;

import com.google.common.collect.ImmutableMap;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

public final class ConsulInfoContributor implements InfoContributor {

    private final ConsulLeaderLatch leaderLatch;

    public ConsulInfoContributor(final ConsulLeaderLatch leaderLatch) {
        this.leaderLatch = leaderLatch;
    }

    @Override
    public void contribute(final Info.Builder builder) {
        final ImmutableMap.Builder<String, String> info = ImmutableMap.<String, String>builder()
                .put("leader", Boolean.toString(leaderLatch.isLeader()))
                .put("state", leaderLatch.getState().name())
                .put("session_name", leaderLatch.getSessionName());

        if (leaderLatch.getSessionId() != null) {
            info.put("session_id", leaderLatch.getSessionId());
        }

        builder.withDetail("consul", info.build());
    }

}
