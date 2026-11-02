package org.cartscheduler.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps the stable REST/database values 0, 1 and 2 to {@link BreakMode}. */
@Converter
public class BreakModeConverter implements AttributeConverter<BreakMode, Short> {
    @Override
    public Short convertToDatabaseColumn(BreakMode attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public BreakMode convertToEntityAttribute(Short databaseValue) {
        return databaseValue == null ? null : BreakMode.fromCode(databaseValue);
    }
}
