package com.jetbrains.qodana.sarif.model.streaming;

public abstract class ResultLocation {
    public static class InRun extends ResultLocation {
        private final int runIndex;

        public InRun(int runIndex) {
            this.runIndex = runIndex;
        }

        public int getRunIndex() {
            return runIndex;
        }
    }

    public static class InProperties extends ResultLocation {
        private final int runIndex;
        private final String propertyName;

        public InProperties(int runIndex, String propertyName) {
            this.runIndex = runIndex;
            this.propertyName = propertyName;
        }

        public int getRunIndex() {
            return runIndex;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }
}
