package com.github.avenderov.leader.consul;

import com.github.avenderov.leader.LeaderLatch;
import com.orbitz.consul.Consul;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsulLeaderLatchTest {

    private final LeaderLatch leaderLatch = new ConsulLeaderLatch(Consul.builder().build(), "app");

    @Test
    public void notLeaderWhenNew() {
        assertThat(leaderLatch.getState()).isSameAs(LeaderLatch.State.NEW);
        assertThat(leaderLatch.isLeader()).isFalse();
    }

}
