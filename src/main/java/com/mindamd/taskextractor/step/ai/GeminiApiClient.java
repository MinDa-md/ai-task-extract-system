package com.mindamd.taskextractor.step.ai;

import org.springframework.stereotype.Component;

@Component
public class GeminiApiClient {

    /**
     * Gemini API에서 추출한 구조화된 요약 결과
     * 가명 처리된 텍스트를 받아 가명 상태 그대로 필드를 추출하여 반환
     */
    public record SummaryResult(String meetingTime, String location, String participantsInfo) {}

    /**
     * 가명 처리된 채팅 로그를 Gemini에 전송하여 요약 결과 수신
     * TODO: 실제 Gemini REST API 연동 구현
     */
    public SummaryResult summarize(String pseudonymizedText) {
        // 구현 예정
        throw new UnsupportedOperationException("Gemini API 연동 미구현");
    }
}