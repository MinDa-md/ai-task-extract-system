package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.domain.Phase;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.entity.PhaseExecution;
import com.mindamd.taskextractor.domain.repository.PhaseExecutionRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
public class PhaseProxy {

    private static final Logger log = LoggerFactory.getLogger(PhaseProxy.class);

    private final PhaseExecutionRepository phaseRepository;

    public PhaseProxy(PhaseExecutionRepository phaseRepository) {
        this.phaseRepository = phaseRepository;
    }

    @Around("execution(* com.mindamd.taskextractor.domain.Phase.execute(..))")
    public Object intercept(ProceedingJoinPoint pjp) throws Throwable {
        String pipelineId = (String) pjp.getArgs()[0];
        Phase phase = (Phase) pjp.getTarget();
        String stepId = phase.getStepId();

        Optional<PhaseExecution> cached = phaseRepository.findByPipelineIdAndStepId(pipelineId, stepId);
        if (cached.isPresent()) {
            log.info("Phase [{}] skipped (cached). pipelineId={}", stepId, pipelineId);
            return phase.deserialize(cached.get().getResult());
        }

        log.info("Phase [{}] started. pipelineId={}", stepId, pipelineId);
        long start = System.currentTimeMillis();
        try {
            PhaseData result = (PhaseData) pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("Phase [{}] completed. pipelineId={}, elapsed={}ms", stepId, pipelineId, elapsed);

            phaseRepository.save(PhaseExecution.builder()
                    .pipelineId(pipelineId)
                    .stepId(stepId)
                    .result(phase.serialize(result))
                    .build());

            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Phase [{}] failed. pipelineId={}, elapsed={}ms, error={}", stepId, pipelineId, elapsed, t.getClass().getSimpleName());
            throw t;
        }
    }
}
