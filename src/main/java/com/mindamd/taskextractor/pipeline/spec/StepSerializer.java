package com.mindamd.taskextractor.pipeline.spec;

import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class StepSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(StepData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new NonRecoverableException("직렬화 실패", e);
        }
    }

    public <T extends StepData> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new NonRecoverableException("역직렬화 실패", e);
        }
    }
}
