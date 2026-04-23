package com.mindamd.taskextractor;

import com.mindamd.taskextractor.domain.Phase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TaskExtractorApplicationTests {

    @MockitoBean Phase stubPhase;

    @Test
    void contextLoads() {
    }

}
