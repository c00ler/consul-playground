package com.github.avenderov.leader.consul;

public final class ConsulLeaderLatchProperties {

    private String applicationName;

    public ConsulLeaderLatchProperties() {
    }

    private ConsulLeaderLatchProperties(final Builder builder) {
        this.applicationName = builder.applicationName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public static class Builder {

        private String applicationName;

        public Builder applicationName(final String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public ConsulLeaderLatchProperties build() {
            return new ConsulLeaderLatchProperties(this);
        }

    }

}
