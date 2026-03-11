/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.theories;

public abstract class PotentialAssignment {
    public static PotentialAssignment forValue(final String name, final Object value) {
        return new PotentialAssignment(){

            public Object getValue() {
                return value;
            }

            public String toString() {
                return String.format("[%s]", value);
            }

            public String getDescription() {
                String valueString;
                if (value == null) {
                    valueString = "null";
                } else {
                    try {
                        valueString = String.format("\"%s\"", value);
                    }
                    catch (Throwable e) {
                        valueString = String.format("[toString() threw %s: %s]", e.getClass().getSimpleName(), e.getMessage());
                    }
                }
                return String.format("%s <from %s>", valueString, name);
            }
        };
    }

    public abstract Object getValue() throws CouldNotGenerateValueException;

    public abstract String getDescription() throws CouldNotGenerateValueException;

    public static class CouldNotGenerateValueException
    extends Exception {
        private static final long serialVersionUID = 1L;

        public CouldNotGenerateValueException() {
        }

        public CouldNotGenerateValueException(Throwable e) {
            super(e);
        }
    }
}

