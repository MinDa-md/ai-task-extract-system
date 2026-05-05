package com.mindamd.taskextractor.domain.dto;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;


public class PrivacyMaskingSerializer extends ValueSerializer<String> {

    private MaskingType maskingType;

    public PrivacyMaskingSerializer() {}

    public PrivacyMaskingSerializer(MaskingType maskingType) {
        this.maskingType = maskingType;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {
        if (value == null || value.isBlank()) {
            gen.writeNull();
            return;
        }

        String maskedValue = value;

        switch (maskingType) {
            case PHONE:
                maskedValue = value.replaceAll("(\\d{3})-?(\\d{4})-?(\\d{4})", "$1-****-****");
                break;
            case NAME:
                if (value.length() == 2) {
                    maskedValue = value.replaceAll("(?<=.{1}).", "*");
                } else if (value.length() > 2) {
                    maskedValue = value.replaceAll("(?<=.{1}).(?=.{1})", "*");
                }
                break;
            case BIRTH:
                // 1990-01-01 -> 1990-**-**
                maskedValue = value.replaceAll("(\\d{4})[-./]?(\\d{2})[-./]?(\\d{2})", "$1-**-**");
                break;
            case LOCATION:
                // 첫 두 단어(시, 구)만 냅두고 뒤는 마스킹 (예: 서울시 강남구 ***)
                maskedValue = value.replaceAll("([가-힣]+[시도]\\s+[가-힣]+[구군시]).*", "$1 ***");
                break;
        }

        gen.writeString(maskedValue);
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) throws JacksonException {
        if (property != null) {
            PrivacyMasking annotation = property.getAnnotation(PrivacyMasking.class);
            if (annotation != null) {
                return new PrivacyMaskingSerializer(annotation.type());
            }
        }
        return this;
    }
}