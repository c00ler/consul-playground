package com.github.avenderov.leader.consul;

public final class ConsulLeaderLatchProperties {

    private String applicationName;

    private boolean ping = true;

    public ConsulLeaderLatchProperties() {
    }

    private ConsulLeaderLatchProperties(final Builder builder) {
        this.applicationName = builder.applicationName;
        if (builder.ping != null) {
            this.ping = builder.ping;
        }
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public boolean isPing() {
        return ping;
    }

    public void setPing(final boolean ping) {
        this.ping = ping;
    }

    public static class Builder {

        private String applicationName;

        private Boolean ping;

        public Builder applicationName(final String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public Builder ping(final Boolean ping) {
            this.ping = ping;
            return this;
        }

        public ConsulLeaderLatchProperties build() {
            return new ConsulLeaderLatchProperties(this);
        }

    }

}
