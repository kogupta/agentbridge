package com.github.catatafishen.agentbridge.nativeagent;

import java.util.Objects;

public record Model(Id id, ReasoningEffort effort) {
    public Model {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(effort, "effort");
    }

    public record Id(String provider, String name) {
        public Id {
            requirePart(provider, "provider");
            requirePart(name, "name");
            if (provider.indexOf('/') >= 0 || name.indexOf('/') >= 0) {
                throw new IllegalArgumentException("Model provider and name must not contain '/'");
            }
        }

        public static Id parse(String value) {
            Objects.requireNonNull(value, "value");
            int separator = value.indexOf('/');
            if (separator <= 0 || separator == value.length() - 1 || separator != value.lastIndexOf('/')) {
                throw new IllegalArgumentException("Model ID must have provider/name format");
            }
            return new Id(value.substring(0, separator), value.substring(separator + 1));
        }

        public String value() { return provider + "/" + name; }

        private static void requirePart(String value, String label) {
            Objects.requireNonNull(value, label);
            if (value.isBlank()) throw new IllegalArgumentException("Model " + label + " must not be blank");
        }
    }

    public enum ReasoningEffort { LOW, MEDIUM, HIGH }
}