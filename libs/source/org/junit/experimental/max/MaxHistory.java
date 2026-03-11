/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.max;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.junit.experimental.max.CouldNotReadCoreException;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class MaxHistory
implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, Long> fDurations = new HashMap<String, Long>();
    private final Map<String, Long> fFailureTimestamps = new HashMap<String, Long>();
    private final File fHistoryStore;

    public static MaxHistory forFolder(File file) {
        if (file.exists()) {
            try {
                return MaxHistory.readHistory(file);
            }
            catch (CouldNotReadCoreException e) {
                e.printStackTrace();
                file.delete();
            }
        }
        return new MaxHistory(file);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive exception aggregation
     */
    private static MaxHistory readHistory(File storedResults) throws CouldNotReadCoreException {
        try {
            FileInputStream file = new FileInputStream(storedResults);
            try {
                MaxHistory maxHistory;
                ObjectInputStream stream = new ObjectInputStream(file);
                try {
                    maxHistory = (MaxHistory)stream.readObject();
                }
                catch (Throwable throwable) {
                    stream.close();
                    throw throwable;
                }
                stream.close();
                return maxHistory;
            }
            finally {
                file.close();
            }
        }
        catch (Exception e) {
            throw new CouldNotReadCoreException(e);
        }
    }

    private MaxHistory(File storedResults) {
        this.fHistoryStore = storedResults;
    }

    private void save() throws IOException {
        ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(this.fHistoryStore));
        stream.writeObject(this);
        stream.close();
    }

    Long getFailureTimestamp(Description key) {
        return this.fFailureTimestamps.get(key.toString());
    }

    void putTestFailureTimestamp(Description key, long end) {
        this.fFailureTimestamps.put(key.toString(), end);
    }

    boolean isNewTest(Description key) {
        return !this.fDurations.containsKey(key.toString());
    }

    Long getTestDuration(Description key) {
        return this.fDurations.get(key.toString());
    }

    void putTestDuration(Description description, long duration) {
        this.fDurations.put(description.toString(), duration);
    }

    public RunListener listener() {
        return new RememberingListener();
    }

    public Comparator<Description> testComparator() {
        return new TestComparator();
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private class TestComparator
    implements Comparator<Description> {
        private TestComparator() {
        }

        @Override
        public int compare(Description o1, Description o2) {
            if (MaxHistory.this.isNewTest(o1)) {
                return -1;
            }
            if (MaxHistory.this.isNewTest(o2)) {
                return 1;
            }
            int result = this.getFailure(o2).compareTo(this.getFailure(o1));
            return result != 0 ? result : MaxHistory.this.getTestDuration(o1).compareTo(MaxHistory.this.getTestDuration(o2));
        }

        private Long getFailure(Description key) {
            Long result = MaxHistory.this.getFailureTimestamp(key);
            if (result == null) {
                return 0L;
            }
            return result;
        }
    }

    private final class RememberingListener
    extends RunListener {
        private long overallStart = System.currentTimeMillis();
        private Map<Description, Long> starts = new HashMap<Description, Long>();

        private RememberingListener() {
        }

        public void testStarted(Description description) throws Exception {
            this.starts.put(description, System.nanoTime());
        }

        public void testFinished(Description description) throws Exception {
            long end = System.nanoTime();
            long start = this.starts.get(description);
            MaxHistory.this.putTestDuration(description, end - start);
        }

        public void testFailure(Failure failure) throws Exception {
            MaxHistory.this.putTestFailureTimestamp(failure.getDescription(), this.overallStart);
        }

        public void testRunFinished(Result result) throws Exception {
            MaxHistory.this.save();
        }
    }
}

