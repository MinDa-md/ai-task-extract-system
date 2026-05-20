package com.mindamd.taskextractor.step.filter;

import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.pipeline.spec.StepData;
import com.mindamd.taskextractor.pipeline.spec.StepResult;
import com.mindamd.taskextractor.step.ingest.RawChatLog;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PrivacyFilterStep implements Step<RawChatLog, AnonymizedText> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("010-?\\d{4}-?\\d{4}");
    private static final Pattern BIRTH_PATTERN = Pattern.compile("\\b(?:19|20)?\\d{2}[-./]?(?:0[1-9]|1[0-2])[-./]?(?:0[1-9]|[12]\\d|3[01])\\b");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("([가-힣]+(시|도)\\s+[가-힣]+(구|군|시)\\s+[가-힣]+(동|읍|면|리)\\s*\\d*-?\\d*)");
    private static final Pattern NAME_PATTERN = Pattern.compile("([가-힣]{2,4})(?=\\s*(님|과장|대리|사원|의|가|은|는|이|와|과))");

    @Override
    public Integer getStepOrder() {
        return 10;
    }

    @Override
    public StepResult<AnonymizedText> execute(RawChatLog input) {
        Map<String, String> dictionary = new HashMap<>();
        String filteredText = pseudonymize(input.rawText(), dictionary);
        AnonymizedText next = new AnonymizedText(input.requestKey(), filteredText);
        List<StepData> artifacts = List.of(new AnonymizationDictionary(dictionary));
        return StepResult.of(next, artifacts);
    }

    private String pseudonymize(String originalText, Map<String, String> dictionary) {
        String tempText = originalText;
        tempText = applyPattern(tempText, PHONE_PATTERN, "PHONE", dictionary);
        tempText = applyPattern(tempText, BIRTH_PATTERN, "BIRTH", dictionary);
        tempText = applyPattern(tempText, LOCATION_PATTERN, "LOC", dictionary);
        tempText = applyPattern(tempText, NAME_PATTERN, "NAME", dictionary);
        return tempText;
    }

    private String applyPattern(String text, Pattern pattern, String prefix, Map<String, String> dictionary) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        int count = 1;

        while (matcher.find()) {
            String realData;
            if (matcher.group(1) != null && pattern == NAME_PATTERN) {
                realData = matcher.group(1);
            } else {
                realData = matcher.group();
            }

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

    private String findPseudonymByValue(Map<String, String> dict, String value) {
        for (Map.Entry<String, String> entry : dict.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
