package iuh.fit.resourcemanagementservice.enums;

public enum SexEnum {
    MALE, FEMALE, OTHER;

    public static SexEnum from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return SexEnum.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
