/*
 * Decompiled with CFR 0.152.
 */
package junit.runner;

public interface TestRunListener {
    public static final int STATUS_ERROR = 1;
    public static final int STATUS_FAILURE = 2;

    public void testRunStarted(String var1, int var2);

    public void testRunEnded(long var1);

    public void testRunStopped(long var1);

    public void testStarted(String var1);

    public void testEnded(String var1);

    public void testFailed(int var1, String var2, String var3);
}

