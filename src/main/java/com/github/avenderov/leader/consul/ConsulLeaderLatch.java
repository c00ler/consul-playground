package com.github.avenderov.leader.consul;

import com.github.avenderov.leader.LeaderLatch;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.session.ImmutableSession;
import com.orbitz.consul.model.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ConsulLeaderLatch implements LeaderLatch {

    private static final Logger LOG = LoggerFactory.getLogger(ConsulLeaderLatch.class);

    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    private final AtomicBoolean leader = new AtomicBoolean();

    private final AtomicReference<String> sessionId = new AtomicReference<>();

    private final Consul consul;

    private final String applicationName;

    public ConsulLeaderLatch(final Consul consul, final String applicationName) {
        this.consul = Objects.requireNonNull(consul, "consul must not be null");
        this.applicationName = applicationName;
    }

    @Override
    public boolean isLeader() {
        return state.get() == State.STARTED && leader.get();
    }

    @Override
    public void start() {
        if (state.get() != State.NEW) {
            return;
        }

        final Session session = ImmutableSession.builder()
                .name(String.format("session-%s-%s", applicationName, UUID.randomUUID().toString()))
                .lockDelay("15s")
                .ttl("0s")
                .addChecks("serfHealth")
                .build();

        final String newSessionId = consul.sessionClient().createSession(session).getId();
        LOG.info("New session id: {}", newSessionId);

        sessionId.set(newSessionId);

        final String lockKey = String.format("service/%s/leader", applicationName);
        final boolean acquired = consul.keyValueClient().acquireLock(lockKey, sessionId.get());
        LOG.info("Lock for the queue {} acquired: {}", lockKey, acquired);

        leader.set(acquired);

        // TODO: start listening for the changes

        state.set(State.STARTED);
    }

    @Override
    public State getState() {
        return state.get();
    }

    @Override
    public void close() throws Exception {
        if (state.get() == State.STARTED) {
            consul.sessionClient().destroySession(sessionId.get());

            LOG.info("Session {} deleted", sessionId.get());

            state.set(State.CLOSED);
        }
    }

}
