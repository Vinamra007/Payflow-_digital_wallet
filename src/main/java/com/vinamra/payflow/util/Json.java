package com.vinamra.payflow.util;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A deliberately small JSON helper (no external libraries).
 * Parsing supports a single flat object of string/number/boolean/null values,
 * which is all this API needs. Writing supports maps, lists, strings, numbers and booleans.
 */
public final class Json {
    private Json() {
    }

    // ---------- writing ----------
    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            quote(sb, s);
        } else if (v instanceof BigDecimal d) {
            sb.append(d.toPlainString());
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                quote(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof List<?> l) {
            sb.append('[');
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(',');
                write(sb, l.get(i));
            }
            sb.append(']');
        } else {
            quote(sb, v.toString());
        }
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ---------- parsing ----------
    public static Map<String, String> parseObject(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("request body is empty");
        }
        Reader r = new Reader(text);
        Map<String, String> result = new LinkedHashMap<>();
        r.skipWs();
        r.expect('{');
        r.skipWs();
        if (r.peek() == '}') {
            r.pos++;
            r.expectEnd();
            return result;
        }
        while (true) {
            r.skipWs();
            String key = r.readString();
            r.skipWs();
            r.expect(':');
            r.skipWs();
            result.put(key, r.readValue());
            r.skipWs();
            char c = r.next();
            if (c == ',') continue;
            if (c == '}') break;
            throw new IllegalArgumentException("malformed JSON: expected ',' or '}'");
        }
        r.expectEnd();
        return result;
    }

    private static final class Reader {
        private final String s;
        private int pos = 0;

        Reader(String s) {
            this.s = s;
        }

        char peek() {
            if (pos >= s.length()) throw new IllegalArgumentException("malformed JSON: unexpected end");
            return s.charAt(pos);
        }

        char next() {
            char c = peek();
            pos++;
            return c;
        }

        void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        void expect(char c) {
            if (next() != c) throw new IllegalArgumentException("malformed JSON: expected '" + c + "'");
        }

        void expectEnd() {
            skipWs();
            if (pos != s.length()) throw new IllegalArgumentException("malformed JSON: trailing characters");
        }

        String readString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = next();
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > s.length()) throw new IllegalArgumentException("malformed JSON: bad escape");
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> throw new IllegalArgumentException("malformed JSON: bad escape");
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        String readValue() {
            char c = peek();
            if (c == '"') return readString();
            if (c == '{' || c == '[') {
                throw new IllegalArgumentException("nested JSON values are not supported");
            }
            int start = pos;
            while (pos < s.length()) {
                char d = s.charAt(pos);
                if (d == ',' || d == '}' || Character.isWhitespace(d)) break;
                pos++;
            }
            String token = s.substring(start, pos);
            if (token.isEmpty()) throw new IllegalArgumentException("malformed JSON: missing value");
            return token.equals("null") ? null : token;
        }
    }
}
