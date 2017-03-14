package com.github.avenderov;

import com.orbitz.consul.Consul;
import com.orbitz.consul.SessionClient;
import com.orbitz.consul.model.session.ImmutableSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
public class ConsulSession {

    private static final Logger LOG = LoggerFactory.getLogger(ConsulSession.class);

    private final String id;

    private final SessionClient sessionClient;

    @Autowired
    public ConsulSession(final Consul consul, @Value("spring.application.name") final String applicationName) {
        sessionClient = consul.sessionClient();
        id = sessionClient.createSession(
                ImmutableSession.builder().name(String.format("%s-session", applicationName)).build()).getId();

        LOG.info("New session id: {}", id);
    }

    @PreDestroy
    void destroy() {
        sessionClient.destroySession(id);
    }

    public String getId() {
        return id;
    }

}
