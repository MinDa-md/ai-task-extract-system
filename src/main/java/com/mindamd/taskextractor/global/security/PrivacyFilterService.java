package com.mindamd.taskextractor.global.security;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PrivacyFilterService {

    // 1. 전화번호 (010-1234-5678, 01012345678)
    private static final Pattern PHONE_PATTERN = Pattern.compile("010-?\\d{4}-?\\d{4}");

    // 2. 생년월일 (YYYY-MM-DD, YYMMDD 등)
    private static final Pattern BIRTH_PATTERN = Pattern.compile("\\b(?:19|20)?\\d{2}[-./]?(?:0[1-9]|1[0-2])[-./]?(?:0[1-9]|[12]\\d|3[01])\\b");

    // 3. 장소/주소 (기본적인 '시/구/동' 형태 매칭)
    private static final Pattern LOCATION_PATTERN = Pattern.compile("([가-힣]+(시|도)\\s+[가-힣]+(구|군|시)\\s+[가-힣]+(동|읍|면|리)\\s*\\d*-?\\d*)");

    // 4. 이름 (한국어 2~4글자 + 뒤에 '님, 대리, 이, 가, 은, 는' 등의 조사가 붙는 문맥을 타겟팅)
    // 자연어에서 이름만 완벽히 뽑아내는 것은 정규식만으로는 한계가 있어, 주로 사용되는 문맥을 활용합니다.
    private static final Pattern NAME_PATTERN = Pattern.compile("([가-힣]{2,4})(?=\\s*(님|과장|대리|사원|의|가|은|는|이|와|과))");

    public record FilterResult(String filteredText, Map<String, String> dictionary) {}

    public FilterResult pseudonymize(String originalText) {
        Map<String, String> dictionary = new HashMap<>();
        String tempText = originalText;

        // 각 패턴별로 순차적으로 가명 처리 (치환) 수행
        tempText = applyPattern(tempText, PHONE_PATTERN, "PHONE", dictionary);
        tempText = applyPattern(tempText, BIRTH_PATTERN, "BIRTH", dictionary);
        tempText = applyPattern(tempText, LOCATION_PATTERN, "LOC", dictionary);
        tempText = applyPattern(tempText, NAME_PATTERN, "NAME", dictionary);

        return new FilterResult(tempText, dictionary);
    }

    /**
     * 특정 정규식 패턴을 찾아 가명 텍스트로 치환하고 사전에 저장합니다.
     */
    private String applyPattern(String text, Pattern pattern, String prefix, Map<String, String> dictionary) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        int count = 1;

        while (matcher.find()) {
            String realData = matcher.group(1) != null && pattern == NAME_PATTERN ? matcher.group(1) : matcher.group();

            // 이미 동일한 값이 사전에 등록되어 있다면 기존 식별자 재사용 (예: 홍길동 -> [NAME_1])
            String pseudonym = findPseudonymByValue(dictionary, realData);

            if (pseudonym == null) {
                pseudonym = "[" + prefix + "_" + count + "]";
                dictionary.put(pseudonym, realData);
                count++;
            }
            matcher.appendReplacement(sb, pseudonym);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 중복된 데이터에 같은 가명 식별자를 부여하기 위해 맵(Value)을 뒤지는 유틸리티
     */
    private String findPseudonymByValue(Map<String, String> dict, String value) {
        for (Map.Entry<String, String> entry : dict.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String restore(String llmResponse, Map<String, String> dictionary) {
        String restored = llmResponse;
        // [PHONE_1], [NAME_1] 등을 다시 원래 데이터로 치환
        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            restored = restored.replace(entry.getKey(), entry.getValue());
        }
        return restored;
    }
}