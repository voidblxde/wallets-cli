package app.cli;

import java.util.*;

public final class CommandParser {
    private CommandParser() {}

    public static ParsedCommand parse(String line) {
        List<String> tokens = tokenize(line);
        if (tokens.isEmpty()) return new ParsedCommand("help", List.of(), Map.of());

        String name = tokens.get(0).toLowerCase(Locale.ROOT);
        List<String> args = new ArrayList<>();
        Map<String, String> kv = new LinkedHashMap<>();

        for (int i = 1; i < tokens.size(); i++) {
            String t = tokens.get(i);
            int eq = t.indexOf('=');
            if (eq > 0) {
                String k = t.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                String v = t.substring(eq + 1).trim();
                kv.put(k, v);
            } else {
                args.add(t);
            }
        }

        return new ParsedCommand(name, List.copyOf(args), Map.copyOf(kv));
    }

    /**
     * Поддержка кавычек:
     *   income Food 1000 "big mac"
     *   list category="Коммунальные услуги" from=2026-01-01
     */
    private static List<String> tokenize(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '"' || c == '\'') {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (quoteChar == c) {
                    inQuotes = false;
                } else {
                    cur.append(c);
                }
                continue;
            }

            if (!inQuotes && Character.isWhitespace(c)) {
                flush(out, cur);
                continue;
            }

            cur.append(c);
        }
        flush(out, cur);
        return out;
    }

    private static void flush(List<String> out, StringBuilder cur) {
        if (cur.length() == 0) return;
        out.add(cur.toString());
        cur.setLength(0);
    }
}
