package com.mindamd.taskextractor.pipeline.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.mindamd.taskextractor.step.ai.AiSummary;
import com.mindamd.taskextractor.step.db.SavedSummary;
import com.mindamd.taskextractor.step.delivery.DeliveryResult;
import com.mindamd.taskextractor.step.filter.AnonymizationDictionary;
import com.mindamd.taskextractor.step.filter.AnonymizedText;
import com.mindamd.taskextractor.step.ingest.RawChatLog;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RawChatLog.class, name = "raw_chat_log"),
        @JsonSubTypes.Type(value = AnonymizedText.class, name = "anonymized_text"),
        @JsonSubTypes.Type(value = AnonymizationDictionary.class, name = "anonymization_dictionary"),
        @JsonSubTypes.Type(value = AiSummary.class, name = "ai_summary"),
        @JsonSubTypes.Type(value = SavedSummary.class, name = "saved_summary"),
        @JsonSubTypes.Type(value = DeliveryResult.class, name = "delivery_result")
})
public interface StepData {}
