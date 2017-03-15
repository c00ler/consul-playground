package com.github.avenderov.leader;

public interface LeaderLatch extends AutoCloseable {

    boolean isLeader();

    void start();

    State getState();

    enum State {

        NEW, STARTED, STOPPED

    }

}
