package com.mindamd.taskextractor;

import com.mindamd.taskextractor.pipeline.spec.Step;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TaskExtractorApplicationTests {

    @MockitoBean
    Step stubStep;

    @Test
    void contextLoads() {
    }

}
