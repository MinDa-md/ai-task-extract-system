package com.mindamd.taskextractor.global.util.encrypt;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false) // 엔티티에서 명시한 필드만 변환
public class CryptoConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return Aes256Util.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return Aes256Util.decrypt(dbData);
    }
}