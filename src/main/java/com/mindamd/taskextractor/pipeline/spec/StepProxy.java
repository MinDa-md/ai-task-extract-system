package com.mindamd.taskextractor.pipeline.spec;

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
        Step step = (Step) pjp.getTarget();
        String pipelineId = (String) pjp.getArgs()[0];
        Integer stepOrder = step.getStepOrder();

        log.info("Step [{}] started. pipelineId={}", stepOrder, pipelineId);
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("Step [{}] completed. pipelineId={}, elapsed={}ms", stepOrder, pipelineId, elapsed);
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Step [{}] failed. pipelineId={}, elapsed={}ms, error={}", stepOrder, pipelineId, elapsed, t.getClass().getSimpleName());
            throw t;
        }
    }
}
