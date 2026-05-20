package com.mindamd.taskextractor.pipeline.util;

import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.pipeline.spec.StepData;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class StepProxy {

    private static final Logger log = LoggerFactory.getLogger(StepProxy.class);

    @Around("execution(* com.mindamd.taskextractor.pipeline.spec.Step.execute(..))")
    public Object intercept(ProceedingJoinPoint pjp) throws Throwable {
        Step<StepData, StepData> step = (Step<StepData, StepData>) pjp.getTarget();
        Integer stepOrder = step.getStepOrder();

        log.info("Step [{}] started.", stepOrder);
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("Step [{}] completed. elapsed={}ms", stepOrder, elapsed);
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Step [{}] failed. elapsed={}ms, error={}", stepOrder, elapsed, t.getClass().getSimpleName());
            throw t;
        }
    }
}
