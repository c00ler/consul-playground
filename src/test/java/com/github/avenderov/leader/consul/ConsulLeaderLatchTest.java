package com.github.avenderov.leader.consul;

import com.github.avenderov.leader.LeaderLatch;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsulLeaderLatchTest {

    private final ConsulLeaderLatch leaderLatch =
            new ConsulLeaderLatch(new ConsulLeaderLatchProperties.Builder().applicationName("app").build());

    @Test
    public void notLeaderWhenNew() {
        assertThat(leaderLatch.getState()).isSameAs(LeaderLatch.State.NEW);
        assertThat(leaderLatch.isLeader()).isFalse();
    }

}
