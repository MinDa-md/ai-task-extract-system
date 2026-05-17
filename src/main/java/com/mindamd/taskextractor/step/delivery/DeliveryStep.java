package com.mindamd.taskextractor.step.delivery;

import com.mindamd.taskextractor.domain.dto.PipelineDto;
import com.mindamd.taskextractor.domain.entity.Summary;
import com.mindamd.taskextractor.domain.repository.SummaryRepository;
import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.step.db.SavedSummary;
import com.mindamd.taskextractor.step.filter.PrivacyFilterStep;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DeliveryStep implements Step<SavedSummary, DeliveryResult> {

    private final JDA jda;
    private final SummaryRepository summaryRepository;
    private final PrivacyFilterStep privacyFilterStep;
    private final ObjectMapper objectMapper;

    @Override
    public Integer getStepOrder() {
        return 99;
    }

    @Override
    public DeliveryResult execute(PipelineDto context, SavedSummary input) {
        Summary summary = summaryRepository.findByRequestKey(context.requestKey())
                .orElseThrow(() -> new NonRecoverableException("Summary not found: " + context.requestKey()));
        TextChannel channel = jda.getTextChannelById(summary.getChannelId());
        if (channel == null) {
            throw new NonRecoverableException("Discord 채널을 찾을 수 없습니다: " + summary.getChannelId());
        }
        Map<String, String> dictionary = parseDictionary(summary.getSecureDictionary());
        channel.sendMessage(formatMessage(summary, dictionary)).queue();
        return new DeliveryResult(true);
    }

    private String formatMessage(Summary summary, Map<String, String> dictionary) {
        String location = privacyFilterStep.restore(summary.getLocation(), dictionary);
        String participants = privacyFilterStep.restore(summary.getParticipantsInfo(), dictionary);
        return String.format("**회의 요약**\n일시: %s\n장소: %s\n참석자: %s",
                summary.getMeetingTime(), location, participants);
    }

    private Map<String, String> parseDictionary(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new NonRecoverableException("딕셔너리 역직렬화 실패", e);
        }
    }

    @Override
    public Class<DeliveryResult> outputType() {
        return DeliveryResult.class;
    }
}
