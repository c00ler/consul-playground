package com.github.avenderov.controllers;

import com.github.avenderov.exceptions.NotFoundException;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/kv")
class KVController {

    private static final Logger LOG = LoggerFactory.getLogger(KVController.class);

    private final KeyValueClient keyValueClient;

    @Autowired
    public KVController(final Consul consul) {
        this.keyValueClient = consul.keyValueClient();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED)
    void create(@Valid @RequestBody final Pair body) {
        LOG.info("Saving kv pair: {}", body);

        final boolean saved = keyValueClient.putValue(body.getKey(), body.getValue());

        LOG.info("kv pair was saved: {}", saved);
    }

    @GetMapping(value = "/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    Pair find(@PathVariable final String key) {
        LOG.info("Retrieving key: {}", key);

        return keyValueClient.getValueAsString(key).transform(v -> new Pair(key, v)).or(NotFoundException::create);
    }

    static class Pair {

        @NotEmpty
        private String key;

        @NotEmpty
        private String value;

        public Pair() {
        }

        public Pair(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(final String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("key", key)
                    .append("value", value)
                    .toString();
        }

    }

}
