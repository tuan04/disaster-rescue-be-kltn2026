package iuh.fit.resourcemanagementservice.utils;

import java.text.Normalizer;
import java.util.Locale;

public final class ItemCode {
    private ItemCode() {}

    public static String generate(String name, long sequence) {
        if (sequence < 1) throw new IllegalArgumentException("Sequence must be positive");
        String normalized = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D')
                .toUpperCase(Locale.ROOT);
        StringBuilder initials = new StringBuilder();
        for (String word : normalized.split("[^A-Z0-9]+")) {
            if (!word.isEmpty() && initials.length() < 8) initials.append(word.charAt(0));
        }
        if (initials.isEmpty()) initials.append("VP");
        return "ITEM-" + initials + "-" + String.format(Locale.ROOT, "%03d", sequence);
    }
}
