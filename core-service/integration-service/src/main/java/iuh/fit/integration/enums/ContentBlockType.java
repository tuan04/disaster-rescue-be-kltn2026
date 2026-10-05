package iuh.fit.integration.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContentBlockType {
    PARAGRAPH,
    HEADING,
    IMAGE;

    @JsonCreator
    public static ContentBlockType from(String value) {
        if (value == null) return null;
        for (ContentBlockType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return null;
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
