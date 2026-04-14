package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.domain.Phase;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.PipelineStatus;
import com.mindamd.taskextractor.domain.entity.PhaseExecution;
import com.mindamd.taskextractor.domain.repository.PhaseExecutionRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
public class PhaseProxy {

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
            return phase.deserialize(cached.get().getResult());
        }

        PhaseData result = (PhaseData) pjp.proceed();

        phaseRepository.save(PhaseExecution.builder()
                .pipelineId(pipelineId)
                .stepId(stepId)
                .status(PipelineStatus.SUCCESS)
                .result(phase.serialize(result))
                .build());

        return result;
    }
}
