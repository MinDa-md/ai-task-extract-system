package com.mindamd.taskextractor.step.ingest;

import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import com.mindamd.taskextractor.pipeline.spec.PipelineRequest;
import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.pipeline.spec.StepResult;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class IngestStep implements Step<PipelineRequest, RawChatLog> {

    private final JDA jda;

    public IngestStep(@Lazy JDA jda) {
        this.jda = jda;
    }

    @Override
    public Integer getStepOrder() {
        return 0;
    }

    @Override
    public StepResult<RawChatLog> execute(PipelineRequest input) {
        TextChannel channel = jda.getTextChannelById(input.channelId());
        if (channel == null) {
            throw new NonRecoverableException("Channel not found: " + input.channelId());
        }
        List<Message> messages = new ArrayList<>(
                channel.getHistoryBefore(input.requestKey(), 50)
                        .complete()
                        .getRetrievedHistory()
        );
        Collections.reverse(messages);
        String rawText = messages.stream()
                .map(m -> m.getAuthor().getName() + ": " + m.getContentRaw())
                .collect(Collectors.joining("\n"));
        return StepResult.of(new RawChatLog(input.requestKey(), rawText));
    }
}
