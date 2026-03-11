/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.internal.Classes;
import org.junit.runner.Computer;
import org.junit.runner.FilterFactories;
import org.junit.runner.FilterFactory;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;
import org.junit.runners.model.InitializationError;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
class JUnitCommandLineParseResult {
    private final List<String> filterSpecs = new ArrayList<String>();
    private final List<Class<?>> classes = new ArrayList();
    private final List<Throwable> parserErrors = new ArrayList<Throwable>();

    JUnitCommandLineParseResult() {
    }

    public List<String> getFilterSpecs() {
        return Collections.unmodifiableList(this.filterSpecs);
    }

    public List<Class<?>> getClasses() {
        return Collections.unmodifiableList(this.classes);
    }

    public static JUnitCommandLineParseResult parse(String[] args) {
        JUnitCommandLineParseResult result = new JUnitCommandLineParseResult();
        result.parseArgs(args);
        return result;
    }

    private void parseArgs(String[] args) {
        this.parseParameters(this.parseOptions(args));
    }

    /*
     * Enabled aggressive block sorting
     */
    String[] parseOptions(String ... args) {
        int i = 0;
        while (i != args.length) {
            String arg = args[i];
            if (arg.equals("--")) {
                return this.copyArray(args, i + 1, args.length);
            }
            if (!arg.startsWith("--")) return this.copyArray(args, i, args.length);
            if (arg.startsWith("--filter=") || arg.equals("--filter")) {
                String filterSpec;
                if (arg.equals("--filter")) {
                    if (++i >= args.length) {
                        this.parserErrors.add(new CommandLineParserError(arg + " value not specified"));
                        return new String[0];
                    }
                    filterSpec = args[i];
                } else {
                    filterSpec = arg.substring(arg.indexOf(61) + 1);
                }
                this.filterSpecs.add(filterSpec);
            } else {
                this.parserErrors.add(new CommandLineParserError("JUnit knows nothing about the " + arg + " option"));
            }
            ++i;
        }
        return new String[0];
    }

    private String[] copyArray(String[] args, int from, int to) {
        ArrayList<String> result = new ArrayList<String>();
        for (int j = from; j != to; ++j) {
            result.add(args[j]);
        }
        return result.toArray(new String[result.size()]);
    }

    void parseParameters(String[] args) {
        for (String arg : args) {
            try {
                this.classes.add(Classes.getClass(arg));
            }
            catch (ClassNotFoundException e) {
                this.parserErrors.add(new IllegalArgumentException("Could not find class [" + arg + "]", e));
            }
        }
    }

    private Request errorReport(Throwable cause) {
        return Request.errorReport(JUnitCommandLineParseResult.class, cause);
    }

    public Request createRequest(Computer computer) {
        if (this.parserErrors.isEmpty()) {
            Request request = Request.classes(computer, this.classes.toArray(new Class[this.classes.size()]));
            return this.applyFilterSpecs(request);
        }
        return this.errorReport(new InitializationError(this.parserErrors));
    }

    private Request applyFilterSpecs(Request request) {
        try {
            for (String filterSpec : this.filterSpecs) {
                Filter filter = FilterFactories.createFilterFromFilterSpec(request, filterSpec);
                request = request.filterWith(filter);
            }
            return request;
        }
        catch (FilterFactory.FilterNotCreatedException e) {
            return this.errorReport(e);
        }
    }

    public static class CommandLineParserError
    extends Exception {
        private static final long serialVersionUID = 1L;

        public CommandLineParserError(String message) {
            super(message);
        }
    }
}

