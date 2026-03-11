/*
 * Decompiled with CFR 0.152.
 */
package junit.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.util.Properties;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestSuite;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public abstract class BaseTestRunner
implements TestListener {
    public static final String SUITE_METHODNAME = "suite";
    private static Properties fPreferences;
    static int fgMaxMessageLength;
    static boolean fgFilterStack;
    boolean fLoading = true;

    @Override
    public synchronized void startTest(Test test) {
        this.testStarted(test.toString());
    }

    protected static void setPreferences(Properties preferences) {
        fPreferences = preferences;
    }

    protected static Properties getPreferences() {
        if (fPreferences == null) {
            fPreferences = new Properties();
            fPreferences.put("loading", "true");
            fPreferences.put("filterstack", "true");
            BaseTestRunner.readPreferences();
        }
        return fPreferences;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void savePreferences() throws IOException {
        FileOutputStream fos = new FileOutputStream(BaseTestRunner.getPreferencesFile());
        try {
            BaseTestRunner.getPreferences().store(fos, "");
        }
        finally {
            fos.close();
        }
    }

    public static void setPreference(String key, String value) {
        BaseTestRunner.getPreferences().put(key, value);
    }

    @Override
    public synchronized void endTest(Test test) {
        this.testEnded(test.toString());
    }

    @Override
    public synchronized void addError(Test test, Throwable e) {
        this.testFailed(1, test, e);
    }

    @Override
    public synchronized void addFailure(Test test, AssertionFailedError e) {
        this.testFailed(2, test, (Throwable)((Object)e));
    }

    public abstract void testStarted(String var1);

    public abstract void testEnded(String var1);

    public abstract void testFailed(int var1, Test var2, Throwable var3);

    public Test getTest(String suiteClassName) {
        if (suiteClassName.length() <= 0) {
            this.clearStatus();
            return null;
        }
        Class<?> testClass = null;
        try {
            testClass = this.loadSuiteClass(suiteClassName);
        }
        catch (ClassNotFoundException e) {
            String clazz = e.getMessage();
            if (clazz == null) {
                clazz = suiteClassName;
            }
            this.runFailed("Class not found \"" + clazz + "\"");
            return null;
        }
        catch (Exception e) {
            this.runFailed("Error: " + e.toString());
            return null;
        }
        Method suiteMethod = null;
        try {
            suiteMethod = testClass.getMethod(SUITE_METHODNAME, new Class[0]);
        }
        catch (Exception e) {
            this.clearStatus();
            return new TestSuite(testClass);
        }
        if (!Modifier.isStatic(suiteMethod.getModifiers())) {
            this.runFailed("Suite() method must be static");
            return null;
        }
        Test test = null;
        try {
            test = (Test)suiteMethod.invoke(null, new Object[0]);
            if (test == null) {
                return test;
            }
        }
        catch (InvocationTargetException e) {
            this.runFailed("Failed to invoke suite():" + e.getTargetException().toString());
            return null;
        }
        catch (IllegalAccessException e) {
            this.runFailed("Failed to invoke suite():" + e.toString());
            return null;
        }
        this.clearStatus();
        return test;
    }

    public String elapsedTimeAsString(long runTime) {
        return NumberFormat.getInstance().format((double)runTime / 1000.0);
    }

    protected String processArguments(String[] args) {
        String suiteName = null;
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-noloading")) {
                this.setLoading(false);
                continue;
            }
            if (args[i].equals("-nofilterstack")) {
                fgFilterStack = false;
                continue;
            }
            if (args[i].equals("-c")) {
                if (args.length > i + 1) {
                    suiteName = this.extractClassName(args[i + 1]);
                } else {
                    System.out.println("Missing Test class name");
                }
                ++i;
                continue;
            }
            suiteName = args[i];
        }
        return suiteName;
    }

    public void setLoading(boolean enable) {
        this.fLoading = enable;
    }

    public String extractClassName(String className) {
        if (className.startsWith("Default package for")) {
            return className.substring(className.lastIndexOf(".") + 1);
        }
        return className;
    }

    public static String truncate(String s) {
        if (fgMaxMessageLength != -1 && s.length() > fgMaxMessageLength) {
            s = s.substring(0, fgMaxMessageLength) + "...";
        }
        return s;
    }

    protected abstract void runFailed(String var1);

    protected Class<?> loadSuiteClass(String suiteClassName) throws ClassNotFoundException {
        return Class.forName(suiteClassName);
    }

    protected void clearStatus() {
    }

    protected boolean useReloadingTestSuiteLoader() {
        return BaseTestRunner.getPreference("loading").equals("true") && this.fLoading;
    }

    private static File getPreferencesFile() {
        String home = System.getProperty("user.home");
        return new File(home, "junit.properties");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void readPreferences() {
        FileInputStream is = null;
        try {
            is = new FileInputStream(BaseTestRunner.getPreferencesFile());
            BaseTestRunner.setPreferences(new Properties(BaseTestRunner.getPreferences()));
            BaseTestRunner.getPreferences().load(is);
        }
        catch (IOException ignored) {
        }
        finally {
            try {
                if (is != null) {
                    ((InputStream)is).close();
                }
            }
            catch (IOException e1) {}
        }
    }

    public static String getPreference(String key) {
        return BaseTestRunner.getPreferences().getProperty(key);
    }

    public static int getPreference(String key, int dflt) {
        String value = BaseTestRunner.getPreference(key);
        int intValue = dflt;
        if (value == null) {
            return intValue;
        }
        try {
            intValue = Integer.parseInt(value);
        }
        catch (NumberFormatException ne) {
            // empty catch block
        }
        return intValue;
    }

    public static String getFilteredTrace(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        e.printStackTrace(writer);
        String trace = stringWriter.toString();
        return BaseTestRunner.getFilteredTrace(trace);
    }

    public static String getFilteredTrace(String stack) {
        if (BaseTestRunner.showStackRaw()) {
            return stack;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StringReader sr = new StringReader(stack);
        BufferedReader br = new BufferedReader(sr);
        try {
            String line;
            while ((line = br.readLine()) != null) {
                if (BaseTestRunner.filterLine(line)) continue;
                pw.println(line);
            }
        }
        catch (Exception IOException2) {
            return stack;
        }
        return sw.toString();
    }

    protected static boolean showStackRaw() {
        return !BaseTestRunner.getPreference("filterstack").equals("true") || !fgFilterStack;
    }

    static boolean filterLine(String line) {
        String[] patterns = new String[]{"junit.framework.TestCase", "junit.framework.TestResult", "junit.framework.TestSuite", "junit.framework.Assert.", "junit.swingui.TestRunner", "junit.awtui.TestRunner", "junit.textui.TestRunner", "java.lang.reflect.Method.invoke("};
        for (int i = 0; i < patterns.length; ++i) {
            if (line.indexOf(patterns[i]) <= 0) continue;
            return true;
        }
        return false;
    }

    static {
        fgMaxMessageLength = 500;
        fgFilterStack = true;
        fgMaxMessageLength = BaseTestRunner.getPreference("maxmessage", fgMaxMessageLength);
    }
}

