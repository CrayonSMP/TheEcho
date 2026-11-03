package de.tamashiimon.theecho.models;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ItemSpec(@NotNull String id, int amount) {

    private static final Pattern PATTERN =
            Pattern.compile("^([a-z0-9_.\\-/:]+)(?:\\[amount=(\\d+)])?$");

    public static @NotNull ItemSpec parse(@NotNull String raw) {
        Matcher matcher = PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid item format: '" + raw + "'");
        }

        int amount = matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2));
        if (amount < 1) {
            throw new IllegalArgumentException("Amount must be at least 1: '" + raw + "'");
        }
        return new ItemSpec(matcher.group(1), amount);
    }
}