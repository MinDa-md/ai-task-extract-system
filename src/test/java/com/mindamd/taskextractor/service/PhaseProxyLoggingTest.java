package com.mindamd.taskextractor.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mindamd.taskextractor.domain.Phase;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.entity.PhaseExecution;
import com.mindamd.taskextractor.domain.repository.PhaseExecutionRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhaseProxyLoggingTest {

    PhaseExecutionRepository mockRepo = mock(PhaseExecutionRepository.class);
    PhaseProxy phaseProxy = new PhaseProxy(mockRepo);

    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    Phase mockPhase = mock(Phase.class);

    Logger logger = (Logger) LoggerFactory.getLogger(PhaseProxy.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    static class TestPhaseData implements PhaseData {}

    @BeforeEach
    void setUp() throws Throwable {
        listAppender.start();
        logger.addAppender(listAppender);

        when(pjp.getArgs()).thenReturn(new Object[]{"pipeline-log-1", new TestPhaseData()});
        when(pjp.getTarget()).thenReturn(mockPhase);
        when(mockPhase.getStepId()).thenReturn("step-1");
        when(mockRepo.findByPipelineIdAndStepId(any(), any())).thenReturn(Optional.empty());
        when(mockRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mockPhase.serialize(any())).thenReturn("{}");
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    // Phase 실행 시작 로그 기록
    void logsPhaseStart() throws Throwable {
        // given
        when(pjp.proceed()).thenReturn(new TestPhaseData());

        // when
        phaseProxy.intercept(pjp);

        // then
        assertThat(listAppender.list)
                .anyMatch(e -> e.getFormattedMessage().contains("started")
                        && e.getFormattedMessage().contains("step-1"));
    }

    @Test
    // Phase 완료 시 소요시간 로그 기록
    void logsPhaseCompletionWithElapsed() throws Throwable {
        // given
        when(pjp.proceed()).thenReturn(new TestPhaseData());

        // when
        phaseProxy.intercept(pjp);

        // then
        assertThat(listAppender.list)
                .anyMatch(e -> e.getFormattedMessage().contains("completed")
                        && e.getFormattedMessage().contains("elapsed"));
    }

    @Test
    // Phase 실패 시 실패 로그 기록
    void logsPhaseFailure() throws Throwable {
        // given
        when(pjp.proceed()).thenThrow(new RuntimeException("api error"));

        // when
        catchThrowable(() -> phaseProxy.intercept(pjp));

        // then
        assertThat(listAppender.list)
                .anyMatch(e -> e.getFormattedMessage().contains("failed"));
    }

    @Test
    // 캐시 히트 시 skip 로그 기록
    void logsCacheHit() throws Throwable {
        // given
        PhaseExecution cached = mock(PhaseExecution.class);
        when(cached.getResult()).thenReturn("{}");
        when(mockRepo.findByPipelineIdAndStepId(any(), any())).thenReturn(Optional.of(cached));
        when(mockPhase.deserialize(any())).thenReturn(new TestPhaseData());

        // when
        phaseProxy.intercept(pjp);

        // then
        assertThat(listAppender.list)
                .anyMatch(e -> e.getFormattedMessage().contains("cached"));
        verify(pjp, never()).proceed();
    }
}
