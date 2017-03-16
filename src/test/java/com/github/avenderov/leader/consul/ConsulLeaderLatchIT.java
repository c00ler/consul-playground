package com.github.avenderov.leader.consul;

import com.github.avenderov.leader.LeaderLatch;
import com.google.common.base.Optional;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.model.session.SessionInfo;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ConsulLeaderLatchIT {

    private final Consul consul = Consul.builder().build();

    private final ConsulLeaderLatch leaderLatch =
            new ConsulLeaderLatch(new ConsulLeaderLatchProperties.Builder().applicationName("app").ping(false).build());

    @After
    public void afterEach() throws Exception {
        leaderLatch.close();
    }

    @Test
    public void leaderWhenStarted() {
        leaderLatch.start();

        await().atMost(10, TimeUnit.SECONDS).until(leaderLatch::isLeader);
        assertThat(leaderLatch.getState()).isSameAs(LeaderLatch.State.STARTED);
    }

    @Test
    public void sessionIdWhenStarted() {
        leaderLatch.start();

        await().atMost(10, TimeUnit.SECONDS).until(leaderLatch::isLeader);
        assertThat(leaderLatch.getSessionId()).isNotEmpty();
    }

    @Test
    public void notLeaderWhenStopped() throws Exception {
        leaderLatch.start();

        await().atMost(10, TimeUnit.SECONDS).until(leaderLatch::isLeader);

        leaderLatch.close();
        assertThat(leaderLatch.getState()).isSameAs(LeaderLatch.State.STOPPED);
        assertThat(leaderLatch.isLeader()).isFalse();
    }

    @Test
    public void leaderAfterRelease() {
        leaderLatch.start();

        await().atMost(10, TimeUnit.SECONDS).until(leaderLatch::isLeader);

        final String sessionId = leaderLatch.getSessionId();
        final boolean released = consul.keyValueClient().releaseLock("service/app/leader", sessionId);

        assertThat(released).isTrue();

        await().atMost(10, TimeUnit.SECONDS).until(leaderLatch::isLeader);
        assertThat(leaderLatch.getSessionId()).isEqualTo(sessionId);
    }

    @Test
    public void releaseSessionWhenStopped() throws Exception {
        leaderLatch.start();

        await().atMost(10, TimeUnit.SECONDS).until(leaderLatch::isLeader);

        final Value beforeClose = consul.keyValueClient().getValue("service/app/leader").get();
        assertThat(beforeClose.getSession().isPresent()).isTrue();

        leaderLatch.close();
        assertThat(leaderLatch.getState()).isSameAs(LeaderLatch.State.STOPPED);

        final Value afterClose = consul.keyValueClient().getValue("service/app/leader").get();
        assertThat(afterClose.getSession().isPresent()).isFalse();
    }

    @Test
    public void deleteSessionWhenStopped() throws Exception {
        leaderLatch.start();

        await().atMost(10, TimeUnit.SECONDS).until(leaderLatch::isLeader);

        final String sessionId = leaderLatch.getSessionId();
        final Optional<SessionInfo> beforeClose = consul.sessionClient().getSessionInfo(sessionId);
        assertThat(beforeClose.isPresent()).isTrue();

        leaderLatch.close();

        final Optional<SessionInfo> afterClose = consul.sessionClient().getSessionInfo(sessionId);
        assertThat(afterClose.isPresent()).isFalse();
    }

}
