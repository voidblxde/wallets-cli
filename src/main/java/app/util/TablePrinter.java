package app.util;

import java.util.ArrayList;
import java.util.List;

public final class TablePrinter {
    private TablePrinter() {}

    public static List<String> format(List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) return List.of();

        int cols = rows.get(0).size();
        int[] w = new int[cols];

        for (List<String> r : rows) {
            for (int i = 0; i < cols; i++) {
                String s = safe(r, i);
                w[i] = Math.max(w[i], s.length());
            }
        }

        List<String> out = new ArrayList<>();
        for (int ri = 0; ri < rows.size(); ri++) {
            List<String> r = rows.get(ri);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cols; i++) {
                String s = safe(r, i);
                sb.append(padRight(s, w[i]));
                if (i != cols - 1) sb.append("  ");
            }
            out.add(sb.toString());

            // разделитель после заголовка
            if (ri == 0) {
                StringBuilder sep = new StringBuilder();
                for (int i = 0; i < cols; i++) {
                    sep.append("-".repeat(w[i]));
                    if (i != cols - 1) sep.append("  ");
                }
                out.add(sep.toString());
            }
        }

        return out;
    }

    private static String safe(List<String> r, int idx) {
        if (r == null || idx >= r.size()) return "";
        String s = r.get(idx);
        return s == null ? "" : s;
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) return s;
        return s + " ".repeat(n - s.length());
    }
}
