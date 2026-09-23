package com.powerpuff.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/** Evidence only: never resolves disagreement or changes supplier data. */
public final class CharacteristicDiagnostics {
    private CharacteristicDiagnostics() {}
    private static final Pattern CURRENT = Pattern.compile(
            "(?iu)(?<![\\p{L}\\d.,])([0-9]+(?:[.,][0-9]+)?)[\\s\\p{Zs}]*(к[аa]|k[аa]|м[аa]|m[аa]|[аa]|ампер(?:а|ов)?)(?!\\p{L})");
    public record Evidence(String source, String rawValue, BigDecimal normalizedValue, String unit) {}
    public record Conflict(String code, String parameter, String message, List<Evidence> evidence) {}
    public static List<Conflict> conflicts(String name, String description, JsonNode properties) {
        var values = new ArrayList<Evidence>();
        collect(values, "name", name, false);
        collect(values, "description", description, false);
        if (properties != null && properties.path("NOMINALNYY_TOK").isValueNode())
            collect(values, "properties.NOMINALNYY_TOK", properties.path("NOMINALNYY_TOK").asText(), true);
        long distinct = values.stream().map(v -> v.normalizedValue().stripTrailingZeros()).distinct().count();
        if (distinct < 2) return List.of();
        String details = values.stream().map(v -> v.source() + ": «" + v.rawValue() + "»").distinct()
                .reduce((a,b) -> a + "; " + b).orElse("");
        return List.of(new Conflict("NOMINAL_CURRENT_CONFLICT", "NOMINALNYY_TOK",
                "Расхождение значений тока: " + details + ". Правильное значение не определено. Уточните у менеджера; совместимость по этому параметру не подтверждена.", List.copyOf(values)));
    }
    private static void collect(List<Evidence> values, String source, String text, boolean nominalProperty) {
        if (text == null) return;
        var matcher = CURRENT.matcher(text);
        while (matcher.find()) {
            String prefix=text.substring(Math.max(0,matcher.start()-40),matcher.start());
            if (source.equals("description") && Pattern.compile("(?iuU)(?:\\bдо|\\bот|[-–—…])[\\s\\p{Zs}]*$").matcher(prefix).find()) continue;
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);
            // kA in prose usually describes breaking capacity, mA leakage; do not mix with nominal current.
            if (!nominalProperty && (unit.startsWith("к") || unit.startsWith("k") || unit.startsWith("м") || unit.startsWith("m"))) continue;
            var number = new BigDecimal(matcher.group(1).replace(',', '.'));
            if (unit.startsWith("к") || unit.startsWith("k")) number = number.multiply(new BigDecimal("1000"));
            if (unit.startsWith("м") || unit.startsWith("m")) number = number.movePointLeft(3);
            values.add(new Evidence(source, matcher.group(), number, "A"));
        }
        if (nominalProperty && text.strip().matches("[0-9]+(?:[.,][0-9]+)?"))
            values.add(new Evidence(source, text, new BigDecimal(text.strip().replace(',', '.')), "A"));
    }
}
