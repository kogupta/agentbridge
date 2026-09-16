package com.github.catatafishen.agentbridge.nativeagent.model;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JDK-only strict reader for Quint's JSON ITF traces. */
public final class ItfTrace {
    private static final Set<String> ACTIONS = Set.of(
        "send", "beginRequest", "limitStop", "providerAttempt", "deliverProvisional",
        "acceptTurn", "nextCall", "admit", "effectFinish", "recordResult", "stop", "close",
        "finish", "newGeneration", "init", "step", "stepCorpus");
    private static final Set<String> READ_ACTIONS = Set.of(
        "startRead", "refuseRead", "smartWaitTick", "dumbEnter", "dumbExit",
        "editDocument", "commitDocuments", "writeAction", "compute", "abortComputation",
        "register", "disposeRegistry", "cancel", "settle");

    private final Map<String, Object> metadata;
    private final List<String> variables;
    private final List<State> states;

    private ItfTrace(Map<String, Object> metadata, List<String> variables, List<State> states) {
        this.metadata = Map.copyOf(metadata);
        this.variables = List.copyOf(variables);
        this.states = List.copyOf(states);
    }

    public static ItfTrace read(Path path) throws IOException {
        return readText(Files.readString(path, StandardCharsets.UTF_8));
    }

    public static ItfTrace read(java.io.InputStream stream) throws IOException {
        return readText(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }

    private static ItfTrace readText(String text) {
        Object root = new Parser(text).parse();
        Map<String, Object> object = object(root, "top level");
        requireKeys(object, Set.of("#meta", "vars", "states"), "top level");
        Map<String, Object> metadata = object(object.get("#meta"), "#meta");
        validateValue(metadata, "#meta");
        if (metadata.containsKey("read_adapter_only")
            && !(metadata.get("read_adapter_only") instanceof Boolean)) {
            throw error("#meta.read_adapter_only must be boolean");
        }
        List<Object> rawVariables = array(object.get("vars"), "vars");
        LinkedHashMap<String, Boolean> seen = new LinkedHashMap<>();
        for (Object value : rawVariables) {
            String name = string(value, "vars entry");
            seen.put(name, Boolean.TRUE);
        }
        List<String> variables = new ArrayList<>(seen.keySet());
        List<Object> rawStates = array(object.get("states"), "states");
        if (rawStates.isEmpty()) throw error("states must not be empty");
        List<State> states = new ArrayList<>(rawStates.size());
        for (int i = 0; i < rawStates.size(); i++) {
            Map<String, Object> raw = object(rawStates.get(i), "state " + i);
            Set<String> expected = new HashSet<>(variables);
            expected.add("#meta");
            requireKeys(raw, expected, "state " + i);
            Map<String, Object> stateMeta = object(raw.get("#meta"), "state " + i + " #meta");
            validateValue(stateMeta, "state " + i + " #meta");
            BigInteger index = integer(stateMeta.get("index"), "state " + i + " index");
            if (!index.equals(BigInteger.valueOf(i))) throw error("state index " + index + " is not " + i);
            String action = string(raw.get("mbt::actionTaken"), "state " + i + " action");
            boolean replayable;
            if (i == 0) {
                if (!action.equals("stepCorpus")) throw error("state 0 action must be stepCorpus");
                replayable = true;
            } else if (ACTIONS.contains(action) && !action.equals("stepCorpus")) {
                replayable = true;
            } else if (READ_ACTIONS.contains(action)
                && Boolean.TRUE.equals(metadata.get("read_adapter_only"))) {
                replayable = false;
            } else {
                throw error("unknown or unapproved replay action " + action + " at state " + i);
            }
            Object nondeterministicPicks = raw.get("mbt::nondetPicks");
            object(nondeterministicPicks, "state " + i + " nondeterministic picks");
            validateValue(nondeterministicPicks, "state " + i + " nondeterministic picks");
            Map<String, Object> values = new LinkedHashMap<>();
            for (String variable : variables) {
                Object value = raw.get(variable);
                validateValue(value, "state " + i + " variable " + variable);
                values.put(variable, value);
            }
            states.add(new State(i, action, replayable, Map.copyOf(values)));
        }
        return new ItfTrace(metadata, variables, states);
    }

    public Map<String, Object> metadata() { return metadata; }
    public List<String> variables() { return variables; }
    public List<State> states() { return states; }

    public record State(int index, String action, boolean replayable, Map<String, Object> values) { }

    private static void validateValue(Object value, String context) {
        if (value instanceof List<?> list) {
            for (Object item : list) validateValue(item, context);
            return;
        }
        if (!(value instanceof Map<?, ?> map)) return;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) throw error(context + " has a non-string key");
            if (key.startsWith("#")) {
                if (map.size() != 1) throw error(context + " has a mixed wrapper object");
                Object wrapped = entry.getValue();
                switch (key) {
                    case "#bigint" -> {
                        String digits = string(wrapped, context + " #bigint");
                        if (!digits.matches("-?(0|[1-9][0-9]*)")) {
                            throw error(context + " #bigint must contain decimal digits");
                        }
                    }
                    case "#tup", "#set" -> {
                        List<Object> items = array(wrapped, context + " " + key);
                        for (Object item : items) validateValue(item, context + " " + key);
                    }
                    case "#map" -> {
                        List<Object> entries = array(wrapped, context + " #map");
                        for (Object item : entries) {
                            List<Object> pair = array(item, context + " #map entry");
                            if (pair.size() != 2) throw error(context + " #map entry must have two values");
                            validateValue(pair.get(0), context + " #map key");
                            validateValue(pair.get(1), context + " #map value");
                        }
                    }
                    default -> throw error(context + " has unknown wrapper " + key);
                }
            } else {
                validateValue(entry.getValue(), context + "." + key);
            }
        }
    }

    private static Map<String, Object> object(Object value, String context) {
        if (!(value instanceof Map<?, ?> map)) throw error(context + " must be an object");
        @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) map;
        return result;
    }

    private static List<Object> array(Object value, String context) {
        if (!(value instanceof List<?> list)) throw error(context + " must be an array");
        @SuppressWarnings("unchecked") List<Object> result = (List<Object>) list;
        return result;
    }

    private static String string(Object value, String context) {
        if (!(value instanceof String result)) throw error(context + " must be a string");
        return result;
    }

    private static BigInteger integer(Object value, String context) {
        if (!(value instanceof BigInteger result)) throw error(context + " must be an integer");
        return result;
    }

    private static void requireKeys(Map<String, Object> object, Set<String> expected, String context) {
        if (!object.keySet().equals(expected)) {
            throw error(context + " keys must be " + expected + ", got " + object.keySet());
        }
    }

    private static IllegalArgumentException error(String message) {
        return new IllegalArgumentException("Invalid ITF trace: " + message);
    }

    private static final class Parser {
        private final String text;
        private int position;

        private Parser(String text) { this.text = text; }

        private Object parse() {
            Object value = value();
            whitespace();
            if (position != text.length()) throw error("trailing input at " + position);
            return value;
        }

        private Object value() {
            whitespace();
            if (position >= text.length()) throw error("unexpected end of input");
            return switch (text.charAt(position)) {
                case '{' -> objectValue();
                case '[' -> arrayValue();
                case '"' -> stringValue();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> throw error("null is not an ITF value");
                default -> numberValue();
            };
        }

        private Object literal(String literal, Object result) {
            if (!text.startsWith(literal, position)) throw error("invalid literal at " + position);
            position += literal.length();
            return result;
        }

        private Map<String, Object> objectValue() {
            position++;
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            whitespace();
            if (take('}')) return result;
            while (true) {
                whitespace();
                String key = stringValue();
                if (!result.isEmpty() && result.containsKey(key)) throw error("duplicate key " + key);
                whitespace();
                require(':');
                Object value = value();
                result.put(key, value);
                whitespace();
                if (take('}')) return result;
                require(',');
            }
        }

        private List<Object> arrayValue() {
            position++;
            ArrayList<Object> result = new ArrayList<>();
            whitespace();
            if (take(']')) return result;
            while (true) {
                result.add(value());
                whitespace();
                if (take(']')) return result;
                require(',');
            }
        }

        private String stringValue() {
            require('"');
            StringBuilder result = new StringBuilder();
            while (position < text.length()) {
                char c = text.charAt(position++);
                if (c == '"') return result.toString();
                if (c < 0x20) throw error("control character in string");
                if (c != '\\') { result.append(c); continue; }
                if (position >= text.length()) throw error("unfinished escape");
                switch (text.charAt(position++)) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(unicodeEscape());
                    default -> throw error("invalid escape");
                }
            }
            throw error("unterminated string");
        }

        private char unicodeEscape() {
            if (position + 4 > text.length()) throw error("short unicode escape");
            int code = 0;
            for (int i = 0; i < 4; i++) {
                int digit = hexDigit(text.charAt(position++));
                if (digit < 0) throw error("invalid unicode escape");
                code = code * 16 + digit;
            }
            return (char) code;
        }

        private BigInteger numberValue() {
            int start = position;
            if (take('-')) { }
            if (position >= text.length() || !asciiDigit(text.charAt(position))) {
                throw error("expected integer at " + start);
            }
            if (text.charAt(position) == '0') {
                position++;
                if (position < text.length() && asciiDigit(text.charAt(position))) {
                    throw error("leading zero at " + start);
                }
            } else {
                while (position < text.length() && asciiDigit(text.charAt(position))) position++;
            }
            if (position < text.length() && ".eE".indexOf(text.charAt(position)) >= 0) {
                throw error("non-integer number at " + start);
            }
            return new BigInteger(text.substring(start, position));
        }

        private void whitespace() {
            while (position < text.length()) {
                char c = text.charAt(position);
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') break;
                position++;
            }
        }
        private static boolean asciiDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private static int hexDigit(char c) {
            if (c >= '0' && c <= '9') return c - '0';
            if (c >= 'a' && c <= 'f') return c - 'a' + 10;
            if (c >= 'A' && c <= 'F') return c - 'A' + 10;
            return -1;
        }

        private void require(char expected) {
            if (!take(expected)) throw error("expected '" + expected + "' at " + position);
        }

        private boolean take(char expected) {
            if (position < text.length() && text.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }
    }
}
