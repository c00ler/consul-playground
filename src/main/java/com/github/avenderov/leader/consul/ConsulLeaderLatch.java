package com.github.avenderov.leader.consul;

import com.github.avenderov.leader.LeaderLatch;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.orbitz.consul.Consul;
import com.orbitz.consul.cache.ConsulCache;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.model.session.ImmutableSession;
import com.orbitz.consul.model.session.Session;
import com.orbitz.consul.model.session.SessionCreatedResponse;
import com.orbitz.consul.option.ConsistencyMode;
import com.orbitz.consul.option.ImmutableQueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ConsulLeaderLatch implements LeaderLatch {

    private static final Logger LOG = LoggerFactory.getLogger(ConsulLeaderLatch.class);

    private final Object lifecycleMonitor = new Object();

    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    private final AtomicReference<String> sessionId = new AtomicReference<>();

    private final AtomicBoolean leader = new AtomicBoolean();

    private final String sessionName;

    private final String lockKey;

    private final Consul consul;

    private final KVCache kvCache;

    public ConsulLeaderLatch(final ConsulLeaderLatchProperties properties) {
        this.consul = Consul.builder()
                .withPing(properties.isPing())
                .build();

        this.sessionName =
                String.format("session-%s-%s", properties.getApplicationName(), UUID.randomUUID().toString());
        this.lockKey = String.format("service/%s/leader", properties.getApplicationName());

        final Consul asyncCallsConsul = Consul.builder()
                .withReadTimeoutMillis(TimeUnit.SECONDS.toMillis(45L))
                .withPing(properties.isPing())
                .build();
        final KVCache kvCache = KVCache.newCache(asyncCallsConsul.keyValueClient(),
                                                 lockKey,
                                                 30,
                                                 ImmutableQueryOptions.builder()
                                                         .consistencyMode(ConsistencyMode.CONSISTENT)
                                                         .build());
        kvCache.addListener(new KeyChangeListener());

        this.kvCache = kvCache;
    }

    @Override
    public boolean isLeader() {
        return state.get() == State.STARTED && leader.get();
    }

    @Override
    public void start() {
        synchronized (this.lifecycleMonitor) {
            if (state.get() != State.NEW) {
                return;
            }

            tryAcquireLock();

            try {
                kvCache.start();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }

            state.set(State.STARTED);
        }
    }

    private void tryAcquireLock() {
        if (sessionId.get() == null || consul.sessionClient().getSessionInfo(sessionId.get()).orNull() == null) {
            createSession();
        }

        final boolean acquired = consul.keyValueClient().acquireLock(lockKey, sessionId.get());
        LOG.info("Lock for the key {} acquired: {}", lockKey, acquired);

        leader.set(acquired);
    }

    private void createSession() {
        final Session session = ImmutableSession.builder()
                .name(sessionName)
                .lockDelay("15s")
                .ttl("0s")
                .addChecks("serfHealth")
                .build();

        final SessionCreatedResponse response = consul.sessionClient().createSession(session);
        LOG.info("Session {} has the new id: {}", sessionName, response.getId());

        this.sessionId.set(response.getId());
    }

    @Nonnull
    @Override
    public State getState() {
        return state.get();
    }

    @Nonnull
    public String getSessionName() {
        return sessionName;
    }

    @Nullable
    public String getSessionId() {
        return sessionId.get();
    }

    @Override
    public void close() throws Exception {
        synchronized (this.lifecycleMonitor) {
            if (state.get() != State.STARTED) {
                return;
            }

            try {
                kvCache.stop();
                if (consul.sessionClient().getSessionInfo(sessionId.get()).isPresent()) {
                    final boolean released = consul.keyValueClient().releaseLock(lockKey, sessionId.get());
                    LOG.info("Lock for the key {} released: {}", lockKey, released);

                    consul.sessionClient().destroySession(sessionId.get());
                    LOG.info("Session {} deleted", sessionId.get());
                }
            } finally {
                state.set(State.STOPPED);
            }
        }
    }

    private class KeyChangeListener implements ConsulCache.Listener<String, Value> {

        @Override
        public void notify(final Map<String, Value> newValues) {
            final Value value = newValues.get("");
            final Optional<String> sessionOpt = value.getSession();

            if (sessionOpt.isPresent()) {
                leader.set(sessionId.get().equals(sessionOpt.get()));
            } else {
                tryAcquireLock();
            }
        }

    }

}
