package com.github.avenderov.leader.aspect;

import com.github.avenderov.leader.LeaderLatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class RunIfLeaderAspect {

    private static final Logger LOG = LoggerFactory.getLogger("consul.leader");

    private final LeaderLatch leaderLatch;

    public RunIfLeaderAspect(final LeaderLatch leaderLatch) {
        this.leaderLatch = leaderLatch;
    }

    @Around("@annotation(com.github.avenderov.leader.annotation.RunIfLeader) && execution(void *(..))")
    public Object methodAnnotatedWithRunIfLeader(final ProceedingJoinPoint joinPoint) throws Throwable {
        if (leaderLatch.isLeader()) {
            joinPoint.proceed();
        } else {
            LOG.debug("Skip '{}' execution, because this node is not a leader", joinPoint.getSignature().toString());
        }

        return null;
    }

}
