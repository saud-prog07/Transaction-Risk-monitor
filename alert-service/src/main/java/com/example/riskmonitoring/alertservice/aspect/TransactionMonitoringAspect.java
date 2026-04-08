package com.example.riskmonitoring.alertservice.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AOP aspect for transaction monitoring and logging.
 * Tracks transaction performance and handles errors.
 */
@Slf4j
@Aspect
@Component
public class TransactionMonitoringAspect {

    /**
     * Monitors execution of transactional methods.
     * Logs transaction duration and any errors.
     *
     * @param joinPoint the join point
     * @return the method result
     * @throws Throwable if an error occurs
     */
    @Around("@annotation(transactional)")
    public Object monitorTransaction(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        long startTime = System.currentTimeMillis();

        try {
            log.debug("Transaction started: {}.{}", className, methodName);

            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Transaction completed successfully: {}.{} ({}ms)", className, methodName, duration);

            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Transaction failed after {}ms: {}.{}, Error: {}",
                    duration, className, methodName, ex.getMessage(), ex);
            throw ex;
        }
    }
}
