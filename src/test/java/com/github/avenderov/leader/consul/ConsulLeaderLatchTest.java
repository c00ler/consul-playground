package com.github.avenderov.leader.consul;

import com.github.avenderov.leader.LeaderLatch;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsulLeaderLatchTest {

    private final ConsulLeaderLatch leaderLatch =
            new ConsulLeaderLatch(new ConsulLeaderLatchProperties.Builder().applicationName("app").ping(false).build());

    @Test
    public void notLeaderWhenNew() {
        assertThat(leaderLatch.getState()).isSameAs(LeaderLatch.State.NEW);
        assertThat(leaderLatch.isLeader()).isFalse();
    }

    @Test
    public void getSessionName() {
        assertThat(leaderLatch.getSessionName()).startsWith("session-app");
    }

    @Test
    public void noSessionIdWhenNew() {
        assertThat(leaderLatch.getState()).isSameAs(LeaderLatch.State.NEW);
        assertThat(leaderLatch.getSessionId()).isNull();
    }

}
