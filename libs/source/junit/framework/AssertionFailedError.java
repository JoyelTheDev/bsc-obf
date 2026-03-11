/*
 * Decompiled with CFR 0.152.
 */
package junit.framework;

public class AssertionFailedError
extends AssertionError {
    private static final long serialVersionUID = 1L;

    public AssertionFailedError() {
    }

    public AssertionFailedError(String message) {
        super((Object)AssertionFailedError.defaultString(message));
    }

    private static String defaultString(String message) {
        return message == null ? "" : message;
    }
}

