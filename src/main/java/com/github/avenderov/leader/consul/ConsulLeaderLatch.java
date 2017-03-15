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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ConsulLeaderLatch implements LeaderLatch {

    private static final Logger LOG = LoggerFactory.getLogger(ConsulLeaderLatch.class);

    private final Object lifecycleMonitor = new Object();

    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    private final AtomicBoolean leader = new AtomicBoolean();

    private final String name;

    private final String lockKey;

    private final String sessionId;

    private final Consul consul;

    private final KVCache kvCache;

    public ConsulLeaderLatch(final ConsulLeaderLatchProperties properties) {
        this.consul = Consul.builder().build();

        this.name = String.format("session-%s-%s", properties.getApplicationName(), UUID.randomUUID().toString());
        final Session session = ImmutableSession.builder()
                .name(name)
                .lockDelay("15s")
                .ttl("0s")
                .addChecks("serfHealth")
                .build();
        this.sessionId = consul.sessionClient().createSession(session).getId();
        LOG.info("Session id: {}", this.sessionId);

        this.lockKey = String.format("service/%s/leader", properties.getApplicationName());
        final Consul asyncCallsConsul = Consul.builder().withReadTimeoutMillis(TimeUnit.SECONDS.toMillis(30L)).build();
        final KVCache kvCache = KVCache.newCache(asyncCallsConsul.keyValueClient(), lockKey, 15);
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
        final boolean acquired = consul.keyValueClient().acquireLock(lockKey, sessionId);
        LOG.info("Lock for the key {} acquired: {}", lockKey, acquired);

        leader.set(acquired);
    }

    @Override
    public State getState() {
        return state.get();
    }

    public String getName() {
        return name;
    }

    @Override
    public void close() throws Exception {
        synchronized (this.lifecycleMonitor) {
            if (state.get() != State.STARTED) {
                return;
            }

            kvCache.stop();
            consul.sessionClient().destroySession(sessionId);
            LOG.info("Session {} deleted", sessionId);

            state.set(State.STOPPED);
        }
    }

    private class KeyChangeListener implements ConsulCache.Listener<String, Value> {

        @Override
        public void notify(final Map<String, Value> newValues) {
            final Value value = newValues.get("");
            final Optional<String> sessionOpt = value.getSession();

            if (sessionOpt.isPresent()) {
                final String session = sessionOpt.get();

                final boolean oldValue = leader.getAndSet(sessionId.equals(session));
            } else {
                tryAcquireLock();
            }
        }

    }

}
