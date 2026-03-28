package com.mindamd.taskextractor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindamd.taskextractor.domain.dto.SummaryResponseDto;
import com.mindamd.taskextractor.domain.entity.Summary;
import com.mindamd.taskextractor.domain.repository.SummaryRepository;
import com.mindamd.taskextractor.global.security.PrivacyFilterService;
import com.mindamd.taskextractor.infrastructure.ai.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SummaryRepository summaryRepository;
    private final PrivacyFilterService privacyFilterService;
    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    /**
     * 채팅 로그를 가명 처리 → LLM 요약 → 가명 상태로 저장 + 딕셔너리 암호화 보관
     * 조회 시점에 딕셔너리를 복호화하여 원본을 복원한 뒤 DTO 반환
     */
    public SummaryResponseDto createSummary(String requestKey, String channelId, List<String> chatLogs) throws Exception {

        // 1. 멱등성 보장: 동일한 requestKey가 이미 처리된 경우 저장 없이 바로 반환
        if (summaryRepository.existsByRequestKey(requestKey)) {
            Summary existing = summaryRepository.findByRequestKey(requestKey).orElseThrow();
            return buildResponseDto(existing);
        }

        // 2. 원본 채팅 로그를 하나의 문자열로 합치기
        String rawText = String.join("\n", chatLogs);

        // 3. 가명 처리 (이름·전화번호·주소 등 → [NAME_1], [PHONE_1], [LOC_1] 형태로 치환)
        //    dictionary: 나중에 복원하기 위한 가명 ↔ 원본 매핑 테이블
        PrivacyFilterService.FilterResult filterResult = privacyFilterService.pseudonymize(rawText);

        // 4. 가명 처리된 텍스트를 Gemini에 전송 (개인정보 없는 안전한 텍스트만 외부 LLM으로 나감)
        GeminiApiClient.SummaryResult llmResult = geminiApiClient.summarize(filterResult.filteredText());

        // 5. 딕셔너리 JSON 직렬화
        //    민감 정보가 아예 없는 경우(치환 없음) null 저장 → DB secure_dictionary NULL 허용
        String dictionaryJson = filterResult.dictionary().isEmpty()
                ? null
                : objectMapper.writeValueAsString(filterResult.dictionary());

        // 6. 가명 상태 그대로 엔티티 저장 + 딕셔너리는 @Convert로 자동 AES-256 암호화
        Summary summary = new Summary(
                requestKey,
                channelId,
                llmResult.meetingTime(),
                llmResult.location(),       // 예: "[LOC_1]"
                llmResult.participantsInfo(), // 예: "[NAME_1] ([PHONE_1])"
                dictionaryJson              // JSON → AES-256 암호화 후 DB 저장
        );
        summaryRepository.save(summary);

        // 7. 응답 DTO 생성: 이 시점이 "조회 시점" — 딕셔너리 복호화 후 가명 → 원본 복원
        return buildResponseDto(summary);
    }

    /**
     * 저장된 엔티티를 DTO로 변환 (조회 시점 복원 로직)
     * secureDictionary는 @Convert에 의해 이미 복호화된 JSON 문자열 상태
     */
    private SummaryResponseDto buildResponseDto(Summary summary) throws Exception {
        Map<String, String> dictionary = deserializeDictionary(summary.getSecureDictionary());

        return SummaryResponseDto.builder()
                .meetingTime(summary.getMeetingTime())
                .location(privacyFilterService.restore(summary.getLocation(), dictionary))
                .participantsInfo(privacyFilterService.restore(summary.getParticipantsInfo(), dictionary))
                .build();
    }

    /**
     * 복호화된 JSON 문자열을 Map으로 역직렬화
     * secureDictionary가 null이면 (민감 정보 없었던 경우) 빈 Map 반환
     */
    private Map<String, String> deserializeDictionary(String secureDictionary) throws Exception {
        if (secureDictionary == null || secureDictionary.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(secureDictionary, new TypeReference<Map<String, String>>() {});
    }
}