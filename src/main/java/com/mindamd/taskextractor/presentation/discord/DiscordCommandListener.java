package com.mindamd.taskextractor.presentation.discord;

import com.mindamd.taskextractor.domain.dto.SummaryResponseDto; // 이미지 구조에 맞춘 경로
import com.mindamd.taskextractor.service.SummaryService;       // 이미지 구조에 맞춘 경로
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DiscordCommandListener extends ListenerAdapter {

    private final SummaryService summaryService;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // 1. 봇 자신이 보낸 메시지는 무한 루프 방지를 위해 무시
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();

        // 2. 명령어 감지
        if (message.equals("/정리해줘")) {
            String channelId = event.getChannel().getId();

            // 3. 멱등성 보장을 위한 고유 키 (명령어를 친 디스코드 메시지의 고유 ID)
            String requestKey = event.getMessageId();

            // 4. 최근 20개의 메시지 이력 가져오기 (JDA 라이브러리 활용)
            // 작성자 이름과 내용을 결합하여 LLM이 맥락을 이해하기 쉽게 만듭니다.
            List<String> chatLogs = event.getChannel().getHistory().retrievePast(20).complete()
                    .stream()
                    .map(msg -> msg.getAuthor().getName() + ": " + msg.getContentRaw())
                    .collect(Collectors.toList());

            try {
                // 5. 오케스트레이션 서비스 호출 (가명처리 -> LLM -> 복원 -> 암호화 저장)
                SummaryResponseDto resultDto = summaryService.createSummary(requestKey, channelId, chatLogs);

                // 6. 최종 결과 디스코드 채널에 전송 (마스킹 적용된 텍스트가 나감)
                event.getChannel().sendMessage(resultDto.getFormattedMessage()).queue();

            } catch (Exception e) {
                // 중복 요청(멱등성 예외)이나 API 통신 실패 등에 대한 에러 메시지 처리
                event.getChannel().sendMessage("요청을 처리하는 중 문제가 발생했습니다: " + e.getMessage()).queue();
            }
        }
    }
}