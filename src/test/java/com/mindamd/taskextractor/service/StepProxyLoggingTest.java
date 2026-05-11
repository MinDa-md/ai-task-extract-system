package com.mindamd.taskextractor.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mindamd.taskextractor.domain.Step;
import com.mindamd.taskextractor.domain.StepData;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StepProxyLoggingTest {

    @Mock
    ProceedingJoinPoint pjp;

    @Mock
    Step step;

    StepProxy stepProxy;
    ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        stepProxy = new StepProxy();

        Logger logger = (Logger) LoggerFactory.getLogger(StepProxy.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        when(pjp.getTarget()).thenReturn(step);
        when(pjp.getArgs()).thenReturn(new Object[]{"P1", mock(StepData.class)});
        when(step.getStepOrder()).thenReturn(10);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(StepProxy.class);
        logger.detachAppender(appender);
    }

    @Test
    void tc10_stepStarted_infoLogRecorded() throws Throwable {
        when(pjp.proceed()).thenReturn(mock(StepData.class));
        stepProxy.intercept(pjp);
        assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void tc11_stepCompleted_infoLogRecorded() throws Throwable {
        when(pjp.proceed()).thenReturn(mock(StepData.class));
        stepProxy.intercept(pjp);
        long infoCount = appender.list.stream()
                .filter(e -> e.getLevel() == Level.INFO)
                .count();
        assertThat(infoCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    void tc12_stepFailed_errorLogRecorded() throws Throwable {
        when(pjp.proceed()).thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> stepProxy.intercept(pjp))
                .isInstanceOf(RuntimeException.class);
        assertThat(appender.list)
                .anyMatch(e -> e.getLevel() == Level.ERROR);
    }
}
