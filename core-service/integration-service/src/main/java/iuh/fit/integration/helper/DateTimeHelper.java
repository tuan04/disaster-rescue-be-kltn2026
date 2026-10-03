package iuh.fit.integration.helper;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
public final class DateTimeHelper {

    private DateTimeHelper() {
    }

    private static final ZoneId VIETNAM_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    public static Instant parseToInstant(String rawDate) {

        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        String cleaned = rawDate
                .replace("'", "")
                .replaceAll("\\s+", " ")
                .trim();

        // ISO Instant: 2026-10-02T08:49:00Z
        try {
            return Instant.parse(cleaned);
        } catch (DateTimeParseException ignored) {
        }

        // ISO date-time with offset: 2026-10-02T15:49:00+07:00
        try {
            return OffsetDateTime.parse(cleaned).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        // Local Vietnamese date/time formats
        for (DateTimeFormatter formatter : FORMATTERS) {

            try {
                LocalDateTime dateTime =
                        LocalDateTime.parse(cleaned, formatter);

                return dateTime
                        .atZone(VIETNAM_ZONE)
                        .toInstant();

            } catch (DateTimeParseException ignored) {
            }

            try {
                LocalDate date =
                        LocalDate.parse(cleaned, formatter);

                return date
                        .atStartOfDay(VIETNAM_ZONE)
                        .toInstant();

            } catch (DateTimeParseException ignored) {
            }
        }

        log.warn("Không thể parse ngày giờ [{}] về Instant", rawDate);
        return null;
    }
}
