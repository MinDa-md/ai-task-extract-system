package com.mindamd.taskextractor.listener;

import com.mindamd.taskextractor.pipeline.Orchestrator;
import com.mindamd.taskextractor.pipeline.spec.PipelineRequest;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiscordCommandListener extends ListenerAdapter {

    private final Orchestrator orchestrator;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();

        if (message.equals("/정리해줘")) {
            String channelId = event.getChannel().getId();
            String requestKey = event.getMessageId();

            try {
                PipelineRequest pipelineRequest = new PipelineRequest(requestKey, channelId);
                String result = orchestrator.run(pipelineRequest);
                event.getChannel().sendMessage(result).queue();
            } catch (Exception e) {
                event.getChannel().sendMessage("요청을 처리하는 중 문제가 발생했습니다: " + e.getMessage()).queue();
            }
        }
    }
}
