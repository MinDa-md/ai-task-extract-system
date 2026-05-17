package com.mindamd.taskextractor.step.ingest;

import com.mindamd.taskextractor.domain.dto.PipelineDto;
import com.mindamd.taskextractor.pipeline.spec.Step;
import org.springframework.stereotype.Component;

@Component
public class IngestStep implements Step<ChatPayload, RawChatLog> {

    @Override
    public Integer getStepOrder() {
        return 0;
    }

    @Override
    public RawChatLog execute(PipelineDto context, ChatPayload input) {
        return new RawChatLog(String.join("\n", input.chatLogs()));
    }

    @Override
    public Class<RawChatLog> outputType() {
        return RawChatLog.class;
    }
}
