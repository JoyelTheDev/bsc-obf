/*
 * Decompiled with CFR 0.152.
 */
package picocli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.BreakIterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class CommandLine {
    public static final String VERSION = "4.6.2";
    private final Tracer tracer = new Tracer();
    private Model.CommandSpec commandSpec;
    private final Interpreter interpreter;
    private final IFactory factory;
    private Object executionResult;
    private PrintWriter out;
    private PrintWriter err;
    private Help.ColorScheme colorScheme = Help.defaultColorScheme(Help.Ansi.AUTO);
    private IExitCodeExceptionMapper exitCodeExceptionMapper;
    private IExecutionStrategy executionStrategy = new RunLast();
    private IParameterExceptionHandler parameterExceptionHandler = new IParameterExceptionHandler(){

        public int handleParseException(ParameterException ex, String[] args) {
            CommandLine cmd = ex.getCommandLine();
            DefaultExceptionHandler.internalHandleParseException(ex, cmd.getErr(), cmd.getColorScheme());
            return CommandLine.mappedExitCode(ex, cmd.getExitCodeExceptionMapper(), cmd.getCommandSpec().exitCodeOnInvalidInput());
        }
    };
    private IExecutionExceptionHandler executionExceptionHandler = new IExecutionExceptionHandler(){

        public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) throws Exception {
            throw ex;
        }
    };

    public CommandLine(Object command) {
        this(command, new DefaultFactory());
    }

    public CommandLine(Object command, IFactory factory) {
        this(command, factory, true);
    }

    private CommandLine(Object command, IFactory factory, boolean userCalled) {
        this.factory = Assert.notNull(factory, "factory");
        this.interpreter = new Interpreter();
        this.commandSpec = Model.CommandSpec.forAnnotatedObject(command, factory);
        this.commandSpec.commandLine(this);
        if (userCalled) {
            this.applyModelTransformations();
        }
        this.commandSpec.validate();
        if (this.commandSpec.unmatchedArgsBindings().size() > 0) {
            this.setUnmatchedArgumentsAllowed(true);
        }
    }

    private void applyModelTransformations() {
        if (this.commandSpec.modelTransformer != null) {
            this.commandSpec = this.commandSpec.modelTransformer.transform(this.commandSpec);
        }
        for (CommandLine cmd : this.getSubcommands().values()) {
            cmd.applyModelTransformations();
        }
    }

    private CommandLine copy() {
        CommandLine result = new CommandLine(this.commandSpec.copy(), this.factory);
        result.err = this.err;
        result.out = this.out;
        result.colorScheme = this.colorScheme;
        result.executionStrategy = this.executionStrategy;
        result.exitCodeExceptionMapper = this.exitCodeExceptionMapper;
        result.executionExceptionHandler = this.executionExceptionHandler;
        result.parameterExceptionHandler = this.parameterExceptionHandler;
        result.interpreter.converterRegistry.clear();
        result.interpreter.converterRegistry.putAll(this.interpreter.converterRegistry);
        return result;
    }

    public Model.CommandSpec getCommandSpec() {
        return this.commandSpec;
    }

    public CommandLine addMixin(String name, Object mixin) {
        this.getCommandSpec().addMixin(name, Model.CommandSpec.forAnnotatedObject(mixin, this.factory));
        return this;
    }

    public Map<String, Object> getMixins() {
        Map<String, Model.CommandSpec> mixins = this.getCommandSpec().mixins();
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        for (String name : mixins.keySet()) {
            result.put(name, mixins.get(name).userObject.getInstance());
        }
        return result;
    }

    public CommandLine addSubcommand(Object command) {
        return this.addSubcommand(null, command, new String[0]);
    }

    public CommandLine addSubcommand(String name, Object command) {
        return this.addSubcommand(name, command, new String[0]);
    }

    public CommandLine addSubcommand(String name, Object command, String ... aliases) {
        CommandLine subcommandLine = CommandLine.toCommandLine(command, this.factory);
        subcommandLine.getCommandSpec().aliases.addAll(Arrays.asList(aliases));
        this.getCommandSpec().addSubcommand(name, subcommandLine);
        return this;
    }

    public Map<String, CommandLine> getSubcommands() {
        return new Model.CaseAwareLinkedMap<String, CommandLine>(this.getCommandSpec().commands);
    }

    public CommandLine getParent() {
        Model.CommandSpec parent = this.getCommandSpec().parent();
        return parent == null ? null : parent.commandLine();
    }

    public <T> T getCommand() {
        return (T)this.getCommandSpec().userObject();
    }

    public IFactory getFactory() {
        return this.factory;
    }

    public boolean isUsageHelpRequested() {
        return this.interpreter.parseResultBuilder != null && this.interpreter.parseResultBuilder.usageHelpRequested;
    }

    public boolean isVersionHelpRequested() {
        return this.interpreter.parseResultBuilder != null && this.interpreter.parseResultBuilder.versionHelpRequested;
    }

    public Help getHelp() {
        return this.getHelpFactory().create(this.getCommandSpec(), this.getColorScheme());
    }

    public IHelpFactory getHelpFactory() {
        return this.getCommandSpec().usageMessage().helpFactory();
    }

    public CommandLine setHelpFactory(IHelpFactory helpFactory) {
        this.getCommandSpec().usageMessage().helpFactory(helpFactory);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setHelpFactory(helpFactory);
        }
        return this;
    }

    public List<String> getHelpSectionKeys() {
        return this.getCommandSpec().usageMessage().sectionKeys();
    }

    public CommandLine setHelpSectionKeys(List<String> keys) {
        this.getCommandSpec().usageMessage().sectionKeys(keys);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setHelpSectionKeys(keys);
        }
        return this;
    }

    public Map<String, IHelpSectionRenderer> getHelpSectionMap() {
        return this.getCommandSpec().usageMessage().sectionMap();
    }

    public CommandLine setHelpSectionMap(Map<String, IHelpSectionRenderer> map) {
        this.getCommandSpec().usageMessage().sectionMap(map);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setHelpSectionMap(map);
        }
        return this;
    }

    public boolean isAdjustLineBreaksForWideCJKCharacters() {
        return this.getCommandSpec().usageMessage().adjustLineBreaksForWideCJKCharacters();
    }

    public CommandLine setAdjustLineBreaksForWideCJKCharacters(boolean adjustForWideChars) {
        this.getCommandSpec().usageMessage().adjustLineBreaksForWideCJKCharacters(adjustForWideChars);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setAdjustLineBreaksForWideCJKCharacters(adjustForWideChars);
        }
        return this;
    }

    public boolean isToggleBooleanFlags() {
        return this.getCommandSpec().parser().toggleBooleanFlags();
    }

    public CommandLine setToggleBooleanFlags(boolean newValue) {
        this.getCommandSpec().parser().toggleBooleanFlags(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setToggleBooleanFlags(newValue);
        }
        return this;
    }

    public boolean isInterpolateVariables() {
        return this.getCommandSpec().interpolateVariables();
    }

    public CommandLine setInterpolateVariables(boolean interpolate) {
        this.getCommandSpec().interpolateVariables(interpolate);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setInterpolateVariables(interpolate);
        }
        return this;
    }

    public boolean isOverwrittenOptionsAllowed() {
        return this.getCommandSpec().parser().overwrittenOptionsAllowed();
    }

    public CommandLine setOverwrittenOptionsAllowed(boolean newValue) {
        this.getCommandSpec().parser().overwrittenOptionsAllowed(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setOverwrittenOptionsAllowed(newValue);
        }
        return this;
    }

    public boolean isPosixClusteredShortOptionsAllowed() {
        return this.getCommandSpec().parser().posixClusteredShortOptionsAllowed();
    }

    public CommandLine setPosixClusteredShortOptionsAllowed(boolean newValue) {
        this.getCommandSpec().parser().posixClusteredShortOptionsAllowed(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setPosixClusteredShortOptionsAllowed(newValue);
        }
        return this;
    }

    public boolean isCaseInsensitiveEnumValuesAllowed() {
        return this.getCommandSpec().parser().caseInsensitiveEnumValuesAllowed();
    }

    public CommandLine setCaseInsensitiveEnumValuesAllowed(boolean newValue) {
        this.getCommandSpec().parser().caseInsensitiveEnumValuesAllowed(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setCaseInsensitiveEnumValuesAllowed(newValue);
        }
        return this;
    }

    public boolean isTrimQuotes() {
        return this.getCommandSpec().parser().trimQuotes();
    }

    public CommandLine setTrimQuotes(boolean newValue) {
        this.getCommandSpec().parser().trimQuotes(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setTrimQuotes(newValue);
        }
        return this;
    }

    @Deprecated
    public boolean isSplitQuotedStrings() {
        return this.getCommandSpec().parser().splitQuotedStrings();
    }

    @Deprecated
    public CommandLine setSplitQuotedStrings(boolean newValue) {
        this.getCommandSpec().parser().splitQuotedStrings(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setSplitQuotedStrings(newValue);
        }
        return this;
    }

    public String getEndOfOptionsDelimiter() {
        return this.getCommandSpec().parser().endOfOptionsDelimiter();
    }

    public CommandLine setEndOfOptionsDelimiter(String delimiter) {
        this.getCommandSpec().parser().endOfOptionsDelimiter(delimiter);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setEndOfOptionsDelimiter(delimiter);
        }
        return this;
    }

    public boolean isSubcommandsCaseInsensitive() {
        return this.getCommandSpec().subcommandsCaseInsensitive();
    }

    public CommandLine setSubcommandsCaseInsensitive(boolean newValue) {
        this.getCommandSpec().subcommandsCaseInsensitive(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setSubcommandsCaseInsensitive(newValue);
        }
        return this;
    }

    public boolean isOptionsCaseInsensitive() {
        return this.getCommandSpec().optionsCaseInsensitive();
    }

    public CommandLine setOptionsCaseInsensitive(boolean newValue) {
        this.getCommandSpec().optionsCaseInsensitive(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setOptionsCaseInsensitive(newValue);
        }
        return this;
    }

    public boolean isAbbreviatedSubcommandsAllowed() {
        return this.getCommandSpec().parser().abbreviatedSubcommandsAllowed();
    }

    public CommandLine setAbbreviatedSubcommandsAllowed(boolean newValue) {
        this.getCommandSpec().parser().abbreviatedSubcommandsAllowed(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setAbbreviatedSubcommandsAllowed(newValue);
        }
        return this;
    }

    public boolean isAbbreviatedOptionsAllowed() {
        return this.getCommandSpec().parser().abbreviatedOptionsAllowed();
    }

    public CommandLine setAbbreviatedOptionsAllowed(boolean newValue) {
        this.getCommandSpec().parser().abbreviatedOptionsAllowed(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setAbbreviatedOptionsAllowed(newValue);
        }
        return this;
    }

    public IDefaultValueProvider getDefaultValueProvider() {
        return this.getCommandSpec().defaultValueProvider();
    }

    public CommandLine setDefaultValueProvider(IDefaultValueProvider newValue) {
        this.getCommandSpec().defaultValueProvider(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setDefaultValueProvider(newValue);
        }
        return this;
    }

    public boolean isStopAtPositional() {
        return this.getCommandSpec().parser().stopAtPositional();
    }

    public CommandLine setStopAtPositional(boolean newValue) {
        this.getCommandSpec().parser().stopAtPositional(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setStopAtPositional(newValue);
        }
        return this;
    }

    public boolean isStopAtUnmatched() {
        return this.getCommandSpec().parser().stopAtUnmatched();
    }

    public CommandLine setStopAtUnmatched(boolean newValue) {
        this.getCommandSpec().parser().stopAtUnmatched(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setStopAtUnmatched(newValue);
        }
        if (newValue) {
            this.setUnmatchedArgumentsAllowed(true);
        }
        return this;
    }

    public boolean isUnmatchedOptionsAllowedAsOptionParameters() {
        return this.getCommandSpec().parser().unmatchedOptionsAllowedAsOptionParameters();
    }

    public CommandLine setUnmatchedOptionsAllowedAsOptionParameters(boolean newValue) {
        this.getCommandSpec().parser().unmatchedOptionsAllowedAsOptionParameters(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setUnmatchedOptionsAllowedAsOptionParameters(newValue);
        }
        return this;
    }

    public boolean isUnmatchedOptionsArePositionalParams() {
        return this.getCommandSpec().parser().unmatchedOptionsArePositionalParams();
    }

    public CommandLine setUnmatchedOptionsArePositionalParams(boolean newValue) {
        this.getCommandSpec().parser().unmatchedOptionsArePositionalParams(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setUnmatchedOptionsArePositionalParams(newValue);
        }
        return this;
    }

    public boolean isUnmatchedArgumentsAllowed() {
        return this.getCommandSpec().parser().unmatchedArgumentsAllowed();
    }

    public CommandLine setUnmatchedArgumentsAllowed(boolean newValue) {
        this.getCommandSpec().parser().unmatchedArgumentsAllowed(newValue);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setUnmatchedArgumentsAllowed(newValue);
        }
        return this;
    }

    public List<String> getUnmatchedArguments() {
        return this.interpreter.parseResultBuilder == null ? Collections.emptyList() : UnmatchedArgumentException.stripErrorMessage(this.interpreter.parseResultBuilder.unmatched);
    }

    private static int mappedExitCode(Throwable t, IExitCodeExceptionMapper mapper, int defaultExitCode) {
        try {
            return mapper != null ? mapper.getExitCode(t) : defaultExitCode;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return defaultExitCode;
        }
    }

    public Help.ColorScheme getColorScheme() {
        return this.colorScheme;
    }

    public CommandLine setColorScheme(Help.ColorScheme colorScheme) {
        this.colorScheme = Assert.notNull(colorScheme, "colorScheme");
        for (CommandLine sub : this.getSubcommands().values()) {
            sub.setColorScheme(colorScheme);
        }
        return this;
    }

    public PrintWriter getOut() {
        if (this.out == null) {
            this.setOut(CommandLine.newPrintWriter(System.out, CommandLine.getStdoutEncoding()));
        }
        return this.out;
    }

    public CommandLine setOut(PrintWriter out) {
        this.out = Assert.notNull(out, "out");
        for (CommandLine sub : this.getSubcommands().values()) {
            sub.setOut(out);
        }
        return this;
    }

    public PrintWriter getErr() {
        if (this.err == null) {
            this.setErr(CommandLine.newPrintWriter(System.err, CommandLine.getStderrEncoding()));
        }
        return this.err;
    }

    public CommandLine setErr(PrintWriter err) {
        this.err = Assert.notNull(err, "err");
        for (CommandLine sub : this.getSubcommands().values()) {
            sub.setErr(err);
        }
        return this;
    }

    public IExitCodeExceptionMapper getExitCodeExceptionMapper() {
        return this.exitCodeExceptionMapper;
    }

    public CommandLine setExitCodeExceptionMapper(IExitCodeExceptionMapper exitCodeExceptionMapper) {
        this.exitCodeExceptionMapper = Assert.notNull(exitCodeExceptionMapper, "exitCodeExceptionMapper");
        for (CommandLine sub : this.getSubcommands().values()) {
            sub.setExitCodeExceptionMapper(exitCodeExceptionMapper);
        }
        return this;
    }

    public IExecutionStrategy getExecutionStrategy() {
        return this.executionStrategy;
    }

    public CommandLine setExecutionStrategy(IExecutionStrategy executionStrategy) {
        this.executionStrategy = Assert.notNull(executionStrategy, "executionStrategy");
        for (CommandLine sub : this.getSubcommands().values()) {
            sub.setExecutionStrategy(executionStrategy);
        }
        return this;
    }

    public IParameterExceptionHandler getParameterExceptionHandler() {
        return this.parameterExceptionHandler;
    }

    public CommandLine setParameterExceptionHandler(IParameterExceptionHandler parameterExceptionHandler) {
        this.parameterExceptionHandler = Assert.notNull(parameterExceptionHandler, "parameterExceptionHandler");
        for (CommandLine sub : this.getSubcommands().values()) {
            sub.setParameterExceptionHandler(parameterExceptionHandler);
        }
        return this;
    }

    public IExecutionExceptionHandler getExecutionExceptionHandler() {
        return this.executionExceptionHandler;
    }

    public CommandLine setExecutionExceptionHandler(IExecutionExceptionHandler executionExceptionHandler) {
        this.executionExceptionHandler = Assert.notNull(executionExceptionHandler, "executionExceptionHandler");
        for (CommandLine sub : this.getSubcommands().values()) {
            sub.setExecutionExceptionHandler(executionExceptionHandler);
        }
        return this;
    }

    public static <T> T populateCommand(T command, String ... args) {
        CommandLine cli = CommandLine.toCommandLine(command, new DefaultFactory());
        cli.parse(args);
        return command;
    }

    public static <T> T populateSpec(Class<T> spec, String ... args) {
        CommandLine cli = CommandLine.toCommandLine(spec, new DefaultFactory());
        cli.parse(args);
        return cli.getCommand();
    }

    @Deprecated
    public List<CommandLine> parse(String ... args) {
        return this.interpreter.parse(args);
    }

    public ParseResult parseArgs(String ... args) {
        this.interpreter.parse(args);
        return this.getParseResult();
    }

    public ParseResult getParseResult() {
        return this.interpreter.parseResultBuilder == null ? null : this.interpreter.parseResultBuilder.build();
    }

    public <T> T getExecutionResult() {
        return (T)this.executionResult;
    }

    public void setExecutionResult(Object result) {
        this.executionResult = result;
    }

    public void clearExecutionResults() {
        this.executionResult = null;
        for (CommandLine sub : this.getSubcommands().values()) {
            sub.clearExecutionResults();
        }
    }

    public static DefaultExceptionHandler<List<Object>> defaultExceptionHandler() {
        return new DefaultExceptionHandler<List<Object>>();
    }

    @Deprecated
    public static boolean printHelpIfRequested(List<CommandLine> parsedCommands, PrintStream out, Help.Ansi ansi) {
        return CommandLine.printHelpIfRequested(parsedCommands, out, out, ansi);
    }

    public static boolean printHelpIfRequested(ParseResult parseResult) {
        return CommandLine.executeHelpRequest(parseResult) != null;
    }

    @Deprecated
    public static boolean printHelpIfRequested(List<CommandLine> parsedCommands, PrintStream out, PrintStream err, Help.Ansi ansi) {
        return CommandLine.printHelpIfRequested(parsedCommands, out, err, Help.defaultColorScheme(ansi));
    }

    @Deprecated
    public static boolean printHelpIfRequested(List<CommandLine> parsedCommands, PrintStream out, PrintStream err, Help.ColorScheme colorScheme) {
        for (CommandLine cmd : parsedCommands) {
            cmd.setOut(CommandLine.newPrintWriter(out, CommandLine.getStdoutEncoding())).setErr(CommandLine.newPrintWriter(err, CommandLine.getStderrEncoding())).setColorScheme(colorScheme);
        }
        return CommandLine.executeHelpRequest(parsedCommands) != null;
    }

    public static Integer executeHelpRequest(ParseResult parseResult) {
        return CommandLine.executeHelpRequest(parseResult.asCommandLineList());
    }

    static Integer executeHelpRequest(List<CommandLine> parsedCommands) {
        for (CommandLine parsed : parsedCommands) {
            Help.ColorScheme colorScheme = parsed.getColorScheme();
            PrintWriter out = parsed.getOut();
            if (parsed.isUsageHelpRequested()) {
                parsed.usage(out, colorScheme);
                return parsed.getCommandSpec().exitCodeOnUsageHelp();
            }
            if (parsed.isVersionHelpRequested()) {
                parsed.printVersionHelp(out, colorScheme.ansi, new Object[0]);
                return parsed.getCommandSpec().exitCodeOnVersionHelp();
            }
            if (!parsed.getCommandSpec().helpCommand()) continue;
            PrintWriter err = parsed.getErr();
            if (parsed.getCommand() instanceof IHelpCommandInitializable2) {
                ((IHelpCommandInitializable2)parsed.getCommand()).init(parsed, colorScheme, out, err);
            } else if (parsed.getCommand() instanceof IHelpCommandInitializable) {
                ((IHelpCommandInitializable)parsed.getCommand()).init(parsed, colorScheme.ansi, System.out, System.err);
            }
            CommandLine.executeUserObject(parsed, new ArrayList<Object>());
            return parsed.getCommandSpec().exitCodeOnUsageHelp();
        }
        return null;
    }

    private static List<Object> executeUserObject(CommandLine parsed, List<Object> executionResultList) {
        Object command = parsed.getCommand();
        if (command instanceof Runnable) {
            try {
                ((Runnable)command).run();
                parsed.setExecutionResult(null);
                executionResultList.add(null);
                return executionResultList;
            }
            catch (ParameterException ex) {
                throw ex;
            }
            catch (ExecutionException ex) {
                throw ex;
            }
            catch (Exception ex) {
                throw new ExecutionException(parsed, "Error while running command (" + command + "): " + ex, ex);
            }
        }
        if (command instanceof Callable) {
            try {
                Callable callable = (Callable)command;
                Object executionResult = callable.call();
                parsed.setExecutionResult(executionResult);
                executionResultList.add(executionResult);
                return executionResultList;
            }
            catch (ParameterException ex) {
                throw ex;
            }
            catch (ExecutionException ex) {
                throw ex;
            }
            catch (Exception ex) {
                throw new ExecutionException(parsed, "Error while calling command (" + command + "): " + ex, ex);
            }
        }
        if (command instanceof Method) {
            try {
                Method method = (Method)command;
                Object[] parsedArgs = parsed.getCommandSpec().commandMethodParamValues();
                Object executionResult = Modifier.isStatic(method.getModifiers()) ? method.invoke(null, parsedArgs) : (parsed.getCommandSpec().parent() != null ? method.invoke(parsed.getCommandSpec().parent().userObject(), parsedArgs) : method.invoke(parsed.factory.create(method.getDeclaringClass()), parsedArgs));
                parsed.setExecutionResult(executionResult);
                executionResultList.add(executionResult);
                return executionResultList;
            }
            catch (InvocationTargetException ex) {
                Throwable t = ex.getTargetException();
                if (t instanceof ParameterException) {
                    throw (ParameterException)t;
                }
                if (t instanceof ExecutionException) {
                    throw (ExecutionException)t;
                }
                throw new ExecutionException(parsed, "Error while calling command (" + command + "): " + t, t);
            }
            catch (Exception ex) {
                throw new ExecutionException(parsed, "Unhandled error while calling command (" + command + "): " + ex, ex);
            }
        }
        if (parsed.getSubcommands().isEmpty()) {
            throw new ExecutionException(parsed, "Parsed command (" + command + ") is not a Method, Runnable or Callable");
        }
        throw new ParameterException(parsed, "Missing required subcommand");
    }

    public int execute(String ... args) {
        ParseResult[] parseResult = new ParseResult[1];
        this.clearExecutionResults();
        try {
            parseResult[0] = this.parseArgs(args);
            return this.enrichForBackwardsCompatibility(this.getExecutionStrategy()).execute(parseResult[0]);
        }
        catch (ParameterException ex) {
            try {
                return this.getParameterExceptionHandler().handleParseException(ex, args);
            }
            catch (Exception ex2) {
                return CommandLine.handleUnhandled(ex2, ex.getCommandLine(), ex.getCommandLine().getCommandSpec().exitCodeOnInvalidInput());
            }
        }
        catch (ExecutionException ex) {
            try {
                ExecutionException cause = ex.getCause() instanceof Exception ? (Exception)ex.getCause() : ex;
                return this.getExecutionExceptionHandler().handleExecutionException(cause, ex.getCommandLine(), parseResult[0]);
            }
            catch (Exception ex2) {
                return CommandLine.handleUnhandled(ex2, ex.getCommandLine(), ex.getCommandLine().getCommandSpec().exitCodeOnExecutionException());
            }
        }
        catch (Exception ex) {
            return CommandLine.handleUnhandled(ex, this, this.getCommandSpec().exitCodeOnExecutionException());
        }
    }

    private static int handleUnhandled(Exception ex, CommandLine cmd, int defaultExitCode) {
        cmd.getErr().print(CommandLine.throwableToColorString(ex, cmd.getColorScheme()));
        cmd.getErr().flush();
        return CommandLine.mappedExitCode(ex, cmd.getExitCodeExceptionMapper(), defaultExitCode);
    }

    private static String throwableToColorString(Throwable t, Help.ColorScheme existingColorScheme) {
        Help.ColorScheme colorScheme = new Help.ColorScheme.Builder(existingColorScheme).applySystemProperties().build();
        ColoredStackTraceWriter stringWriter = new ColoredStackTraceWriter(colorScheme);
        t.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private <T> T enrichForBackwardsCompatibility(T obj) {
        if (obj instanceof AbstractHandler) {
            AbstractHandler handler = (AbstractHandler)obj;
            if (handler.out() != System.out) {
                this.setOut(CommandLine.newPrintWriter(handler.out(), CommandLine.getStdoutEncoding()));
            }
            if (handler.err() != System.err) {
                this.setErr(CommandLine.newPrintWriter(handler.err(), CommandLine.getStderrEncoding()));
            }
            if (handler.ansi() != Help.Ansi.AUTO) {
                this.setColorScheme(handler.colorScheme());
            }
        }
        return obj;
    }

    @Deprecated
    public List<Object> parseWithHandler(IParseResultHandler handler, PrintStream out, String ... args) {
        return this.parseWithHandlers(handler, out, Help.Ansi.AUTO, CommandLine.defaultExceptionHandler(), args);
    }

    @Deprecated
    public <R> R parseWithHandler(IParseResultHandler2<R> handler, String[] args) {
        return this.parseWithHandlers(handler, new DefaultExceptionHandler(), args);
    }

    @Deprecated
    public List<Object> parseWithHandlers(IParseResultHandler handler, PrintStream out, Help.Ansi ansi, IExceptionHandler exceptionHandler, String ... args) {
        this.clearExecutionResults();
        try {
            List<CommandLine> result = this.parse(args);
            return handler.handleParseResult(result, out, ansi);
        }
        catch (ParameterException ex) {
            return exceptionHandler.handleException(ex, out, ansi, args);
        }
    }

    @Deprecated
    public <R> R parseWithHandlers(IParseResultHandler2<R> handler, IExceptionHandler2<R> exceptionHandler, String ... args) {
        this.clearExecutionResults();
        ParseResult parseResult = null;
        try {
            parseResult = this.parseArgs(args);
            return handler.handleParseResult(parseResult);
        }
        catch (ParameterException ex) {
            return exceptionHandler.handleParseException(ex, args);
        }
        catch (ExecutionException ex) {
            return exceptionHandler.handleExecutionException(ex, parseResult);
        }
    }

    static String versionString() {
        return String.format("%s, JVM: %s (%s %s %s), OS: %s %s %s", VERSION, System.getProperty("java.version"), System.getProperty("java.vendor"), System.getProperty("java.vm.name"), System.getProperty("java.vm.version"), System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
    }

    public static void usage(Object command, PrintStream out) {
        CommandLine.toCommandLine(command, new DefaultFactory()).usage(out);
    }

    public static void usage(Object command, PrintStream out, Help.Ansi ansi) {
        CommandLine.toCommandLine(command, new DefaultFactory()).usage(out, ansi);
    }

    public static void usage(Object command, PrintStream out, Help.ColorScheme colorScheme) {
        CommandLine.toCommandLine(command, new DefaultFactory()).usage(out, colorScheme);
    }

    public void usage(PrintStream out) {
        this.usage(out, this.getColorScheme());
    }

    public void usage(PrintWriter writer) {
        this.usage(writer, this.getColorScheme());
    }

    public void usage(PrintStream out, Help.Ansi ansi) {
        this.usage(out, Help.defaultColorScheme(ansi));
    }

    public void usage(PrintWriter writer, Help.Ansi ansi) {
        this.usage(writer, Help.defaultColorScheme(ansi));
    }

    public void usage(PrintStream out, Help.ColorScheme colorScheme) {
        out.print(this.usage(new StringBuilder(), this.getHelpFactory().create(this.getCommandSpec(), colorScheme)));
        out.flush();
    }

    public void usage(PrintWriter writer, Help.ColorScheme colorScheme) {
        writer.print(this.usage(new StringBuilder(), this.getHelpFactory().create(this.getCommandSpec(), colorScheme)));
        writer.flush();
    }

    public String getUsageMessage() {
        return this.usage(new StringBuilder(), this.getHelp()).toString();
    }

    public String getUsageMessage(Help.Ansi ansi) {
        return this.usage(new StringBuilder(), this.getHelpFactory().create(this.getCommandSpec(), Help.defaultColorScheme(ansi))).toString();
    }

    public String getUsageMessage(Help.ColorScheme colorScheme) {
        return this.usage(new StringBuilder(), this.getHelpFactory().create(this.getCommandSpec(), colorScheme)).toString();
    }

    private StringBuilder usage(StringBuilder sb, Help help) {
        for (String key : this.getHelpSectionKeys()) {
            IHelpSectionRenderer renderer = this.getHelpSectionMap().get(key);
            if (renderer == null) continue;
            sb.append(renderer.render(help));
        }
        return sb;
    }

    public void printVersionHelp(PrintStream out) {
        this.printVersionHelp(out, this.getColorScheme().ansi());
    }

    public void printVersionHelp(PrintStream out, Help.Ansi ansi) {
        for (String versionInfo : this.getCommandSpec().version()) {
            Help.Ansi ansi2 = ansi;
            ((Object)((Object)ansi2)).getClass();
            out.println(ansi2.new Help.Ansi.Text(versionInfo, this.getColorScheme()));
        }
        out.flush();
    }

    public void printVersionHelp(PrintStream out, Help.Ansi ansi, Object ... params) {
        for (String versionInfo : this.getCommandSpec().version()) {
            Help.Ansi ansi2 = ansi;
            ((Object)((Object)ansi2)).getClass();
            out.println(ansi2.new Help.Ansi.Text(CommandLine.format(versionInfo, params), this.getColorScheme()));
        }
        out.flush();
    }

    public void printVersionHelp(PrintWriter out) {
        this.printVersionHelp(out, this.getColorScheme().ansi(), new Object[0]);
    }

    public void printVersionHelp(PrintWriter out, Help.Ansi ansi, Object ... params) {
        for (String versionInfo : this.getCommandSpec().version()) {
            Help.Ansi ansi2 = ansi;
            ((Object)((Object)ansi2)).getClass();
            out.println(ansi2.new Help.Ansi.Text(CommandLine.format(versionInfo, params), this.getColorScheme()));
        }
        out.flush();
    }

    @Deprecated
    public static <C extends Callable<T>, T> T call(C callable, String ... args) {
        CommandLine cmd = new CommandLine(callable);
        List<Object> results = cmd.parseWithHandler(new RunLast(), args);
        return CommandLine.firstElement(results);
    }

    @Deprecated
    public static <C extends Callable<T>, T> T call(C callable, PrintStream out, String ... args) {
        return CommandLine.call(callable, out, System.err, Help.Ansi.AUTO, args);
    }

    @Deprecated
    public static <C extends Callable<T>, T> T call(C callable, PrintStream out, Help.Ansi ansi, String ... args) {
        return CommandLine.call(callable, out, System.err, ansi, args);
    }

    @Deprecated
    public static <C extends Callable<T>, T> T call(C callable, PrintStream out, PrintStream err, Help.Ansi ansi, String ... args) {
        CommandLine cmd = new CommandLine(callable);
        List results = (List)cmd.parseWithHandlers((IParseResultHandler2)((AbstractParseResultHandler)new RunLast().useOut(out)).useAnsi(ansi), (IExceptionHandler2)((DefaultExceptionHandler)new DefaultExceptionHandler().useErr(err)).useAnsi(ansi), args);
        return CommandLine.firstElement(results);
    }

    @Deprecated
    public static <C extends Callable<T>, T> T call(Class<C> callableClass, IFactory factory, String ... args) {
        CommandLine cmd = new CommandLine(callableClass, factory);
        List<Object> results = cmd.parseWithHandler(new RunLast(), args);
        return CommandLine.firstElement(results);
    }

    @Deprecated
    public static <C extends Callable<T>, T> T call(Class<C> callableClass, IFactory factory, PrintStream out, String ... args) {
        return CommandLine.call(callableClass, factory, out, System.err, Help.Ansi.AUTO, args);
    }

    @Deprecated
    public static <C extends Callable<T>, T> T call(Class<C> callableClass, IFactory factory, PrintStream out, Help.Ansi ansi, String ... args) {
        return CommandLine.call(callableClass, factory, out, System.err, ansi, args);
    }

    @Deprecated
    public static <C extends Callable<T>, T> T call(Class<C> callableClass, IFactory factory, PrintStream out, PrintStream err, Help.Ansi ansi, String ... args) {
        CommandLine cmd = new CommandLine(callableClass, factory);
        List results = (List)cmd.parseWithHandlers((IParseResultHandler2)((AbstractParseResultHandler)new RunLast().useOut(out)).useAnsi(ansi), (IExceptionHandler2)((DefaultExceptionHandler)new DefaultExceptionHandler().useErr(err)).useAnsi(ansi), args);
        return CommandLine.firstElement(results);
    }

    private static <T> T firstElement(List<Object> results) {
        return (T)(results == null || results.isEmpty() ? null : results.get(0));
    }

    @Deprecated
    public static <R extends Runnable> void run(R runnable, String ... args) {
        CommandLine.run(runnable, System.out, System.err, Help.Ansi.AUTO, args);
    }

    @Deprecated
    public static <R extends Runnable> void run(R runnable, PrintStream out, String ... args) {
        CommandLine.run(runnable, out, System.err, Help.Ansi.AUTO, args);
    }

    @Deprecated
    public static <R extends Runnable> void run(R runnable, PrintStream out, Help.Ansi ansi, String ... args) {
        CommandLine.run(runnable, out, System.err, ansi, args);
    }

    @Deprecated
    public static <R extends Runnable> void run(R runnable, PrintStream out, PrintStream err, Help.Ansi ansi, String ... args) {
        CommandLine cmd = new CommandLine(runnable);
        cmd.parseWithHandlers((IParseResultHandler2)((AbstractParseResultHandler)new RunLast().useOut(out)).useAnsi(ansi), (IExceptionHandler2)((DefaultExceptionHandler)new DefaultExceptionHandler().useErr(err)).useAnsi(ansi), args);
    }

    @Deprecated
    public static <R extends Runnable> void run(Class<R> runnableClass, IFactory factory, String ... args) {
        CommandLine.run(runnableClass, factory, System.out, System.err, Help.Ansi.AUTO, args);
    }

    @Deprecated
    public static <R extends Runnable> void run(Class<R> runnableClass, IFactory factory, PrintStream out, String ... args) {
        CommandLine.run(runnableClass, factory, out, System.err, Help.Ansi.AUTO, args);
    }

    @Deprecated
    public static <R extends Runnable> void run(Class<R> runnableClass, IFactory factory, PrintStream out, Help.Ansi ansi, String ... args) {
        CommandLine.run(runnableClass, factory, out, System.err, ansi, args);
    }

    @Deprecated
    public static <R extends Runnable> void run(Class<R> runnableClass, IFactory factory, PrintStream out, PrintStream err, Help.Ansi ansi, String ... args) {
        CommandLine cmd = new CommandLine(runnableClass, factory);
        cmd.parseWithHandlers((IParseResultHandler2)((AbstractParseResultHandler)new RunLast().useOut(out)).useAnsi(ansi), (IExceptionHandler2)((DefaultExceptionHandler)new DefaultExceptionHandler().useErr(err)).useAnsi(ansi), args);
    }

    @Deprecated
    public static Object invoke(String methodName, Class<?> cls, String ... args) {
        return CommandLine.invoke(methodName, cls, System.out, System.err, Help.Ansi.AUTO, args);
    }

    @Deprecated
    public static Object invoke(String methodName, Class<?> cls, PrintStream out, String ... args) {
        return CommandLine.invoke(methodName, cls, out, System.err, Help.Ansi.AUTO, args);
    }

    @Deprecated
    public static Object invoke(String methodName, Class<?> cls, PrintStream out, Help.Ansi ansi, String ... args) {
        return CommandLine.invoke(methodName, cls, out, System.err, ansi, args);
    }

    @Deprecated
    public static Object invoke(String methodName, Class<?> cls, PrintStream out, PrintStream err, Help.Ansi ansi, String ... args) {
        List<Method> candidates = CommandLine.getCommandMethods(cls, methodName);
        if (candidates.size() != 1) {
            throw new InitializationException("Expected exactly one @Command-annotated method for " + cls.getName() + "::" + methodName + "(...), but got: " + candidates);
        }
        Method method = candidates.get(0);
        CommandLine cmd = new CommandLine(method);
        List list = (List)cmd.parseWithHandlers((IParseResultHandler2)((AbstractParseResultHandler)new RunLast().useOut(out)).useAnsi(ansi), (IExceptionHandler2)((DefaultExceptionHandler)new DefaultExceptionHandler().useErr(err)).useAnsi(ansi), args);
        return list == null ? null : list.get(0);
    }

    public static List<Method> getCommandMethods(Class<?> cls, String methodName) {
        return CommandLine.getCommandMethods(cls, methodName, true);
    }

    private static List<Method> getCommandMethods(Class<?> cls, String methodName, boolean includeInherited) {
        HashSet<Method> candidates = new HashSet<Method>();
        if (includeInherited) {
            candidates.addAll(Arrays.asList(Assert.notNull(cls, "class").getMethods()));
        }
        candidates.addAll(Arrays.asList(Assert.notNull(cls, "class").getDeclaredMethods()));
        ArrayList<Method> result = new ArrayList<Method>();
        for (Method method : candidates) {
            if (!method.isAnnotationPresent(Command.class) || methodName != null && !methodName.equals(method.getName())) continue;
            result.add(method);
        }
        Collections.sort(result, new Comparator<Method>(){

            @Override
            public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return result;
    }

    public <K> CommandLine registerConverter(Class<K> cls, ITypeConverter<K> converter) {
        this.interpreter.converterRegistry.put(Assert.notNull(cls, "class"), Assert.notNull(converter, "converter"));
        for (CommandLine command : this.getCommandSpec().commands.values()) {
            command.registerConverter(cls, converter);
        }
        return this;
    }

    public String getSeparator() {
        return this.getCommandSpec().parser().separator();
    }

    public CommandLine setSeparator(String separator) {
        this.getCommandSpec().parser().separator(Assert.notNull(separator, "separator"));
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setSeparator(separator);
        }
        return this;
    }

    public ResourceBundle getResourceBundle() {
        return this.getCommandSpec().resourceBundle();
    }

    public CommandLine setResourceBundle(ResourceBundle bundle) {
        this.getCommandSpec().resourceBundle(bundle);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setResourceBundle(bundle);
        }
        return this;
    }

    public int getUsageHelpWidth() {
        return this.getCommandSpec().usageMessage().width();
    }

    public CommandLine setUsageHelpWidth(int width) {
        this.getCommandSpec().usageMessage().width(width);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setUsageHelpWidth(width);
        }
        return this;
    }

    public int getUsageHelpLongOptionsMaxWidth() {
        return this.getCommandSpec().usageMessage().longOptionsMaxWidth();
    }

    public CommandLine setUsageHelpLongOptionsMaxWidth(int columnWidth) {
        this.getCommandSpec().usageMessage().longOptionsMaxWidth(columnWidth);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setUsageHelpLongOptionsMaxWidth(columnWidth);
        }
        return this;
    }

    public boolean isUsageHelpAutoWidth() {
        return this.getCommandSpec().usageMessage().autoWidth();
    }

    public CommandLine setUsageHelpAutoWidth(boolean detectTerminalSize) {
        this.getCommandSpec().usageMessage().autoWidth(detectTerminalSize);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setUsageHelpAutoWidth(detectTerminalSize);
        }
        return this;
    }

    public String getCommandName() {
        return this.getCommandSpec().name();
    }

    public CommandLine setCommandName(String commandName) {
        this.getCommandSpec().name(Assert.notNull(commandName, "commandName"));
        return this;
    }

    public boolean isExpandAtFiles() {
        return this.getCommandSpec().parser().expandAtFiles();
    }

    public CommandLine setExpandAtFiles(boolean expandAtFiles) {
        this.getCommandSpec().parser().expandAtFiles(expandAtFiles);
        return this;
    }

    public Character getAtFileCommentChar() {
        return this.getCommandSpec().parser().atFileCommentChar();
    }

    public CommandLine setAtFileCommentChar(Character atFileCommentChar) {
        this.getCommandSpec().parser().atFileCommentChar(atFileCommentChar);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setAtFileCommentChar(atFileCommentChar);
        }
        return this;
    }

    public boolean isUseSimplifiedAtFiles() {
        return this.getCommandSpec().parser().useSimplifiedAtFiles();
    }

    public CommandLine setUseSimplifiedAtFiles(boolean simplifiedAtFiles) {
        this.getCommandSpec().parser().useSimplifiedAtFiles(simplifiedAtFiles);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setUseSimplifiedAtFiles(simplifiedAtFiles);
        }
        return this;
    }

    public INegatableOptionTransformer getNegatableOptionTransformer() {
        return this.getCommandSpec().negatableOptionTransformer();
    }

    public CommandLine setNegatableOptionTransformer(INegatableOptionTransformer transformer) {
        this.getCommandSpec().negatableOptionTransformer(transformer);
        for (CommandLine command : this.getCommandSpec().subcommands().values()) {
            command.setNegatableOptionTransformer(transformer);
        }
        return this;
    }

    private static boolean empty(String str) {
        return str == null || str.trim().length() == 0;
    }

    private static boolean empty(Object[] array) {
        return array == null || array.length == 0;
    }

    private static String str(String[] arr, int i) {
        return arr == null || arr.length <= i ? "" : arr[i];
    }

    private static boolean isBoolean(Class<?>[] types) {
        return CommandLine.isBoolean(types[0]) || CommandLine.isOptional(types[0]) && CommandLine.isBoolean(types[1]);
    }

    private static boolean isBoolean(Class<?> type) {
        return type == Boolean.class || type == Boolean.TYPE;
    }

    private static CommandLine toCommandLine(Object obj, IFactory factory) {
        return obj instanceof CommandLine ? (CommandLine)obj : new CommandLine(obj, factory, false);
    }

    private static boolean isMultiValue(Class<?> cls) {
        return cls.isArray() || Collection.class.isAssignableFrom(cls) || Map.class.isAssignableFrom(cls);
    }

    private static boolean isOptional(Class<?> cls) {
        return cls != null && "java.util.Optional".equals(cls.getName());
    }

    private static Object getOptionalEmpty() throws Exception {
        return Class.forName("java.util.Optional").getMethod("empty", new Class[0]).invoke(null, new Object[0]);
    }

    private static Object getOptionalOfNullable(Object newValue) throws Exception {
        return Class.forName("java.util.Optional").getMethod("ofNullable", Object.class).invoke(null, newValue);
    }

    private static String format(String formatString, Object ... params) {
        try {
            return formatString == null ? "" : String.format(formatString, params);
        }
        catch (IllegalFormatException ex) {
            new Tracer().warn("Could not format '%s' (Underlying error: %s). Using raw String: '%%n' format strings have not been replaced with newlines. Please ensure to escape '%%' characters with another '%%'.%n", formatString, ex.getMessage());
            return formatString;
        }
    }

    private static Map<String, Object> mapOf(String key, Object value, Object ... other) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put(key, value);
        for (int i = 0; i < other.length - 1; i += 2) {
            result.put(String.valueOf(other[i]), other[i + 1]);
        }
        return result;
    }

    public static IFactory defaultFactory() {
        return new DefaultFactory();
    }

    private static void validatePositionalParameters(List<Model.PositionalParamSpec> positionals) {
        int min = 0;
        for (Model.PositionalParamSpec positional : positionals) {
            Range index = positional.index();
            if (index.min > min && !index.isRelative()) {
                ArrayList<String> indices = new ArrayList<String>();
                for (Model.PositionalParamSpec pos : positionals) {
                    indices.add(pos.index().internalToString());
                }
                throw new ParameterIndexGapException("Command definition should have a positional parameter with index=" + min + ". Nearest positional parameter '" + positional.paramLabel() + "' has index=" + index + ". (Full list: " + indices + ")");
            }
            min = (min = Math.max(min, index.max)) == Integer.MAX_VALUE ? min : min + 1;
        }
    }

    private static Stack<String> copy(Stack<String> stack) {
        return (Stack)stack.clone();
    }

    private static <T> Stack<T> reverse(Stack<T> stack) {
        Collections.reverse(stack);
        return stack;
    }

    private static <T> List<T> reverseList(List<T> list) {
        Collections.reverse(list);
        return list;
    }

    private static <T> T[] reverseArray(T[] all) {
        for (int i = 0; i < all.length / 2; ++i) {
            T temp = all[i];
            all[i] = all[all.length - i - 1];
            all[all.length - i - 1] = temp;
        }
        return all;
    }

    static <K, T> void addValueToListInMap(Map<K, List<T>> map, K key, T value) {
        List<T> values = map.get(key);
        if (values == null) {
            values = new ArrayList<T>();
            map.put(key, values);
        }
        values.add(value);
    }

    static <T> List<T> flatList(Collection<? extends Collection<T>> collection) {
        ArrayList<T> result = new ArrayList<T>();
        for (Collection<T> sub : collection) {
            result.addAll(sub);
        }
        return result;
    }

    private static String optionDescription(String prefix, Model.ArgSpec argSpec, int optionParamIndex) {
        String desc;
        if (argSpec.isOption()) {
            desc = prefix + "option '" + ((Model.OptionSpec)argSpec).longestName() + "'";
            if (optionParamIndex >= 0) {
                if (argSpec.arity().max > 1) {
                    desc = desc + " at index " + optionParamIndex;
                }
                if (argSpec.arity().max > 0) {
                    desc = desc + " (" + argSpec.paramLabel() + ")";
                }
            }
        } else {
            desc = prefix + "positional parameter at index " + ((Model.PositionalParamSpec)argSpec).index() + " (" + argSpec.paramLabel() + ")";
        }
        return desc;
    }

    private static String createMissingParameterMessage(Model.ArgSpec argSpec, Range arity, List<Model.PositionalParamSpec> missingList, Stack<String> args, int available) {
        if (arity.min == 1) {
            if (argSpec.isOption()) {
                return "Missing required parameter for " + CommandLine.optionDescription("", argSpec, 0);
            }
            String sep = "";
            String names = ": ";
            String indices = "";
            String infix = " at index ";
            int count = 0;
            for (Model.PositionalParamSpec missing : missingList) {
                if (missing.arity().min <= 0) continue;
                names = names + sep + "'" + missing.paramLabel() + "'";
                indices = indices + sep + missing.index();
                sep = ", ";
                ++count;
            }
            String msg = "Missing required parameter";
            if (count > 1 || arity.min - available > 1) {
                msg = msg + "s";
            }
            if (count > 1) {
                infix = " at indices ";
            }
            return System.getProperty("picocli.verbose.errors") != null ? msg + names + infix + indices : msg + names;
        }
        if (args.isEmpty()) {
            return CommandLine.optionDescription("", argSpec, 0) + " requires at least " + arity.min + " values, but none were specified.";
        }
        return CommandLine.optionDescription("", argSpec, 0) + " requires at least " + arity.min + " values, but only " + available + " were specified: " + CommandLine.reverse(args);
    }

    String smartUnquoteIfEnabled(String value) {
        if (value == null || !this.commandSpec.parser().trimQuotes()) {
            return value;
        }
        return CommandLine.smartUnquote(value);
    }

    static String smartUnquote(String value) {
        int ch;
        String unquoted = CommandLine.unquote(value);
        if (unquoted == value) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        int slashCount = 0;
        for (int i = 0; i < unquoted.length(); i += Character.charCount(ch)) {
            ch = unquoted.codePointAt(i);
            switch (ch) {
                case 92: {
                    ++slashCount;
                    break;
                }
                case 34: {
                    if (slashCount == 0) {
                        return value;
                    }
                    slashCount = 0;
                    break;
                }
                default: {
                    slashCount = 0;
                }
            }
            if ((slashCount & 1) != 0) continue;
            result.appendCodePoint(ch);
        }
        return result.toString();
    }

    private static String unquote(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 1 && value.startsWith("\"") && value.endsWith("\"") ? value.substring(1, value.length() - 1) : value;
    }

    static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        }
        catch (Exception ex) {
            new Tracer().warn("Could not close " + closeable + ": " + ex.toString(), new Object[0]);
        }
    }

    static Charset getStdoutEncoding() {
        String encoding = System.getProperty("sun.stdout.encoding");
        return encoding != null ? Charset.forName(encoding) : Charset.defaultCharset();
    }

    static Charset getStderrEncoding() {
        String encoding = System.getProperty("sun.stderr.encoding");
        return encoding != null ? Charset.forName(encoding) : Charset.defaultCharset();
    }

    static PrintWriter newPrintWriter(OutputStream stream, Charset charset) {
        return new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, charset)), true);
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    static class AbbreviationMatcher {
        AbbreviationMatcher() {
        }

        public static List<String> splitIntoChunks(String command, boolean caseInsensitive) {
            String chunk;
            int start;
            int codepoint;
            ArrayList<String> result = new ArrayList<String>();
            StringBuilder nonAlphabeticPrefix = new StringBuilder();
            for (start = 0; start < command.length() && !Character.isLetterOrDigit(codepoint = command.codePointAt(start)); start += Character.charCount(codepoint)) {
                nonAlphabeticPrefix.appendCodePoint(codepoint);
            }
            if (nonAlphabeticPrefix.length() > 0) {
                result.add(nonAlphabeticPrefix.toString());
            }
            for (int i = start; i < command.length(); i += Character.charCount(codepoint)) {
                codepoint = command.codePointAt(i);
                if ((caseInsensitive || !Character.isUpperCase(codepoint)) && 45 != codepoint) continue;
                String chunk2 = AbbreviationMatcher.makeCanonical(command.substring(start, i));
                if (chunk2.length() > 0) {
                    result.add(chunk2);
                }
                start = i;
            }
            if (start < command.length() && (chunk = AbbreviationMatcher.makeCanonical(command.substring(start))).length() > 0) {
                result.add(chunk);
            }
            return result;
        }

        private static String makeCanonical(String str) {
            if ("-".equals(str)) {
                return "";
            }
            if (str.startsWith("-") && str.length() > 1) {
                String uppercase = String.valueOf(Character.toChars(Character.toUpperCase(str.codePointAt(1))));
                return uppercase + str.substring(1 + uppercase.length());
            }
            return str;
        }

        public static String match(Set<String> set, String abbreviation, boolean caseInsensitive, CommandLine source) {
            if (set.contains(abbreviation) || set.isEmpty()) {
                return abbreviation;
            }
            List<String> abbreviatedKeyChunks = AbbreviationMatcher.splitIntoChunks(abbreviation, caseInsensitive);
            ArrayList<String> candidates = new ArrayList<String>();
            for (String key : set) {
                List<String> keyChunks = AbbreviationMatcher.splitIntoChunks(key, caseInsensitive);
                if (!AbbreviationMatcher.matchKeyChunks(abbreviatedKeyChunks, keyChunks, caseInsensitive)) continue;
                candidates.add(key);
            }
            if (candidates.size() > 1) {
                String str = ((Object)candidates).toString();
                throw new ParameterException(source, "Error: '" + abbreviation + "' is not unique: it matches '" + str.substring(1, str.length() - 1).replace(", ", "', '") + "'");
            }
            return candidates.isEmpty() ? abbreviation : (String)candidates.get(0);
        }

        private static boolean matchKeyChunks(List<String> abbreviatedKeyChunks, List<String> keyChunks, boolean caseInsensitive) {
            if (abbreviatedKeyChunks.size() > keyChunks.size()) {
                return false;
            }
            int matchCount = 0;
            if (AbbreviationMatcher.isNonAlphabetic(keyChunks.get(0))) {
                if (!keyChunks.get(0).equals(abbreviatedKeyChunks.get(0))) {
                    return false;
                }
                ++matchCount;
            }
            if (!AbbreviationMatcher.startsWith(keyChunks.get(matchCount), abbreviatedKeyChunks.get(matchCount), caseInsensitive)) {
                return false;
            }
            int i = ++matchCount;
            int lastMatchChunk = matchCount;
            while (i < abbreviatedKeyChunks.size()) {
                boolean found = false;
                for (int j = lastMatchChunk; j < keyChunks.size(); ++j) {
                    found = AbbreviationMatcher.startsWith(keyChunks.get(j), abbreviatedKeyChunks.get(i), caseInsensitive);
                    if (!found) continue;
                    lastMatchChunk = j + 1;
                    break;
                }
                if (!found) break;
                ++i;
                ++matchCount;
            }
            return matchCount == abbreviatedKeyChunks.size();
        }

        private static boolean startsWith(String str, String prefix, boolean caseInsensitive) {
            if (prefix.length() > str.length()) {
                return false;
            }
            String strPrefix = str.substring(0, prefix.length());
            return caseInsensitive ? strPrefix.equalsIgnoreCase(prefix) : strPrefix.equals(prefix);
        }

        private static boolean isNonAlphabetic(String str) {
            int codepoint;
            for (int i = 0; i < str.length(); i += Character.charCount(codepoint)) {
                codepoint = str.codePointAt(i);
                if (!Character.isLetterOrDigit(codepoint)) continue;
                return false;
            }
            return true;
        }
    }

    public static class PropertiesDefaultProvider
    implements IDefaultValueProvider {
        private Properties properties;
        private File location;

        public PropertiesDefaultProvider() {
        }

        public PropertiesDefaultProvider(Properties properties) {
            this.properties = properties;
        }

        public PropertiesDefaultProvider(File file) {
            this(PropertiesDefaultProvider.createProperties(file, null));
            this.properties.remove("__picocli_internal_location");
            this.location = file;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private static Properties createProperties(File file, Model.CommandSpec commandSpec) {
            if (file == null) {
                throw new NullPointerException("file is null");
            }
            Tracer tracer = new Tracer();
            Properties result = new Properties();
            if (file.exists() && file.canRead()) {
                FileInputStream in = null;
                try {
                    String command = commandSpec == null ? "unknown command" : commandSpec.qualifiedName();
                    tracer.debug("Reading defaults from %s for %s%n", file.getAbsolutePath(), command);
                    in = new FileInputStream(file);
                    result.load(in);
                    result.put("__picocli_internal_location", file);
                    CommandLine.close(in);
                }
                catch (IOException ioe) {
                    tracer.warn("could not read defaults from %s: %s%n", file.getAbsolutePath(), ioe);
                }
                finally {
                    CommandLine.close(in);
                }
            } else {
                tracer.warn("defaults configuration file %s does not exist or is not readable%n", file.getAbsolutePath());
            }
            return result;
        }

        private static Properties loadProperties(Model.CommandSpec commandSpec) {
            if (commandSpec == null) {
                return null;
            }
            Properties p = System.getProperties();
            for (String name : commandSpec.names()) {
                String path = p.getProperty("picocli.defaults." + name + ".path");
                File defaultPath = new File(p.getProperty("user.home"), "." + name + ".properties");
                File file = path == null ? defaultPath : new File(path);
                if (!file.canRead()) continue;
                return PropertiesDefaultProvider.createProperties(file, commandSpec);
            }
            return PropertiesDefaultProvider.loadProperties(commandSpec.parent());
        }

        public String defaultValue(Model.ArgSpec argSpec) throws Exception {
            if (this.properties == null) {
                this.properties = PropertiesDefaultProvider.loadProperties(argSpec.command());
                File file = this.location = this.properties == null ? null : (File)this.properties.remove("__picocli_internal_location");
            }
            if (this.properties == null || this.properties.isEmpty()) {
                return null;
            }
            return argSpec.isOption() ? this.optionDefaultValue((Model.OptionSpec)argSpec) : this.positionalDefaultValue((Model.PositionalParamSpec)argSpec);
        }

        private String optionDefaultValue(Model.OptionSpec option) {
            String result = this.getValue(option.descriptionKey(), option.command());
            result = result != null ? result : this.getValue(PropertiesDefaultProvider.stripPrefix(option.longestName()), option.command());
            return result;
        }

        private static String stripPrefix(String prefixed) {
            for (int i = 0; i < prefixed.length(); ++i) {
                if (!Character.isJavaIdentifierPart(prefixed.charAt(i))) continue;
                return prefixed.substring(i);
            }
            return prefixed;
        }

        private String positionalDefaultValue(Model.PositionalParamSpec positional) {
            String result = this.getValue(positional.descriptionKey(), positional.command());
            result = result != null ? result : this.getValue(positional.paramLabel(), positional.command());
            return result;
        }

        private String getValue(String key, Model.CommandSpec spec) {
            String result = null;
            if (spec != null) {
                String cmd = spec.qualifiedName(".");
                result = this.properties.getProperty(cmd + "." + key);
            }
            if (result != null) {
                return result;
            }
            return key == null ? null : this.properties.getProperty(key);
        }

        public String toString() {
            return this.getClass().getSimpleName() + "[" + this.location + "]";
        }
    }

    public static class MissingTypeConverterException
    extends ParameterException {
        private static final long serialVersionUID = -6050931703233083760L;

        public MissingTypeConverterException(CommandLine commandLine, String msg) {
            super(commandLine, msg);
        }
    }

    public static class OverwrittenOptionException
    extends ParameterException {
        private static final long serialVersionUID = 1338029208271055776L;
        private final Model.ArgSpec overwrittenArg;

        public OverwrittenOptionException(CommandLine commandLine, Model.ArgSpec overwritten, String msg) {
            super(commandLine, msg);
            this.overwrittenArg = overwritten;
        }

        public Model.ArgSpec getOverwritten() {
            return this.overwrittenArg;
        }
    }

    public static class MaxValuesExceededException
    extends ParameterException {
        private static final long serialVersionUID = 6536145439570100641L;

        public MaxValuesExceededException(CommandLine commandLine, String msg) {
            super(commandLine, msg);
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static class UnmatchedArgumentException
    extends ParameterException {
        private static final long serialVersionUID = -8700426380701452440L;
        private List<String> unmatched = Collections.emptyList();

        public UnmatchedArgumentException(CommandLine commandLine, String msg) {
            super(commandLine, msg);
        }

        public UnmatchedArgumentException(CommandLine commandLine, Stack<String> args) {
            this(commandLine, new ArrayList<String>(CommandLine.reverse(args)));
        }

        public UnmatchedArgumentException(CommandLine commandLine, List<String> args) {
            this(commandLine, args, "");
        }

        public UnmatchedArgumentException(CommandLine commandLine, List<String> args, String extraMsg) {
            this(commandLine, UnmatchedArgumentException.describe(Assert.notNull(args, "unmatched list"), commandLine) + ": " + UnmatchedArgumentException.quoteElements(args) + extraMsg);
            this.unmatched = new ArrayList<String>(args);
        }

        public static boolean printSuggestions(ParameterException ex, PrintStream out) {
            return ex instanceof UnmatchedArgumentException && ((UnmatchedArgumentException)ex).printSuggestions(out);
        }

        public static boolean printSuggestions(ParameterException ex, PrintWriter writer) {
            return ex instanceof UnmatchedArgumentException && ((UnmatchedArgumentException)ex).printSuggestions(writer);
        }

        public List<String> getUnmatched() {
            return UnmatchedArgumentException.stripErrorMessage(this.unmatched);
        }

        static List<String> stripErrorMessage(List<String> unmatched) {
            ArrayList<String> result = new ArrayList<String>();
            for (String s : unmatched) {
                if (s == null) {
                    result.add(null);
                    continue;
                }
                int pos = s.indexOf(" (while processing option:");
                result.add(pos < 0 ? s : s.substring(0, pos));
            }
            return Collections.unmodifiableList(result);
        }

        public boolean isUnknownOption() {
            return UnmatchedArgumentException.isUnknownOption(this.unmatched, this.getCommandLine());
        }

        public boolean printSuggestions(PrintStream out) {
            return this.printSuggestions(CommandLine.newPrintWriter(out, CommandLine.getStdoutEncoding()));
        }

        public boolean printSuggestions(PrintWriter writer) {
            List<String> suggestions = this.getSuggestions();
            if (!suggestions.isEmpty()) {
                writer.println(this.isUnknownOption() ? "Possible solutions: " + UnmatchedArgumentException.str(suggestions) : "Did you mean: " + UnmatchedArgumentException.str(suggestions).replace(", ", " or ") + "?");
                writer.flush();
            }
            return !suggestions.isEmpty();
        }

        private static String str(List<String> list) {
            String s = list.toString();
            return s.substring(0, s.length() - 1).substring(1);
        }

        public List<String> getSuggestions() {
            if (this.unmatched.isEmpty()) {
                return Collections.emptyList();
            }
            String arg = this.unmatched.get(0);
            String stripped = Model.CommandSpec.stripPrefix(arg);
            Model.CommandSpec spec = this.getCommandLine().getCommandSpec();
            if (spec.resemblesOption(arg, null)) {
                return spec.findVisibleOptionNamesWithPrefix(stripped.substring(0, Math.min(2, stripped.length())));
            }
            if (!spec.subcommands().isEmpty()) {
                ArrayList<String> visibleSubs = new ArrayList<String>();
                for (Map.Entry<String, CommandLine> entry : spec.subcommands().entrySet()) {
                    if (entry.getValue().getCommandSpec().usageMessage().hidden()) continue;
                    visibleSubs.add(entry.getKey());
                }
                List<String> mostSimilar = CosineSimilarity.mostSimilar(arg, visibleSubs);
                return mostSimilar.subList(0, Math.min(3, mostSimilar.size()));
            }
            return Collections.emptyList();
        }

        private static boolean isUnknownOption(List<String> unmatch, CommandLine cmd) {
            return unmatch != null && !unmatch.isEmpty() && cmd.getCommandSpec().resemblesOption(unmatch.get(0), null);
        }

        private static String describe(List<String> unmatch, CommandLine cmd) {
            String plural;
            String string = plural = unmatch.size() == 1 ? "" : "s";
            if (UnmatchedArgumentException.isUnknownOption(unmatch, cmd)) {
                return "Unknown option" + plural;
            }
            String at = unmatch.size() == 1 ? " at" : " from";
            int index = cmd.interpreter.parseResultBuilder == null ? 0 : cmd.interpreter.parseResultBuilder.firstUnmatchedPosition;
            return "Unmatched argument" + plural + at + " index " + index;
        }

        static String quoteElements(List<String> list) {
            String result = "";
            String suffix = "";
            for (String element : list) {
                int pos;
                if (result.length() > 0) {
                    result = result + ", ";
                }
                if (element != null && (pos = element.indexOf(" (while processing option:")) >= 0) {
                    suffix = element.substring(pos);
                    element = element.substring(0, pos);
                }
                result = result + "'" + element + "'" + suffix;
                suffix = "";
            }
            return result;
        }
    }

    public static class ParameterIndexGapException
    extends InitializationException {
        private static final long serialVersionUID = -1520981133257618319L;

        public ParameterIndexGapException(String msg) {
            super(msg);
        }
    }

    public static class DuplicateOptionAnnotationsException
    extends DuplicateNameException {
        private static final long serialVersionUID = -3355128012575075641L;

        public DuplicateOptionAnnotationsException(String msg) {
            super(msg);
        }

        private static DuplicateOptionAnnotationsException create(String name, Model.ArgSpec argSpec1, Model.ArgSpec argSpec2) {
            return new DuplicateOptionAnnotationsException("Option name '" + name + "' is used by both " + argSpec1.toString() + " and " + argSpec2.toString());
        }
    }

    public static class DuplicateNameException
    extends InitializationException {
        private static final long serialVersionUID = -4126747467955626054L;

        public DuplicateNameException(String msg) {
            super(msg);
        }
    }

    public static class MutuallyExclusiveArgsException
    extends ParameterException {
        private static final long serialVersionUID = -5557715106221420986L;

        public MutuallyExclusiveArgsException(CommandLine commandLine, String msg) {
            super(commandLine, msg);
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static class MissingParameterException
    extends ParameterException {
        private static final long serialVersionUID = 5075678535706338753L;
        private final List<Model.ArgSpec> missing;

        public MissingParameterException(CommandLine commandLine, Model.ArgSpec missing, String msg) {
            this(commandLine, Collections.singletonList(missing), msg);
        }

        public MissingParameterException(CommandLine commandLine, Collection<Model.ArgSpec> missing, String msg) {
            super(commandLine, msg);
            this.missing = Collections.unmodifiableList(new ArrayList<Model.ArgSpec>(missing));
        }

        public List<Model.ArgSpec> getMissing() {
            return this.missing;
        }

        private static MissingParameterException create(CommandLine cmd, Collection<Model.ArgSpec> missing, String separator) {
            String missingArgs = Model.ArgSpec.describe(missing, ", ", separator, "'", "'");
            String types = Model.ArgSpec.describeTypes(missing);
            return new MissingParameterException(cmd, missing, "Missing required " + types + ": " + missingArgs);
        }
    }

    public static class ParameterException
    extends PicocliException {
        private static final long serialVersionUID = 1477112829129763139L;
        private final CommandLine commandLine;
        private Model.ArgSpec argSpec = null;
        private String value = null;

        public ParameterException(CommandLine commandLine, String msg) {
            super(msg);
            this.commandLine = Assert.notNull(commandLine, "commandLine");
        }

        public ParameterException(CommandLine commandLine, String msg, Throwable t) {
            super(msg, t);
            this.commandLine = Assert.notNull(commandLine, "commandLine");
        }

        public ParameterException(CommandLine commandLine, String msg, Throwable t, Model.ArgSpec argSpec, String value) {
            super(msg, t);
            this.commandLine = Assert.notNull(commandLine, "commandLine");
            if (argSpec == null && value == null) {
                throw new IllegalArgumentException("ArgSpec and value cannot both be null");
            }
            this.argSpec = argSpec;
            this.value = value;
        }

        public ParameterException(CommandLine commandLine, String msg, Model.ArgSpec argSpec, String value) {
            super(msg);
            this.commandLine = Assert.notNull(commandLine, "commandLine");
            if (argSpec == null && value == null) {
                throw new IllegalArgumentException("ArgSpec and value cannot both be null");
            }
            this.argSpec = argSpec;
            this.value = value;
        }

        public CommandLine getCommandLine() {
            return this.commandLine;
        }

        public Model.ArgSpec getArgSpec() {
            return this.argSpec;
        }

        public String getValue() {
            return this.value;
        }

        private static ParameterException create(CommandLine cmd, Exception ex, String arg, int i, String[] args) {
            String msg = ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage() + " while processing argument at or before arg[" + i + "] '" + arg + "' in " + Arrays.toString(args) + ": " + ex.toString();
            return new ParameterException(cmd, msg, ex, null, arg);
        }
    }

    public static class TypeConversionException
    extends PicocliException {
        private static final long serialVersionUID = 4251973913816346114L;

        public TypeConversionException(String msg) {
            super(msg);
        }
    }

    public static class ExecutionException
    extends PicocliException {
        private static final long serialVersionUID = 7764539594267007998L;
        private final CommandLine commandLine;

        public ExecutionException(CommandLine commandLine, String msg) {
            super(msg);
            this.commandLine = Assert.notNull(commandLine, "commandLine");
        }

        public ExecutionException(CommandLine commandLine, String msg, Throwable t) {
            super(msg, t);
            this.commandLine = Assert.notNull(commandLine, "commandLine");
        }

        public CommandLine getCommandLine() {
            return this.commandLine;
        }
    }

    public static class InitializationException
    extends PicocliException {
        private static final long serialVersionUID = 8423014001666638895L;

        public InitializationException(String msg) {
            super(msg);
        }

        public InitializationException(String msg, Exception ex) {
            super(msg, ex);
        }
    }

    public static class PicocliException
    extends RuntimeException {
        private static final long serialVersionUID = -2574128880125050818L;

        public PicocliException(String msg) {
            super(msg);
        }

        public PicocliException(String msg, Throwable t) {
            super(msg, t);
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class CosineSimilarity {
        private CosineSimilarity() {
        }

        static List<String> mostSimilar(String pattern, Iterable<String> candidates) {
            return CosineSimilarity.mostSimilar(pattern, candidates, 0.0);
        }

        static List<String> mostSimilar(String pattern, Iterable<String> candidates, double threshold) {
            pattern = pattern.toLowerCase();
            TreeMap<Double, String> sorted = new TreeMap<Double, String>();
            for (String candidate : candidates) {
                double score = CosineSimilarity.similarity(pattern, candidate.toLowerCase(), 2);
                if (!(score > threshold)) continue;
                sorted.put(score, candidate);
            }
            return CommandLine.reverseList(new ArrayList(sorted.values()));
        }

        private static double similarity(String sequence1, String sequence2, int degree) {
            Map<String, Integer> m1 = CosineSimilarity.countNgramFrequency(sequence1, degree);
            Map<String, Integer> m2 = CosineSimilarity.countNgramFrequency(sequence2, degree);
            return CosineSimilarity.dotProduct(m1, m2) / Math.sqrt(CosineSimilarity.dotProduct(m1, m1) * CosineSimilarity.dotProduct(m2, m2));
        }

        private static Map<String, Integer> countNgramFrequency(String sequence, int degree) {
            HashMap<String, Integer> m = new HashMap<String, Integer>();
            int i = 0;
            while (i + degree <= sequence.length()) {
                String gram;
                m.put(gram, 1 + (m.containsKey(gram = sequence.substring(i, i + degree)) ? (Integer)m.get(gram) : 0));
                ++i;
            }
            return m;
        }

        private static double dotProduct(Map<String, Integer> m1, Map<String, Integer> m2) {
            double result = 0.0;
            for (String key : m1.keySet()) {
                result += (double)(m1.get(key) * (m2.containsKey(key) ? m2.get(key) : 0));
            }
            return result;
        }
    }

    static class Tracer {
        TraceLevel level = TraceLevel.lookup(System.getProperty("picocli.trace"));
        PrintStream stream = System.err;

        Tracer() {
        }

        void warn(String msg, Object ... params) {
            TraceLevel.WARN.print(this, msg, params);
        }

        void info(String msg, Object ... params) {
            TraceLevel.INFO.print(this, msg, params);
        }

        void debug(String msg, Object ... params) {
            TraceLevel.DEBUG.print(this, msg, params);
        }

        boolean isWarn() {
            return this.level.isEnabled(TraceLevel.WARN);
        }

        boolean isInfo() {
            return this.level.isEnabled(TraceLevel.INFO);
        }

        boolean isDebug() {
            return this.level.isEnabled(TraceLevel.DEBUG);
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static enum TraceLevel {
        OFF,
        WARN,
        INFO,
        DEBUG;


        public boolean isEnabled(TraceLevel other) {
            return this.ordinal() >= other.ordinal();
        }

        private void print(Tracer tracer, String msg, Object ... params) {
            if (tracer.level.isEnabled(this)) {
                tracer.stream.printf(this.prefix(msg), params);
            }
        }

        private String prefix(String msg) {
            return "[picocli " + (Object)((Object)this) + "] " + msg;
        }

        static TraceLevel lookup(String key) {
            return key == null ? WARN : (CommandLine.empty(key) || "true".equalsIgnoreCase(key) ? INFO : TraceLevel.valueOf(key.toUpperCase(Locale.ENGLISH)));
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class Assert {
        static <T> T notNull(T object, String description) {
            if (object == null) {
                throw new NullPointerException(description);
            }
            return object;
        }

        static boolean equals(Object obj1, Object obj2) {
            return obj1 == null ? obj2 == null : obj1.equals(obj2);
        }

        static int hashCode(Object obj) {
            return obj == null ? 0 : obj.hashCode();
        }

        static int hashCode(boolean bool) {
            return bool ? 1 : 0;
        }

        static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new IllegalStateException(message);
            }
        }

        static void assertTrue(boolean condition, IHelpSectionRenderer producer) {
            if (!condition) {
                throw new IllegalStateException(producer.render(null));
            }
        }

        private Assert() {
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static class Help {
        protected static final String DEFAULT_COMMAND_NAME = "<main class>";
        protected static final String DEFAULT_SEPARATOR = "=";
        public final Model.PositionalParamSpec AT_FILE_POSITIONAL_PARAM = ((Model.PositionalParamSpec.Builder)((Model.PositionalParamSpec.Builder)((Model.PositionalParamSpec.Builder)((Model.PositionalParamSpec.Builder)Model.PositionalParamSpec.builder().paramLabel("${picocli.atfile.label:-@<filename>}")).description("${picocli.atfile.description:-One or more argument files containing options.}")).arity("0..*")).descriptionKey("picocli.atfile")).build();
        public final Model.OptionSpec END_OF_OPTIONS_OPTION = this.createEndOfOptionsOption("--");
        private final Model.CommandSpec commandSpec;
        private final ColorScheme colorScheme;
        private final Map<String, Help> allCommands = new LinkedHashMap<String, Help>();
        private final Map<String, Help> visibleCommands = new LinkedHashMap<String, Help>();
        private List<String> aliases;
        private final IParamLabelRenderer parameterLabelRenderer;

        private Model.OptionSpec createEndOfOptionsOption(String name) {
            return ((Model.OptionSpec.Builder)((Model.OptionSpec.Builder)((Model.OptionSpec.Builder)Model.OptionSpec.builder(name, new String[0]).description("${picocli.endofoptions.description:-This option can be used to separate command-line options from the list of positional parameters.}")).arity("0")).descriptionKey("picocli.endofoptions")).build();
        }

        public Help(Object command) {
            this(command, Ansi.AUTO);
        }

        public Help(Object command, Ansi ansi) {
            this(command, Help.defaultColorScheme(ansi));
        }

        @Deprecated
        public Help(Object command, ColorScheme colorScheme) {
            this(Model.CommandSpec.forAnnotatedObject(command, new DefaultFactory()), colorScheme);
        }

        public Help(Model.CommandSpec commandSpec, ColorScheme colorScheme) {
            this.commandSpec = Assert.notNull(commandSpec, "commandSpec");
            this.aliases = new ArrayList<String>(Arrays.asList(commandSpec.aliases()));
            this.aliases.add(0, commandSpec.name());
            this.colorScheme = new ColorScheme.Builder(colorScheme).applySystemProperties().build();
            this.parameterLabelRenderer = new DefaultParamLabelRenderer(commandSpec);
            this.registerSubcommands(commandSpec.subcommands());
            this.AT_FILE_POSITIONAL_PARAM.commandSpec = commandSpec;
        }

        Help withCommandNames(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        public Model.CommandSpec commandSpec() {
            return this.commandSpec;
        }

        public ColorScheme colorScheme() {
            return this.colorScheme;
        }

        private IHelpFactory getHelpFactory() {
            return this.commandSpec.usageMessage().helpFactory();
        }

        public Map<String, Help> subcommands() {
            return Collections.unmodifiableMap(this.visibleCommands);
        }

        public Map<String, Help> allSubcommands() {
            return Collections.unmodifiableMap(this.allCommands);
        }

        protected List<String> aliases() {
            return Collections.unmodifiableList(this.aliases);
        }

        public IParamLabelRenderer parameterLabelRenderer() {
            return this.parameterLabelRenderer;
        }

        public Help addAllSubcommands(Map<String, CommandLine> subcommands) {
            if (subcommands != null) {
                this.registerSubcommands(subcommands);
            }
            return this;
        }

        private void registerSubcommands(Map<String, CommandLine> subcommands) {
            IdentityHashMap<CommandLine, ArrayList<String>> done = new IdentityHashMap<CommandLine, ArrayList<String>>();
            for (CommandLine commandLine : subcommands.values()) {
                if (done.containsKey(commandLine)) continue;
                done.put(commandLine, new ArrayList<String>(Arrays.asList(commandLine.commandSpec.aliases())));
            }
            for (Map.Entry entry : subcommands.entrySet()) {
                List aliases = (List)done.get(entry.getValue());
                if (aliases.contains(entry.getKey())) continue;
                aliases.add(0, entry.getKey());
            }
            for (Map.Entry entry : subcommands.entrySet()) {
                CommandLine commandLine = (CommandLine)entry.getValue();
                List commandNames = (List)done.remove(commandLine);
                if (commandNames == null) continue;
                String key = commandNames.toString().substring(1, commandNames.toString().length() - 1);
                Help sub = this.getHelpFactory().create(commandLine.commandSpec, this.colorScheme).withCommandNames(commandNames);
                this.allCommands.put(key, sub);
                if (sub.commandSpec().usageMessage().hidden()) continue;
                this.visibleCommands.put(key, sub);
            }
        }

        @Deprecated
        public Help addSubcommand(String commandName, Object command) {
            Help sub = this.getHelpFactory().create(Model.CommandSpec.forAnnotatedObject(command, this.commandSpec.commandLine().factory), Help.defaultColorScheme(Ansi.AUTO));
            this.visibleCommands.put(commandName, sub);
            this.allCommands.put(commandName, sub);
            return this;
        }

        List<Model.OptionSpec> options() {
            return this.commandSpec.options();
        }

        List<Model.PositionalParamSpec> positionalParameters() {
            return this.commandSpec.positionalParameters();
        }

        String commandName() {
            return this.commandSpec.name();
        }

        public String fullSynopsis() {
            return this.synopsisHeading(new Object[0]) + this.synopsis(this.synopsisHeadingLength());
        }

        @Deprecated
        public String synopsis() {
            return this.synopsis(0);
        }

        public String synopsis(int synopsisHeadingLength) {
            if (!CommandLine.empty(this.commandSpec.usageMessage().customSynopsis())) {
                return this.customSynopsis(new Object[0]);
            }
            return this.commandSpec.usageMessage().abbreviateSynopsis() ? this.abbreviatedSynopsis() : this.detailedSynopsis(synopsisHeadingLength, Help.createShortOptionArityAndNameComparator(), true);
        }

        public String abbreviatedSynopsis() {
            StringBuilder sb = new StringBuilder();
            if (!this.commandSpec.optionsMap().isEmpty()) {
                sb.append(" [OPTIONS]");
            }
            for (Model.PositionalParamSpec positionalParam : this.commandSpec.positionalParameters()) {
                if (positionalParam.hidden()) continue;
                sb.append(' ').append(this.parameterLabelRenderer().renderParameterLabel(positionalParam, this.ansi(), this.colorScheme.parameterStyles));
            }
            if (!this.commandSpec.subcommands().isEmpty()) {
                sb.append(" ").append(this.commandSpec.usageMessage().synopsisSubcommandLabel());
            }
            return this.colorScheme.commandText(this.commandSpec.qualifiedName()).toString() + sb.toString() + System.getProperty("line.separator");
        }

        @Deprecated
        public String detailedSynopsis(Comparator<Model.OptionSpec> optionSort, boolean clusterBooleanOptions) {
            return this.detailedSynopsis(0, optionSort, clusterBooleanOptions);
        }

        public String detailedSynopsis(int synopsisHeadingLength, Comparator<Model.OptionSpec> optionSort, boolean clusterBooleanOptions) {
            HashSet<Model.ArgSpec> argsInGroups = new HashSet<Model.ArgSpec>();
            Ansi.Text groupsText = this.createDetailedSynopsisGroupsText(argsInGroups);
            Ansi.Text optionText = this.createDetailedSynopsisOptionsText(argsInGroups, optionSort, clusterBooleanOptions);
            Ansi.Text endOfOptionsText = this.createDetailedSynopsisEndOfOptionsText();
            Ansi.Text positionalParamText = this.createDetailedSynopsisPositionalsText(argsInGroups);
            Ansi.Text commandText = this.createDetailedSynopsisCommandText();
            return this.makeSynopsisFromParts(synopsisHeadingLength, optionText, groupsText, endOfOptionsText, positionalParamText, commandText);
        }

        protected String makeSynopsisFromParts(int synopsisHeadingLength, Ansi.Text optionText, Ansi.Text groupsText, Ansi.Text endOfOptionsText, Ansi.Text positionalParamText, Ansi.Text commandText) {
            boolean positionalsOnly = true;
            for (Model.ArgGroupSpec group : this.commandSpec().argGroups()) {
                if (!group.validate()) continue;
                positionalsOnly &= group.allOptionsNested().isEmpty();
            }
            Ansi.Text text = positionalsOnly ? optionText.concat(endOfOptionsText).concat(groupsText).concat(positionalParamText).concat(commandText) : optionText.concat(groupsText).concat(endOfOptionsText).concat(positionalParamText).concat(commandText);
            return this.insertSynopsisCommandName(synopsisHeadingLength, text);
        }

        protected Ansi.Text createDetailedSynopsisGroupsText(Set<Model.ArgSpec> outparam_groupArgs) {
            Ansi ansi = this.ansi();
            ((Object)((Object)ansi)).getClass();
            Ansi.Text groupText = ansi.new Ansi.Text(0);
            for (Model.ArgGroupSpec group : this.commandSpec().argGroups()) {
                if (!group.validate()) continue;
                groupText = groupText.concat(" ").concat(group.synopsisText(this.colorScheme(), outparam_groupArgs));
            }
            return groupText;
        }

        protected Ansi.Text createDetailedSynopsisOptionsText(Collection<Model.ArgSpec> done, Comparator<Model.OptionSpec> optionSort, boolean clusterBooleanOptions) {
            return this.createDetailedSynopsisOptionsText(done, this.commandSpec.options(), optionSort, clusterBooleanOptions);
        }

        protected Ansi.Text createDetailedSynopsisOptionsText(Collection<Model.ArgSpec> done, List<Model.OptionSpec> optionList, Comparator<Model.OptionSpec> optionSort, boolean clusterBooleanOptions) {
            Ansi ansi = this.ansi();
            ((Object)((Object)ansi)).getClass();
            Ansi.Text optionText = ansi.new Ansi.Text(0);
            ArrayList<Model.OptionSpec> options = new ArrayList<Model.OptionSpec>(optionList);
            if (optionSort != null) {
                Collections.sort(options, optionSort);
            }
            options.removeAll(done);
            if (clusterBooleanOptions) {
                ArrayList<Model.OptionSpec> booleanOptions = new ArrayList<Model.OptionSpec>();
                StringBuilder clusteredRequired = new StringBuilder("-");
                StringBuilder clusteredOptional = new StringBuilder("-");
                for (Model.OptionSpec option : options) {
                    String shortestName;
                    boolean isFlagOption;
                    if (option.hidden() || !(isFlagOption = option.typeInfo().isBoolean()) || option.arity().max > 0 || (shortestName = option.shortestName()).length() != 2 || !shortestName.startsWith("-") || option.negatable() && !shortestName.equals(this.commandSpec.negatableOptionTransformer().makeSynopsis(shortestName, this.commandSpec))) continue;
                    booleanOptions.add(option);
                    if (option.required()) {
                        clusteredRequired.append(shortestName.substring(1));
                        continue;
                    }
                    clusteredOptional.append(shortestName.substring(1));
                }
                options.removeAll(booleanOptions);
                if (clusteredRequired.length() > 1) {
                    optionText = optionText.concat(" ").concat(this.colorScheme.optionText(clusteredRequired.toString()));
                }
                if (clusteredOptional.length() > 1) {
                    optionText = optionText.concat(" [").concat(this.colorScheme.optionText(clusteredOptional.toString())).concat("]");
                }
            }
            for (Model.OptionSpec option : options) {
                optionText = Help.concatOptionText(" ", optionText, this.colorScheme, option, this.parameterLabelRenderer());
            }
            return optionText;
        }

        static Ansi.Text concatOptionText(String prefix, Ansi.Text text, ColorScheme colorScheme, Model.OptionSpec option, IParamLabelRenderer parameterLabelRenderer) {
            if (!option.hidden()) {
                String nameString = option.shortestName();
                if (option.negatable) {
                    RegexTransformer trans = option.commandSpec == null ? RegexTransformer.createDefault() : option.commandSpec.negatableOptionTransformer();
                    nameString = trans.makeSynopsis(option.shortestName(), option.commandSpec);
                }
                Ansi.Text name = colorScheme.optionText(nameString);
                Ansi.Text param = parameterLabelRenderer.renderParameterLabel(option, colorScheme.ansi(), colorScheme.optionParamStyles);
                text = text.concat(prefix);
                if (option.required()) {
                    text = text.concat(name).concat(param).concat("");
                    if (option.isMultiValue()) {
                        text = text.concat(" [").concat(name).concat(param).concat("]...");
                    }
                } else {
                    text = text.concat("[").concat(name).concat(param).concat("]");
                    if (option.isMultiValue()) {
                        text = text.concat("...");
                    }
                }
            }
            return text;
        }

        protected Ansi.Text createDetailedSynopsisEndOfOptionsText() {
            if (!this.commandSpec.usageMessage.showEndOfOptionsDelimiterInUsageHelp()) {
                Ansi ansi = this.ansi();
                ((Object)((Object)ansi)).getClass();
                return ansi.new Ansi.Text(0);
            }
            Ansi ansi = this.ansi();
            ((Object)((Object)ansi)).getClass();
            return ansi.new Ansi.Text(0).concat(" [").concat(this.colorScheme.optionText(this.commandSpec.parser().endOfOptionsDelimiter())).concat("]");
        }

        protected Ansi.Text createDetailedSynopsisPositionalsText(Collection<Model.ArgSpec> done) {
            Ansi ansi = this.ansi();
            ((Object)((Object)ansi)).getClass();
            Ansi.Text positionalParamText = ansi.new Ansi.Text(0);
            ArrayList<Model.PositionalParamSpec> positionals = new ArrayList<Model.PositionalParamSpec>(this.commandSpec.positionalParameters());
            if (this.hasAtFileParameter()) {
                positionals.add(0, this.AT_FILE_POSITIONAL_PARAM);
                this.AT_FILE_POSITIONAL_PARAM.messages(this.commandSpec.usageMessage().messages());
            }
            positionals.removeAll(done);
            for (Model.PositionalParamSpec positionalParam : positionals) {
                positionalParamText = Help.concatPositionalText(" ", positionalParamText, this.colorScheme, positionalParam, this.parameterLabelRenderer());
            }
            return positionalParamText;
        }

        static Ansi.Text concatPositionalText(String prefix, Ansi.Text text, ColorScheme colorScheme, Model.PositionalParamSpec positionalParam, IParamLabelRenderer parameterLabelRenderer) {
            if (!positionalParam.hidden()) {
                Ansi.Text label = parameterLabelRenderer.renderParameterLabel(positionalParam, colorScheme.ansi(), colorScheme.parameterStyles);
                text = text.concat(prefix).concat(label);
            }
            return text;
        }

        protected Ansi.Text createDetailedSynopsisCommandText() {
            Ansi ansi = this.ansi();
            ((Object)((Object)ansi)).getClass();
            Ansi.Text commandText = ansi.new Ansi.Text(0);
            if (!this.commandSpec.subcommands().isEmpty()) {
                return commandText.concat(" ").concat(this.commandSpec.usageMessage().synopsisSubcommandLabel());
            }
            return commandText;
        }

        protected String insertSynopsisCommandName(int synopsisHeadingLength, Ansi.Text optionsAndPositionalsAndCommandsDetails) {
            if (synopsisHeadingLength < 0) {
                throw new IllegalArgumentException("synopsisHeadingLength must be a positive number but was " + synopsisHeadingLength);
            }
            String commandName = this.commandSpec.qualifiedName();
            int indent = synopsisHeadingLength + commandName.length() + 1;
            if ((double)indent > this.commandSpec.usageMessage().synopsisAutoIndentThreshold() * (double)this.width()) {
                indent = this.commandSpec.usageMessage().synopsisIndent() < 0 ? synopsisHeadingLength : this.commandSpec.usageMessage().synopsisIndent();
                indent = Math.min(indent, (int)(0.9 * (double)this.width()));
            }
            TextTable textTable = TextTable.forColumnWidths(this.colorScheme, this.width());
            textTable.setAdjustLineBreaksForWideCJKCharacters(this.commandSpec.usageMessage().adjustLineBreaksForWideCJKCharacters());
            textTable.indentWrappedLines = indent;
            Ansi ansi = Ansi.OFF;
            ((Object)((Object)ansi)).getClass();
            Ansi.Text PADDING = ansi.new Ansi.Text(Help.stringOf('X', synopsisHeadingLength), optionsAndPositionalsAndCommandsDetails.colorScheme);
            textTable.addRowValues(PADDING.concat(this.colorScheme.commandText(commandName)).concat(optionsAndPositionalsAndCommandsDetails));
            return textTable.toString().substring(synopsisHeadingLength);
        }

        public int synopsisHeadingLength() {
            Ansi ansi = Ansi.OFF;
            ((Object)((Object)ansi)).getClass();
            String[] lines = ansi.new Ansi.Text(this.commandSpec.usageMessage().synopsisHeading()).toString().split("\\r?\\n|\\r|%n", -1);
            return lines[lines.length - 1].length();
        }

        private List<Model.OptionSpec> excludeHiddenAndGroupOptions(List<Model.OptionSpec> all) {
            ArrayList<Model.OptionSpec> result = new ArrayList<Model.OptionSpec>(all);
            for (Model.ArgGroupSpec group : this.optionSectionGroups()) {
                result.removeAll(group.allOptionsNested());
            }
            Iterator iter = result.iterator();
            while (iter.hasNext()) {
                if (!((Model.OptionSpec)iter.next()).hidden()) continue;
                iter.remove();
            }
            return result;
        }

        private List<Model.PositionalParamSpec> excludeHiddenAndGroupParams(List<Model.PositionalParamSpec> all) {
            ArrayList<Model.PositionalParamSpec> result = new ArrayList<Model.PositionalParamSpec>(all);
            for (Model.ArgGroupSpec group : this.optionSectionGroups()) {
                result.removeAll(group.allPositionalParametersNested());
            }
            Iterator iter = result.iterator();
            while (iter.hasNext()) {
                if (!((Model.PositionalParamSpec)iter.next()).hidden()) continue;
                iter.remove();
            }
            return result;
        }

        private static Comparator<Model.OptionSpec> createOrderComparatorIfNecessary(List<Model.OptionSpec> options) {
            for (Model.OptionSpec option : options) {
                if (option.order() == -1) continue;
                return Help.createOrderComparator();
            }
            return null;
        }

        public Comparator<Model.OptionSpec> createDefaultOptionSort() {
            return this.commandSpec.usageMessage().sortOptions() ? Help.createShortOptionNameComparator() : Help.createOrderComparatorIfNecessary(this.commandSpec.options());
        }

        public String optionList() {
            return this.optionList(this.createDefaultLayout(), this.createDefaultOptionSort(), this.parameterLabelRenderer());
        }

        public String optionListExcludingGroups(List<Model.OptionSpec> options) {
            return this.optionListExcludingGroups(options, this.createDefaultLayout(), this.createDefaultOptionSort(), this.parameterLabelRenderer());
        }

        public String optionList(Layout layout, Comparator<Model.OptionSpec> optionSort, IParamLabelRenderer valueLabelRenderer) {
            List<Model.OptionSpec> visibleOptionsNotInGroups = this.excludeHiddenAndGroupOptions(this.options());
            return this.optionListExcludingGroups(visibleOptionsNotInGroups, layout, optionSort, valueLabelRenderer) + this.optionListGroupSections();
        }

        public String optionListExcludingGroups(List<Model.OptionSpec> optionList, Layout layout, Comparator<Model.OptionSpec> optionSort, IParamLabelRenderer valueLabelRenderer) {
            ArrayList<Model.OptionSpec> options = new ArrayList<Model.OptionSpec>(optionList);
            if (optionSort != null) {
                Collections.sort(options, optionSort);
            }
            layout.addAllOptions(options, valueLabelRenderer);
            return layout.toString();
        }

        public String optionListGroupSections() {
            return this.optionListGroupSections(this.optionSectionGroups(), this.createDefaultOptionSort(), this.parameterLabelRenderer());
        }

        private String optionListGroupSections(List<Model.ArgGroupSpec> groupList, Comparator<Model.OptionSpec> optionSort, IParamLabelRenderer paramLabelRenderer) {
            HashSet<Model.ArgSpec> done = new HashSet<Model.ArgSpec>();
            ArrayList<Model.ArgGroupSpec> groups = new ArrayList<Model.ArgGroupSpec>(groupList);
            Collections.sort(groups, new SortByOrder());
            StringBuilder sb = new StringBuilder();
            for (Model.ArgGroupSpec group : groups) {
                ArrayList<Model.OptionSpec> groupOptions = new ArrayList<Model.OptionSpec>(group.allOptionsNested());
                if (optionSort != null) {
                    Collections.sort(groupOptions, optionSort);
                }
                groupOptions.removeAll(done);
                done.addAll(groupOptions);
                ArrayList<Model.PositionalParamSpec> groupPositionals = new ArrayList<Model.PositionalParamSpec>(group.allPositionalParametersNested());
                groupPositionals.removeAll(done);
                done.addAll(groupPositionals);
                Layout groupLayout = this.createDefaultLayout();
                groupLayout.addPositionalParameters(groupPositionals, paramLabelRenderer);
                groupLayout.addOptions(groupOptions, paramLabelRenderer);
                sb.append(this.createHeading(group.heading(), new Object[0]));
                sb.append(groupLayout);
            }
            return sb.toString();
        }

        public List<Model.ArgGroupSpec> optionSectionGroups() {
            ArrayList<Model.ArgGroupSpec> result = new ArrayList<Model.ArgGroupSpec>();
            Help.optionSectionGroups(this.commandSpec.argGroups(), result);
            return result;
        }

        private static void optionSectionGroups(List<Model.ArgGroupSpec> groups, List<Model.ArgGroupSpec> result) {
            for (Model.ArgGroupSpec group : groups) {
                Help.optionSectionGroups(group.subgroups(), result);
                if (group.heading() == null) continue;
                result.add(group);
            }
        }

        public String parameterList() {
            return this.parameterList(this.excludeHiddenAndGroupParams(this.positionalParameters()));
        }

        public String parameterList(List<Model.PositionalParamSpec> positionalParams) {
            return this.parameterList(positionalParams, this.createDefaultLayout(), this.parameterLabelRenderer());
        }

        public String parameterList(Layout layout, IParamLabelRenderer paramLabelRenderer) {
            return this.parameterList(this.excludeHiddenAndGroupParams(this.positionalParameters()), layout, paramLabelRenderer);
        }

        public String parameterList(List<Model.PositionalParamSpec> positionalParams, Layout layout, IParamLabelRenderer paramLabelRenderer) {
            layout.addAllPositionalParameters(positionalParams, paramLabelRenderer);
            return layout.toString();
        }

        public boolean hasAtFileParameter() {
            return this.commandSpec.parser.expandAtFiles() && this.commandSpec.usageMessage.showAtFileInUsageHelp();
        }

        public String atFileParameterList() {
            if (this.hasAtFileParameter()) {
                this.AT_FILE_POSITIONAL_PARAM.messages(this.commandSpec.usageMessage().messages());
                Layout layout = this.createDefaultLayout();
                layout.addPositionalParameter(this.AT_FILE_POSITIONAL_PARAM, this.parameterLabelRenderer());
                return layout.toString();
            }
            return "";
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public String endOfOptionsList() {
            if (!this.commandSpec.usageMessage.showEndOfOptionsDelimiterInUsageHelp()) {
                return "";
            }
            Model.OptionSpec endOfOptionsOption = "--".equals(this.commandSpec.parser().endOfOptionsDelimiter()) ? this.END_OF_OPTIONS_OPTION : this.createEndOfOptionsOption(this.commandSpec.parser().endOfOptionsDelimiter());
            endOfOptionsOption.commandSpec = this.commandSpec;
            try {
                endOfOptionsOption.messages(this.commandSpec.usageMessage().messages());
                Layout layout = this.createDefaultLayout();
                layout.addOption(endOfOptionsOption, this.parameterLabelRenderer());
                String string = layout.toString();
                return string;
            }
            finally {
                endOfOptionsOption.commandSpec = null;
            }
        }

        private static String heading(Ansi ansi, int usageWidth, boolean adjustCJK, String values, Object ... params) {
            StringBuilder sb = Help.join(ansi, usageWidth, adjustCJK, new String[]{values}, new StringBuilder(), params);
            return Help.trimLineSeparator(sb.toString()) + new String(Help.spaces(Help.countTrailingSpaces(values)));
        }

        static String trimLineSeparator(String result) {
            return result.endsWith(System.getProperty("line.separator")) ? result.substring(0, result.length() - System.getProperty("line.separator").length()) : result;
        }

        private static char[] spaces(int length) {
            char[] result = new char[length];
            Arrays.fill(result, ' ');
            return result;
        }

        private static int countTrailingSpaces(String str) {
            if (str == null) {
                return 0;
            }
            int trailingSpaces = 0;
            for (int i = str.length() - 1; i >= 0 && str.charAt(i) == ' '; --i) {
                ++trailingSpaces;
            }
            return trailingSpaces;
        }

        @Deprecated
        public static StringBuilder join(Ansi ansi, int usageHelpWidth, String[] values, StringBuilder sb, Object ... params) {
            return Help.join(ansi, usageHelpWidth, Model.UsageMessageSpec.DEFAULT_ADJUST_CJK, values, sb, params);
        }

        public static StringBuilder join(Ansi ansi, int usageHelpWidth, boolean adjustCJK, String[] values, StringBuilder sb, Object ... params) {
            if (values != null) {
                TextTable table = TextTable.forColumnWidths(ansi, usageHelpWidth);
                table.setAdjustLineBreaksForWideCJKCharacters(adjustCJK);
                table.indentWrappedLines = 0;
                for (String summaryLine : values) {
                    table.addRowValues(CommandLine.format(summaryLine, params));
                }
                table.toString(sb);
            }
            return sb;
        }

        private int width() {
            return this.commandSpec.usageMessage().width();
        }

        private boolean adjustCJK() {
            return this.commandSpec.usageMessage().adjustLineBreaksForWideCJKCharacters();
        }

        public String customSynopsis(Object ... params) {
            return Help.join(this.ansi(), this.width(), this.adjustCJK(), this.commandSpec.usageMessage().customSynopsis(), new StringBuilder(), params).toString();
        }

        public String description(Object ... params) {
            return Help.join(this.ansi(), this.width(), this.adjustCJK(), this.commandSpec.usageMessage().description(), new StringBuilder(), params).toString();
        }

        public String header(Object ... params) {
            return Help.join(this.ansi(), this.width(), this.adjustCJK(), this.commandSpec.usageMessage().header(), new StringBuilder(), params).toString();
        }

        public String footer(Object ... params) {
            return Help.join(this.ansi(), this.width(), this.adjustCJK(), this.commandSpec.usageMessage().footer(), new StringBuilder(), params).toString();
        }

        public String headerHeading(Object ... params) {
            return this.createHeading(this.commandSpec.usageMessage().headerHeading(), params);
        }

        public String synopsisHeading(Object ... params) {
            return this.createHeading(this.commandSpec.usageMessage().synopsisHeading(), params);
        }

        public String descriptionHeading(Object ... params) {
            return CommandLine.empty(this.commandSpec.usageMessage().descriptionHeading()) ? "" : this.createHeading(this.commandSpec.usageMessage().descriptionHeading(), params);
        }

        public String parameterListHeading(Object ... params) {
            if (this.hasAtFileParameter() || !this.commandSpec.positionalParameters().isEmpty()) {
                return this.createHeading(this.commandSpec.usageMessage().parameterListHeading(), params);
            }
            return "";
        }

        public String optionListHeading(Object ... params) {
            boolean hasVisibleOption = false;
            for (Model.OptionSpec option : this.commandSpec.options()) {
                hasVisibleOption |= !option.hidden();
            }
            if (this.commandSpec.usageMessage().showEndOfOptionsDelimiterInUsageHelp() || hasVisibleOption) {
                return this.createHeading(this.commandSpec.usageMessage().optionListHeading(), params);
            }
            return "";
        }

        public String commandListHeading(Object ... params) {
            return this.visibleCommands.isEmpty() ? "" : this.createHeading(this.commandSpec.usageMessage().commandListHeading(), params);
        }

        public String footerHeading(Object ... params) {
            return this.createHeading(this.commandSpec.usageMessage().footerHeading(), params);
        }

        public String exitCodeListHeading(Object ... params) {
            return this.createHeading(this.commandSpec.usageMessage().exitCodeListHeading(), params);
        }

        public String exitCodeList() {
            return this.createTextTable(this.commandSpec.usageMessage().exitCodeList()).toString();
        }

        public String createHeading(String text, Object ... params) {
            return Help.heading(this.ansi(), this.width(), this.adjustCJK(), text, params);
        }

        public TextTable createTextTable(Map<?, ?> map) {
            if (map == null || map.isEmpty()) {
                return TextTable.forColumnWidths(this.colorScheme, 10, this.width() - 10);
            }
            int spacing = 3;
            int indent = 2;
            int keyLength = Math.min(this.width() - spacing - 1, Help.maxLength(map.keySet()));
            TextTable textTable = TextTable.forColumns(this.ansi(), new Column(keyLength + spacing, indent, Column.Overflow.SPAN), new Column(this.width() - (keyLength + spacing), indent, Column.Overflow.WRAP));
            textTable.setAdjustLineBreaksForWideCJKCharacters(this.adjustCJK());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                textTable.addRowValues(CommandLine.format(String.valueOf(entry.getKey()), new Object[0]), CommandLine.format(String.valueOf(entry.getValue()), new Object[0]));
            }
            return textTable;
        }

        public String commandList() {
            return this.commandList(this.subcommands());
        }

        public String commandList(Map<String, Help> subcommands) {
            if (subcommands.isEmpty()) {
                return "";
            }
            int commandLength = Help.maxLength(subcommands.keySet());
            TextTable textTable = TextTable.forColumns(this.colorScheme().ansi(), new Column(commandLength + 2, 2, Column.Overflow.SPAN), new Column(this.width() - (commandLength + 2), 2, Column.Overflow.WRAP));
            textTable.setAdjustLineBreaksForWideCJKCharacters(this.adjustCJK());
            for (Map.Entry<String, Help> entry : subcommands.entrySet()) {
                Help help = entry.getValue();
                Model.UsageMessageSpec usage = help.commandSpec().usageMessage();
                String header = !CommandLine.empty(usage.header()) ? usage.header()[0] : (!CommandLine.empty(usage.description()) ? usage.description()[0] : "");
                Ansi.Text[] lines = this.colorScheme().text(CommandLine.format(header, new Object[0])).splitLines();
                for (int i = 0; i < lines.length; ++i) {
                    textTable.addRowValues(i == 0 ? help.commandNamesText(", ") : Ansi.EMPTY_TEXT, lines[i]);
                }
            }
            return textTable.toString();
        }

        private static int maxLength(Collection<?> any) {
            int result = 0;
            for (Object value : any) {
                result = Math.max(result, String.valueOf(value).length());
            }
            return result;
        }

        public Ansi.Text commandNamesText(String separator) {
            Ansi.Text result = this.colorScheme().commandText(this.aliases().get(0));
            for (int i = 1; i < this.aliases().size(); ++i) {
                result = result.concat(separator).concat(this.colorScheme().commandText(this.aliases().get(i)));
            }
            return result;
        }

        private static String join(String[] names, int offset, int length, String separator) {
            if (names == null) {
                return "";
            }
            StringBuilder result = new StringBuilder();
            for (int i = offset; i < offset + length; ++i) {
                result.append(i > offset ? separator : "").append(names[i]);
            }
            return result.toString();
        }

        private static String stringOf(char chr, int length) {
            char[] buff = new char[length];
            Arrays.fill(buff, chr);
            return new String(buff);
        }

        public Layout createDefaultLayout() {
            return this.createDefaultLayout(this.options(), this.positionalParameters(), this.colorScheme());
        }

        public Layout createDefaultLayout(List<Model.OptionSpec> options, List<Model.PositionalParamSpec> positionals, ColorScheme aColorScheme) {
            return this.createLayout(this.calcLongOptionColumnWidth(options, positionals, aColorScheme), aColorScheme);
        }

        private Layout createLayout(int longOptionsColumnWidth, ColorScheme aColorScheme) {
            TextTable tt = TextTable.forDefaultColumns(aColorScheme, longOptionsColumnWidth, this.width());
            tt.setAdjustLineBreaksForWideCJKCharacters(this.commandSpec.usageMessage().adjustLineBreaksForWideCJKCharacters());
            return new Layout(aColorScheme, tt, this.createDefaultOptionRenderer(), this.createDefaultParameterRenderer());
        }

        public int calcLongOptionColumnWidth(List<Model.OptionSpec> options, List<Model.PositionalParamSpec> positionals, ColorScheme aColorScheme) {
            int max = 0;
            IOptionRenderer optionRenderer = this.createDefaultOptionRenderer();
            boolean cjk = this.commandSpec.usageMessage().adjustLineBreaksForWideCJKCharacters();
            int longOptionsColWidth = this.commandSpec.usageMessage().longOptionsMaxWidth() + 1;
            for (Model.OptionSpec option : options) {
                if (option.hidden()) continue;
                Ansi.Text[][] values = optionRenderer.render(option, this.parameterLabelRenderer(), aColorScheme);
                int len = cjk ? values[0][3].getCJKAdjustedLength() : values[0][3].length;
                if (len >= longOptionsColWidth) continue;
                max = Math.max(max, len);
            }
            ArrayList<Model.PositionalParamSpec> positionalsWithAtFile = new ArrayList<Model.PositionalParamSpec>(positionals);
            if (this.hasAtFileParameter()) {
                positionalsWithAtFile.add(0, this.AT_FILE_POSITIONAL_PARAM);
                this.AT_FILE_POSITIONAL_PARAM.messages(this.commandSpec.usageMessage().messages());
            }
            for (Model.PositionalParamSpec positional : positionalsWithAtFile) {
                if (positional.hidden()) continue;
                Ansi.Text label = this.parameterLabelRenderer().renderParameterLabel(positional, aColorScheme.ansi(), aColorScheme.parameterStyles);
                int len = cjk ? label.getCJKAdjustedLength() : label.length;
                if (len >= longOptionsColWidth) continue;
                max = Math.max(max, len);
            }
            return max + 3;
        }

        public IOptionRenderer createDefaultOptionRenderer() {
            return new DefaultOptionRenderer(this.commandSpec.usageMessage.showDefaultValues(), "" + this.commandSpec.usageMessage().requiredOptionMarker());
        }

        public static IOptionRenderer createMinimalOptionRenderer() {
            return new MinimalOptionRenderer();
        }

        public IParameterRenderer createDefaultParameterRenderer() {
            return new DefaultParameterRenderer(this.commandSpec.usageMessage.showDefaultValues(), "" + this.commandSpec.usageMessage().requiredOptionMarker());
        }

        public static IParameterRenderer createMinimalParameterRenderer() {
            return new MinimalParameterRenderer();
        }

        public static IParamLabelRenderer createMinimalParamLabelRenderer() {
            return new IParamLabelRenderer(){

                @Override
                public Ansi.Text renderParameterLabel(Model.ArgSpec argSpec, Ansi ansi, List<Ansi.IStyle> styles) {
                    return argSpec.command() != null && argSpec.command().commandLine() != null ? argSpec.command().commandLine().getColorScheme().apply(argSpec.paramLabel(), styles) : ansi.apply(argSpec.paramLabel(), styles);
                }

                @Override
                public String separator() {
                    return "";
                }
            };
        }

        public IParamLabelRenderer createDefaultParamLabelRenderer() {
            return new DefaultParamLabelRenderer(this.commandSpec);
        }

        public static Comparator<Model.OptionSpec> createShortOptionNameComparator() {
            return new SortByShortestOptionNameAlphabetically();
        }

        public static Comparator<Model.OptionSpec> createShortOptionArityAndNameComparator() {
            return new SortByOptionArityAndNameAlphabetically();
        }

        public static Comparator<String> shortestFirst() {
            return new ShortestFirst();
        }

        static Comparator<Model.OptionSpec> createOrderComparator() {
            return new SortByOrder<Model.OptionSpec>();
        }

        public Ansi ansi() {
            return this.colorScheme.ansi;
        }

        private static void addTrailingDefaultLine(List<Ansi.Text[]> result, Model.ArgSpec arg, ColorScheme scheme) {
            Ansi.Text EMPTY = Ansi.EMPTY_TEXT;
            Ansi.Text[] textArray = new Ansi.Text[5];
            textArray[0] = EMPTY;
            textArray[1] = EMPTY;
            textArray[2] = EMPTY;
            textArray[3] = EMPTY;
            Ansi ansi = scheme.ansi();
            ((Object)((Object)ansi)).getClass();
            textArray[4] = ansi.new Ansi.Text("  Default: " + arg.defaultValueString(true), scheme);
            result.add(textArray);
        }

        private static Ansi.Text[] createDescriptionFirstLines(ColorScheme scheme, Model.ArgSpec arg, String[] description, boolean[] showDefault) {
            Ansi ansi = scheme.ansi();
            ((Object)((Object)ansi)).getClass();
            Ansi.Text[] result = ansi.new Ansi.Text(CommandLine.str(description, 0), scheme).splitLines();
            if (result.length == 0 || result.length == 1 && result[0].plain.length() == 0) {
                if (showDefault[0]) {
                    Ansi.Text[] textArray = new Ansi.Text[1];
                    Ansi ansi2 = scheme.ansi();
                    ((Object)((Object)ansi2)).getClass();
                    textArray[0] = ansi2.new Ansi.Text("  Default: " + arg.defaultValueString(true), scheme);
                    result = textArray;
                    showDefault[0] = false;
                } else {
                    result = new Ansi.Text[]{Ansi.EMPTY_TEXT};
                }
            }
            return result;
        }

        public static ColorScheme defaultColorScheme(Ansi ansi) {
            return new ColorScheme.Builder(ansi).commands(Ansi.Style.bold).options(Ansi.Style.fg_yellow).parameters(Ansi.Style.fg_yellow).optionParams(Ansi.Style.italic).errors(Ansi.Style.fg_red, Ansi.Style.bold).stackTraces(Ansi.Style.italic).build();
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static enum Ansi {
            AUTO,
            ON,
            OFF;

            static Text EMPTY_TEXT;
            static Boolean tty;

            static boolean isTTY() {
                if (tty == null) {
                    tty = Ansi.calcTTY();
                }
                return tty;
            }

            static final boolean isWindows() {
                return System.getProperty("os.name").toLowerCase().contains("win");
            }

            static final boolean isMac() {
                return System.getProperty("os.name").toLowerCase().contains("mac");
            }

            static final boolean isXterm() {
                return System.getenv("TERM") != null && System.getenv("TERM").startsWith("xterm");
            }

            static final boolean isCygwin() {
                return System.getenv("TERM") != null && System.getenv("TERM").toLowerCase(Locale.ENGLISH).contains("cygwin");
            }

            static final boolean hasOsType() {
                return System.getenv("OSTYPE") != null;
            }

            static final boolean hintDisabled() {
                return "0".equals(System.getenv("CLICOLOR")) || "OFF".equals(System.getenv("ConEmuANSI"));
            }

            static final boolean hintEnabled() {
                return System.getenv("ANSICON") != null || "1".equals(System.getenv("CLICOLOR")) || "ON".equals(System.getenv("ConEmuANSI"));
            }

            static final boolean forceDisabled() {
                return System.getenv("NO_COLOR") != null;
            }

            static final boolean forceEnabled() {
                return System.getenv("CLICOLOR_FORCE") != null && !"0".equals(System.getenv("CLICOLOR_FORCE"));
            }

            static boolean calcTTY() {
                try {
                    return System.class.getDeclaredMethod("console", new Class[0]).invoke(null, new Object[0]) != null;
                }
                catch (Throwable reflectionFailed) {
                    return true;
                }
            }

            static boolean isPseudoTTY() {
                return Ansi.isWindows() && (Ansi.isXterm() || Ansi.isCygwin() || Ansi.hasOsType());
            }

            static boolean ansiPossible() {
                if (Ansi.forceDisabled()) {
                    return false;
                }
                if (Ansi.forceEnabled()) {
                    return true;
                }
                if (Ansi.isWindows() && Ansi.isJansiConsoleInstalled()) {
                    return true;
                }
                if (Ansi.hintDisabled()) {
                    return false;
                }
                if (!Ansi.isTTY() && !Ansi.isPseudoTTY()) {
                    return false;
                }
                return Ansi.hintEnabled() || !Ansi.isWindows() || Ansi.isXterm() || Ansi.isCygwin() || Ansi.hasOsType();
            }

            static boolean isJansiConsoleInstalled() {
                try {
                    if (Boolean.getBoolean("org.fusesource.jansi.Ansi.disable")) {
                        return false;
                    }
                    Class<?> ansi = Class.forName("org.fusesource.jansi.Ansi");
                    Boolean enabled = (Boolean)ansi.getDeclaredMethod("isEnabled", new Class[0]).invoke(null, new Object[0]);
                    if (!enabled.booleanValue()) {
                        return false;
                    }
                    Class<?> ansiConsole = Class.forName("org.fusesource.jansi.AnsiConsole");
                    Field out = ansiConsole.getField("out");
                    return out.get(null) == System.out;
                }
                catch (Exception reflectionFailed) {
                    return false;
                }
            }

            public boolean enabled() {
                boolean tty;
                if (this == ON) {
                    return true;
                }
                if (this == OFF) {
                    return false;
                }
                String ansi = System.getProperty("picocli.ansi");
                boolean auto = ansi == null || "AUTO".equalsIgnoreCase(ansi);
                boolean bl = tty = "TTY".equalsIgnoreCase(ansi) && (Ansi.isTTY() || Ansi.isPseudoTTY());
                return auto ? Ansi.ansiPossible() : tty || Boolean.getBoolean("picocli.ansi");
            }

            public Text text(String stringWithMarkup) {
                return new Text(stringWithMarkup);
            }

            public String string(String stringWithMarkup) {
                return new Text(stringWithMarkup).toString();
            }

            public static Ansi valueOf(boolean enabled) {
                return enabled ? ON : OFF;
            }

            @Deprecated
            public Text apply(String plainText, List<IStyle> styles) {
                return Help.defaultColorScheme(this).apply(plainText, styles);
            }

            static {
                Ansi ansi = OFF;
                ((Object)((Object)ansi)).getClass();
                EMPTY_TEXT = ansi.new Text(0);
            }

            public class Text
            implements Cloneable {
                private final int maxLength;
                private int from;
                private int length;
                private StringBuilder plain = new StringBuilder();
                private List<StyledSection> sections = new ArrayList<StyledSection>();
                private ColorScheme colorScheme;

                public Text(int maxLength) {
                    this(maxLength, Help.defaultColorScheme(this$0));
                }

                public Text(int maxLength, ColorScheme colorScheme) {
                    this.maxLength = maxLength;
                    this.colorScheme = colorScheme;
                }

                public Text(Text other) {
                    this.maxLength = other.maxLength;
                    this.from = other.from;
                    this.length = other.length;
                    this.plain = new StringBuilder(other.plain);
                    this.sections = new ArrayList<StyledSection>(other.sections);
                    this.colorScheme = other.colorScheme;
                }

                public Text(String input) {
                    this(input, Help.defaultColorScheme(this$0));
                }

                public Text(String input, ColorScheme colorScheme) {
                    this.colorScheme = colorScheme;
                    this.maxLength = -1;
                    this.plain.setLength(0);
                    int i = 0;
                    while (true) {
                        int j;
                        if ((j = input.indexOf("@|", i)) == -1) {
                            if (i == 0) {
                                this.plain.append(input);
                                this.length = this.plain.length();
                                return;
                            }
                            this.plain.append(input.substring(i));
                            this.length = this.plain.length();
                            return;
                        }
                        this.plain.append(input, i, j);
                        int k = input.indexOf("|@", j);
                        if (k == -1) {
                            this.plain.append(input);
                            this.length = this.plain.length();
                            return;
                        }
                        String spec = input.substring(j += 2, k);
                        String[] items = spec.split(" ", 2);
                        if (items.length == 1) {
                            this.plain.append(input);
                            this.length = this.plain.length();
                            return;
                        }
                        Object[] styles = colorScheme.parse(items[0]);
                        this.addStyledSection(this.plain.length(), items[1].length(), Style.on((IStyle[])styles), Style.off((IStyle[])CommandLine.reverseArray(styles)) + colorScheme.resetStyle().off());
                        this.plain.append(items[1]);
                        i = k + 2;
                    }
                }

                private void addStyledSection(int start, int length, String startStyle, String endStyle) {
                    this.sections.add(new StyledSection(start, length, startStyle, endStyle));
                }

                public Object clone() {
                    return new Text(this);
                }

                public Text[] splitLines() {
                    ArrayList<Text> result = new ArrayList<Text>();
                    int start = 0;
                    int end = 0;
                    int i = 0;
                    while (i < this.plain.length()) {
                        boolean eol;
                        char c = this.plain.charAt(i);
                        boolean bl = eol = c == '\n';
                        if (c == '\r' && i + 1 < this.plain.length() && this.plain.charAt(i + 1) == '\n') {
                            eol = true;
                            ++i;
                        }
                        if (eol |= c == '\r') {
                            result.add(this.substring(start, end));
                            start = i + 1;
                        }
                        end = ++i;
                    }
                    result.add(this.substring(start, this.plain.length()));
                    return result.toArray(new Text[result.size()]);
                }

                public Text substring(int start) {
                    return this.substring(start, this.length);
                }

                public Text substring(int start, int end) {
                    Text result = (Text)this.clone();
                    result.from = this.from + start;
                    result.length = end - start;
                    result.sections.clear();
                    for (StyledSection section : this.sections) {
                        if (section.startIndex >= result.from + result.length || section.startIndex + section.length <= result.from) continue;
                        result.sections.add(section);
                    }
                    return result;
                }

                @Deprecated
                public Text append(String string) {
                    return this.concat(string);
                }

                @Deprecated
                public Text append(Text text) {
                    return this.concat(text);
                }

                public Text concat(String string) {
                    return this.concat(new Text(string, this.colorScheme));
                }

                public Text concat(Text other) {
                    Text result = (Text)this.clone();
                    result.plain = new StringBuilder(this.plain.toString().substring(this.from, this.from + this.length));
                    result.from = 0;
                    result.sections = new ArrayList<StyledSection>();
                    for (StyledSection section : this.sections) {
                        result.sections.add(section.withStartIndex(section.startIndex - this.from));
                    }
                    result.plain.append(other.plain.toString(), other.from, other.from + other.length);
                    for (StyledSection section : other.sections) {
                        int index = result.length + section.startIndex - other.from;
                        result.sections.add(section.withStartIndex(index));
                    }
                    result.length = result.plain.length();
                    return result;
                }

                public void getStyledChars(int from, int length, Text destination, int offset) {
                    if (destination.length < offset) {
                        for (int i = destination.length; i < offset; ++i) {
                            destination.plain.append(' ');
                        }
                        destination.length = offset;
                    }
                    for (StyledSection section : this.sections) {
                        if (section.startIndex - from + section.length < 0) continue;
                        destination.sections.add(section.withStartIndex(section.startIndex - from + destination.length));
                    }
                    destination.plain.append(this.plain.toString(), from, from + length);
                    destination.length = destination.plain.length();
                }

                public String plainString() {
                    return this.plain.toString().substring(this.from, this.from + this.length);
                }

                public boolean equals(Object obj) {
                    return this.toString().equals(String.valueOf(obj));
                }

                public int hashCode() {
                    return this.toString().hashCode();
                }

                public String toString() {
                    if (!Ansi.this.enabled()) {
                        return this.plain.toString().substring(this.from, this.from + this.length);
                    }
                    if (this.length == 0) {
                        return "";
                    }
                    StringBuilder sb = new StringBuilder(this.plain.length() + 20 * this.sections.size());
                    StyledSection current = null;
                    int end = Math.min(this.from + this.length, this.plain.length());
                    for (int i = this.from; i < end; ++i) {
                        StyledSection section = this.findSectionContaining(i);
                        if (section != current) {
                            if (current != null) {
                                sb.append(current.endStyles);
                            }
                            if (section != null) {
                                sb.append(section.startStyles);
                            }
                            current = section;
                        }
                        sb.append(this.plain.charAt(i));
                    }
                    if (current != null) {
                        sb.append(current.endStyles);
                    }
                    return sb.toString();
                }

                private StyledSection findSectionContaining(int index) {
                    for (StyledSection section : this.sections) {
                        if (index < section.startIndex || index >= section.startIndex + section.length) continue;
                        return section;
                    }
                    return null;
                }

                public int getCJKAdjustedLength() {
                    return this.getCJKAdjustedLength(this.from, this.length);
                }

                public int getCJKAdjustedLength(int fromPosition, int charCount) {
                    int result = 0;
                    for (int i = fromPosition; i < fromPosition + charCount; ++i) {
                        result += Model.UsageMessageSpec.isCharCJK(this.plain.charAt(i)) ? 2 : 1;
                    }
                    return result;
                }
            }

            private static class StyledSection {
                int startIndex;
                int length;
                String startStyles;
                String endStyles;

                StyledSection(int start, int len, String style1, String style2) {
                    this.startIndex = start;
                    this.length = len;
                    this.startStyles = style1;
                    this.endStyles = style2;
                }

                StyledSection withStartIndex(int newStart) {
                    return new StyledSection(newStart, this.length, this.startStyles, this.endStyles);
                }
            }

            static class Palette256Color
            implements IStyle {
                private final int fgbg;
                private final int color;

                Palette256Color(boolean foreground, String color) {
                    this.fgbg = foreground ? 38 : 48;
                    String[] rgb = color.split(";");
                    this.color = rgb.length == 3 ? 16 + 36 * Integer.decode(rgb[0]) + 6 * Integer.decode(rgb[1]) + Integer.decode(rgb[2]) : Integer.decode(color);
                }

                public String on() {
                    return String.format("\u001b[%d;5;%dm", this.fgbg, this.color);
                }

                public String off() {
                    return "\u001b[" + (this.fgbg + 1) + "m";
                }

                public boolean equals(Object obj) {
                    if (obj == this) {
                        return true;
                    }
                    if (!(obj instanceof Palette256Color)) {
                        return false;
                    }
                    Palette256Color other = (Palette256Color)obj;
                    return other.fgbg == this.fgbg && other.color == this.color;
                }

                public int hashCode() {
                    return (17 + this.fgbg) * 37 + this.color;
                }
            }

            /*
             * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
             */
            public static enum Style implements IStyle
            {
                reset(0, 0),
                bold(1, 21),
                faint(2, 22),
                italic(3, 23),
                underline(4, 24),
                blink(5, 25),
                reverse(7, 27),
                fg_black(30, 39),
                fg_red(31, 39),
                fg_green(32, 39),
                fg_yellow(33, 39),
                fg_blue(34, 39),
                fg_magenta(35, 39),
                fg_cyan(36, 39),
                fg_white(37, 39),
                bg_black(40, 49),
                bg_red(41, 49),
                bg_green(42, 49),
                bg_yellow(43, 49),
                bg_blue(44, 49),
                bg_magenta(45, 49),
                bg_cyan(46, 49),
                bg_white(47, 49);

                private final int startCode;
                private final int endCode;

                private Style(int startCode, int endCode) {
                    this.startCode = startCode;
                    this.endCode = endCode;
                }

                @Override
                public String on() {
                    return "\u001b[" + this.startCode + "m";
                }

                @Override
                public String off() {
                    return "\u001b[" + this.endCode + "m";
                }

                public static String on(IStyle ... styles) {
                    StringBuilder result = new StringBuilder();
                    for (IStyle style : styles) {
                        result.append(style.on());
                    }
                    return result.toString();
                }

                public static String off(IStyle ... styles) {
                    StringBuilder result = new StringBuilder();
                    for (IStyle style : styles) {
                        result.append(style.off());
                    }
                    return result.toString();
                }

                public static IStyle fg(String str) {
                    try {
                        return Style.valueOf(str.toLowerCase(Locale.ENGLISH));
                    }
                    catch (Exception exception) {
                        try {
                            return Style.valueOf("fg_" + str.toLowerCase(Locale.ENGLISH));
                        }
                        catch (Exception exception2) {
                            return new Palette256Color(true, str);
                        }
                    }
                }

                public static IStyle bg(String str) {
                    try {
                        return Style.valueOf(str.toLowerCase(Locale.ENGLISH));
                    }
                    catch (Exception exception) {
                        try {
                            return Style.valueOf("bg_" + str.toLowerCase(Locale.ENGLISH));
                        }
                        catch (Exception exception2) {
                            return new Palette256Color(false, str);
                        }
                    }
                }

                public static IStyle[] parse(String commaSeparatedCodes) {
                    String[] codes = commaSeparatedCodes.split(",");
                    IStyle[] styles = new IStyle[codes.length];
                    for (int i = 0; i < codes.length; ++i) {
                        int end;
                        if (codes[i].toLowerCase(Locale.ENGLISH).startsWith("fg(")) {
                            end = codes[i].indexOf(41);
                            styles[i] = Style.fg(codes[i].substring(3, end < 0 ? codes[i].length() : end));
                            continue;
                        }
                        if (codes[i].toLowerCase(Locale.ENGLISH).startsWith("bg(")) {
                            end = codes[i].indexOf(41);
                            styles[i] = Style.bg(codes[i].substring(3, end < 0 ? codes[i].length() : end));
                            continue;
                        }
                        styles[i] = Style.fg(codes[i]);
                    }
                    return styles;
                }
            }

            public static interface IStyle {
                public static final String CSI = "\u001b[";

                public String on();

                public String off();
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class ColorScheme {
            private static final Ansi.IStyle EMPTY_STYLE = new Ansi.IStyle(){

                public String on() {
                    return "";
                }

                public String off() {
                    return "";
                }
            };
            private final List<Ansi.IStyle> commandStyles;
            private final List<Ansi.IStyle> optionStyles;
            private final List<Ansi.IStyle> parameterStyles;
            private final List<Ansi.IStyle> optionParamStyles;
            private final List<Ansi.IStyle> errorStyles;
            private final List<Ansi.IStyle> stackTraceStyles;
            private final Ansi ansi;
            private final Map<String, Ansi.IStyle> markupMap;

            ColorScheme(Builder builder) {
                Assert.notNull(builder, "builder");
                this.ansi = Assert.notNull(builder.ansi(), "ansi");
                this.commandStyles = Collections.unmodifiableList(new ArrayList<Ansi.IStyle>(builder.commandStyles()));
                this.optionStyles = Collections.unmodifiableList(new ArrayList<Ansi.IStyle>(builder.optionStyles()));
                this.parameterStyles = Collections.unmodifiableList(new ArrayList<Ansi.IStyle>(builder.parameterStyles()));
                this.optionParamStyles = Collections.unmodifiableList(new ArrayList<Ansi.IStyle>(builder.optionParamStyles()));
                this.errorStyles = Collections.unmodifiableList(new ArrayList<Ansi.IStyle>(builder.errorStyles()));
                this.stackTraceStyles = Collections.unmodifiableList(new ArrayList<Ansi.IStyle>(builder.stackTraceStyles()));
                this.markupMap = builder.markupMap == null ? null : Collections.unmodifiableMap(new HashMap(builder.markupMap));
            }

            public Ansi.Text commandText(String command) {
                return this.apply(command, this.commandStyles);
            }

            public Ansi.Text optionText(String option) {
                return this.apply(option, this.optionStyles);
            }

            public Ansi.Text parameterText(String parameter) {
                return this.apply(parameter, this.parameterStyles);
            }

            public Ansi.Text optionParamText(String optionParam) {
                return this.apply(optionParam, this.optionParamStyles);
            }

            public Ansi.Text errorText(String error) {
                return this.apply(error, this.errorStyles);
            }

            public Ansi.Text stackTraceText(String stackTrace) {
                return this.apply(stackTrace, this.stackTraceStyles);
            }

            public Ansi.Text stackTraceText(Throwable t) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw, true));
                return this.stackTraceText(sw.toString());
            }

            public String richStackTraceString(Throwable t) {
                return CommandLine.throwableToColorString(t, this);
            }

            public Ansi ansi() {
                return this.ansi;
            }

            public List<Ansi.IStyle> commandStyles() {
                return this.commandStyles;
            }

            public List<Ansi.IStyle> optionStyles() {
                return this.optionStyles;
            }

            public List<Ansi.IStyle> parameterStyles() {
                return this.parameterStyles;
            }

            public List<Ansi.IStyle> optionParamStyles() {
                return this.optionParamStyles;
            }

            public List<Ansi.IStyle> errorStyles() {
                return this.errorStyles;
            }

            public List<Ansi.IStyle> stackTraceStyles() {
                return this.stackTraceStyles;
            }

            public Map<String, Ansi.IStyle> customMarkupMap() {
                return this.markupMap == null ? Collections.emptyMap() : this.markupMap;
            }

            public Ansi.IStyle[] parse(String commaSeparatedCodes) {
                if (this.markupMap == null) {
                    return Ansi.Style.parse(commaSeparatedCodes);
                }
                String[] codes = commaSeparatedCodes.split(",");
                ArrayList<Ansi.IStyle> styles = new ArrayList<Ansi.IStyle>();
                for (String code : codes) {
                    Ansi.IStyle found;
                    Ansi.IStyle iStyle = found = this.markupMap.containsKey(code = code.toLowerCase(Locale.ENGLISH).replace("(", "_").replace(")", "")) ? this.markupMap.get(code) : this.markupMap.get("fg_" + code);
                    if (found == null) continue;
                    styles.add(found);
                }
                return styles.toArray(new Ansi.IStyle[0]);
            }

            public Ansi.IStyle resetStyle() {
                if (this.markupMap == null) {
                    return Ansi.Style.reset;
                }
                return this.markupMap.containsKey("reset") ? this.markupMap.get("reset") : EMPTY_STYLE;
            }

            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (!(obj instanceof ColorScheme)) {
                    return false;
                }
                ColorScheme other = (ColorScheme)obj;
                return this.ansi.equals((Object)other.ansi) && this.commandStyles.equals(other.commandStyles) && this.optionStyles.equals(other.optionStyles) && this.parameterStyles.equals(other.parameterStyles) && this.optionParamStyles.equals(other.optionParamStyles) && this.errorStyles.equals(other.errorStyles) && this.stackTraceStyles.equals(other.stackTraceStyles) && this.markupMap == null ? other.markupMap == null : this.markupMap.equals(other.markupMap);
            }

            public int hashCode() {
                int result = 17;
                result = result * 37 + this.ansi.hashCode();
                result = result * 37 + this.commandStyles.hashCode();
                result = result * 37 + this.optionStyles.hashCode();
                result = result * 37 + this.parameterStyles.hashCode();
                result = result * 37 + this.optionParamStyles.hashCode();
                result = result * 37 + this.errorStyles.hashCode();
                result = result * 37 + this.stackTraceStyles.hashCode();
                result = result * 37 + (this.markupMap == null ? 0 : this.markupMap.hashCode());
                return result;
            }

            public String toString() {
                return "ColorScheme[ansi=" + (Object)((Object)this.ansi) + ", commands=" + this.commandStyles + ", optionStyles=" + this.optionStyles + ", parameterStyles=" + this.parameterStyles + ", optionParamStyles=" + this.optionParamStyles + ", errorStyles=" + this.errorStyles + ", stackTraceStyles=" + this.stackTraceStyles + ", customMarkupMap=" + this.markupMap + "]";
            }

            public Ansi.Text apply(String plainText, List<Ansi.IStyle> styles) {
                Ansi ansi = this.ansi();
                ((Object)((Object)ansi)).getClass();
                Ansi.Text result = ansi.new Ansi.Text(plainText.length());
                result.colorScheme = this;
                if (plainText.length() == 0) {
                    return result;
                }
                Object[] all = styles.toArray(new Ansi.IStyle[styles.size()]);
                result.sections.add(new Ansi.StyledSection(0, plainText.length(), Ansi.Style.on((Ansi.IStyle[])all), Ansi.Style.off((Ansi.IStyle[])CommandLine.reverseArray(all)) + this.resetStyle().off()));
                result.plain.append(plainText);
                result.length = result.plain.length();
                return result;
            }

            public Ansi.Text text(String stringWithMarkup) {
                Ansi ansi = this.ansi();
                ((Object)((Object)ansi)).getClass();
                return ansi.new Ansi.Text(stringWithMarkup, this);
            }

            public String string(String stringWithMarkup) {
                Ansi ansi = this.ansi();
                ((Object)((Object)ansi)).getClass();
                return ansi.new Ansi.Text(stringWithMarkup, this).toString();
            }

            /*
             * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
             */
            public static class Builder {
                private final List<Ansi.IStyle> commandStyles = new ArrayList<Ansi.IStyle>();
                private final List<Ansi.IStyle> optionStyles = new ArrayList<Ansi.IStyle>();
                private final List<Ansi.IStyle> parameterStyles = new ArrayList<Ansi.IStyle>();
                private final List<Ansi.IStyle> optionParamStyles = new ArrayList<Ansi.IStyle>();
                private final List<Ansi.IStyle> errorStyles = new ArrayList<Ansi.IStyle>();
                private final List<Ansi.IStyle> stackTraceStyles = new ArrayList<Ansi.IStyle>();
                private Ansi ansi = Ansi.AUTO;
                private Map<String, Ansi.IStyle> markupMap;

                public Builder() {
                }

                public Builder(Ansi ansi) {
                    this.ansi = Assert.notNull(ansi, "ansi");
                }

                public Builder(ColorScheme existing) {
                    Assert.notNull(existing, "colorScheme");
                    this.ansi = Assert.notNull(existing.ansi(), "ansi");
                    this.commandStyles.addAll(existing.commandStyles());
                    this.optionStyles.addAll(existing.optionStyles());
                    this.parameterStyles.addAll(existing.parameterStyles());
                    this.optionParamStyles.addAll(existing.optionParamStyles());
                    this.errorStyles.addAll(existing.errorStyles());
                    this.stackTraceStyles.addAll(existing.stackTraceStyles());
                    if (existing.markupMap != null) {
                        this.markupMap = new HashMap<String, Ansi.IStyle>(existing.markupMap);
                    }
                }

                public Ansi ansi() {
                    return this.ansi;
                }

                public Builder ansi(Ansi ansi) {
                    this.ansi = Assert.notNull(ansi, "ansi");
                    return this;
                }

                public List<Ansi.IStyle> commandStyles() {
                    return this.commandStyles;
                }

                public List<Ansi.IStyle> optionStyles() {
                    return this.optionStyles;
                }

                public List<Ansi.IStyle> parameterStyles() {
                    return this.parameterStyles;
                }

                public List<Ansi.IStyle> optionParamStyles() {
                    return this.optionParamStyles;
                }

                public List<Ansi.IStyle> errorStyles() {
                    return this.errorStyles;
                }

                public List<Ansi.IStyle> stackTraceStyles() {
                    return this.stackTraceStyles;
                }

                public Map<String, Ansi.IStyle> customMarkupMap() {
                    return this.markupMap;
                }

                public Builder customMarkupMap(Map<String, Ansi.IStyle> newValue) {
                    this.markupMap = newValue;
                    return this;
                }

                public Builder commands(Ansi.IStyle ... styles) {
                    return this.addAll(this.commandStyles, styles);
                }

                public Builder options(Ansi.IStyle ... styles) {
                    return this.addAll(this.optionStyles, styles);
                }

                public Builder parameters(Ansi.IStyle ... styles) {
                    return this.addAll(this.parameterStyles, styles);
                }

                public Builder optionParams(Ansi.IStyle ... styles) {
                    return this.addAll(this.optionParamStyles, styles);
                }

                public Builder errors(Ansi.IStyle ... styles) {
                    return this.addAll(this.errorStyles, styles);
                }

                public Builder stackTraces(Ansi.IStyle ... styles) {
                    return this.addAll(this.stackTraceStyles, styles);
                }

                public Builder applySystemProperties() {
                    this.replace(this.commandStyles, System.getProperty("picocli.color.commands"));
                    this.replace(this.optionStyles, System.getProperty("picocli.color.options"));
                    this.replace(this.parameterStyles, System.getProperty("picocli.color.parameters"));
                    this.replace(this.optionParamStyles, System.getProperty("picocli.color.optionParams"));
                    this.replace(this.errorStyles, System.getProperty("picocli.color.errors"));
                    this.replace(this.stackTraceStyles, System.getProperty("picocli.color.stackTraces"));
                    return this;
                }

                private void replace(List<Ansi.IStyle> styles, String property) {
                    if (property != null) {
                        styles.clear();
                        this.addAll(styles, Ansi.Style.parse(property));
                    }
                }

                private Builder addAll(List<Ansi.IStyle> styles, Ansi.IStyle ... add) {
                    styles.addAll(Arrays.asList(add));
                    return this;
                }

                public ColorScheme build() {
                    return new ColorScheme(this);
                }
            }
        }

        public static class Column {
            public final int width;
            public int indent;
            public final Overflow overflow;

            public Column(int width, int indent, Overflow overflow) {
                this.width = width;
                this.indent = indent;
                this.overflow = Assert.notNull(overflow, "overflow");
            }

            public boolean equals(Object obj) {
                return obj instanceof Column && ((Column)obj).width == this.width && ((Column)obj).indent == this.indent && ((Column)obj).overflow == this.overflow;
            }

            public int hashCode() {
                return 17 * this.width + 37 * this.indent + 37 * this.overflow.hashCode();
            }

            public String toString() {
                return String.format("Column[width=%d, indent=%d, overflow=%s]", new Object[]{this.width, this.indent, this.overflow});
            }

            /*
             * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
             */
            public static enum Overflow {
                TRUNCATE,
                SPAN,
                WRAP;

            }
        }

        public static class TextTable {
            private static final int OPTION_SEPARATOR_COLUMN = 2;
            private static final int LONG_OPTION_COLUMN = 3;
            private final Column[] columns;
            protected final List<Ansi.Text> columnValues = new ArrayList<Ansi.Text>();
            public int indentWrappedLines = 2;
            private final ColorScheme colorScheme;
            private final int tableWidth;
            private boolean adjustLineBreaksForWideCJKCharacters = true;

            @Deprecated
            public static TextTable forDefaultColumns(Ansi ansi, int usageHelpWidth) {
                return TextTable.forDefaultColumns(Help.defaultColorScheme(ansi), 24, usageHelpWidth);
            }

            @Deprecated
            public static TextTable forDefaultColumns(Ansi ansi, int longOptionsColumnWidth, int usageHelpWidth) {
                return TextTable.forDefaultColumns(Help.defaultColorScheme(ansi), longOptionsColumnWidth, usageHelpWidth);
            }

            public static TextTable forDefaultColumns(ColorScheme colorScheme, int longOptionsColumnWidth, int usageHelpWidth) {
                int descriptionWidth = usageHelpWidth - 5 - longOptionsColumnWidth;
                return TextTable.forColumns(colorScheme, new Column(2, 0, Column.Overflow.TRUNCATE), new Column(2, 0, Column.Overflow.SPAN), new Column(1, 0, Column.Overflow.TRUNCATE), new Column(longOptionsColumnWidth, 1, Column.Overflow.SPAN), new Column(descriptionWidth, 1, Column.Overflow.WRAP));
            }

            @Deprecated
            public static TextTable forColumnWidths(Ansi ansi, int ... columnWidths) {
                return TextTable.forColumnWidths(Help.defaultColorScheme(ansi), columnWidths);
            }

            public static TextTable forColumnWidths(ColorScheme colorScheme, int ... columnWidths) {
                Column[] columns = new Column[columnWidths.length];
                for (int i = 0; i < columnWidths.length; ++i) {
                    columns[i] = new Column(columnWidths[i], 0, i == columnWidths.length - 1 ? Column.Overflow.WRAP : Column.Overflow.SPAN);
                }
                return new TextTable(colorScheme, columns);
            }

            @Deprecated
            public static TextTable forColumns(Ansi ansi, Column ... columns) {
                return new TextTable(ansi, columns);
            }

            public static TextTable forColumns(ColorScheme colorScheme, Column ... columns) {
                return new TextTable(colorScheme, columns);
            }

            @Deprecated
            protected TextTable(Ansi ansi, Column[] columns) {
                this(Help.defaultColorScheme(ansi), columns);
            }

            protected TextTable(ColorScheme colorScheme, Column[] columns) {
                this.colorScheme = Assert.notNull(colorScheme, "ansi");
                this.columns = (Column[])Assert.notNull(columns, "columns").clone();
                if (columns.length == 0) {
                    throw new IllegalArgumentException("At least one column is required");
                }
                int totalWidth = 0;
                for (Column col : columns) {
                    totalWidth += col.width;
                }
                this.tableWidth = totalWidth;
            }

            public boolean isAdjustLineBreaksForWideCJKCharacters() {
                return this.adjustLineBreaksForWideCJKCharacters;
            }

            public TextTable setAdjustLineBreaksForWideCJKCharacters(boolean adjustLineBreaksForWideCJKCharacters) {
                this.adjustLineBreaksForWideCJKCharacters = adjustLineBreaksForWideCJKCharacters;
                return this;
            }

            public Column[] columns() {
                return (Column[])this.columns.clone();
            }

            public Ansi.Text textAt(int row, int col) {
                return this.columnValues.get(col + row * this.columns.length);
            }

            @Deprecated
            public Ansi.Text cellAt(int row, int col) {
                return this.textAt(row, col);
            }

            public int rowCount() {
                return this.columnValues.size() / this.columns.length;
            }

            public void addEmptyRow() {
                for (Column column : this.columns) {
                    Ansi ansi = this.colorScheme.ansi();
                    ((Object)((Object)ansi)).getClass();
                    this.columnValues.add(ansi.new Ansi.Text(column.width, this.colorScheme));
                }
            }

            public void addRowValues(String ... values) {
                int numColumns = values.length;
                Ansi.Text[][] cells = new Ansi.Text[numColumns][];
                int maxRows = 0;
                for (int col = 0; col < numColumns; ++col) {
                    Ansi.Text[] textArray;
                    if (values[col] == null) {
                        Ansi.Text[] textArray2 = new Ansi.Text[1];
                        textArray = textArray2;
                        textArray2[0] = Ansi.EMPTY_TEXT;
                    } else {
                        textArray = this.colorScheme.text(values[col]).splitLines();
                    }
                    cells[col] = textArray;
                    maxRows = Math.max(maxRows, cells[col].length);
                }
                Object[] rowValues = new Ansi.Text[numColumns];
                for (int row = 0; row < maxRows; ++row) {
                    Arrays.fill(rowValues, Ansi.EMPTY_TEXT);
                    for (int col = 0; col < numColumns; ++col) {
                        if (row >= cells[col].length) continue;
                        rowValues[col] = cells[col][row];
                    }
                    this.addRowValues((Ansi.Text[])rowValues);
                }
            }

            public void addRowValues(Ansi.Text ... values) {
                if (values.length > this.columns.length) {
                    throw new IllegalArgumentException(values.length + " values don't fit in " + this.columns.length + " columns");
                }
                this.addEmptyRow();
                int oldIndent = this.unindent(values);
                for (int col = 0; col < values.length; ++col) {
                    int row = this.rowCount() - 1;
                    Cell cell = this.putValue(row, col, values[col]);
                    if (cell.row == row && cell.column == col || col == values.length - 1) continue;
                    this.addEmptyRow();
                }
                this.reindent(oldIndent);
            }

            private int unindent(Ansi.Text[] values) {
                if (this.columns.length <= 3) {
                    return 0;
                }
                int oldIndent = this.columns[3].indent;
                if (Help.DEFAULT_SEPARATOR.equals(values[2].toString())) {
                    this.columns[3].indent = 0;
                }
                return oldIndent;
            }

            private void reindent(int oldIndent) {
                if (this.columns.length <= 3) {
                    return;
                }
                this.columns[3].indent = oldIndent;
            }

            public Cell putValue(int row, int col, Ansi.Text value) {
                if (row > this.rowCount() - 1) {
                    throw new IllegalArgumentException("Cannot write to row " + row + ": rowCount=" + this.rowCount());
                }
                if (value == null || value.plain.length() == 0) {
                    return new Cell(col, row);
                }
                Column column = this.columns[col];
                int indent = column.indent;
                switch (column.overflow) {
                    case TRUNCATE: {
                        this.copy(value, this.textAt(row, col), indent);
                        return new Cell(col, row);
                    }
                    case SPAN: {
                        int startColumn = col;
                        do {
                            boolean lastColumn = col == this.columns.length - 1;
                            int charsWritten = lastColumn ? this.copy(BreakIterator.getLineInstance(), value, this.textAt(row, col), indent) : this.copy(value, this.textAt(row, col), indent);
                            value = value.substring(charsWritten);
                            indent = 0;
                            if (value.length > 0) {
                                ++col;
                            }
                            if (value.length <= 0 || col < this.columns.length) continue;
                            this.addEmptyRow();
                            ++row;
                            col = startColumn;
                            indent = column.indent + this.indentWrappedLines;
                        } while (value.length > 0);
                        return new Cell(col, row);
                    }
                    case WRAP: {
                        BreakIterator lineBreakIterator = BreakIterator.getLineInstance();
                        do {
                            int charsWritten = this.copy(lineBreakIterator, value, this.textAt(row, col), indent);
                            value = value.substring(charsWritten);
                            indent = column.indent + this.indentWrappedLines;
                            if (value.length <= 0) continue;
                            ++row;
                            this.addEmptyRow();
                        } while (value.length > 0);
                        return new Cell(col, row);
                    }
                }
                throw new IllegalStateException(column.overflow.toString());
            }

            private int length(Ansi.Text str) {
                return str.getCJKAdjustedLength();
            }

            private int length(Ansi.Text str, int from, int length) {
                if (!this.adjustLineBreaksForWideCJKCharacters) {
                    return length - from;
                }
                return str.getCJKAdjustedLength(from, length);
            }

            private int copy(BreakIterator line, Ansi.Text text, Ansi.Text columnValue, int offset) {
                line.setText(text.plainString().replace("-", "\u00ff"));
                Count count = new Count();
                int start = line.first();
                int end = line.next();
                while (end != -1) {
                    Ansi.Text word = text.substring(start, end);
                    if (columnValue.maxLength < offset + count.columnCount + this.length(word)) break;
                    this.copy(word, columnValue, offset + count.charCount, count);
                    start = end;
                    end = line.next();
                }
                if (count.charCount == 0 && this.length(text) + offset > columnValue.maxLength) {
                    this.copy(text, columnValue, offset, count);
                }
                return count.charCount;
            }

            private int copy(Ansi.Text value, Ansi.Text destination, int offset) {
                Count count = new Count();
                this.copy(value, destination, offset, count);
                return count.charCount;
            }

            private void copy(Ansi.Text value, Ansi.Text destination, int offset, Count count) {
                int length = Math.min(value.length, destination.maxLength - offset);
                value.getStyledChars(value.from, length, destination, offset);
                count.columnCount += this.length(value, value.from, length);
                count.charCount += length;
            }

            public StringBuilder toString(StringBuilder text) {
                int columnCount = this.columns.length;
                StringBuilder row = new StringBuilder(this.tableWidth);
                for (int i = 0; i < this.columnValues.size(); ++i) {
                    int lastChar;
                    Ansi.Text column = this.columnValues.get(i);
                    row.append(column.toString());
                    row.append(new String(Help.spaces(this.columns[i % columnCount].width - column.length)));
                    if (i % columnCount != columnCount - 1) continue;
                    for (lastChar = row.length() - 1; lastChar >= 0 && row.charAt(lastChar) == ' '; --lastChar) {
                    }
                    row.setLength(lastChar + 1);
                    text.append(row.toString()).append(System.getProperty("line.separator"));
                    row.setLength(0);
                }
                return text;
            }

            public String toString() {
                return this.toString(new StringBuilder()).toString();
            }

            static class Count {
                int charCount;
                int columnCount;

                Count() {
                }
            }

            public static class Cell {
                public final int column;
                public final int row;

                public Cell(int column, int row) {
                    this.column = column;
                    this.row = row;
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class SortByOrder<T extends Model.IOrdered>
        implements Comparator<T> {
            SortByOrder() {
            }

            @Override
            public int compare(T o1, T o2) {
                return Integer.signum(o1.order() - o2.order());
            }
        }

        static class SortByOptionArityAndNameAlphabetically
        extends SortByShortestOptionNameAlphabetically {
            SortByOptionArityAndNameAlphabetically() {
            }

            public int compare(Model.OptionSpec o1, Model.OptionSpec o2) {
                Range arity1 = o1.arity();
                Range arity2 = o2.arity();
                int result = arity1.max - arity2.max;
                if (result == 0) {
                    result = arity1.min - arity2.min;
                }
                if (result == 0) {
                    if (o1.isMultiValue() && !o2.isMultiValue()) {
                        result = 1;
                    }
                    if (!o1.isMultiValue() && o2.isMultiValue()) {
                        result = -1;
                    }
                }
                return result == 0 ? super.compare(o1, o2) : result;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class SortByShortestOptionNameAlphabetically
        implements Comparator<Model.OptionSpec> {
            SortByShortestOptionNameAlphabetically() {
            }

            @Override
            public int compare(Model.OptionSpec o1, Model.OptionSpec o2) {
                if (o1 == null) {
                    return 1;
                }
                if (o2 == null) {
                    return -1;
                }
                String[] names1 = ShortestFirst.sort(o1.names());
                String[] names2 = ShortestFirst.sort(o2.names());
                String s1 = Model.CommandSpec.stripPrefix(names1[0]);
                String s2 = Model.CommandSpec.stripPrefix(names2[0]);
                int result = s1.toUpperCase().compareTo(s2.toUpperCase());
                int n = result = result == 0 ? -s1.compareTo(s2) : result;
                return o1.help() == o2.help() ? result : (o2.help() ? -1 : 1);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class ShortestFirst
        implements Comparator<String> {
            ShortestFirst() {
            }

            @Override
            public int compare(String o1, String o2) {
                return o1.length() - o2.length();
            }

            public static String[] sort(String[] names) {
                Arrays.sort(names, new ShortestFirst());
                return names;
            }

            public static String[] longestFirst(String[] names) {
                Arrays.sort(names, Collections.reverseOrder(new ShortestFirst()));
                return names;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class Layout {
            protected final ColorScheme colorScheme;
            protected final TextTable table;
            protected IOptionRenderer optionRenderer;
            protected IParameterRenderer parameterRenderer;

            public Layout(ColorScheme colorScheme, int tableWidth) {
                this(colorScheme, TextTable.forDefaultColumns(colorScheme, 24, tableWidth));
            }

            public Layout(ColorScheme colorScheme, TextTable textTable) {
                this(colorScheme, textTable, new DefaultOptionRenderer(false, " "), new DefaultParameterRenderer(false, " "));
            }

            public Layout(ColorScheme colorScheme, TextTable textTable, IOptionRenderer optionRenderer, IParameterRenderer parameterRenderer) {
                this.colorScheme = Assert.notNull(colorScheme, "colorScheme");
                this.table = Assert.notNull(textTable, "textTable");
                this.optionRenderer = Assert.notNull(optionRenderer, "optionRenderer");
                this.parameterRenderer = Assert.notNull(parameterRenderer, "parameterRenderer");
            }

            public void layout(Model.ArgSpec argSpec, Ansi.Text[][] cellValues) {
                for (Ansi.Text[] oneRow : cellValues) {
                    this.table.addRowValues(oneRow);
                }
            }

            public void addOptions(List<Model.OptionSpec> options, IParamLabelRenderer paramLabelRenderer) {
                for (Model.OptionSpec option : options) {
                    if (option.hidden()) continue;
                    this.addOption(option, paramLabelRenderer);
                }
            }

            public void addAllOptions(List<Model.OptionSpec> options, IParamLabelRenderer paramLabelRenderer) {
                for (Model.OptionSpec option : options) {
                    this.addOption(option, paramLabelRenderer);
                }
            }

            public void addOption(Model.OptionSpec option, IParamLabelRenderer paramLabelRenderer) {
                Ansi.Text[][] values = this.optionRenderer.render(option, paramLabelRenderer, this.colorScheme);
                this.layout(option, values);
            }

            public void addPositionalParameters(List<Model.PositionalParamSpec> params, IParamLabelRenderer paramLabelRenderer) {
                for (Model.PositionalParamSpec param : params) {
                    if (param.hidden()) continue;
                    this.addPositionalParameter(param, paramLabelRenderer);
                }
            }

            public void addAllPositionalParameters(List<Model.PositionalParamSpec> params, IParamLabelRenderer paramLabelRenderer) {
                for (Model.PositionalParamSpec param : params) {
                    this.addPositionalParameter(param, paramLabelRenderer);
                }
            }

            public void addPositionalParameter(Model.PositionalParamSpec param, IParamLabelRenderer paramLabelRenderer) {
                Ansi.Text[][] values = this.parameterRenderer.render(param, paramLabelRenderer, this.colorScheme);
                this.layout(param, values);
            }

            public String toString() {
                return this.table.toString();
            }

            public ColorScheme colorScheme() {
                return this.colorScheme;
            }

            public TextTable textTable() {
                return this.table;
            }

            public IOptionRenderer optionRenderer() {
                return this.optionRenderer;
            }

            public IParameterRenderer parameterRenderer() {
                return this.parameterRenderer;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class DefaultParamLabelRenderer
        implements IParamLabelRenderer {
            private final Model.CommandSpec commandSpec;

            public DefaultParamLabelRenderer(Model.CommandSpec commandSpec) {
                this.commandSpec = Assert.notNull(commandSpec, "commandSpec");
            }

            @Override
            public String separator() {
                return this.commandSpec.parser().separator();
            }

            @Override
            public Ansi.Text renderParameterLabel(Model.ArgSpec argSpec, Ansi ansi, List<Ansi.IStyle> styles) {
                String optionSeparator;
                boolean effectivelyVariable;
                Ansi.Text paramName;
                ColorScheme colorScheme;
                Range capacity = argSpec.isOption() ? argSpec.arity() : ((Model.PositionalParamSpec)argSpec).capacity();
                ColorScheme colorScheme2 = colorScheme = this.commandSpec.commandLine() == null ? Help.defaultColorScheme(ansi) : this.commandSpec.commandLine().getColorScheme();
                if (capacity.max == 0) {
                    Ansi ansi2 = ansi;
                    ((Object)((Object)ansi2)).getClass();
                    return ansi2.new Ansi.Text("", colorScheme);
                }
                if (argSpec.hideParamSyntax()) {
                    return colorScheme.apply((argSpec.isOption() ? this.separator() : "") + argSpec.paramLabel(), styles);
                }
                String split = argSpec.splitRegex();
                if (!CommandLine.empty(split)) {
                    split = CommandLine.empty(argSpec.splitRegexSynopsisLabel()) ? split : argSpec.splitRegexSynopsisLabel();
                }
                String mandatorySep = CommandLine.empty(split) ? " " : split;
                String optionalSep = CommandLine.empty(split) ? " [" : "[" + split;
                boolean unlimitedSplit = !CommandLine.empty(split) && !this.commandSpec.parser().limitSplit();
                boolean limitedSplit = !CommandLine.empty(split) && this.commandSpec.parser().limitSplit();
                Ansi.Text repeating = paramName = colorScheme.apply(argSpec.paramLabel(), styles);
                int paramCount = 1;
                if (unlimitedSplit) {
                    repeating = paramName.concat("[" + split).concat(paramName).concat("...]");
                    ++paramCount;
                    mandatorySep = " ";
                    optionalSep = " [";
                }
                Ansi.Text result = repeating;
                for (int done = 1; done < capacity.min; ++done) {
                    result = result.concat(mandatorySep).concat(repeating);
                    paramCount += paramCount;
                }
                if (!capacity.isVariable) {
                    int i;
                    for (i = done; i < capacity.max; ++i) {
                        result = result.concat(optionalSep).concat(paramName);
                        ++paramCount;
                    }
                    for (i = done; i < capacity.max; ++i) {
                        result = result.concat("]");
                    }
                }
                boolean bl = effectivelyVariable = capacity.isVariable || limitedSplit && paramCount == 1;
                if (limitedSplit && effectivelyVariable && paramCount == 1) {
                    result = result.concat(optionalSep).concat(repeating).concat("]");
                }
                if (effectivelyVariable) {
                    if (!argSpec.arity().isVariable && argSpec.arity().min > 1) {
                        Ansi ansi3 = ansi;
                        ((Object)((Object)ansi3)).getClass();
                        result = ansi3.new Ansi.Text("(", colorScheme).concat(result).concat(")");
                    }
                    result = result.concat("...");
                }
                String string = optionSeparator = argSpec.isOption() ? this.separator() : "";
                if (capacity.min == 0) {
                    String sep2 = CommandLine.empty(optionSeparator.trim()) ? optionSeparator + "[" : "[" + optionSeparator;
                    Ansi ansi4 = ansi;
                    ((Object)((Object)ansi4)).getClass();
                    result = ansi4.new Ansi.Text(sep2, colorScheme).concat(result).concat("]");
                } else {
                    Ansi ansi5 = ansi;
                    ((Object)((Object)ansi5)).getClass();
                    result = ansi5.new Ansi.Text(optionSeparator, colorScheme).concat(result);
                }
                return result;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static interface IParamLabelRenderer {
            public Ansi.Text renderParameterLabel(Model.ArgSpec var1, Ansi var2, List<Ansi.IStyle> var3);

            public String separator();
        }

        static class DefaultParameterRenderer
        implements IParameterRenderer {
            private final String requiredMarker;
            private final boolean showDefaultValues;

            public DefaultParameterRenderer(boolean showDefaultValues, String requiredMarker) {
                this.showDefaultValues = showDefaultValues;
                this.requiredMarker = Assert.notNull(requiredMarker, "requiredMarker");
            }

            public Ansi.Text[][] render(Model.PositionalParamSpec param, IParamLabelRenderer paramLabelRenderer, ColorScheme scheme) {
                int i;
                Ansi.Text label = paramLabelRenderer.renderParameterLabel(param, scheme.ansi(), scheme.parameterStyles);
                Ansi.Text requiredParameter = scheme.parameterText(param.arity().min > 0 ? this.requiredMarker : "");
                Ansi.Text EMPTY = Ansi.EMPTY_TEXT;
                boolean[] showDefault = new boolean[]{param.internalShowDefaultValue(this.showDefaultValues)};
                ArrayList<Ansi.Text[]> result = new ArrayList<Ansi.Text[]>();
                String[] description = param.description();
                Ansi.Text[] descriptionFirstLines = Help.createDescriptionFirstLines(scheme, param, description, showDefault);
                result.add(new Ansi.Text[]{requiredParameter, EMPTY, EMPTY, label, descriptionFirstLines[0]});
                for (i = 1; i < descriptionFirstLines.length; ++i) {
                    result.add(new Ansi.Text[]{EMPTY, EMPTY, EMPTY, EMPTY, descriptionFirstLines[i]});
                }
                for (i = 1; i < description.length; ++i) {
                    Ansi.Text[] descriptionNextLines;
                    Ansi ansi = scheme.ansi();
                    ((Object)((Object)ansi)).getClass();
                    for (Ansi.Text line : descriptionNextLines = ansi.new Ansi.Text(description[i], scheme).splitLines()) {
                        result.add(new Ansi.Text[]{EMPTY, EMPTY, EMPTY, EMPTY, line});
                    }
                }
                if (showDefault[0]) {
                    Help.addTrailingDefaultLine(result, param, scheme);
                }
                return (Ansi.Text[][])result.toArray((T[])new Ansi.Text[result.size()][]);
            }
        }

        public static interface IParameterRenderer {
            public Ansi.Text[][] render(Model.PositionalParamSpec var1, IParamLabelRenderer var2, ColorScheme var3);
        }

        static class MinimalParameterRenderer
        implements IParameterRenderer {
            MinimalParameterRenderer() {
            }

            public Ansi.Text[][] render(Model.PositionalParamSpec param, IParamLabelRenderer parameterLabelRenderer, ColorScheme scheme) {
                Ansi.Text[][] textArray = new Ansi.Text[1][];
                Ansi.Text[] textArray2 = new Ansi.Text[2];
                textArray2[0] = parameterLabelRenderer.renderParameterLabel(param, scheme.ansi(), scheme.parameterStyles);
                Ansi ansi = scheme.ansi();
                ((Object)((Object)ansi)).getClass();
                textArray2[1] = ansi.new Ansi.Text(param.description().length == 0 ? "" : param.description()[0], scheme);
                textArray[0] = textArray2;
                return textArray;
            }
        }

        static class MinimalOptionRenderer
        implements IOptionRenderer {
            MinimalOptionRenderer() {
            }

            public Ansi.Text[][] render(Model.OptionSpec option, IParamLabelRenderer parameterLabelRenderer, ColorScheme scheme) {
                Ansi.Text optionText = option.negatable() ? scheme.optionText(option.commandSpec.negatableOptionTransformer().makeSynopsis(option.names()[0], option.commandSpec)) : scheme.optionText(option.names()[0]);
                Ansi.Text paramLabelText = parameterLabelRenderer.renderParameterLabel(option, scheme.ansi(), scheme.optionParamStyles);
                optionText = optionText.concat(paramLabelText);
                Ansi.Text[][] textArray = new Ansi.Text[1][];
                Ansi.Text[] textArray2 = new Ansi.Text[2];
                textArray2[0] = optionText;
                Ansi ansi = scheme.ansi();
                ((Object)((Object)ansi)).getClass();
                textArray2[1] = ansi.new Ansi.Text(option.description().length == 0 ? "" : option.description()[0], scheme);
                textArray[0] = textArray2;
                return textArray;
            }
        }

        static class DefaultOptionRenderer
        implements IOptionRenderer {
            private final String requiredMarker;
            private final boolean showDefaultValues;
            private String sep;

            public DefaultOptionRenderer(boolean showDefaultValues, String requiredMarker) {
                this.showDefaultValues = showDefaultValues;
                this.requiredMarker = Assert.notNull(requiredMarker, "requiredMarker");
            }

            public Ansi.Text[][] render(Model.OptionSpec option, IParamLabelRenderer paramLabelRenderer, ColorScheme scheme) {
                String[] names = ShortestFirst.sort(option.names());
                int shortOptionCount = names[0].length() == 2 ? 1 : 0;
                String shortOption = shortOptionCount > 0 ? names[0] : "";
                String string = this.sep = shortOptionCount > 0 && names.length > 1 ? "," : "";
                if (option.negatable()) {
                    INegatableOptionTransformer transformer = option.commandSpec.negatableOptionTransformer();
                    if (shortOptionCount > 0) {
                        shortOption = transformer.makeSynopsis(shortOption, option.commandSpec);
                    }
                    for (int i = 0; i < names.length; ++i) {
                        names[i] = transformer.makeSynopsis(names[i], option.commandSpec);
                    }
                }
                String longOption = Help.join(names, shortOptionCount, names.length - shortOptionCount, ", ");
                Ansi.Text longOptionText = this.createLongOptionText(option, paramLabelRenderer, scheme, longOption);
                String requiredOption = option.required() ? this.requiredMarker : "";
                return this.renderDescriptionLines(option, scheme, requiredOption, shortOption, longOptionText);
            }

            private Ansi.Text createLongOptionText(Model.OptionSpec option, IParamLabelRenderer renderer, ColorScheme scheme, String longOption) {
                Ansi.Text paramLabelText = renderer.renderParameterLabel(option, scheme.ansi(), scheme.optionParamStyles);
                if (paramLabelText.length > 0 && longOption.length() == 0) {
                    this.sep = renderer.separator();
                    int sepStart = paramLabelText.plainString().indexOf(this.sep);
                    Ansi.Text prefix = paramLabelText.substring(0, sepStart);
                    paramLabelText = prefix.concat(paramLabelText.substring(sepStart + this.sep.length()));
                }
                Ansi.Text longOptionText = scheme.optionText(longOption);
                longOptionText = longOptionText.concat(paramLabelText);
                return longOptionText;
            }

            private Ansi.Text[][] renderDescriptionLines(Model.OptionSpec option, ColorScheme scheme, String requiredOption, String shortOption, Ansi.Text longOptionText) {
                int i;
                Ansi.Text EMPTY = Ansi.EMPTY_TEXT;
                boolean[] showDefault = new boolean[]{option.internalShowDefaultValue(this.showDefaultValues)};
                ArrayList<Ansi.Text[]> result = new ArrayList<Ansi.Text[]>();
                String[] description = option.description();
                Ansi.Text[] descriptionFirstLines = Help.createDescriptionFirstLines(scheme, option, description, showDefault);
                Ansi.Text[] textArray = new Ansi.Text[5];
                textArray[0] = scheme.optionText(requiredOption);
                textArray[1] = scheme.optionText(shortOption);
                Ansi ansi = scheme.ansi();
                ((Object)((Object)ansi)).getClass();
                textArray[2] = ansi.new Ansi.Text(this.sep, scheme);
                textArray[3] = longOptionText;
                textArray[4] = descriptionFirstLines[0];
                result.add(textArray);
                for (i = 1; i < descriptionFirstLines.length; ++i) {
                    result.add(new Ansi.Text[]{EMPTY, EMPTY, EMPTY, EMPTY, descriptionFirstLines[i]});
                }
                for (i = 1; i < description.length; ++i) {
                    Ansi.Text[] descriptionNextLines;
                    Ansi ansi2 = scheme.ansi();
                    ((Object)((Object)ansi2)).getClass();
                    for (Ansi.Text line : descriptionNextLines = ansi2.new Ansi.Text(description[i], scheme).splitLines()) {
                        result.add(new Ansi.Text[]{EMPTY, EMPTY, EMPTY, EMPTY, line});
                    }
                }
                if (showDefault[0]) {
                    Help.addTrailingDefaultLine(result, option, scheme);
                }
                return (Ansi.Text[][])result.toArray((T[])new Ansi.Text[result.size()][]);
            }
        }

        public static interface IOptionRenderer {
            public Ansi.Text[][] render(Model.OptionSpec var1, IParamLabelRenderer var2, ColorScheme var3);
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static enum Visibility {
            ALWAYS,
            NEVER,
            ON_DEMAND;

        }
    }

    public static interface IHelpSectionRenderer {
        public String render(Help var1);
    }

    public static interface IHelpCommandInitializable2 {
        public void init(CommandLine var1, Help.ColorScheme var2, PrintWriter var3, PrintWriter var4);
    }

    @Deprecated
    public static interface IHelpCommandInitializable {
        @Deprecated
        public void init(CommandLine var1, Help.Ansi var2, PrintStream var3, PrintStream var4);
    }

    @Command(name="help", header={"Displays help information about the specified command"}, synopsisHeading="%nUsage: ", helpCommand=true, description={"%nWhen no COMMAND is given, the usage help for the main command is displayed.", "If a COMMAND is specified, the help for that command is shown.%n"})
    public static final class HelpCommand
    implements IHelpCommandInitializable,
    IHelpCommandInitializable2,
    Runnable {
        @Option(names={"-h", "--help"}, usageHelp=true, descriptionKey="helpCommand.help", description={"Show usage help for the help command and exit."})
        private boolean helpRequested;
        @Parameters(paramLabel="COMMAND", descriptionKey="helpCommand.command", description={"The COMMAND to display the usage help message for."})
        private String[] commands = new String[0];
        private CommandLine self;
        private PrintStream out;
        private PrintStream err;
        private PrintWriter outWriter;
        private PrintWriter errWriter;
        private Help.Ansi ansi;
        private Help.ColorScheme colorScheme;

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public void run() {
            Help.ColorScheme colors;
            CommandLine parent;
            if (this.self == null) {
                return;
            }
            CommandLine commandLine = parent = this.self.getParent();
            if (parent == null) {
                return;
            }
            Help.ColorScheme colorScheme = colors = this.colorScheme != null ? this.colorScheme : Help.defaultColorScheme(this.ansi);
            if (this.commands.length > 0) {
                CommandLine subcommand;
                Map<String, CommandLine> parentSubcommands = parent.getCommandSpec().subcommands();
                String fullName = this.commands[0];
                if (parent.isAbbreviatedSubcommandsAllowed()) {
                    fullName = AbbreviationMatcher.match(parentSubcommands.keySet(), fullName, parent.isSubcommandsCaseInsensitive(), this.self);
                }
                if ((subcommand = parentSubcommands.get(fullName)) == null) throw new ParameterException(parent, "Unknown subcommand '" + this.commands[0] + "'.", null, this.commands[0]);
                if (this.outWriter != null) {
                    subcommand.usage(this.outWriter, colors);
                    return;
                } else {
                    subcommand.usage(this.out, colors);
                }
                return;
            } else if (this.outWriter != null) {
                parent.usage(this.outWriter, colors);
                return;
            } else {
                parent.usage(this.out, colors);
            }
        }

        @Deprecated
        public void init(CommandLine helpCommandLine, Help.Ansi ansi, PrintStream out, PrintStream err) {
            this.self = Assert.notNull(helpCommandLine, "helpCommandLine");
            this.ansi = Assert.notNull(ansi, "ansi");
            this.out = Assert.notNull(out, "out");
            this.err = Assert.notNull(err, "err");
        }

        public void init(CommandLine helpCommandLine, Help.ColorScheme colorScheme, PrintWriter out, PrintWriter err) {
            this.self = Assert.notNull(helpCommandLine, "helpCommandLine");
            this.colorScheme = Assert.notNull(colorScheme, "colorScheme");
            this.outWriter = Assert.notNull(out, "outWriter");
            this.errWriter = Assert.notNull(err, "errWriter");
        }
    }

    static class AutoHelpMixin {
        private static final String KEY = "mixinStandardHelpOptions";
        @Option(names={"${picocli.help.name.0:--h}", "${picocli.help.name.1:---help}"}, usageHelp=true, descriptionKey="mixinStandardHelpOptions.help", description={"Show this help message and exit."})
        private boolean helpRequested;
        @Option(names={"${picocli.version.name.0:--V}", "${picocli.version.name.1:---version}"}, versionHelp=true, descriptionKey="mixinStandardHelpOptions.version", description={"Print version information and exit."})
        private boolean versionRequested;

        AutoHelpMixin() {
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class BuiltIn {
        static Set<String> traced = new HashSet<String>();

        private static TypeConversionException fail(String value, Class<?> c) {
            return BuiltIn.fail(value, c, "'%s' is not a %s");
        }

        private static TypeConversionException fail(String value, Class<?> c, String template) {
            return new TypeConversionException(String.format(template, value, c.getSimpleName()));
        }

        static void handle(Exception e, String fqcn, Tracer tracer) {
            if (!traced.contains(fqcn)) {
                tracer.debug("Could not register converter for %s: %s%n", fqcn, e.toString());
            }
            traced.add(fqcn);
        }

        static boolean excluded(String fqcn, Tracer tracer) {
            String[] excludes;
            for (String regex : excludes = System.getProperty("picocli.converters.excludes", "").split(",")) {
                if (!fqcn.matches(regex)) continue;
                tracer.debug("BuiltIn type converter for %s is not loaded: (picocli.converters.excludes=%s)%n", fqcn, System.getProperty("picocli.converters.excludes"));
                return true;
            }
            return false;
        }

        private BuiltIn() {
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class ReflectionConverter
        implements ITypeConverter<Object> {
            private final Method method;
            private final Class<?>[] paramTypes;

            public ReflectionConverter(Method method, Class<?> ... paramTypes) {
                this.method = Assert.notNull(method, "method");
                this.paramTypes = Assert.notNull(paramTypes, "paramTypes");
            }

            @Override
            public Object convert(String s) {
                try {
                    if (this.paramTypes.length > 1) {
                        return this.method.invoke(null, s, new String[0]);
                    }
                    return this.method.invoke(null, s);
                }
                catch (InvocationTargetException e) {
                    throw new TypeConversionException(String.format("cannot convert '%s' to %s (%s)", s, this.method.getReturnType(), e.getTargetException()));
                }
                catch (Exception e) {
                    throw new TypeConversionException(String.format("Internal error converting '%s' to %s (%s)", s, this.method.getReturnType(), e));
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class NetworkInterfaceConverter
        implements ITypeConverter<NetworkInterface> {
            NetworkInterfaceConverter() {
            }

            @Override
            public NetworkInterface convert(String s) throws Exception {
                try {
                    InetAddress addr = new InetAddressConverter().convert(s);
                    return NetworkInterface.getByInetAddress(addr);
                }
                catch (Exception ex) {
                    try {
                        return NetworkInterface.getByName(s);
                    }
                    catch (Exception ex2) {
                        throw new TypeConversionException("'" + s + "' is not an InetAddress or NetworkInterface name");
                    }
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class ClassConverter
        implements ITypeConverter<Class<?>> {
            ClassConverter() {
            }

            @Override
            public Class<?> convert(String s) throws Exception {
                return Class.forName(s);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class ByteOrderConverter
        implements ITypeConverter<ByteOrder> {
            ByteOrderConverter() {
            }

            @Override
            public ByteOrder convert(String s) throws Exception {
                if (s.equalsIgnoreCase(ByteOrder.BIG_ENDIAN.toString())) {
                    return ByteOrder.BIG_ENDIAN;
                }
                if (s.equalsIgnoreCase(ByteOrder.LITTLE_ENDIAN.toString())) {
                    return ByteOrder.LITTLE_ENDIAN;
                }
                throw new TypeConversionException("'" + s + "' is not a valid ByteOrder");
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class TimeZoneConverter
        implements ITypeConverter<TimeZone> {
            TimeZoneConverter() {
            }

            @Override
            public TimeZone convert(String s) throws Exception {
                return TimeZone.getTimeZone(s);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class CurrencyConverter
        implements ITypeConverter<Currency> {
            CurrencyConverter() {
            }

            @Override
            public Currency convert(String s) throws Exception {
                return Currency.getInstance(s);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class UUIDConverter
        implements ITypeConverter<UUID> {
            UUIDConverter() {
            }

            @Override
            public UUID convert(String s) throws Exception {
                return UUID.fromString(s);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class PatternConverter
        implements ITypeConverter<Pattern> {
            PatternConverter() {
            }

            @Override
            public Pattern convert(String s) {
                return Pattern.compile(s);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class InetAddressConverter
        implements ITypeConverter<InetAddress> {
            InetAddressConverter() {
            }

            @Override
            public InetAddress convert(String s) throws Exception {
                return InetAddress.getByName(s);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class CharsetConverter
        implements ITypeConverter<Charset> {
            CharsetConverter() {
            }

            @Override
            public Charset convert(String s) {
                return Charset.forName(s);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class BigIntegerConverter
        implements ITypeConverter<BigInteger> {
            BigIntegerConverter() {
            }

            @Override
            public BigInteger convert(String value) {
                return new BigInteger(value);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class BigDecimalConverter
        implements ITypeConverter<BigDecimal> {
            BigDecimalConverter() {
            }

            @Override
            public BigDecimal convert(String value) {
                return new BigDecimal(value);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class ISO8601TimeConverter
        implements ITypeConverter<Object> {
            private final Constructor<?> constructor;

            ISO8601TimeConverter(Constructor<?> constructor) throws NoSuchMethodException {
                this.constructor = Assert.notNull(constructor, "time class constructor");
            }

            @Override
            public Object convert(String value) {
                try {
                    if (value.length() <= 5) {
                        return this.createTime(new SimpleDateFormat("HH:mm").parse(value).getTime());
                    }
                    if (value.length() <= 8) {
                        return this.createTime(new SimpleDateFormat("HH:mm:ss").parse(value).getTime());
                    }
                    if (value.length() <= 12) {
                        try {
                            return this.createTime(new SimpleDateFormat("HH:mm:ss.SSS").parse(value).getTime());
                        }
                        catch (ParseException e2) {
                            return this.createTime(new SimpleDateFormat("HH:mm:ss,SSS").parse(value).getTime());
                        }
                    }
                }
                catch (ParseException parseException) {
                    // empty catch block
                }
                throw new TypeConversionException("'" + value + "' is not a HH:mm[:ss[.SSS]] time");
            }

            private Object createTime(long epochMillis) {
                try {
                    return this.constructor.newInstance(epochMillis);
                }
                catch (Exception e) {
                    throw new TypeConversionException("Unable to create new java.sql.Time with long value " + epochMillis + ": " + e.getMessage());
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class ISO8601DateConverter
        implements ITypeConverter<Date> {
            ISO8601DateConverter() {
            }

            @Override
            public Date convert(String value) {
                try {
                    return new SimpleDateFormat("yyyy-MM-dd").parse(value);
                }
                catch (ParseException e) {
                    throw new TypeConversionException("'" + value + "' is not a yyyy-MM-dd date");
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class URIConverter
        implements ITypeConverter<URI> {
            URIConverter() {
            }

            @Override
            public URI convert(String value) throws URISyntaxException {
                return new URI(value);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class URLConverter
        implements ITypeConverter<URL> {
            URLConverter() {
            }

            @Override
            public URL convert(String value) throws MalformedURLException {
                return new URL(value);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class FileConverter
        implements ITypeConverter<File> {
            FileConverter() {
            }

            @Override
            public File convert(String value) {
                return new File(value);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class DoubleConverter
        implements ITypeConverter<Double> {
            DoubleConverter() {
            }

            @Override
            public Double convert(String value) {
                try {
                    return Double.valueOf(value);
                }
                catch (Exception ex) {
                    throw BuiltIn.fail(value, Double.TYPE);
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class FloatConverter
        implements ITypeConverter<Float> {
            FloatConverter() {
            }

            @Override
            public Float convert(String value) {
                try {
                    return Float.valueOf(value);
                }
                catch (Exception ex) {
                    throw BuiltIn.fail(value, Float.TYPE);
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class LongConverter
        implements ITypeConverter<Long> {
            LongConverter() {
            }

            @Override
            public Long convert(String value) {
                try {
                    return Long.valueOf(value);
                }
                catch (Exception ex) {
                    throw BuiltIn.fail(value, Long.TYPE);
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class IntegerConverter
        implements ITypeConverter<Integer> {
            IntegerConverter() {
            }

            @Override
            public Integer convert(String value) {
                try {
                    return Integer.valueOf(value);
                }
                catch (Exception ex) {
                    throw BuiltIn.fail(value, Integer.TYPE, "'%s' is not an %s");
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class ShortConverter
        implements ITypeConverter<Short> {
            ShortConverter() {
            }

            @Override
            public Short convert(String value) {
                try {
                    return Short.valueOf(value);
                }
                catch (Exception ex) {
                    throw BuiltIn.fail(value, Short.TYPE);
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class ByteConverter
        implements ITypeConverter<Byte> {
            ByteConverter() {
            }

            @Override
            public Byte convert(String value) {
                try {
                    return Byte.valueOf(value);
                }
                catch (Exception ex) {
                    throw BuiltIn.fail(value, Byte.TYPE);
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class CharacterConverter
        implements ITypeConverter<Character> {
            CharacterConverter() {
            }

            @Override
            public Character convert(String value) {
                if (value.length() > 1) {
                    throw new TypeConversionException("'" + value + "' is not a single character");
                }
                return Character.valueOf(value.charAt(0));
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class BooleanConverter
        implements ITypeConverter<Boolean> {
            BooleanConverter() {
            }

            @Override
            public Boolean convert(String value) {
                if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    return Boolean.parseBoolean(value);
                }
                throw new TypeConversionException("'" + value + "' is not a boolean");
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class CharSequenceConverter
        implements ITypeConverter<CharSequence> {
            CharSequenceConverter() {
            }

            @Override
            public String convert(String value) {
                return value;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class StringBuilderConverter
        implements ITypeConverter<StringBuilder> {
            StringBuilderConverter() {
            }

            @Override
            public StringBuilder convert(String value) {
                return new StringBuilder(value);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class StringConverter
        implements ITypeConverter<String> {
            StringConverter() {
            }

            @Override
            public String convert(String value) {
                return value;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class CharArrayConverter
        implements ITypeConverter<char[]> {
            CharArrayConverter() {
            }

            @Override
            public char[] convert(String value) {
                return value.toCharArray();
            }
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    static class PositionalParametersSorter
    implements Comparator<Model.ArgSpec> {
        private static final Range OPTION_INDEX = new Range(0, 0, false, true, "0");

        PositionalParametersSorter() {
        }

        @Override
        public int compare(Model.ArgSpec p1, Model.ArgSpec p2) {
            int result = this.index(p1).compareTo(this.index(p2));
            return result == 0 ? p1.arity().compareTo(p2.arity()) : result;
        }

        private Range index(Model.ArgSpec arg) {
            return arg.isOption() ? OPTION_INDEX : ((Model.PositionalParamSpec)arg).index();
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private class Interpreter {
        private final Map<Class<?>, ITypeConverter<?>> converterRegistry = new HashMap();
        private boolean isHelpRequested;
        private int position;
        private int interactiveCount;
        private boolean endOfOptions;
        private ParseResult.Builder parseResultBuilder;

        Interpreter() {
            this.registerBuiltInConverters();
        }

        private void registerBuiltInConverters() {
            this.converterRegistry.put(Object.class, new BuiltIn.StringConverter());
            this.converterRegistry.put(String.class, new BuiltIn.StringConverter());
            this.converterRegistry.put(StringBuilder.class, new BuiltIn.StringBuilderConverter());
            this.converterRegistry.put(char[].class, new BuiltIn.CharArrayConverter());
            this.converterRegistry.put(CharSequence.class, new BuiltIn.CharSequenceConverter());
            this.converterRegistry.put(Byte.class, new BuiltIn.ByteConverter());
            this.converterRegistry.put(Byte.TYPE, new BuiltIn.ByteConverter());
            this.converterRegistry.put(Boolean.class, new BuiltIn.BooleanConverter());
            this.converterRegistry.put(Boolean.TYPE, new BuiltIn.BooleanConverter());
            this.converterRegistry.put(Character.class, new BuiltIn.CharacterConverter());
            this.converterRegistry.put(Character.TYPE, new BuiltIn.CharacterConverter());
            this.converterRegistry.put(Short.class, new BuiltIn.ShortConverter());
            this.converterRegistry.put(Short.TYPE, new BuiltIn.ShortConverter());
            this.converterRegistry.put(Integer.class, new BuiltIn.IntegerConverter());
            this.converterRegistry.put(Integer.TYPE, new BuiltIn.IntegerConverter());
            this.converterRegistry.put(Long.class, new BuiltIn.LongConverter());
            this.converterRegistry.put(Long.TYPE, new BuiltIn.LongConverter());
            this.converterRegistry.put(Float.class, new BuiltIn.FloatConverter());
            this.converterRegistry.put(Float.TYPE, new BuiltIn.FloatConverter());
            this.converterRegistry.put(Double.class, new BuiltIn.DoubleConverter());
            this.converterRegistry.put(Double.TYPE, new BuiltIn.DoubleConverter());
            this.converterRegistry.put(File.class, new BuiltIn.FileConverter());
            this.converterRegistry.put(URI.class, new BuiltIn.URIConverter());
            this.converterRegistry.put(URL.class, new BuiltIn.URLConverter());
            this.converterRegistry.put(Date.class, new BuiltIn.ISO8601DateConverter());
            this.converterRegistry.put(BigDecimal.class, new BuiltIn.BigDecimalConverter());
            this.converterRegistry.put(BigInteger.class, new BuiltIn.BigIntegerConverter());
            this.converterRegistry.put(Charset.class, new BuiltIn.CharsetConverter());
            this.converterRegistry.put(InetAddress.class, new BuiltIn.InetAddressConverter());
            this.converterRegistry.put(Pattern.class, new BuiltIn.PatternConverter());
            this.converterRegistry.put(UUID.class, new BuiltIn.UUIDConverter());
            this.converterRegistry.put(Currency.class, new BuiltIn.CurrencyConverter());
            this.converterRegistry.put(TimeZone.class, new BuiltIn.TimeZoneConverter());
            this.converterRegistry.put(ByteOrder.class, new BuiltIn.ByteOrderConverter());
            this.converterRegistry.put(Class.class, new BuiltIn.ClassConverter());
            this.converterRegistry.put(NetworkInterface.class, new BuiltIn.NetworkInterfaceConverter());
            if (!this.excl("java.sql.Time")) {
                try {
                    this.reg(Class.forName("java.sql.Time"), new BuiltIn.ISO8601TimeConverter(Class.forName("java.sql.Time").getDeclaredConstructor(Long.TYPE)));
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.sql.Time", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.sql.Connection")) {
                try {
                    this.reg(Class.forName("java.sql.Connection"), Class.forName("java.sql.DriverManager").getDeclaredMethod("getConnection", String.class), String.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.sql.Connection", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.sql.Driver")) {
                try {
                    this.reg(Class.forName("java.sql.Driver"), Class.forName("java.sql.DriverManager").getDeclaredMethod("getDriver", String.class), String.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.sql.DriverManager", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.sql.Timestamp")) {
                try {
                    this.reg(Class.forName("java.sql.Timestamp"), Class.forName("java.sql.Timestamp").getDeclaredMethod("valueOf", String.class), String.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.sql.Timestamp", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.Duration")) {
                try {
                    this.reg(Class.forName("java.time.Duration"), Class.forName("java.time.Duration").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.Duration", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.Instant")) {
                try {
                    this.reg(Class.forName("java.time.Instant"), Class.forName("java.time.Instant").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.Instant", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.LocalDate")) {
                try {
                    this.reg(Class.forName("java.time.LocalDate"), Class.forName("java.time.LocalDate").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.LocalDate", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.LocalDateTime")) {
                try {
                    this.reg(Class.forName("java.time.LocalDateTime"), Class.forName("java.time.LocalDateTime").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.LocalDateTime", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.LocalTime")) {
                try {
                    this.reg(Class.forName("java.time.LocalTime"), Class.forName("java.time.LocalTime").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.LocalTime", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.MonthDay")) {
                try {
                    this.reg(Class.forName("java.time.MonthDay"), Class.forName("java.time.MonthDay").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.MonthDay", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.OffsetDateTime")) {
                try {
                    this.reg(Class.forName("java.time.OffsetDateTime"), Class.forName("java.time.OffsetDateTime").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.OffsetDateTime", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.OffsetTime")) {
                try {
                    this.reg(Class.forName("java.time.OffsetTime"), Class.forName("java.time.OffsetTime").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.OffsetTime", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.Period")) {
                try {
                    this.reg(Class.forName("java.time.Period"), Class.forName("java.time.Period").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.Period", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.Year")) {
                try {
                    this.reg(Class.forName("java.time.Year"), Class.forName("java.time.Year").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.Year", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.YearMonth")) {
                try {
                    this.reg(Class.forName("java.time.YearMonth"), Class.forName("java.time.YearMonth").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.YearMonth", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.ZonedDateTime")) {
                try {
                    this.reg(Class.forName("java.time.ZonedDateTime"), Class.forName("java.time.ZonedDateTime").getDeclaredMethod("parse", CharSequence.class), CharSequence.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.ZonedDateTime", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.ZoneId")) {
                try {
                    this.reg(Class.forName("java.time.ZoneId"), Class.forName("java.time.ZoneId").getDeclaredMethod("of", String.class), String.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.ZoneId", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.time.ZoneOffset")) {
                try {
                    this.reg(Class.forName("java.time.ZoneOffset"), Class.forName("java.time.ZoneOffset").getDeclaredMethod("of", String.class), String.class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.time.ZoneOffset", CommandLine.this.tracer);
                }
            }
            if (!this.excl("java.nio.file.Path")) {
                try {
                    this.reg(Class.forName("java.nio.file.Path"), Class.forName("java.nio.file.Paths").getDeclaredMethod("get", String.class, String[].class), String.class, String[].class);
                }
                catch (Exception e) {
                    BuiltIn.handle(e, "java.nio.file.Path", CommandLine.this.tracer);
                }
            }
        }

        private boolean excl(String fqcn) {
            return BuiltIn.excluded(fqcn, CommandLine.this.tracer);
        }

        private void reg(Class<?> cls, Method method, Class<?> ... paramTypes) {
            this.converterRegistry.put(cls, new BuiltIn.ReflectionConverter(method, paramTypes));
        }

        private void reg(Class<?> timeClass, BuiltIn.ISO8601TimeConverter converter) {
            this.converterRegistry.put(timeClass, converter);
        }

        private Model.ParserSpec config() {
            return CommandLine.this.commandSpec.parser();
        }

        List<CommandLine> parse(String ... args) {
            Assert.notNull(args, "argument array");
            if (CommandLine.this.tracer.isInfo()) {
                CommandLine.this.tracer.info("Picocli version: %s%n", CommandLine.versionString());
            }
            if (CommandLine.this.tracer.isInfo()) {
                CommandLine.this.tracer.info("Parsing %d command line args %s%n", args.length, Arrays.toString(args));
            }
            if (CommandLine.this.tracer.isDebug()) {
                CommandLine.this.tracer.debug("Parser configuration: optionsCaseInsensitive=%s, subcommandsCaseInsensitive=%s, %s%n", CommandLine.this.commandSpec.optionsCaseInsensitive(), CommandLine.this.commandSpec.subcommandsCaseInsensitive(), this.config());
            }
            if (CommandLine.this.tracer.isDebug()) {
                CommandLine.this.tracer.debug("(ANSI is %s by default: systemproperty[picocli.ansi]=%s, isatty=%s, TERM=%s, OSTYPE=%s, isWindows=%s, JansiConsoleInstalled=%s, ANSICON=%s, ConEmuANSI=%s, NO_COLOR=%s, CLICOLOR=%s, CLICOLOR_FORCE=%s)%n", Help.Ansi.AUTO.enabled() ? "enabled" : "disabled", System.getProperty("picocli.ansi"), Help.Ansi.isTTY(), System.getenv("TERM"), System.getenv("OSTYPE"), Help.Ansi.isWindows(), Help.Ansi.isJansiConsoleInstalled(), System.getenv("ANSICON"), System.getenv("ConEmuANSI"), System.getenv("NO_COLOR"), System.getenv("CLICOLOR"), System.getenv("CLICOLOR_FORCE"));
            }
            ArrayList<String> expanded = new ArrayList<String>();
            for (String arg : args) {
                this.addOrExpand(arg, expanded, new LinkedHashSet<String>());
            }
            Stack<String> arguments = new Stack<String>();
            arguments.addAll(CommandLine.reverseList(expanded));
            ArrayList<CommandLine> result = new ArrayList<CommandLine>();
            this.parse(result, arguments, args, new ArrayList<Object>(), new HashSet<Model.ArgSpec>());
            return result;
        }

        private void addOrExpand(String arg, List<String> arguments, Set<String> visited) {
            if (this.config().expandAtFiles() && !arg.equals("@") && arg.startsWith("@")) {
                if ((arg = arg.substring(1)).startsWith("@")) {
                    if (CommandLine.this.tracer.isInfo()) {
                        CommandLine.this.tracer.info("Not expanding @-escaped argument %s (trimmed leading '@' char)%n", arg);
                    }
                } else {
                    if (CommandLine.this.tracer.isInfo()) {
                        CommandLine.this.tracer.info("Expanding argument file @%s%n", arg);
                    }
                    this.expandArgumentFile(arg, arguments, visited);
                    return;
                }
            }
            arguments.add(arg);
        }

        private void expandArgumentFile(String fileName, List<String> arguments, Set<String> visited) {
            File file = new File(fileName);
            if (!file.canRead()) {
                if (CommandLine.this.tracer.isInfo()) {
                    CommandLine.this.tracer.info("File %s does not exist or cannot be read; treating argument literally%n", fileName);
                }
                arguments.add("@" + fileName);
            } else if (visited.contains(file.getAbsolutePath())) {
                if (CommandLine.this.tracer.isInfo()) {
                    CommandLine.this.tracer.info("Already visited file %s; ignoring...%n", file.getAbsolutePath());
                }
            } else {
                this.expandValidArgumentFile(fileName, file, arguments, visited);
            }
        }

        private void expandValidArgumentFile(String fileName, File file, List<String> arguments, Set<String> visited) {
            ArrayList<String> result = new ArrayList<String>();
            LineNumberReader reader = null;
            try {
                visited.add(file.getAbsolutePath());
                reader = new LineNumberReader(new FileReader(file));
                if (CommandLine.this.commandSpec.parser().useSimplifiedAtFiles()) {
                    String token;
                    while ((token = reader.readLine()) != null) {
                        if (token.length() <= 0 || token.trim().startsWith(String.valueOf(CommandLine.this.commandSpec.parser().atFileCommentChar()))) continue;
                        this.addOrExpand(token, result, visited);
                    }
                } else {
                    StreamTokenizer tok = new StreamTokenizer(reader);
                    tok.resetSyntax();
                    tok.wordChars(32, 255);
                    tok.whitespaceChars(0, 32);
                    tok.quoteChar(34);
                    tok.quoteChar(39);
                    if (CommandLine.this.commandSpec.parser().atFileCommentChar() != null) {
                        tok.commentChar(CommandLine.this.commandSpec.parser().atFileCommentChar().charValue());
                    }
                    while (tok.nextToken() != -1) {
                        this.addOrExpand(tok.sval, result, visited);
                    }
                }
            }
            catch (Exception ex) {
                try {
                    throw new InitializationException("Could not read argument file @" + fileName, ex);
                }
                catch (Throwable throwable) {
                    CommandLine.close(reader);
                    throw throwable;
                }
            }
            CommandLine.close(reader);
            if (CommandLine.this.tracer.isInfo()) {
                CommandLine.this.tracer.info("Expanded file @%s to arguments %s%n", fileName, result);
            }
            arguments.addAll(result);
        }

        private void clear() {
            CommandLine.this.getCommandSpec().userObject();
            this.position = 0;
            this.endOfOptions = false;
            this.isHelpRequested = false;
            this.parseResultBuilder = ParseResult.builder(CommandLine.this.getCommandSpec());
            for (Model.OptionSpec option : CommandLine.this.getCommandSpec().options()) {
                this.clear(option);
            }
            for (Model.PositionalParamSpec positional : CommandLine.this.getCommandSpec().positionalParameters()) {
                this.clear(positional);
            }
            for (Model.ArgGroupSpec group : CommandLine.this.getCommandSpec().argGroups()) {
                this.clear(group);
            }
            for (Model.UnmatchedArgsBinding unmatched : CommandLine.this.getCommandSpec().unmatchedArgsBindings()) {
                unmatched.clear();
            }
        }

        private void clear(Model.ArgSpec argSpec) {
            argSpec.resetStringValues();
            argSpec.resetOriginalStringValues();
            argSpec.typedValues.clear();
            argSpec.typedValueAtPosition.clear();
            if (argSpec.inherited()) {
                CommandLine.this.tracer.debug("Not applying initial value for inherited %s%n", CommandLine.optionDescription("", argSpec, -1));
            } else if (argSpec.group() == null) {
                argSpec.applyInitialValue(CommandLine.this.tracer);
            }
        }

        private void clear(Model.ArgGroupSpec group) {
            for (Model.ArgSpec arg : group.args()) {
                this.clear(arg);
            }
            for (Model.ArgGroupSpec sub : group.subgroups()) {
                this.clear(sub);
            }
        }

        void maybeThrow(PicocliException ex) throws PicocliException {
            if (!CommandLine.this.commandSpec.parser().collectErrors) {
                throw ex;
            }
            this.parseResultBuilder.addError(ex);
        }

        private void parse(List<CommandLine> parsedCommands, Stack<String> argumentStack, String[] originalArgs, List<Object> nowProcessing, Collection<Model.ArgSpec> inheritedRequired) {
            this.parse(parsedCommands, argumentStack, originalArgs, nowProcessing, inheritedRequired, new LinkedHashSet<Model.ArgSpec>());
        }

        private void parse(List<CommandLine> parsedCommands, Stack<String> argumentStack, String[] originalArgs, List<Object> nowProcessing, Collection<Model.ArgSpec> inheritedRequired, Set<Model.ArgSpec> initialized) {
            if (CommandLine.this.tracer.isDebug()) {
                CommandLine.this.tracer.debug("Initializing %s: %d options, %d positional parameters, %d required, %d groups, %d subcommands.%n", CommandLine.this.commandSpec.toString(), new HashSet<Model.OptionSpec>(CommandLine.this.commandSpec.optionsMap().values()).size(), CommandLine.this.commandSpec.positionalParameters().size(), CommandLine.this.commandSpec.requiredArgs().size(), CommandLine.this.commandSpec.argGroups().size(), CommandLine.this.commandSpec.subcommands().size());
            }
            this.clear();
            parsedCommands.add(CommandLine.this);
            ArrayList<Model.ArgSpec> required = new ArrayList<Model.ArgSpec>(CommandLine.this.commandSpec.requiredArgs());
            this.addPostponedRequiredArgs(inheritedRequired, required);
            Collections.sort(required, new PositionalParametersSorter());
            boolean continueOnError = CommandLine.this.commandSpec.parser().collectErrors();
            Map info = CommandLine.mapOf("versionHelpRequested", this.parseResultBuilder.versionHelpRequested, new Object[]{"usageHelpRequested", this.parseResultBuilder.usageHelpRequested});
            if (CommandLine.this.commandSpec.preprocessor().preprocess(argumentStack, CommandLine.this.commandSpec, null, info)) {
                this.parseResultBuilder.versionHelpRequested = (Boolean)info.get("versionHelpRequested");
                this.parseResultBuilder.usageHelpRequested = (Boolean)info.get("usageHelpRequested");
                return;
            }
            do {
                int stackSize = argumentStack.size();
                try {
                    this.processArguments(parsedCommands, argumentStack, required, initialized, originalArgs, nowProcessing);
                    this.applyDefaultValues(required, initialized);
                }
                catch (InitializationException ex) {
                    this.maybeThrow(ex);
                }
                catch (ParameterException ex) {
                    this.maybeThrow(ex);
                }
                catch (Exception ex) {
                    int offendingArgIndex = originalArgs.length - argumentStack.size() - 1;
                    String arg = offendingArgIndex >= 0 && offendingArgIndex < originalArgs.length ? originalArgs[offendingArgIndex] : "?";
                    this.maybeThrow(ParameterException.create(CommandLine.this, ex, arg, offendingArgIndex, originalArgs));
                }
                if (!continueOnError || stackSize != argumentStack.size() || stackSize <= 0) continue;
                this.parseResultBuilder.addUnmatched(this.parseResultBuilder.totalArgCount() - argumentStack.size(), argumentStack.pop());
            } while (!argumentStack.isEmpty() && continueOnError);
            boolean anyHelpRequested = this.isAnyHelpRequested();
            CommandLine parsed = CommandLine.this;
            while (parsed.getParent() != null) {
                parsed = parsed.getParent();
                anyHelpRequested |= parsed.interpreter.isAnyHelpRequested();
            }
            if (!anyHelpRequested) {
                this.validateConstraints(argumentStack, required, initialized);
            }
        }

        private void addPostponedRequiredArgs(Collection<Model.ArgSpec> inheritedRequired, List<Model.ArgSpec> required) {
            for (Model.ArgSpec postponed : inheritedRequired) {
                if (postponed.isOption()) {
                    Model.OptionSpec inherited = CommandLine.this.commandSpec.findOption(((Model.OptionSpec)postponed).longestName());
                    Assert.notNull(inherited, "inherited option " + postponed);
                    required.add(inherited);
                    continue;
                }
                Model.PositionalParamSpec positional = (Model.PositionalParamSpec)postponed;
                for (Model.PositionalParamSpec existing : CommandLine.this.commandSpec.positionalParameters()) {
                    if (!existing.inherited() || !existing.index().equals(positional.index()) || !existing.arity().equals(positional.arity()) || !existing.typeInfo().equals(positional.typeInfo()) || !Assert.equals(existing.paramLabel(), positional.paramLabel()) || !Assert.equals(existing.hideParamSyntax(), positional.hideParamSyntax()) || !Assert.equals(existing.required(), positional.required()) || !Assert.equals(existing.splitRegex(), positional.splitRegex()) || !Arrays.equals(existing.description(), positional.description()) || !Assert.equals(existing.descriptionKey(), positional.descriptionKey()) || !Assert.equals(existing.parameterConsumer(), positional.parameterConsumer())) continue;
                    required.add(existing);
                }
            }
        }

        private void validateConstraints(Stack<String> argumentStack, List<Model.ArgSpec> required, Set<Model.ArgSpec> matched) {
            if (!required.isEmpty()) {
                for (Model.ArgSpec missing : required) {
                    Assert.assertTrue(missing.group() == null, "Arguments in a group are not necessarily required for the command");
                    if (missing.isOption()) {
                        this.maybeThrow(MissingParameterException.create(CommandLine.this, required, this.config().separator()));
                        continue;
                    }
                    this.assertNoMissingParameters(missing, missing.arity(), argumentStack);
                }
            }
            if (!this.parseResultBuilder.unmatched.isEmpty()) {
                String[] unmatched = this.parseResultBuilder.unmatched.toArray(new String[0]);
                for (Model.UnmatchedArgsBinding unmatchedArgsBinding : CommandLine.this.getCommandSpec().unmatchedArgsBindings()) {
                    unmatchedArgsBinding.addAll((String[])unmatched.clone());
                }
                if (!CommandLine.this.isUnmatchedArgumentsAllowed()) {
                    this.maybeThrow(new UnmatchedArgumentException(CommandLine.this, Collections.unmodifiableList(this.parseResultBuilder.unmatched)));
                }
                if (CommandLine.this.tracer.isInfo()) {
                    CommandLine.this.tracer.info("Unmatched arguments: %s%n", this.parseResultBuilder.unmatched);
                }
            }
            ParseResult pr = this.parseResultBuilder.build();
            pr.validateGroups();
        }

        private void applyDefaultValues(List<Model.ArgSpec> required, Set<Model.ArgSpec> initialized) throws Exception {
            this.parseResultBuilder.isInitializingDefaultValues = true;
            CommandLine.this.tracer.debug("Applying default values for command '%s'%n", CommandLine.this.commandSpec.qualifiedName());
            for (Model.ArgSpec arg : CommandLine.this.commandSpec.args()) {
                if (arg.group() != null || initialized.contains(arg)) continue;
                if (arg.inherited()) {
                    CommandLine.this.tracer.debug("Not applying default value for inherited %s%n", CommandLine.optionDescription("", arg, -1));
                    continue;
                }
                if (!this.applyDefault(CommandLine.this.commandSpec.defaultValueProvider(), arg)) continue;
                required.remove(arg);
            }
            for (Model.ArgGroupSpec group : CommandLine.this.commandSpec.argGroups()) {
                this.applyGroupDefaults(CommandLine.this.commandSpec.defaultValueProvider(), group, required, initialized);
            }
            this.parseResultBuilder.isInitializingDefaultValues = false;
        }

        private void applyGroupDefaults(IDefaultValueProvider defaultValueProvider, Model.ArgGroupSpec group, List<Model.ArgSpec> required, Set<Model.ArgSpec> initialized) throws Exception {
            CommandLine.this.tracer.debug("Applying default values for group '%s'%n", group.synopsis());
            for (Model.ArgSpec arg : group.args()) {
                if (arg.scope().get() == null || initialized.contains(arg)) continue;
                if (arg.inherited()) {
                    CommandLine.this.tracer.debug("Not applying default value for inherited %s%n", CommandLine.optionDescription("", arg, -1));
                    continue;
                }
                if (!this.applyDefault(defaultValueProvider, arg)) continue;
                required.remove(arg);
            }
            for (Model.ArgGroupSpec sub : group.subgroups()) {
                this.applyGroupDefaults(defaultValueProvider, sub, required, initialized);
            }
        }

        private boolean applyDefault(IDefaultValueProvider defaultValueProvider, Model.ArgSpec arg) throws Exception {
            String defaultValue;
            String fromProvider = defaultValueProvider == null ? null : defaultValueProvider.defaultValue(arg);
            String string = defaultValue = fromProvider == null ? arg.defaultValue() : fromProvider;
            if (defaultValue != null && !"_NULL_".equals(defaultValue)) {
                String provider;
                String string2 = provider = defaultValueProvider == null ? "" : " from " + defaultValueProvider.toString();
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("Applying defaultValue (%s)%s to %s on %s%n", defaultValue, provider, arg, arg.scopeString());
                }
                Range arity = arg.arity().min(Math.max(1, arg.arity().min));
                this.applyOption(arg, false, LookBehind.SEPARATE, false, arity, this.stack(defaultValue), new HashSet<Model.ArgSpec>(), arg.toString);
            } else if (arg.typeInfo().isOptional()) {
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("Applying Optional.empty() to %s on %s%n", arg, arg.scopeString());
                }
                arg.setValue(CommandLine.getOptionalEmpty());
            } else {
                CommandLine.this.tracer.debug("defaultValue not defined for %s%n", arg);
            }
            return defaultValue != null;
        }

        private Stack<String> stack(String value) {
            Stack<String> result = new Stack<String>();
            result.push(value);
            return result;
        }

        private void processArguments(List<CommandLine> parsedCommands, Stack<String> args, Collection<Model.ArgSpec> required, Set<Model.ArgSpec> initialized, String[] originalArgs, List<Object> nowProcessing) throws Exception {
            if (this.parseResultBuilder.expandedArgList.isEmpty()) {
                ArrayList<String> expandedArgs = new ArrayList<String>(args);
                Collections.reverse(expandedArgs);
                this.parseResultBuilder.expandedArgs(expandedArgs);
                this.parseResultBuilder.originalArgs(originalArgs);
                this.parseResultBuilder.nowProcessing = nowProcessing;
            }
            String separator = this.config().separator();
            while (!args.isEmpty()) {
                String arg;
                boolean actuallyUnquoted;
                if (this.endOfOptions) {
                    this.processRemainderAsPositionalParameters(required, initialized, args);
                    return;
                }
                String originalArg = args.pop();
                boolean bl = actuallyUnquoted = !originalArg.equals(arg = CommandLine.this.smartUnquoteIfEnabled(originalArg));
                if (CommandLine.this.tracer.isDebug()) {
                    int argIndex = originalArgs.length - (args.size() + 1);
                    if (actuallyUnquoted) {
                        CommandLine.this.tracer.debug("[%d] Processing argument '%s' (trimmed from '%s'). Remainder=%s%n", argIndex, arg, originalArg, CommandLine.reverse(CommandLine.copy(args)));
                    } else {
                        CommandLine.this.tracer.debug("[%d] Processing argument '%s'. Remainder=%s%n", argIndex, arg, CommandLine.reverse(CommandLine.copy(args)));
                    }
                }
                if (CommandLine.this.commandSpec.parser.endOfOptionsDelimiter().equals(arg)) {
                    CommandLine.this.tracer.info("Found end-of-options delimiter '%s'. Treating remainder as positional parameters.%n", CommandLine.this.commandSpec.parser.endOfOptionsDelimiter());
                    this.endOfOptions = true;
                    this.processRemainderAsPositionalParameters(required, initialized, args);
                    return;
                }
                if (CommandLine.this.commandSpec.parser().abbreviatedSubcommandsAllowed()) {
                    arg = AbbreviationMatcher.match(CommandLine.this.commandSpec.subcommands().keySet(), arg, CommandLine.this.commandSpec.subcommandsCaseInsensitive(), CommandLine.this);
                }
                if (CommandLine.this.commandSpec.subcommands().containsKey(arg)) {
                    CommandLine subcommand = CommandLine.this.commandSpec.subcommands().get(arg);
                    this.processSubcommand(subcommand, this.parseResultBuilder, parsedCommands, args, required, initialized, originalArgs, nowProcessing, separator, arg);
                    return;
                }
                if (CommandLine.this.commandSpec.parent() != null && CommandLine.this.commandSpec.parent().subcommandsRepeatable() && CommandLine.this.commandSpec.parent().subcommands().containsKey(arg)) {
                    CommandLine.this.tracer.debug("'%s' is a repeatable subcommand of %s%n", arg, CommandLine.this.commandSpec.parent().qualifiedName());
                    CommandLine subcommand = CommandLine.this.commandSpec.parent().subcommands().get(arg);
                    Set<Model.ArgSpec> inheritedInitialized = initialized;
                    if (((CommandLine)subcommand).interpreter.parseResultBuilder != null) {
                        CommandLine.this.tracer.debug("Subcommand '%s' has been matched before. Making a copy...%n", subcommand.getCommandName());
                        subcommand = subcommand.copy();
                        subcommand.getCommandSpec().parent(CommandLine.this.commandSpec.parent());
                        inheritedInitialized = new LinkedHashSet<Model.ArgSpec>(inheritedInitialized);
                    }
                    this.processSubcommand(subcommand, ((CommandLine)CommandLine.this.getParent()).interpreter.parseResultBuilder, parsedCommands, args, required, inheritedInitialized, originalArgs, nowProcessing, separator, arg);
                    continue;
                }
                LinkedHashSet<String> aggregatedOptionNames = new LinkedHashSet<String>();
                if (CommandLine.this.commandSpec.parser().abbreviatedOptionsAllowed()) {
                    aggregatedOptionNames.addAll(CommandLine.this.commandSpec.optionsMap().keySet());
                    aggregatedOptionNames.addAll(CommandLine.this.commandSpec.negatedOptionsMap().keySet());
                    arg = AbbreviationMatcher.match(aggregatedOptionNames, arg, CommandLine.this.commandSpec.optionsCaseInsensitive(), CommandLine.this);
                }
                LookBehind lookBehind = LookBehind.SEPARATE;
                int separatorIndex = arg.indexOf(separator);
                if (separatorIndex > 0) {
                    String key = arg.substring(0, separatorIndex);
                    if (this.isStandaloneOption(key = AbbreviationMatcher.match(aggregatedOptionNames, key, CommandLine.this.commandSpec.optionsCaseInsensitive(), CommandLine.this)) && this.isStandaloneOption(arg)) {
                        CommandLine.this.tracer.warn("Both '%s' and '%s' are valid option names in %s. Using '%s'...%n", arg, key, CommandLine.this.getCommandName(), arg);
                    } else if (this.isStandaloneOption(key)) {
                        lookBehind = LookBehind.ATTACHED_WITH_SEPARATOR;
                        String optionParam = arg.substring(separatorIndex + separator.length());
                        args.push(optionParam);
                        arg = key;
                        if (CommandLine.this.tracer.isDebug()) {
                            CommandLine.this.tracer.debug("Separated '%s' option from '%s' option parameter%n", key, optionParam);
                        }
                    } else if (CommandLine.this.tracer.isDebug()) {
                        CommandLine.this.tracer.debug("'%s' contains separator '%s' but '%s' is not a known option%n", arg, separator, key);
                    }
                } else if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("'%s' cannot be separated into <option>%s<option-parameter>%n", arg, separator);
                }
                if (this.isStandaloneOption(arg)) {
                    this.processStandaloneOption(required, initialized, arg, actuallyUnquoted, args, lookBehind);
                    continue;
                }
                if (this.config().posixClusteredShortOptionsAllowed() && arg.length() > 2 && arg.startsWith("-")) {
                    if (CommandLine.this.tracer.isDebug()) {
                        CommandLine.this.tracer.debug("Trying to process '%s' as clustered short options%n", arg, args);
                    }
                    this.processClusteredShortOptions(required, initialized, arg, actuallyUnquoted, args);
                    continue;
                }
                args.push(arg);
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("Could not find option '%s', deciding whether to treat as unmatched option or positional parameter...%n", arg);
                }
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("No option named '%s' found. Processing as positional parameter%n", arg);
                }
                this.processPositionalParameter(required, initialized, actuallyUnquoted, args);
            }
        }

        private void processSubcommand(CommandLine subcommand, ParseResult.Builder builder, List<CommandLine> parsedCommands, Stack<String> args, Collection<Model.ArgSpec> required, Set<Model.ArgSpec> initialized, String[] originalArgs, List<Object> nowProcessing, String separator, String arg) {
            if (CommandLine.this.tracer.isDebug()) {
                CommandLine.this.tracer.debug("Found subcommand '%s' (%s)%n", arg, subcommand.commandSpec.toString());
            }
            nowProcessing.add(subcommand.commandSpec);
            this.updateHelpRequested(subcommand.commandSpec);
            ArrayList<Model.ArgSpec> inheritedRequired = new ArrayList<Model.ArgSpec>();
            if (CommandLine.this.tracer.isDebug()) {
                CommandLine.this.tracer.debug("Checking required args for parent %s...%n", subcommand.commandSpec.parent());
            }
            Iterator<Model.ArgSpec> requiredIter = required.iterator();
            while (requiredIter.hasNext()) {
                Model.ArgSpec requiredArg = requiredIter.next();
                if (requiredArg.scopeType() != ScopeType.INHERIT && !requiredArg.inherited()) continue;
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("Postponing validation for required %s: scopeType=%s, inherited=%s%n", new Object[]{CommandLine.optionDescription("", requiredArg, -1), requiredArg.scopeType(), requiredArg.inherited()});
                }
                if (!inheritedRequired.contains(requiredArg)) {
                    inheritedRequired.add(requiredArg);
                }
                requiredIter.remove();
            }
            if (!this.isAnyHelpRequested() && !required.isEmpty()) {
                throw MissingParameterException.create(CommandLine.this, required, separator);
            }
            LinkedHashSet<Model.ArgSpec> inheritedInitialized = new LinkedHashSet<Model.ArgSpec>();
            subcommand.interpreter.parse(parsedCommands, args, originalArgs, nowProcessing, inheritedRequired, inheritedInitialized);
            initialized.addAll(inheritedInitialized);
            builder.subcommand(((CommandLine)subcommand).interpreter.parseResultBuilder.build());
        }

        private boolean isStandaloneOption(String arg) {
            return CommandLine.this.commandSpec.optionsMap().containsKey(arg) || CommandLine.this.commandSpec.negatedOptionsMap().containsKey(arg);
        }

        private void handleUnmatchedArgument(Stack<String> args) throws Exception {
            if (!args.isEmpty()) {
                this.parseResultBuilder.addUnmatched(this.parseResultBuilder.totalArgCount() - args.size(), args.pop());
            }
            if (this.config().stopAtUnmatched()) {
                this.parseResultBuilder.addUnmatched(args);
            }
        }

        private void processRemainderAsPositionalParameters(Collection<Model.ArgSpec> required, Set<Model.ArgSpec> initialized, Stack<String> args) throws Exception {
            while (!args.empty()) {
                this.processPositionalParameter(required, initialized, false, args);
            }
        }

        private void processPositionalParameter(Collection<Model.ArgSpec> required, Set<Model.ArgSpec> initialized, boolean alreadyUnquoted, Stack<String> args) throws Exception {
            String arg = args.peek();
            if (!this.endOfOptions && CommandLine.this.commandSpec.resemblesOption(arg, CommandLine.this.tracer)) {
                if (!CommandLine.this.commandSpec.parser().unmatchedOptionsArePositionalParams()) {
                    this.handleUnmatchedArgument(args);
                    return;
                }
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("Parser is configured to treat all unmatched options as positional parameter%n", arg);
                }
            }
            int argIndex = this.parseResultBuilder.originalArgList.size() - args.size();
            if (CommandLine.this.tracer.isDebug()) {
                CommandLine.this.tracer.debug("[%d] Processing next arg as a positional parameter. Command-local position=%d. Remainder=%s%n", argIndex, this.position, CommandLine.reverse(CommandLine.copy(args)));
            }
            if (this.config().stopAtPositional()) {
                if (!this.endOfOptions && CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("Parser was configured with stopAtPositional=true, treating remaining arguments as positional parameters.%n", new Object[0]);
                }
                this.endOfOptions = true;
            }
            int originalInteractiveCount = this.interactiveCount;
            int consumedByGroup = 0;
            int argsConsumed = 0;
            int interactiveConsumed = 0;
            int originalNowProcessingSize = this.parseResultBuilder.nowProcessing.size();
            ArrayList<1> bookKeeping = new ArrayList<1>();
            for (int i = 0; i < CommandLine.this.commandSpec.positionalParameters().size(); ++i) {
                ParseResult.GroupMatchContainer groupMatchContainer;
                Model.PositionalParamSpec positionalParam = CommandLine.this.commandSpec.positionalParameters().get(i);
                Range range = positionalParam.index();
                int localPosition = this.getPosition(positionalParam);
                if (positionalParam.group() != null ? !(groupMatchContainer = this.parseResultBuilder.groupMatchContainer.findOrCreateMatchingGroup(positionalParam, CommandLine.this.commandSpec.commandLine())).canMatchPositionalParam(positionalParam) : !range.contains(localPosition) || positionalParam.typedValueAtPosition.get(localPosition) != null) continue;
                Stack argsCopy = CommandLine.copy(args);
                Range arity = positionalParam.arity();
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("Position %s is in index range %s. Trying to assign args to %s, arity=%s%n", this.positionDesc(positionalParam), range.internalToString(), positionalParam, arity);
                }
                if (!this.assertNoMissingParameters(positionalParam, arity, argsCopy)) break;
                int originalSize = argsCopy.size();
                int actuallyConsumed = this.applyOption(positionalParam, false, LookBehind.SEPARATE, alreadyUnquoted, arity, argsCopy, initialized, "args[" + range.internalToString() + "] at position " + localPosition);
                int count = originalSize - argsCopy.size();
                if (count > 0 || actuallyConsumed > 0) {
                    required.remove(positionalParam);
                    interactiveConsumed = this.interactiveCount - originalInteractiveCount;
                }
                if (positionalParam.group() == null) {
                    argsConsumed = Math.max(argsConsumed, count);
                } else {
                    final int updatedPosition = localPosition + count;
                    final ParseResult.GroupMatchContainer groupMatchContainer2 = this.parseResultBuilder.groupMatchContainer.findOrCreateMatchingGroup(positionalParam, CommandLine.this.commandSpec.commandLine());
                    bookKeeping.add(new Runnable(){

                        public void run() {
                            if (groupMatchContainer2 != null) {
                                groupMatchContainer2.lastMatch().position = updatedPosition;
                                if (CommandLine.this.tracer.isDebug()) {
                                    CommandLine.this.tracer.debug("Updated group position to %s for group %s.%n", groupMatchContainer2.lastMatch().position, groupMatchContainer2);
                                }
                            }
                        }
                    });
                    consumedByGroup = Math.max(consumedByGroup, count);
                }
                while (this.parseResultBuilder.nowProcessing.size() > originalNowProcessingSize + count) {
                    this.parseResultBuilder.nowProcessing.remove(this.parseResultBuilder.nowProcessing.size() - 1);
                }
            }
            int maxConsumed = Math.max(consumedByGroup, argsConsumed);
            for (int i = 0; i < maxConsumed; ++i) {
                args.pop();
            }
            this.position += argsConsumed + interactiveConsumed;
            if (CommandLine.this.tracer.isDebug()) {
                CommandLine.this.tracer.debug("Consumed %d arguments and %d interactive values, moving command-local position to index %d.%n", argsConsumed, interactiveConsumed, this.position);
            }
            for (Runnable runnable : bookKeeping) {
                runnable.run();
            }
            if (consumedByGroup == 0 && argsConsumed == 0 && interactiveConsumed == 0 && !args.isEmpty()) {
                this.handleUnmatchedArgument(args);
            }
        }

        private void processStandaloneOption(Collection<Model.ArgSpec> required, Set<Model.ArgSpec> initialized, String arg, boolean alreadyUnquoted, Stack<String> args, LookBehind lookBehind) throws Exception {
            boolean negated;
            Model.ArgSpec argSpec = CommandLine.this.commandSpec.optionsMap().get(arg);
            boolean bl = negated = argSpec == null;
            if (negated) {
                argSpec = CommandLine.this.commandSpec.negatedOptionsMap().get(arg);
            }
            required.remove(argSpec);
            Range arity = argSpec.arity();
            if (lookBehind.isAttached()) {
                arity = arity.min(Math.max(1, arity.min));
            }
            if (CommandLine.this.tracer.isDebug()) {
                CommandLine.this.tracer.debug("Found option named '%s': %s, arity=%s%n", arg, argSpec, arity);
            }
            this.parseResultBuilder.nowProcessing.add(argSpec);
            this.applyOption(argSpec, negated, lookBehind, alreadyUnquoted, arity, args, initialized, "option " + arg);
        }

        private void processClusteredShortOptions(Collection<Model.ArgSpec> required, Set<Model.ArgSpec> initialized, String arg, boolean alreadyUnquoted, Stack<String> args) throws Exception {
            String prefix = arg.substring(0, 1);
            String cluster = arg.substring(1);
            boolean paramAttachedToOption = true;
            boolean first = true;
            while (cluster.length() > 0 && CommandLine.this.commandSpec.posixOptionsMap().containsKey(Character.valueOf(cluster.charAt(0)))) {
                LookBehind lookBehind;
                Model.ArgSpec argSpec = CommandLine.this.commandSpec.posixOptionsMap().get(Character.valueOf(cluster.charAt(0)));
                Range arity = argSpec.arity();
                String argDescription = "option " + prefix + cluster.charAt(0);
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("Found option '%s%s' in %s: %s, arity=%s%n", prefix, Character.valueOf(cluster.charAt(0)), arg, argSpec, arity);
                }
                required.remove(argSpec);
                cluster = cluster.substring(1);
                paramAttachedToOption = cluster.length() > 0;
                LookBehind lookBehind2 = lookBehind = paramAttachedToOption ? LookBehind.ATTACHED : LookBehind.SEPARATE;
                if (cluster.startsWith(this.config().separator())) {
                    lookBehind = LookBehind.ATTACHED_WITH_SEPARATOR;
                    cluster = cluster.substring(this.config().separator().length());
                    arity = arity.min(Math.max(1, arity.min));
                }
                if (arity.min > 0 && !CommandLine.empty(cluster) && CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("Trying to process '%s' as option parameter%n", cluster);
                }
                if (!CommandLine.empty(cluster)) {
                    args.push(cluster);
                }
                if (first) {
                    this.parseResultBuilder.nowProcessing.add(argSpec);
                    first = false;
                } else {
                    this.parseResultBuilder.nowProcessing.set(this.parseResultBuilder.nowProcessing.size() - 1, argSpec);
                }
                int argCount = args.size();
                int consumed = this.applyOption(argSpec, false, lookBehind, alreadyUnquoted, arity, args, initialized, argDescription);
                if (CommandLine.empty(cluster) || args.isEmpty() || args.size() < argCount) {
                    return;
                }
                cluster = args.pop();
            }
            if (cluster.length() == 0) {
                return;
            }
            if (arg.endsWith(cluster)) {
                args.push(paramAttachedToOption ? prefix + cluster : cluster);
                if (args.peek().equals(arg)) {
                    if (CommandLine.this.tracer.isDebug()) {
                        CommandLine.this.tracer.debug("Could not match any short options in %s, deciding whether to treat as unmatched option or positional parameter...%n", arg);
                    }
                    this.processPositionalParameter(required, initialized, alreadyUnquoted, args);
                    return;
                }
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("No option found for %s in %s%n", cluster, arg);
                }
                String tmp = args.pop();
                tmp = tmp + " (while processing option: '" + arg + "')";
                args.push(tmp);
                this.handleUnmatchedArgument(args);
            } else {
                args.push(cluster);
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("%s is not an option parameter for %s%n", cluster, arg);
                }
                this.processPositionalParameter(required, initialized, alreadyUnquoted, args);
            }
        }

        private int applyOption(Model.ArgSpec argSpec, boolean negated, LookBehind lookBehind, boolean alreadyUnquoted, Range arity, Stack<String> args, Set<Model.ArgSpec> initialized, String argDescription) throws Exception {
            this.updateHelpRequested(argSpec);
            this.parseResultBuilder.beforeMatchingGroupElement(argSpec);
            int originalSize = args.size();
            Map info = CommandLine.mapOf("separator", lookBehind.toString(CommandLine.this.commandSpec.parser().separator()), new Object[]{"negated", negated, "unquoted", alreadyUnquoted, "versionHelpRequested", this.parseResultBuilder.versionHelpRequested, "usageHelpRequested", this.parseResultBuilder.usageHelpRequested});
            boolean done = argSpec.preprocessor().preprocess(args, CommandLine.this.commandSpec, argSpec, info);
            this.parseResultBuilder.versionHelpRequested = (Boolean)info.get("versionHelpRequested");
            this.parseResultBuilder.usageHelpRequested = (Boolean)info.get("usageHelpRequested");
            negated = (Boolean)info.get("negated");
            alreadyUnquoted = (Boolean)info.get("unquoted");
            lookBehind = LookBehind.parse(String.valueOf(info.get("separator")));
            if (done) {
                return args.size() - originalSize;
            }
            if (argSpec.parameterConsumer() != null) {
                argSpec.parameterConsumer().consumeParameters(args, argSpec, CommandLine.this.commandSpec);
                return args.size() - originalSize;
            }
            boolean consumeOnlyOne = CommandLine.this.commandSpec.parser().aritySatisfiedByAttachedOptionParam() && lookBehind.isAttached();
            Stack<String> workingStack = args;
            if (consumeOnlyOne) {
                workingStack = args.isEmpty() ? args : this.stack(args.pop());
            } else if (!this.assertNoMissingParameters(argSpec, arity, args)) {
                return 0;
            }
            int result = argSpec.type().isArray() && (!argSpec.interactive() || argSpec.type() != char[].class) ? this.applyValuesToArrayField(argSpec, negated, lookBehind, alreadyUnquoted, arity, workingStack, initialized, argDescription) : (Collection.class.isAssignableFrom(argSpec.type()) ? this.applyValuesToCollectionField(argSpec, negated, lookBehind, alreadyUnquoted, arity, workingStack, initialized, argDescription) : (Map.class.isAssignableFrom(argSpec.type()) ? this.applyValuesToMapField(argSpec, lookBehind, alreadyUnquoted, arity, workingStack, initialized, argDescription) : this.applyValueToSingleValuedField(argSpec, negated, lookBehind, alreadyUnquoted, arity, workingStack, initialized, argDescription)));
            if (workingStack != args && !workingStack.isEmpty()) {
                args.push(workingStack.pop());
                Assert.assertTrue(workingStack.isEmpty(), "Working stack should be empty but was " + new ArrayList<String>(workingStack));
            }
            return result;
        }

        private void addToInitialized(Model.ArgSpec argSpec, Set<Model.ArgSpec> initialized) {
            initialized.add(argSpec);
            Model.ArgSpec rootArgSpec = argSpec.root();
            if (rootArgSpec != null) {
                initialized.add(rootArgSpec);
            }
        }

        private int applyValueToSingleValuedField(Model.ArgSpec argSpec, boolean negated, LookBehind lookBehind, boolean alreadyUnquoted, Range derivedArity, Stack<String> args, Set<Model.ArgSpec> initialized, String argDescription) throws Exception {
            Object newValue;
            Range arity;
            String value;
            boolean noMoreValues = args.isEmpty();
            String quotedValue = value = noMoreValues ? null : args.pop();
            if (CommandLine.this.commandSpec.parser().trimQuotes() && !alreadyUnquoted) {
                value = CommandLine.unquote(value);
            }
            Range range = arity = argSpec.arity().isUnspecified ? derivedArity : argSpec.arity();
            if (arity.max == 0 && !arity.isUnspecified && lookBehind == LookBehind.ATTACHED_WITH_SEPARATOR) {
                throw new MaxValuesExceededException(CommandLine.this, CommandLine.optionDescription("", argSpec, 0) + " should be specified without '" + value + "' parameter");
            }
            if (arity.min > 0) {
                args.push(quotedValue);
                boolean discontinue = this.assertNoMissingMandatoryParameter(argSpec, args, 0, arity) || this.isArgResemblesOptionThereforeDiscontinue(argSpec, args, 0, arity);
                args.pop();
                if (discontinue) {
                    return 0;
                }
            }
            int consumed = arity.min;
            String actualValue = value;
            Object interactiveValue = null;
            Class<?> cls = argSpec.auxiliaryTypes()[0];
            if (arity.min <= 0) {
                boolean optionalValueExists = true;
                consumed = 1;
                if (cls == Boolean.class || cls == Boolean.TYPE) {
                    boolean optionalWithBooleanValue;
                    boolean bl = optionalWithBooleanValue = arity.max > 0 && ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value));
                    if (!optionalWithBooleanValue && lookBehind != LookBehind.ATTACHED_WITH_SEPARATOR) {
                        Boolean oppositeValue;
                        Boolean defaultValue = this.booleanValue(argSpec, argSpec.calcDefaultValue(true));
                        if (argSpec.isOption() && !CommandLine.empty(((Model.OptionSpec)argSpec).fallbackValue())) {
                            defaultValue = !this.booleanValue(argSpec, ((Model.OptionSpec)argSpec).fallbackValue());
                        }
                        Boolean bl2 = oppositeValue = CommandLine.this.commandSpec.parser().toggleBooleanFlags() ? (Boolean)argSpec.getValue() : defaultValue;
                        if (oppositeValue == null) {
                            oppositeValue = false;
                        }
                        actualValue = String.valueOf(oppositeValue == false);
                        if (argSpec.isOption() && ((Model.OptionSpec)argSpec).negatable() && negated) {
                            actualValue = String.valueOf(oppositeValue);
                        }
                        optionalValueExists = false;
                        consumed = 0;
                    }
                } else {
                    String fallbackValue;
                    String string = fallbackValue = argSpec.isOption() ? ((Model.OptionSpec)argSpec).fallbackValue() : "";
                    if (this.isOption(value)) {
                        actualValue = fallbackValue;
                        optionalValueExists = false;
                        consumed = 0;
                    } else if (value == null) {
                        actualValue = fallbackValue;
                        optionalValueExists = false;
                        consumed = 0;
                    }
                }
                if (argSpec.interactive() && (arity.max == 0 || !optionalValueExists)) {
                    interactiveValue = this.readUserInput(argSpec);
                    consumed = 0;
                }
            }
            if (consumed == 0) {
                if (value != null) {
                    args.push(value);
                }
            } else if (!lookBehind.isAttached()) {
                this.parseResultBuilder.nowProcessing(argSpec, value);
            }
            String initValueMessage = "Setting %s to '%3$s' (was '%2$s') for %4$s on %5$s%n";
            String overwriteValueMessage = "Overwriting %s value '%s' with '%s' for %s on %s%n";
            Object object = newValue = interactiveValue != null ? interactiveValue : actualValue;
            if (noMoreValues && actualValue == null && interactiveValue == null) {
                consumed = 0;
            } else {
                consumed = 1;
                if (interactiveValue != null) {
                    if (argSpec.echo()) {
                        initValueMessage = "Setting %s to %3$s (interactive value) for %4$s on %5$s%n";
                        overwriteValueMessage = "Overwriting %s value with %3$s (interactive value) for %s on %5$s%n";
                    } else {
                        initValueMessage = "Setting %s to *** (masked interactive value) for %4$s on %5$s%n";
                        overwriteValueMessage = "Overwriting %s value with *** (masked interactive value) for %s on %5$s%n";
                    }
                }
                if (!char[].class.equals(cls) && !char[].class.equals(argSpec.type())) {
                    if (interactiveValue != null) {
                        actualValue = new String((char[])interactiveValue);
                    }
                    ITypeConverter<?> converter = this.getTypeConverter(argSpec.auxiliaryTypes(), argSpec, 0);
                    newValue = this.tryConvert(argSpec, -1, converter, actualValue, 0);
                } else if (interactiveValue == null) {
                    newValue = actualValue.toCharArray();
                } else {
                    actualValue = this.getMaskedValue(argSpec, new String((char[])interactiveValue));
                    newValue = interactiveValue;
                }
            }
            Object oldValue = argSpec.getValue();
            String traceMessage = initValueMessage;
            if (argSpec.group() == null && initialized.contains(argSpec)) {
                if (!CommandLine.this.isOverwrittenOptionsAllowed()) {
                    throw new OverwrittenOptionException(CommandLine.this, argSpec, CommandLine.optionDescription("", argSpec, 0) + " should be specified only once");
                }
                traceMessage = overwriteValueMessage;
            }
            this.addToInitialized(argSpec, initialized);
            if (argSpec.typeInfo().isOptional()) {
                newValue = CommandLine.getOptionalOfNullable(newValue);
            }
            if (CommandLine.this.tracer.isInfo()) {
                CommandLine.this.tracer.info(traceMessage, argSpec.toString(), String.valueOf(oldValue), String.valueOf(newValue), argDescription, argSpec.scopeString());
            }
            int pos = this.getPosition(argSpec);
            argSpec.setValue(newValue);
            this.parseResultBuilder.addOriginalStringValue(argSpec, actualValue);
            this.parseResultBuilder.addStringValue(argSpec, actualValue);
            this.parseResultBuilder.addTypedValues(argSpec, pos, newValue);
            this.parseResultBuilder.add(argSpec, pos);
            return consumed;
        }

        private int applyValuesToMapField(Model.ArgSpec argSpec, LookBehind lookBehind, boolean alreadyUnquoted, Range arity, Stack<String> args, Set<Model.ArgSpec> initialized, String argDescription) throws Exception {
            if (argSpec.auxiliaryTypes().length < 2) {
                throw new ParameterException(CommandLine.this, argSpec.toString() + " needs two types (one for the map key, one for the value) but only has " + argSpec.auxiliaryTypes().length + " types configured.", argSpec, null);
            }
            Map<Object, Object> map = (Map<Object, Object>)argSpec.getValue();
            if (map == null || !initialized.contains(argSpec)) {
                CommandLine.this.tracer.debug("Initializing binding for %s on %s with empty %s%n", CommandLine.optionDescription("", argSpec, 0), argSpec.scopeString(), argSpec.type().getSimpleName());
                map = this.createMap(argSpec.type());
                argSpec.setValue(map);
            }
            this.addToInitialized(argSpec, initialized);
            int originalSize = map.size();
            int pos = this.getPosition(argSpec);
            this.consumeMapArguments(argSpec, lookBehind, alreadyUnquoted, arity, args, map, argDescription);
            this.parseResultBuilder.add(argSpec, pos);
            argSpec.setValue(map);
            return map.size() - originalSize;
        }

        private void consumeMapArguments(Model.ArgSpec argSpec, LookBehind lookBehind, boolean alreadyUnquoted, Range arity, Stack<String> args, Map<Object, Object> result, String argDescription) throws Exception {
            String fallback;
            int currentPosition = this.getPosition(argSpec);
            Class<?>[] classes = argSpec.auxiliaryTypes();
            ITypeConverter<?> keyConverter = this.getTypeConverter(classes, argSpec, 0);
            ITypeConverter<?> valueConverter = this.getTypeConverter(classes, argSpec, 1);
            int initialSize = argSpec.stringValues().size();
            int consumed = this.consumedCountMap(0, initialSize, argSpec);
            int i = 0;
            while (consumed < arity.min && !args.isEmpty()) {
                LinkedHashMap<Object, Object> typedValuesAtPosition = new LinkedHashMap<Object, Object>();
                this.parseResultBuilder.addTypedValues(argSpec, currentPosition++, typedValuesAtPosition);
                if (this.assertNoMissingMandatoryParameter(argSpec, args, i, arity) || this.isArgResemblesOptionThereforeDiscontinue(argSpec, args, i, arity)) break;
                this.consumeOneMapArgument(argSpec, lookBehind, alreadyUnquoted, arity, consumed, args.pop(), classes, keyConverter, valueConverter, typedValuesAtPosition, i, argDescription);
                result.putAll(typedValuesAtPosition);
                consumed = this.consumedCountMap(i + 1, initialSize, argSpec);
                lookBehind = LookBehind.SEPARATE;
                alreadyUnquoted = false;
                ++i;
            }
            String string = fallback = consumed == 0 && argSpec.isOption() && !"".equals(((Model.OptionSpec)argSpec).fallbackValue()) ? ((Model.OptionSpec)argSpec).fallbackValue() : null;
            if (fallback != null && (args.isEmpty() || !this.varargCanConsumeNextValue(argSpec, args.peek()))) {
                args.push(fallback);
            }
            int i2 = consumed;
            while (consumed < arity.max && !args.isEmpty() && this.varargCanConsumeNextValue(argSpec, args.peek())) {
                LinkedHashMap<Object, Object> typedValuesAtPosition = new LinkedHashMap<Object, Object>();
                this.parseResultBuilder.addTypedValues(argSpec, currentPosition++, typedValuesAtPosition);
                if (!this.canConsumeOneMapArgument(argSpec, lookBehind, alreadyUnquoted, arity, consumed, args.peek(), classes, keyConverter, valueConverter, argDescription) || this.isArgResemblesOptionThereforeDiscontinue(argSpec, args, i2, arity)) break;
                this.consumeOneMapArgument(argSpec, lookBehind, alreadyUnquoted, arity, consumed, args.pop(), classes, keyConverter, valueConverter, typedValuesAtPosition, i2, argDescription);
                result.putAll(typedValuesAtPosition);
                consumed = this.consumedCountMap(i2 + 1, initialSize, argSpec);
                lookBehind = LookBehind.SEPARATE;
                alreadyUnquoted = false;
                ++i2;
            }
        }

        private void consumeOneMapArgument(Model.ArgSpec argSpec, LookBehind lookBehind, boolean alreadyUnquoted, Range arity, int consumed, String arg, Class<?>[] classes, ITypeConverter<?> keyConverter, ITypeConverter<?> valueConverter, Map<Object, Object> result, int index, String argDescription) throws Exception {
            String[] values;
            if (!lookBehind.isAttached()) {
                this.parseResultBuilder.nowProcessing(argSpec, arg);
            }
            for (String value : values = this.unquoteAndSplit(argSpec, lookBehind, alreadyUnquoted, arity, consumed, arg)) {
                String[] keyValue = this.splitKeyValue(argSpec, value);
                Object mapKey = this.tryConvert(argSpec, index, keyConverter, keyValue[0], 0);
                String rawMapValue = keyValue.length == 1 ? argSpec.mapFallbackValue() : keyValue[1];
                Object mapValue = this.tryConvert(argSpec, index, valueConverter, rawMapValue, 1);
                result.put(mapKey, mapValue);
                if (CommandLine.this.tracer.isInfo()) {
                    CommandLine.this.tracer.info("Putting [%s : %s] in %s<%s, %s> %s for %s on %s%n", String.valueOf(mapKey), String.valueOf(mapValue), result.getClass().getSimpleName(), classes[0].getSimpleName(), classes[1].getSimpleName(), argSpec.toString(), argDescription, argSpec.scopeString());
                }
                this.parseResultBuilder.addStringValue(argSpec, keyValue[0]);
                this.parseResultBuilder.addStringValue(argSpec, rawMapValue);
            }
            this.parseResultBuilder.addOriginalStringValue(argSpec, arg);
        }

        private String[] unquoteAndSplit(Model.ArgSpec argSpec, LookBehind lookBehind, boolean alreadyUnquoted, Range arity, int consumed, String arg) {
            String raw = lookBehind.isAttached() && alreadyUnquoted ? arg : CommandLine.this.smartUnquoteIfEnabled(arg);
            return argSpec.splitValue(raw, CommandLine.this.commandSpec.parser(), arity, consumed);
        }

        private boolean canConsumeOneMapArgument(Model.ArgSpec argSpec, LookBehind lookBehind, boolean alreadyUnquoted, Range arity, int consumed, String arg, Class<?>[] classes, ITypeConverter<?> keyConverter, ITypeConverter<?> valueConverter, String argDescription) {
            String[] values = this.unquoteAndSplit(argSpec, lookBehind, alreadyUnquoted, arity, consumed, arg);
            try {
                for (String value : values) {
                    String[] keyValue = this.splitKeyValue(argSpec, value);
                    this.tryConvert(argSpec, -1, keyConverter, keyValue[0], 0);
                    String mapValue = keyValue.length == 1 ? argSpec.mapFallbackValue() : keyValue[1];
                    this.tryConvert(argSpec, -1, valueConverter, mapValue, 1);
                }
                return true;
            }
            catch (PicocliException ex) {
                CommandLine.this.tracer.debug("%s cannot be assigned to %s: type conversion fails: %s.%n", arg, argDescription, ex.getMessage());
                return false;
            }
        }

        private String[] splitKeyValue(Model.ArgSpec argSpec, String value) {
            String[] keyValue = Model.ArgSpec.splitRespectingQuotedStrings(value, 2, this.config(), argSpec, "=");
            if (keyValue.length < 2 && "__unspecified__".equals(argSpec.mapFallbackValue())) {
                String splitRegex = argSpec.splitRegex();
                if (splitRegex.length() == 0) {
                    throw new ParameterException(CommandLine.this, "Value for option " + CommandLine.optionDescription("", argSpec, 0) + " should be in KEY=VALUE format but was " + value, argSpec, value);
                }
                throw new ParameterException(CommandLine.this, "Value for option " + CommandLine.optionDescription("", argSpec, 0) + " should be in KEY=VALUE[" + splitRegex + "KEY=VALUE]... format but was " + value, argSpec, value);
            }
            return keyValue;
        }

        private boolean assertNoMissingMandatoryParameter(Model.ArgSpec argSpec, Stack<String> args, int i, Range arity) {
            if (!this.varargCanConsumeNextValue(argSpec, args.peek())) {
                String msg = this.createMissingParameterMessageFoundOtherOption(argSpec, args, i, arity);
                this.maybeThrow(new MissingParameterException(CommandLine.this, argSpec, msg));
                return true;
            }
            return false;
        }

        private String createMissingParameterMessageFoundOtherOption(Model.ArgSpec argSpec, Stack<String> args, int i, Range arity) {
            String desc = arity.min > 1 ? i + 1 + " (of " + arity.min + " mandatory parameters) " : "";
            return "Expected parameter " + desc + "for " + CommandLine.optionDescription("", argSpec, -1) + " but found '" + args.peek() + "'";
        }

        private boolean isArgResemblesOptionThereforeDiscontinue(Model.ArgSpec argSpec, Stack<String> args, int i, Range arity) throws Exception {
            boolean result = false;
            String arg = args.peek();
            if (CommandLine.this.commandSpec.resemblesOption(arg, CommandLine.this.tracer)) {
                if (argSpec.isPositional() && !this.endOfOptions && !CommandLine.this.commandSpec.parser().unmatchedOptionsArePositionalParams()) {
                    this.handleUnmatchedArgument(args);
                    result = true;
                }
                if (argSpec.isOption() && !CommandLine.this.commandSpec.parser().unmatchedOptionsAllowedAsOptionParameters()) {
                    String msg = "Unknown option: '" + arg + "'; " + this.createMissingParameterMessageFoundOtherOption(argSpec, args, i, arity);
                    this.maybeThrow(new UnmatchedArgumentException(CommandLine.this.commandSpec.commandLine(), msg));
                    result = true;
                }
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("Parser is configured to allow unmatched option '%s' as option or positional parameter.%n", arg);
                }
            }
            return result;
        }

        private int applyValuesToArrayField(Model.ArgSpec argSpec, boolean negated, LookBehind lookBehind, boolean alreadyUnquoted, Range arity, Stack<String> args, Set<Model.ArgSpec> initialized, String argDescription) throws Exception {
            Object existing = argSpec.getValue();
            int length = existing == null ? 0 : Array.getLength(existing);
            int pos = this.getPosition(argSpec);
            List<Object> converted = this.consumeArguments(argSpec, negated, lookBehind, alreadyUnquoted, alreadyUnquoted, arity, args, argDescription);
            ArrayList<Object> newValues = new ArrayList<Object>();
            if (initialized.contains(argSpec)) {
                for (int i = 0; i < length; ++i) {
                    newValues.add(Array.get(existing, i));
                }
            }
            this.addToInitialized(argSpec, initialized);
            for (Object obj : converted) {
                if (obj instanceof Collection) {
                    newValues.addAll((Collection)obj);
                    continue;
                }
                newValues.add(obj);
            }
            Object array = Array.newInstance(argSpec.auxiliaryTypes()[0], newValues.size());
            for (int i = 0; i < newValues.size(); ++i) {
                Array.set(array, i, newValues.get(i));
            }
            argSpec.setValue(array);
            this.parseResultBuilder.add(argSpec, pos);
            return converted.size();
        }

        private int applyValuesToCollectionField(Model.ArgSpec argSpec, boolean negated, LookBehind lookBehind, boolean alreadyUnquoted, Range arity, Stack<String> args, Set<Model.ArgSpec> initialized, String argDescription) throws Exception {
            Collection<Object> collection = (Collection<Object>)argSpec.getValue();
            int pos = this.getPosition(argSpec);
            List<Object> converted = this.consumeArguments(argSpec, negated, lookBehind, alreadyUnquoted, alreadyUnquoted, arity, args, argDescription);
            if (collection == null || !initialized.contains(argSpec)) {
                CommandLine.this.tracer.debug("Initializing binding for %s on %s with empty %s%n", CommandLine.optionDescription("", argSpec, 0), argSpec.scopeString(), argSpec.type().getSimpleName());
                collection = this.createCollection(argSpec.type(), argSpec.auxiliaryTypes());
                argSpec.setValue(collection);
            }
            this.addToInitialized(argSpec, initialized);
            for (Object element : converted) {
                if (element instanceof Collection) {
                    collection.addAll((Collection)element);
                    continue;
                }
                collection.add(element);
            }
            this.parseResultBuilder.add(argSpec, pos);
            argSpec.setValue(collection);
            return converted.size();
        }

        private List<Object> consumeArguments(Model.ArgSpec argSpec, boolean negated, LookBehind lookBehind, boolean alreadyUnquoted, boolean unquoted, Range arity, Stack<String> args, String argDescription) throws Exception {
            String fallback;
            ArrayList<Object> result = new ArrayList<Object>();
            int currentPosition = this.getPosition(argSpec);
            int initialSize = argSpec.stringValues().size();
            int consumed = this.consumedCount(0, initialSize, argSpec);
            int i = 0;
            while (consumed < arity.min && !args.isEmpty()) {
                ArrayList<Object> typedValuesAtPosition = new ArrayList<Object>();
                this.parseResultBuilder.addTypedValues(argSpec, currentPosition++, typedValuesAtPosition);
                if (this.assertNoMissingMandatoryParameter(argSpec, args, i, arity) || this.isArgResemblesOptionThereforeDiscontinue(argSpec, args, i, arity)) break;
                this.consumeOneArgument(argSpec, lookBehind, alreadyUnquoted, arity, consumed, args.pop(), typedValuesAtPosition, i, argDescription);
                result.addAll(typedValuesAtPosition);
                consumed = this.consumedCount(i + 1, initialSize, argSpec);
                lookBehind = LookBehind.SEPARATE;
                alreadyUnquoted = false;
                ++i;
            }
            if (argSpec.interactive() && argSpec.arity().max == 0) {
                consumed = this.addUserInputToList(argSpec, result, consumed, argDescription);
            }
            String string = fallback = consumed == 0 && argSpec.isOption() && !"".equals(((Model.OptionSpec)argSpec).fallbackValue()) ? ((Model.OptionSpec)argSpec).fallbackValue() : null;
            if (fallback != null && (args.isEmpty() || !this.varargCanConsumeNextValue(argSpec, args.peek()))) {
                args.push(fallback);
            }
            int i2 = consumed;
            while (consumed < arity.max && !args.isEmpty()) {
                if (argSpec.interactive() && argSpec.arity().max == 1 && !this.varargCanConsumeNextValue(argSpec, args.peek())) {
                    consumed = this.addUserInputToList(argSpec, result, consumed, argDescription);
                } else {
                    if (!this.varargCanConsumeNextValue(argSpec, args.peek())) break;
                    ArrayList<Object> typedValuesAtPosition = new ArrayList<Object>();
                    this.parseResultBuilder.addTypedValues(argSpec, currentPosition++, typedValuesAtPosition);
                    if (!this.canConsumeOneArgument(argSpec, lookBehind, alreadyUnquoted, arity, consumed, args.peek(), argDescription) || this.isArgResemblesOptionThereforeDiscontinue(argSpec, args, i2, arity)) break;
                    this.consumeOneArgument(argSpec, lookBehind, alreadyUnquoted, arity, consumed, args.pop(), typedValuesAtPosition, i2, argDescription);
                    result.addAll(typedValuesAtPosition);
                    consumed = this.consumedCount(i2 + 1, initialSize, argSpec);
                    lookBehind = LookBehind.SEPARATE;
                    alreadyUnquoted = false;
                }
                ++i2;
            }
            if (result.isEmpty() && arity.min == 0 && arity.max <= 1 && CommandLine.isBoolean(argSpec.auxiliaryTypes())) {
                if (argSpec.isOption() && ((Model.OptionSpec)argSpec).negatable()) {
                    Object defaultValue = argSpec.calcDefaultValue(true);
                    boolean booleanDefault = false;
                    if (defaultValue instanceof String) {
                        booleanDefault = Boolean.parseBoolean(String.valueOf(defaultValue));
                    }
                    if (negated) {
                        return Collections.singletonList(booleanDefault);
                    }
                    return Collections.singletonList(!booleanDefault);
                }
                return Collections.singletonList(Boolean.TRUE);
            }
            return result;
        }

        private int consumedCount(int i, int initialSize, Model.ArgSpec arg) {
            return CommandLine.this.commandSpec.parser().splitFirst() ? arg.stringValues().size() - initialSize : i;
        }

        private int consumedCountMap(int i, int initialSize, Model.ArgSpec arg) {
            return CommandLine.this.commandSpec.parser().splitFirst() ? (arg.stringValues().size() - initialSize) / 2 : i;
        }

        private int addUserInputToList(Model.ArgSpec argSpec, List<Object> result, int consumed, String argDescription) {
            char[] input = this.readUserInput(argSpec);
            String inputString = new String(input);
            if (CommandLine.this.tracer.isInfo()) {
                String value = argSpec.echo() ? inputString + " (interactive value)" : "*** (masked interactive value)";
                CommandLine.this.tracer.info("Adding %s to %s for %s on %s%n", value, argSpec.toString(), argDescription, argSpec.scopeString());
            }
            String maskedValue = this.getMaskedValue(argSpec, inputString);
            this.parseResultBuilder.addStringValue(argSpec, maskedValue);
            this.parseResultBuilder.addOriginalStringValue(argSpec, maskedValue);
            if (!char[].class.equals(argSpec.auxiliaryTypes()[0]) && !char[].class.equals(argSpec.type())) {
                Object value = this.tryConvert(argSpec, consumed, this.getTypeConverter(argSpec.auxiliaryTypes(), argSpec, 0), new String(input), 0);
                result.add(value);
            } else {
                result.add(input);
            }
            return ++consumed;
        }

        private String getMaskedValue(Model.ArgSpec argSpec, String input) {
            return argSpec.echo() ? input : "***";
        }

        private int consumeOneArgument(Model.ArgSpec argSpec, LookBehind lookBehind, boolean alreadyUnquoted, Range arity, int consumed, String arg, List<Object> result, int index, String argDescription) {
            if (!lookBehind.isAttached()) {
                this.parseResultBuilder.nowProcessing(argSpec, arg);
            }
            String[] values = this.unquoteAndSplit(argSpec, lookBehind, alreadyUnquoted, arity, consumed, arg);
            ITypeConverter<?> converter = this.getTypeConverter(argSpec.auxiliaryTypes(), argSpec, 0);
            for (String value : values) {
                Object stronglyTypedValue = this.tryConvert(argSpec, index, converter, value, 0);
                result.add(stronglyTypedValue);
                if (CommandLine.this.tracer.isInfo()) {
                    CommandLine.this.tracer.info("Adding [%s] to %s for %s on %s%n", String.valueOf(result.get(result.size() - 1)), argSpec.toString(), argDescription, argSpec.scopeString());
                }
                this.parseResultBuilder.addStringValue(argSpec, value);
            }
            this.parseResultBuilder.addOriginalStringValue(argSpec, arg);
            return ++index;
        }

        private boolean canConsumeOneArgument(Model.ArgSpec argSpec, LookBehind lookBehind, boolean alreadyUnquoted, Range arity, int consumed, String arg, String argDescription) {
            if (char[].class.equals(argSpec.auxiliaryTypes()[0]) || char[].class.equals(argSpec.type())) {
                return true;
            }
            ITypeConverter<?> converter = this.getTypeConverter(argSpec.auxiliaryTypes(), argSpec, 0);
            try {
                String[] values;
                for (String value : values = this.unquoteAndSplit(argSpec, lookBehind, alreadyUnquoted, arity, consumed, arg)) {
                    this.tryConvert(argSpec, -1, converter, value, 0);
                }
                return true;
            }
            catch (PicocliException ex) {
                CommandLine.this.tracer.debug("%s cannot be assigned to %s: type conversion fails: %s.%n", arg, argDescription, ex.getMessage());
                return false;
            }
        }

        private boolean varargCanConsumeNextValue(Model.ArgSpec argSpec, String nextValue) {
            if (this.endOfOptions && argSpec.isPositional()) {
                return true;
            }
            boolean isCommand = CommandLine.this.commandSpec.subcommands().containsKey(nextValue);
            return !isCommand && !this.isOption(nextValue);
        }

        private boolean isOption(String arg) {
            if (arg == null) {
                return false;
            }
            if (CommandLine.this.commandSpec.parser().endOfOptionsDelimiter().equals(arg)) {
                return true;
            }
            if (CommandLine.this.commandSpec.optionsMap().containsKey(arg)) {
                return true;
            }
            if (CommandLine.this.commandSpec.negatedOptionsByNameMap.containsKey(arg)) {
                return true;
            }
            if (CommandLine.this.commandSpec.subcommands().containsKey(arg)) {
                return true;
            }
            if (CommandLine.this.commandSpec.parent() != null && CommandLine.this.commandSpec.parent().subcommandsRepeatable() && CommandLine.this.commandSpec.parent().subcommands().containsKey(arg)) {
                return true;
            }
            int separatorIndex = arg.indexOf(this.config().separator());
            if (separatorIndex > 0 && CommandLine.this.commandSpec.optionsMap().containsKey(arg.substring(0, separatorIndex))) {
                return true;
            }
            return arg.length() > 2 && arg.startsWith("-") && CommandLine.this.commandSpec.posixOptionsMap().containsKey(Character.valueOf(arg.charAt(1)));
        }

        private Object tryConvert(Model.ArgSpec argSpec, int index, ITypeConverter<?> converter, String value, int typeIndex) throws ParameterException {
            try {
                return converter.convert(value);
            }
            catch (TypeConversionException ex) {
                String msg = String.format("Invalid value for %s: %s", CommandLine.optionDescription("", argSpec, index), ex.getMessage());
                throw new ParameterException(CommandLine.this, msg, ex, argSpec, value);
            }
            catch (Exception other) {
                String desc = CommandLine.optionDescription("", argSpec, index);
                String typeDescr = argSpec.auxiliaryTypes()[typeIndex].getSimpleName();
                if (CommandLine.isOptional(argSpec.auxiliaryTypes()[typeIndex])) {
                    typeDescr = typeDescr + "<" + argSpec.auxiliaryTypes()[typeIndex + 1].getSimpleName() + ">";
                }
                String msg = String.format("Invalid value for %s: cannot convert '%s' to %s (%s)", desc, value, typeDescr, other);
                throw new ParameterException(CommandLine.this, msg, other, argSpec, value);
            }
        }

        private boolean isAnyHelpRequested() {
            return this.isHelpRequested || this.parseResultBuilder.versionHelpRequested || this.parseResultBuilder.usageHelpRequested;
        }

        private void updateHelpRequested(Model.CommandSpec command) {
            this.isHelpRequested |= command.helpCommand();
        }

        private void updateHelpRequested(Model.ArgSpec argSpec) {
            if (!this.parseResultBuilder.isInitializingDefaultValues && argSpec.isOption()) {
                Model.OptionSpec option = (Model.OptionSpec)argSpec;
                this.isHelpRequested |= this.is(argSpec, "help", option.help());
                ParseResult.Builder builder = this.parseResultBuilder;
                builder.versionHelpRequested = builder.versionHelpRequested | this.is(argSpec, "versionHelp", option.versionHelp());
                builder = this.parseResultBuilder;
                builder.usageHelpRequested = builder.usageHelpRequested | this.is(argSpec, "usageHelp", option.usageHelp());
            }
        }

        private boolean is(Model.ArgSpec p, String attribute, boolean value) {
            if (value && CommandLine.this.tracer.isInfo()) {
                CommandLine.this.tracer.info("%s has '%s' annotation: not validating required fields%n", p.toString(), attribute);
            }
            return value;
        }

        private Collection<Object> createCollection(Class<?> collectionClass, Class<?>[] elementType) throws Exception {
            if (EnumSet.class.isAssignableFrom(collectionClass) && Enum.class.isAssignableFrom(elementType[0])) {
                EnumSet<?> enumSet = EnumSet.noneOf(elementType[0]);
                return enumSet;
            }
            return (Collection)CommandLine.this.factory.create(collectionClass);
        }

        private Map<Object, Object> createMap(Class<?> mapClass) throws Exception {
            return (Map)CommandLine.this.factory.create(mapClass);
        }

        private ITypeConverter<?> getTypeConverter(Class<?>[] types, Model.ArgSpec argSpec, int index) {
            if (argSpec.converters().length > index) {
                return argSpec.converters()[index];
            }
            Class<?> type = types[index];
            if (CommandLine.isOptional(type)) {
                if (types.length <= index + 1) {
                    throw new PicocliException("Cannot create converter for types " + Arrays.asList(types) + " for " + argSpec);
                }
                final ITypeConverter<?> converter = this.getActualTypeConverter(types[index + 1], argSpec);
                return new ITypeConverter<Object>(){

                    @Override
                    public Object convert(String value) throws Exception {
                        return value == null ? CommandLine.getOptionalEmpty() : CommandLine.getOptionalOfNullable(converter.convert(value));
                    }
                };
            }
            return this.getActualTypeConverter(type, argSpec);
        }

        private ITypeConverter<?> getActualTypeConverter(Class<?> type, Model.ArgSpec argSpec) {
            if (char[].class.equals(argSpec.type()) && argSpec.interactive()) {
                return this.converterRegistry.get(char[].class);
            }
            if (this.converterRegistry.containsKey(type)) {
                return this.converterRegistry.get(type);
            }
            if (type.isEnum()) {
                return this.getEnumTypeConverter(type);
            }
            throw new MissingTypeConverterException(CommandLine.this, "No TypeConverter registered for " + type.getName() + " of " + argSpec);
        }

        private ITypeConverter<Object> getEnumTypeConverter(final Class<?> type) {
            return new ITypeConverter<Object>(){

                @Override
                public Object convert(String value) throws Exception {
                    try {
                        return Enum.valueOf(type, value);
                    }
                    catch (IllegalArgumentException ex) {
                        boolean insensitive = CommandLine.this.commandSpec.parser().caseInsensitiveEnumValuesAllowed();
                        for (Enum enumConstant : (Enum[])type.getEnumConstants()) {
                            String str = enumConstant.toString();
                            String name = enumConstant.name();
                            if (!value.equals(str) && !value.equals(name) && (!insensitive || !value.equalsIgnoreCase(str) && !value.equalsIgnoreCase(name))) continue;
                            return enumConstant;
                        }
                        String sensitivity = insensitive ? "case-insensitive" : "case-sensitive";
                        Enum[] constants = (Enum[])type.getEnumConstants();
                        ArrayList<String> names = new ArrayList<String>();
                        for (Enum constant : constants) {
                            names.add(constant.name());
                            if (names.contains(constant.toString()) || insensitive && constant.name().equalsIgnoreCase(constant.toString())) continue;
                            names.add(constant.toString());
                        }
                        throw new TypeConversionException(CommandLine.format("expected one of %s (%s) but was '%s'", new Object[]{names, sensitivity, value}));
                    }
                }
            };
        }

        private boolean booleanValue(Model.ArgSpec argSpec, Object value) {
            String stringValue;
            if (value == null) {
                return false;
            }
            if (CommandLine.isOptional(value.getClass())) {
                try {
                    value = value.getClass().getMethod("orElse", Object.class).invoke(value, "null");
                }
                catch (Exception e) {
                    throw new TypeConversionException("Could not convert '" + value + "' to an Optional<Boolean>: " + e.getMessage());
                }
            }
            if (CommandLine.empty(stringValue = String.valueOf(value)) || "null".equals(stringValue) || "Optional.empty".equals(value)) {
                return false;
            }
            ITypeConverter<?> converter = this.getTypeConverter(new Class[]{Boolean.class}, argSpec, 0);
            try {
                return (Boolean)converter.convert(stringValue);
            }
            catch (TypeConversionException e) {
                throw e;
            }
            catch (Exception e) {
                throw new TypeConversionException("Could not convert '" + value + "' to a boolean: " + e.getMessage());
            }
        }

        private boolean assertNoMissingParameters(Model.ArgSpec argSpec, Range arity, Stack<String> args) {
            if (argSpec.interactive()) {
                return true;
            }
            int available = args.size();
            if (available > 0 && CommandLine.this.commandSpec.parser().splitFirst() && argSpec.splitRegex().length() > 0) {
                available += argSpec.splitValue(args.peek(), CommandLine.this.commandSpec.parser(), arity, 0).length - 1;
            }
            if (arity.min > available) {
                List<Object> missingList = Collections.emptyList();
                List<Model.PositionalParamSpec> positionals = CommandLine.this.commandSpec.positionalParameters();
                if (argSpec.isPositional() && positionals.contains(argSpec)) {
                    missingList = positionals.subList(positionals.indexOf(argSpec), positionals.size());
                }
                String msg = CommandLine.createMissingParameterMessage(argSpec, arity, missingList, args, available);
                this.maybeThrow(new MissingParameterException(CommandLine.this, argSpec, msg));
                return false;
            }
            return true;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        char[] readUserInput(Model.ArgSpec argSpec) {
            String name = argSpec.isOption() ? ((Model.OptionSpec)argSpec).longestName() : "position " + this.position;
            String prompt = !CommandLine.empty(argSpec.prompt()) ? argSpec.prompt() : String.format("Enter value for %s (%s): ", name, CommandLine.str(argSpec.description(), 0));
            try {
                char[] result;
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug("Reading value for %s from console...%n", name);
                }
                char[] cArray = result = argSpec.echo() ? this.readUserInputWithEchoing(prompt) : this.readPassword(prompt);
                if (CommandLine.this.tracer.isDebug()) {
                    CommandLine.this.tracer.debug(this.createUserInputDebugString(argSpec, result, name), new Object[0]);
                }
                char[] cArray2 = result;
                return cArray2;
            }
            finally {
                ++this.interactiveCount;
            }
        }

        private String createUserInputDebugString(Model.ArgSpec argSpec, char[] result, String name) {
            return argSpec.echo() ? String.format("User entered %s for %s.%n", new String(result), name) : String.format("User entered %d characters for %s.%n", result.length, name);
        }

        char[] readPassword(String prompt) {
            try {
                Object console = System.class.getDeclaredMethod("console", new Class[0]).invoke(null, new Object[0]);
                Method method = Class.forName("java.io.Console").getDeclaredMethod("readPassword", String.class, Object[].class);
                return (char[])method.invoke(console, prompt, new Object[0]);
            }
            catch (Exception e) {
                return this.readUserInputWithEchoing(prompt);
            }
        }

        char[] readUserInputWithEchoing(String prompt) {
            System.out.print(prompt);
            InputStreamReader isr = new InputStreamReader(System.in);
            BufferedReader in = new BufferedReader(isr);
            try {
                String input = in.readLine();
                return input == null ? new char[]{} : input.toCharArray();
            }
            catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        int getPosition(Model.ArgSpec arg) {
            if (arg.group() == null) {
                return this.position;
            }
            ParseResult.GroupMatchContainer container = this.parseResultBuilder.groupMatchContainer.findLastMatchContainer(arg.group());
            return container == null ? 0 : container.lastMatch().position;
        }

        String positionDesc(Model.ArgSpec arg) {
            int pos = this.getPosition(arg);
            return arg.group() == null ? pos + " (command-local)" : pos + " (in group " + arg.group().synopsis() + ")";
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static enum LookBehind {
        SEPARATE,
        ATTACHED,
        ATTACHED_WITH_SEPARATOR;


        static LookBehind parse(String separator) {
            if ("".equals(separator)) {
                return ATTACHED;
            }
            if (" ".equals(separator)) {
                return SEPARATE;
            }
            return ATTACHED_WITH_SEPARATOR;
        }

        String toString(String separator) {
            return this == ATTACHED ? "" : (this == SEPARATE ? " " : separator);
        }

        public boolean isAttached() {
            return this != SEPARATE;
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static class ParseResult {
        private final Model.CommandSpec commandSpec;
        private final Set<Model.OptionSpec> matchedUniqueOptions;
        private final Set<Model.PositionalParamSpec> matchedUniquePositionals;
        private final List<Model.ArgSpec> matchedArgs;
        private final List<Model.OptionSpec> matchedOptions;
        private final List<Model.PositionalParamSpec> matchedPositionals;
        private final List<String> originalArgs;
        private final List<String> expandedArgs;
        private final List<String> unmatched;
        private final List<List<Model.PositionalParamSpec>> matchedPositionalParams;
        private final List<Exception> errors;
        private final GroupMatchContainer groupMatchContainer;
        private final List<ParseResult> subcommands;
        final List<Object> tentativeMatch;
        private final boolean usageHelpRequested;
        private final boolean versionHelpRequested;

        private ParseResult(Builder builder) {
            this.commandSpec = builder.commandSpec;
            this.subcommands = builder.subcommands;
            this.matchedOptions = new ArrayList<Model.OptionSpec>(builder.matchedOptionsList);
            this.matchedUniqueOptions = new LinkedHashSet<Model.OptionSpec>(builder.options);
            this.unmatched = new ArrayList<String>(builder.unmatched);
            this.originalArgs = new ArrayList<String>(builder.originalArgList);
            this.expandedArgs = new ArrayList<String>(builder.expandedArgList);
            this.matchedArgs = new ArrayList<Model.ArgSpec>(builder.matchedArgsList);
            this.matchedUniquePositionals = new LinkedHashSet<Model.PositionalParamSpec>(builder.positionals);
            this.matchedPositionals = new ArrayList<Model.PositionalParamSpec>(builder.matchedPositionalsList);
            this.matchedPositionalParams = new ArrayList<List<Model.PositionalParamSpec>>(builder.positionalParams);
            this.errors = new ArrayList<Exception>(builder.errors);
            this.usageHelpRequested = builder.usageHelpRequested;
            this.versionHelpRequested = builder.versionHelpRequested;
            this.tentativeMatch = builder.nowProcessing;
            this.groupMatchContainer = builder.groupMatchContainer.trim();
        }

        public static Builder builder(Model.CommandSpec commandSpec) {
            return new Builder(commandSpec);
        }

        public List<GroupMatchContainer> findMatches(Model.ArgGroupSpec group) {
            return this.groupMatchContainer.findMatchContainers(group, new ArrayList<GroupMatchContainer>());
        }

        public List<GroupMatch> getGroupMatches() {
            return this.groupMatchContainer.matches();
        }

        public Model.OptionSpec matchedOption(char shortName) {
            return Model.CommandSpec.findOption(shortName, this.matchedOptions);
        }

        public Model.OptionSpec matchedOption(String name) {
            return Model.CommandSpec.findOption(name, this.matchedOptions);
        }

        public Model.PositionalParamSpec matchedPositional(int position) {
            if (this.matchedPositionalParams.size() <= position || this.matchedPositionalParams.get(position).isEmpty()) {
                return null;
            }
            return this.matchedPositionalParams.get(position).get(0);
        }

        public List<Model.PositionalParamSpec> matchedPositionals(int position) {
            if (this.matchedPositionalParams.size() <= position) {
                return Collections.emptyList();
            }
            return this.matchedPositionalParams.get(position) == null ? Collections.emptyList() : this.matchedPositionalParams.get(position);
        }

        public Model.CommandSpec commandSpec() {
            return this.commandSpec;
        }

        public boolean hasMatchedOption(char shortName) {
            return this.matchedOption(shortName) != null;
        }

        public boolean hasMatchedOption(String name) {
            return this.matchedOption(name) != null;
        }

        public boolean hasMatchedOption(Model.OptionSpec option) {
            return this.matchedOptions.contains(option);
        }

        public boolean hasMatchedPositional(int position) {
            return this.matchedPositional(position) != null;
        }

        public boolean hasMatchedPositional(Model.PositionalParamSpec positional) {
            return this.matchedUniquePositionals.contains(positional);
        }

        public Set<Model.OptionSpec> matchedOptionsSet() {
            return Collections.unmodifiableSet(this.matchedUniqueOptions);
        }

        public List<Model.OptionSpec> matchedOptions() {
            return Collections.unmodifiableList(this.matchedOptions);
        }

        public Set<Model.PositionalParamSpec> matchedPositionalsSet() {
            return Collections.unmodifiableSet(this.matchedUniquePositionals);
        }

        public List<Model.PositionalParamSpec> matchedPositionals() {
            return Collections.unmodifiableList(this.matchedPositionals);
        }

        public List<Model.ArgSpec> matchedArgs() {
            return Collections.unmodifiableList(this.matchedArgs);
        }

        public List<String> unmatched() {
            return Collections.unmodifiableList(this.unmatched);
        }

        public List<String> originalArgs() {
            return Collections.unmodifiableList(this.originalArgs);
        }

        public List<String> expandedArgs() {
            return Collections.unmodifiableList(this.expandedArgs);
        }

        public List<Exception> errors() {
            return Collections.unmodifiableList(this.errors);
        }

        public <T> T matchedOptionValue(char shortName, T defaultValue) {
            return this.matchedOptionValue(this.matchedOption(shortName), defaultValue);
        }

        public <T> T matchedOptionValue(String name, T defaultValue) {
            return this.matchedOptionValue(this.matchedOption(name), defaultValue);
        }

        private <T> T matchedOptionValue(Model.OptionSpec option, T defaultValue) {
            return option == null ? defaultValue : option.getValue();
        }

        public <T> T matchedPositionalValue(int position, T defaultValue) {
            return this.matchedPositionalValue(this.matchedPositional(position), defaultValue);
        }

        private <T> T matchedPositionalValue(Model.PositionalParamSpec positional, T defaultValue) {
            return positional == null ? defaultValue : positional.getValue();
        }

        public boolean hasSubcommand() {
            return !this.subcommands.isEmpty();
        }

        public ParseResult subcommand() {
            return !this.hasSubcommand() ? null : this.subcommands.get(this.subcommands.size() - 1);
        }

        public List<ParseResult> subcommands() {
            return Collections.unmodifiableList(this.subcommands);
        }

        public boolean isUsageHelpRequested() {
            return this.usageHelpRequested;
        }

        public boolean isVersionHelpRequested() {
            return this.versionHelpRequested;
        }

        public List<CommandLine> asCommandLineList() {
            return this.recursivelyAddCommandLineTo(new ArrayList<CommandLine>());
        }

        private List<CommandLine> recursivelyAddCommandLineTo(List<CommandLine> result) {
            result.add(this.commandSpec().commandLine());
            for (ParseResult sub : this.subcommands()) {
                sub.recursivelyAddCommandLineTo(result);
            }
            return result;
        }

        void validateGroups() {
            for (Model.ArgGroupSpec group : this.commandSpec.argGroups()) {
                this.groupMatchContainer.updateUnmatchedGroups(group);
            }
            this.groupMatchContainer.validate(this.commandSpec.commandLine());
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class GroupMatch {
            int position;
            final int startPosition;
            final GroupMatchContainer container;
            Map<Model.ArgGroupSpec, GroupMatchContainer> matchedSubgroups = new LinkedHashMap<Model.ArgGroupSpec, GroupMatchContainer>(2);
            Map<Model.ArgSpec, List<Object>> matchedValues = new IdentityHashMap<Model.ArgSpec, List<Object>>();
            Map<Model.ArgSpec, List<String>> originalStringValues = new LinkedHashMap<Model.ArgSpec, List<String>>();
            Map<Model.ArgSpec, Map<Integer, List<Object>>> matchedValuesAtPosition = new IdentityHashMap<Model.ArgSpec, Map<Integer, List<Object>>>();
            private GroupValidationResult validationResult;

            GroupMatch(GroupMatchContainer container) {
                this.container = container;
                if (!container.matches().isEmpty()) {
                    this.position = container.matches().get((int)(container.matches().size() - 1)).position;
                }
                this.startPosition = this.position;
            }

            public boolean isEmpty() {
                return this.originalStringValues.isEmpty() && this.matchedSubgroups.isEmpty();
            }

            public Model.ArgGroupSpec group() {
                return this.container.group;
            }

            public GroupMatchContainer container() {
                return this.container;
            }

            public Map<Model.ArgGroupSpec, GroupMatchContainer> matchedSubgroups() {
                return Collections.unmodifiableMap(this.matchedSubgroups);
            }

            int matchCount(Model.ArgSpec argSpec) {
                return this.matchedValues.get(argSpec) == null ? 0 : this.matchedValues.get(argSpec).size();
            }

            public List<Object> matchedValues(Model.ArgSpec argSpec) {
                return this.matchedValues.get(argSpec) == null ? Collections.emptyList() : Collections.unmodifiableList(this.matchedValues.get(argSpec));
            }

            void addOriginalStringValue(Model.ArgSpec argSpec, String value) {
                CommandLine.addValueToListInMap(this.originalStringValues, argSpec, value);
            }

            void addMatchedValue(Model.ArgSpec argSpec, int matchPosition, Object stronglyTypedValue, Tracer tracer) {
                CommandLine.addValueToListInMap(this.matchedValues, argSpec, stronglyTypedValue);
                Map<Integer, List<Object>> positionalValues = this.matchedValuesAtPosition.get(argSpec);
                if (positionalValues == null) {
                    positionalValues = new TreeMap<Integer, List<Object>>();
                    this.matchedValuesAtPosition.put(argSpec, positionalValues);
                }
                CommandLine.addValueToListInMap(positionalValues, matchPosition, stronglyTypedValue);
            }

            boolean hasMatchedValueAtPosition(Model.ArgSpec arg, int position) {
                Map<Integer, List<Object>> atPos = this.matchedValuesAtPosition.get(arg);
                return atPos != null && atPos.containsKey(position);
            }

            boolean matchedMinElements() {
                return this.matchedFully(false);
            }

            boolean matchedMaxElements() {
                return this.matchedFully(true);
            }

            private boolean matchedFully(boolean allRequired) {
                if (this.group().exclusive()) {
                    return !this.matchedValues.isEmpty() || this.hasFullyMatchedSubgroup(allRequired);
                }
                for (Model.ArgSpec arg : this.group().args()) {
                    if (this.matchedValues.get(arg) != null || !arg.required() && !allRequired) continue;
                    return false;
                }
                for (Model.ArgGroupSpec subgroup : this.group().subgroups()) {
                    GroupMatchContainer groupMatchContainer = this.matchedSubgroups.get(subgroup);
                    if (!(groupMatchContainer != null ? !groupMatchContainer.matchedFully(allRequired) : allRequired || subgroup.multiplicity().min > 0)) continue;
                    return false;
                }
                return true;
            }

            private boolean hasFullyMatchedSubgroup(boolean allRequired) {
                for (GroupMatchContainer sub : this.matchedSubgroups.values()) {
                    if (!sub.matchedFully(allRequired)) continue;
                    return true;
                }
                return false;
            }

            public String toString() {
                return this.toString(new StringBuilder()).toString();
            }

            private StringBuilder toString(StringBuilder result) {
                int originalLength = result.length();
                for (Model.ArgSpec arg : this.originalStringValues.keySet()) {
                    List<String> values = this.originalStringValues.get(arg);
                    for (String value : values) {
                        if (result.length() != originalLength) {
                            result.append(" ");
                        }
                        result.append(Model.ArgSpec.describe(arg, "=", value));
                    }
                }
                for (GroupMatchContainer sub : this.matchedSubgroups.values()) {
                    if (result.length() != originalLength) {
                        result.append(" ");
                    }
                    if (originalLength == 0) {
                        result.append(sub.toString());
                        continue;
                    }
                    sub.toString(result);
                }
                return result;
            }

            void validate(CommandLine commandLine) {
                this.validationResult = GroupValidationResult.SUCCESS_PRESENT;
                if (this.group() != null && !this.group().validate()) {
                    return;
                }
                for (GroupMatchContainer sub : this.matchedSubgroups.values()) {
                    sub.validate(commandLine);
                    if (!sub.validationResult.blockingFailure()) continue;
                    this.validationResult = sub.validationResult;
                    return;
                }
                if (this.group() != null) {
                    this.validationResult = this.group().validateArgs(commandLine, this.matchedValues.keySet());
                    if (this.validationResult.blockingFailure()) {
                        return;
                    }
                    LinkedHashSet<Model.ArgSpec> intersection = new LinkedHashSet<Model.ArgSpec>(this.group().args());
                    LinkedHashSet<Model.ArgSpec> missing = new LinkedHashSet<Model.ArgSpec>(this.group().requiredArgs());
                    LinkedHashSet<Model.ArgSpec> found = new LinkedHashSet<Model.ArgSpec>(this.matchedValues.keySet());
                    missing.removeAll(this.matchedValues.keySet());
                    intersection.retainAll(found);
                    String exclusiveElements = Model.ArgSpec.describe(intersection);
                    String requiredElements = Model.ArgSpec.describe(this.group().requiredArgs());
                    String missingElements = Model.ArgSpec.describe(missing);
                    LinkedHashSet<Model.ArgGroupSpec> missingSubgroups = new LinkedHashSet<Model.ArgGroupSpec>(this.group().subgroups());
                    missingSubgroups.removeAll(this.matchedSubgroups.keySet());
                    int missingRequiredSubgroupCount = 0;
                    for (Model.ArgGroupSpec missingSubgroup : missingSubgroups) {
                        if (missingSubgroup.multiplicity().min() <= 0) continue;
                        ++missingRequiredSubgroupCount;
                        if (missingElements.length() > 0) {
                            missingElements = missingElements + " and ";
                        }
                        missingElements = missingElements + missingSubgroup.synopsisUnit();
                    }
                    for (Model.ArgGroupSpec subgroup : this.group().subgroups()) {
                        if (exclusiveElements.length() > 0) {
                            exclusiveElements = exclusiveElements + " and ";
                        }
                        exclusiveElements = exclusiveElements + subgroup.synopsisUnit();
                        if (subgroup.multiplicity().min <= 0) continue;
                        if (requiredElements.length() > 0) {
                            requiredElements = requiredElements + " and ";
                        }
                        requiredElements = requiredElements + subgroup.synopsisUnit();
                    }
                    int presentCount = this.matchedValues.size() + this.matchedSubgroups.size();
                    boolean haveMissing = !missing.isEmpty() || missingRequiredSubgroupCount > 0;
                    this.validationResult = this.group().validate(commandLine, presentCount, haveMissing, presentCount > 0 && haveMissing, exclusiveElements, requiredElements, missingElements);
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class GroupMatchContainer {
            private final Model.ArgGroupSpec group;
            private GroupMatchContainer parentContainer;
            private final List<Model.ArgGroupSpec> unmatchedSubgroups = new ArrayList<Model.ArgGroupSpec>();
            private final List<GroupMatch> matches = new ArrayList<GroupMatch>();
            private GroupValidationResult validationResult;

            GroupMatchContainer(Model.ArgGroupSpec group, CommandLine cmd) {
                this.group = group;
                this.addMatch(cmd);
            }

            public Model.ArgGroupSpec group() {
                return this.group;
            }

            public List<GroupMatch> matches() {
                return Collections.unmodifiableList(this.matches);
            }

            void addMatch(CommandLine commandLine) {
                Tracer tracer;
                Tracer tracer2 = tracer = commandLine == null ? new Tracer() : commandLine.tracer;
                if (this.group != null && this.isMaxMultiplicityReached()) {
                    tracer.info("Completing GroupMatchContainer %s: max multiplicity is reached.%n", this);
                    this.complete(commandLine);
                } else {
                    if (this.group != null) {
                        tracer.info("Adding match to GroupMatchContainer %s (group=%s %s).%n", this, this.group.id(), this.group.synopsisUnit());
                    }
                    this.matches.add(new GroupMatch(this));
                    if (this.group == null) {
                        return;
                    }
                }
                this.group.initUserObject(commandLine);
            }

            void complete(CommandLine commandLine) {
                if (this.parentContainer == null) {
                    this.addMatch(commandLine);
                } else {
                    this.parentContainer.addMatch(commandLine);
                }
            }

            GroupMatch lastMatch() {
                return this.matches.get(this.matches.size() - 1);
            }

            boolean isMaxMultiplicityReached() {
                return this.matches.size() >= ((Model.ArgGroupSpec)this.group).multiplicity.max;
            }

            boolean isMinMultiplicityReached() {
                return this.matches.size() >= ((Model.ArgGroupSpec)this.group).multiplicity.min;
            }

            boolean matchedMinElements() {
                return this.matchedFully(false);
            }

            boolean matchedMaxElements() {
                return this.matchedFully(true);
            }

            private boolean matchedFully(boolean allRequired) {
                for (GroupMatch multiple : this.matches) {
                    boolean actuallyAllRequired;
                    if (multiple.matchedFully(actuallyAllRequired = allRequired && multiple == this.lastMatch())) continue;
                    return false;
                }
                return allRequired ? this.isMaxMultiplicityReached() : this.isMinMultiplicityReached();
            }

            private GroupMatchContainer findOrCreateMatchingGroup(Model.ArgSpec argSpec, CommandLine commandLine) {
                GroupMatchContainer container;
                Model.ArgGroupSpec searchGroup = Assert.notNull(argSpec.group(), "group for " + argSpec);
                if (searchGroup == (container = this).group()) {
                    return container;
                }
                ArrayList<Model.ArgGroupSpec> keys = new ArrayList<Model.ArgGroupSpec>();
                while (searchGroup != null) {
                    keys.add(searchGroup);
                    searchGroup = searchGroup.parentGroup();
                }
                Collections.reverse(keys);
                for (Model.ArgGroupSpec key : keys) {
                    GroupMatchContainer sub = container.lastMatch().matchedSubgroups().get(key);
                    if (sub == null) {
                        sub = this.createGroupMatchContainer(key, container, commandLine);
                    }
                    container = sub;
                }
                return container;
            }

            private GroupMatchContainer createGroupMatchContainer(Model.ArgGroupSpec group, GroupMatchContainer parent, CommandLine commandLine) {
                GroupMatchContainer result = new GroupMatchContainer(group, commandLine);
                result.parentContainer = parent;
                parent.lastMatch().matchedSubgroups.put(group, result);
                return result;
            }

            GroupMatchContainer trim() {
                Iterator<GroupMatch> iter = this.matches.iterator();
                while (iter.hasNext()) {
                    GroupMatch multiple = iter.next();
                    if (multiple.isEmpty()) {
                        iter.remove();
                    }
                    for (GroupMatchContainer sub : multiple.matchedSubgroups.values()) {
                        sub.trim();
                    }
                }
                return this;
            }

            List<GroupMatchContainer> findMatchContainers(Model.ArgGroupSpec group, List<GroupMatchContainer> result) {
                if (this.group == group) {
                    result.add(this);
                    return result;
                }
                for (GroupMatch multiple : this.matches()) {
                    for (GroupMatchContainer mg : multiple.matchedSubgroups.values()) {
                        mg.findMatchContainers(group, result);
                    }
                }
                return result;
            }

            GroupMatchContainer findLastMatchContainer(Model.ArgGroupSpec group) {
                List<GroupMatchContainer> all = this.findMatchContainers(group, new ArrayList<GroupMatchContainer>());
                return all.isEmpty() ? null : all.get(all.size() - 1);
            }

            public String toString() {
                return this.toString(new StringBuilder()).toString();
            }

            private StringBuilder toString(StringBuilder result) {
                String suffix;
                String prefix = result.length() == 0 ? "={" : "";
                String string = suffix = result.length() == 0 ? "}" : "";
                if (this.group != null && result.length() == 0) {
                    result.append(this.group.synopsis());
                }
                result.append(prefix);
                String infix = "";
                for (GroupMatch occurrence : this.matches) {
                    result.append(infix);
                    occurrence.toString(result);
                    infix = " ";
                }
                return result.append(suffix);
            }

            void updateUnmatchedGroups(final Model.ArgGroupSpec group) {
                Assert.assertTrue(Assert.equals(this.group(), group.parentGroup()), new IHelpSectionRenderer(){

                    public String render(Help h) {
                        return "Internal error: expected " + group.parentGroup() + " (the parent of " + group + "), but was " + GroupMatchContainer.this.group();
                    }
                });
                List<GroupMatchContainer> groupMatchContainers = this.findMatchContainers(group, new ArrayList<GroupMatchContainer>());
                if (groupMatchContainers.isEmpty()) {
                    this.unmatchedSubgroups.add(group);
                }
                for (GroupMatchContainer groupMatchContainer : groupMatchContainers) {
                    for (Model.ArgGroupSpec subGroup : group.subgroups()) {
                        groupMatchContainer.updateUnmatchedGroups(subGroup);
                    }
                }
            }

            void validate(CommandLine commandLine) {
                if (this.group() == null && this.matches.size() > 1) {
                    this.failGroupMultiplicityExceeded(this.matches, commandLine);
                }
                this.validationResult = this.matches.isEmpty() ? GroupValidationResult.SUCCESS_ABSENT : GroupValidationResult.SUCCESS_PRESENT;
                for (Model.ArgGroupSpec missing : this.unmatchedSubgroups) {
                    if (!missing.validate() || missing.multiplicity().min <= 0) continue;
                    int presentCount = 0;
                    boolean haveMissing = true;
                    boolean someButNotAllSpecified = false;
                    String exclusiveElements = missing.synopsisUnit();
                    String missingElements = missing.synopsisUnit();
                    this.validationResult = missing.validate(commandLine, presentCount, haveMissing, someButNotAllSpecified, exclusiveElements, missingElements, missingElements);
                }
                this.validateGroupMultiplicity(commandLine);
                if (this.validationResult.blockingFailure()) {
                    commandLine.interpreter.maybeThrow(this.validationResult.exception);
                }
                for (GroupMatch match : this.matches()) {
                    match.validate(commandLine);
                    if (!match.validationResult.blockingFailure()) continue;
                    this.validationResult = match.validationResult;
                    break;
                }
                if (this.validationResult.blockingFailure()) {
                    commandLine.interpreter.maybeThrow(this.validationResult.exception);
                }
                if (this.group() == null && !this.validationResult.success()) {
                    commandLine.interpreter.maybeThrow(this.validationResult.exception);
                }
            }

            private void failGroupMultiplicityExceeded(List<GroupMatch> groupMatches, CommandLine commandLine) {
                LinkedHashMap<Model.ArgGroupSpec, List<List<GroupMatch>>> matchesPerGroup = new LinkedHashMap<Model.ArgGroupSpec, List<List<GroupMatch>>>();
                String msg = "";
                for (GroupMatch match : groupMatches) {
                    if (msg.length() > 0) {
                        msg = msg + " and ";
                    }
                    msg = msg + match;
                    Map<Model.ArgGroupSpec, GroupMatchContainer> subgroups = match.matchedSubgroups();
                    for (Model.ArgGroupSpec group : subgroups.keySet()) {
                        if (!group.validate()) continue;
                        CommandLine.addValueToListInMap(matchesPerGroup, group, subgroups.get(group).matches());
                    }
                }
                if (!matchesPerGroup.isEmpty() && !this.simplifyErrorMessageForSingleGroup(matchesPerGroup, commandLine)) {
                    commandLine.interpreter.maybeThrow(new MaxValuesExceededException(commandLine, "Error: expected only one match but got " + msg));
                }
            }

            private boolean simplifyErrorMessageForSingleGroup(Map<Model.ArgGroupSpec, List<List<GroupMatch>>> matchesPerGroup, CommandLine commandLine) {
                for (Model.ArgGroupSpec group : matchesPerGroup.keySet()) {
                    List<GroupMatch> flat = CommandLine.flatList((Collection)matchesPerGroup.get(group));
                    LinkedHashSet<Model.ArgSpec> matchedArgs = new LinkedHashSet<Model.ArgSpec>();
                    for (GroupMatch match : flat) {
                        if (!match.matchedSubgroups().isEmpty()) {
                            return false;
                        }
                        matchedArgs.addAll(match.matchedValues.keySet());
                    }
                    GroupValidationResult validationResult = group.validateArgs(commandLine, matchedArgs);
                    if (validationResult.exception == null) continue;
                    commandLine.interpreter.maybeThrow(validationResult.exception);
                    return true;
                }
                return false;
            }

            private void validateGroupMultiplicity(CommandLine commandLine) {
                boolean checkMinimum;
                if (this.group == null || !this.group.validate()) {
                    return;
                }
                int matchCount = this.matches().size();
                boolean bl = checkMinimum = matchCount > 0 || !this.group.args().isEmpty();
                if (checkMinimum && matchCount < this.group.multiplicity().min) {
                    if (this.validationResult.success()) {
                        this.validationResult = new GroupValidationResult(matchCount == 0 ? GroupValidationResult.Type.FAILURE_ABSENT : GroupValidationResult.Type.FAILURE_PARTIAL, new MissingParameterException(commandLine, this.group.args(), "Error: Group: " + this.group.synopsisUnit() + " must be specified " + this.group.multiplicity().min + " times but was matched " + matchCount + " times"));
                    }
                } else if (matchCount > this.group.multiplicity().max && !this.validationResult.blockingFailure()) {
                    this.validationResult = new GroupValidationResult(GroupValidationResult.Type.FAILURE_PRESENT, new MaxValuesExceededException(commandLine, "Error: Group: " + this.group.synopsisUnit() + " can only be specified " + this.group.multiplicity().max + " times but was matched " + matchCount + " times."));
                }
            }

            boolean canMatchPositionalParam(Model.PositionalParamSpec positionalParam) {
                int positionalParamCount;
                boolean mustCreateNewMatch;
                boolean mayCreateNewMatch = !this.matches.isEmpty() && this.lastMatch().matchedMinElements();
                boolean bl = mustCreateNewMatch = !this.matches.isEmpty() && this.lastMatch().matchedMaxElements();
                if (mustCreateNewMatch && this.isMaxMultiplicityReached()) {
                    return false;
                }
                int startPosition = this.matches.isEmpty() ? 0 : this.lastMatch().startPosition;
                int accumulatedPosition = this.matches.isEmpty() ? 0 : this.lastMatch().position;
                int localPosition = accumulatedPosition - startPosition;
                if (mayCreateNewMatch && (positionalParamCount = positionalParam.group().localPositionalParamCount()) != 0) {
                    localPosition %= positionalParamCount;
                }
                return positionalParam.index().contains(localPosition) && !this.lastMatch().hasMatchedValueAtPosition(positionalParam, accumulatedPosition);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class GroupValidationResult {
            static final GroupValidationResult SUCCESS_PRESENT = new GroupValidationResult(Type.SUCCESS_PRESENT);
            static final GroupValidationResult SUCCESS_ABSENT = new GroupValidationResult(Type.SUCCESS_ABSENT);
            Type type;
            ParameterException exception;

            GroupValidationResult(Type type) {
                this.type = type;
            }

            GroupValidationResult(Type type, ParameterException exception) {
                this.type = type;
                this.exception = exception;
            }

            static GroupValidationResult extractBlockingFailure(List<GroupValidationResult> set) {
                for (GroupValidationResult element : set) {
                    if (!element.blockingFailure()) continue;
                    return element;
                }
                return null;
            }

            boolean blockingFailure() {
                return this.type == Type.FAILURE_PRESENT || this.type == Type.FAILURE_PARTIAL;
            }

            boolean present() {
                return this.type == Type.SUCCESS_PRESENT;
            }

            boolean success() {
                return this.type == Type.SUCCESS_ABSENT || this.type == Type.SUCCESS_PRESENT;
            }

            public String toString() {
                return (Object)((Object)this.type) + (this.exception == null ? "" : ": " + this.exception.getMessage());
            }

            /*
             * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
             */
            static enum Type {
                SUCCESS_PRESENT,
                SUCCESS_ABSENT,
                FAILURE_PRESENT,
                FAILURE_ABSENT,
                FAILURE_PARTIAL;

            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class Builder {
            private final Model.CommandSpec commandSpec;
            private final List<Model.ArgSpec> matchedArgsList = new ArrayList<Model.ArgSpec>();
            private final List<Model.OptionSpec> matchedOptionsList = new ArrayList<Model.OptionSpec>();
            private final List<Model.PositionalParamSpec> matchedPositionalsList = new ArrayList<Model.PositionalParamSpec>();
            private final Set<Model.OptionSpec> options = new LinkedHashSet<Model.OptionSpec>();
            private final Set<Model.PositionalParamSpec> positionals = new LinkedHashSet<Model.PositionalParamSpec>();
            private final List<String> unmatched = new ArrayList<String>();
            private int firstUnmatchedPosition = Integer.MAX_VALUE;
            private final List<String> originalArgList = new ArrayList<String>();
            private final List<String> expandedArgList = new ArrayList<String>();
            private final List<List<Model.PositionalParamSpec>> positionalParams = new ArrayList<List<Model.PositionalParamSpec>>();
            private final List<ParseResult> subcommands = new ArrayList<ParseResult>();
            private boolean usageHelpRequested;
            private boolean versionHelpRequested;
            boolean isInitializingDefaultValues;
            private final List<Exception> errors = new ArrayList<Exception>(1);
            private List<Object> nowProcessing;
            private final GroupMatchContainer groupMatchContainer = new GroupMatchContainer(null, null);

            private Builder(Model.CommandSpec spec) {
                this.commandSpec = Assert.notNull(spec, "commandSpec");
            }

            public ParseResult build() {
                return new ParseResult(this);
            }

            private void nowProcessing(Model.ArgSpec spec, Object value) {
                if (this.nowProcessing != null && !this.isInitializingDefaultValues) {
                    this.nowProcessing.add(spec.isPositional() ? spec : value);
                }
            }

            public Builder add(Model.ArgSpec arg, int position) {
                if (arg.isOption()) {
                    this.addOption((Model.OptionSpec)arg);
                } else {
                    this.addPositionalParam((Model.PositionalParamSpec)arg, position);
                }
                return this;
            }

            public Builder addOption(Model.OptionSpec option) {
                if (!this.isInitializingDefaultValues) {
                    this.options.add(option);
                    this.matchedOptionsList.add(option);
                    this.matchedArgsList.add(option);
                }
                return this;
            }

            public Builder addPositionalParam(Model.PositionalParamSpec positionalParam, int position) {
                if (this.isInitializingDefaultValues) {
                    return this;
                }
                this.positionals.add(positionalParam);
                this.matchedPositionalsList.add(positionalParam);
                this.matchedArgsList.add(positionalParam);
                while (this.positionalParams.size() <= position) {
                    this.positionalParams.add(new ArrayList());
                }
                this.positionalParams.get(position).add(positionalParam);
                return this;
            }

            private Builder addUnmatched(int position, String arg) {
                if (position >= 0) {
                    this.firstUnmatchedPosition = Math.min(position, this.firstUnmatchedPosition);
                }
                this.unmatched.add(arg);
                return this;
            }

            public Builder addUnmatched(String arg) {
                return this.addUnmatched(-1, arg);
            }

            public Builder addUnmatched(Stack<String> args) {
                while (!args.isEmpty()) {
                    this.addUnmatched(this.totalArgCount() - args.size(), args.pop());
                }
                return this;
            }

            private int totalArgCount() {
                CommandLine c = this.commandSpec.root().commandLine;
                Builder b = c == null || c.interpreter.parseResultBuilder == null ? this : c.interpreter.parseResultBuilder;
                return b.expandedArgList.size();
            }

            public Builder subcommand(ParseResult subcommand) {
                this.subcommands.add(0, subcommand);
                return this;
            }

            public Builder originalArgs(String[] originalArgs) {
                this.originalArgList.addAll(Arrays.asList(originalArgs));
                return this;
            }

            public Builder expandedArgs(Collection<String> expandedArgs) {
                this.expandedArgList.addAll(expandedArgs);
                return this;
            }

            void addStringValue(Model.ArgSpec argSpec, String value) {
                if (!this.isInitializingDefaultValues) {
                    argSpec.stringValues.add(value);
                }
            }

            void addOriginalStringValue(Model.ArgSpec argSpec, String value) {
                if (!this.isInitializingDefaultValues) {
                    argSpec.originalStringValues.add(value);
                    if (argSpec.group() != null) {
                        GroupMatchContainer groupMatchContainer = this.groupMatchContainer.findLastMatchContainer(argSpec.group());
                        groupMatchContainer.lastMatch().addOriginalStringValue(argSpec, value);
                    }
                }
            }

            void addTypedValues(Model.ArgSpec argSpec, int position, Object typedValue) {
                if (!this.isInitializingDefaultValues) {
                    argSpec.typedValues.add(typedValue);
                    if (argSpec.group() == null) {
                        argSpec.typedValueAtPosition.put(position, typedValue);
                    } else {
                        GroupMatchContainer groupMatchContainer = this.groupMatchContainer.findLastMatchContainer(argSpec.group());
                        groupMatchContainer.lastMatch().addMatchedValue(argSpec, position, typedValue, this.commandSpec.commandLine.tracer);
                    }
                }
            }

            public void addError(PicocliException ex) {
                this.errors.add(Assert.notNull(ex, "exception"));
            }

            void beforeMatchingGroupElement(Model.ArgSpec argSpec) throws Exception {
                Model.ArgGroupSpec group = argSpec.group();
                if (group == null || this.isInitializingDefaultValues) {
                    return;
                }
                Tracer tracer = this.commandSpec.commandLine.tracer;
                GroupMatchContainer foundGroupMatchContainer = this.groupMatchContainer.findOrCreateMatchingGroup(argSpec, this.commandSpec.commandLine);
                GroupMatch match = foundGroupMatchContainer.lastMatch();
                boolean greedy = true;
                boolean allowMultipleMatchesInGroup = greedy && argSpec.isMultiValue();
                String elementDescription = Model.ArgSpec.describe(argSpec, "=");
                if (match.matchedMinElements() && (argSpec.required() || match.matchCount(argSpec) > 0) && !allowMultipleMatchesInGroup) {
                    String previousMatch = argSpec.required() ? "is required" : "has already been matched";
                    tracer.info("GroupMatch %s is complete: its mandatory elements are all matched. (User object: %s.) %s %s in the group, so it starts a new GroupMatch.%n", foundGroupMatchContainer.lastMatch(), foundGroupMatchContainer.group.userObject(), elementDescription, previousMatch);
                    foundGroupMatchContainer.addMatch(this.commandSpec.commandLine);
                    this.groupMatchContainer.findOrCreateMatchingGroup(argSpec, this.commandSpec.commandLine);
                } else if (match.matchCount(argSpec) > 0 && !allowMultipleMatchesInGroup) {
                    tracer.info("GroupMatch %s is incomplete: its mandatory elements are not all matched. (User object: %s.) However, %s has already been matched in the group, so it starts a new GroupMatch.%n", foundGroupMatchContainer.lastMatch(), foundGroupMatchContainer.group.userObject(), elementDescription);
                    foundGroupMatchContainer.addMatch(this.commandSpec.commandLine);
                    this.groupMatchContainer.findOrCreateMatchingGroup(argSpec, this.commandSpec.commandLine);
                }
            }
        }
    }

    public static final class Model {
        private Model() {
        }

        private static boolean initializable(Object current, Object candidate, Object defaultValue) {
            return current == null && Model.isNonDefault(candidate, defaultValue);
        }

        private static boolean initializable(Object current, Object[] candidate, Object[] defaultValue) {
            return current == null && Model.isNonDefault(candidate, defaultValue);
        }

        private static boolean isNonDefault(Object candidate, Object defaultValue) {
            return !Assert.notNull(defaultValue, "defaultValue").equals(candidate);
        }

        private static boolean isNonDefault(Object[] candidate, Object[] defaultValue) {
            return !Arrays.equals(Assert.notNull(defaultValue, "defaultValue"), candidate);
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class Interpolator {
            private final CommandSpec commandSpec;
            private final Map<String, ILookup> lookups = new LinkedHashMap<String, ILookup>();

            public Interpolator(final CommandSpec commandSpec) {
                this.commandSpec = commandSpec;
                this.lookups.put("sys:", new ILookup(){

                    public String get(String key) {
                        return System.getProperty(key);
                    }
                });
                this.lookups.put("env:", new ILookup(){

                    public String get(String key) {
                        return System.getenv(key);
                    }
                });
                this.lookups.put("bundle:", new ILookup(){

                    public String get(String key) {
                        return Interpolator.bundleValue(commandSpec.resourceBundle(), key);
                    }
                });
                this.lookups.put("", new ILookup(){

                    public String get(String key) {
                        String result;
                        String string = "COMMAND-NAME".equals(key) ? commandSpec.name() : ("COMMAND-FULL-NAME".equals(key) ? commandSpec.qualifiedName() : ("PARENT-COMMAND-NAME".equals(key) && commandSpec.parent() != null ? commandSpec.parent().name() : ("PARENT-COMMAND-FULL-NAME".equals(key) && commandSpec.parent() != null ? commandSpec.parent().qualifiedName() : (result = "ROOT-COMMAND-NAME".equals(key) ? commandSpec.root().name() : null))));
                        if (result == null) {
                            result = System.getProperty(key);
                        }
                        if (result == null) {
                            result = System.getenv(key);
                        }
                        if (result == null) {
                            result = Interpolator.bundleValue(commandSpec.resourceBundle(), key);
                        }
                        return result;
                    }
                });
            }

            private static String bundleValue(ResourceBundle rb, String key) {
                if (rb != null) {
                    try {
                        return rb.getString(key);
                    }
                    catch (MissingResourceException ex) {
                        return null;
                    }
                }
                return null;
            }

            public String[] interpolate(String[] values) {
                if (values == null || values.length == 0) {
                    return values;
                }
                String[] result = new String[values.length];
                for (int i = 0; i < result.length; ++i) {
                    result[i] = this.interpolate(values[i]);
                }
                return result;
            }

            public String interpolate(String original) {
                if (original == null || !this.commandSpec.interpolateVariables()) {
                    return original;
                }
                return this.resolveLookups(original, new HashSet<String>(), new HashMap<String, String>());
            }

            public String interpolateCommandName(String original) {
                if (original == null || !this.commandSpec.interpolateVariables()) {
                    return original;
                }
                return this.resolveLookups(original, new HashSet<String>(), new HashMap<String, String>());
            }

            private String resolveLookups(String text, Set<String> visited, Map<String, String> resolved) {
                if (text == null) {
                    return null;
                }
                for (String lookupKey : this.lookups.keySet()) {
                    ILookup lookup = this.lookups.get(lookupKey);
                    String prefix = "${" + lookupKey;
                    int startPos = 0;
                    while ((startPos = this.findOpeningDollar(text, prefix, startPos)) >= 0) {
                        String value;
                        String fullKey;
                        int endPos = this.findClosingBrace(text, startPos + prefix.length());
                        if (endPos < 0) {
                            endPos = text.length() - 1;
                        }
                        String actualKey = fullKey = text.substring(startPos + prefix.length(), endPos);
                        int defaultStartPos = fullKey.indexOf(":-");
                        if (defaultStartPos >= 0) {
                            actualKey = fullKey.substring(0, defaultStartPos);
                        }
                        String string = value = resolved.containsKey(prefix + actualKey) ? resolved.get(prefix + actualKey) : lookup.get(actualKey);
                        if (visited.contains(prefix + actualKey) && !resolved.containsKey(prefix + actualKey)) {
                            throw new InitializationException("Lookup '" + prefix + actualKey + "' has a circular reference.");
                        }
                        visited.add(prefix + actualKey);
                        if (value == null && defaultStartPos >= 0) {
                            String defaultValue = fullKey.substring(defaultStartPos + 2);
                            value = this.resolveLookups(defaultValue, visited, resolved);
                        }
                        resolved.put(prefix + actualKey, value);
                        if (value == null && startPos == 0 && endPos == text.length() - 1) {
                            return null;
                        }
                        text = text.substring(0, startPos) + value + text.substring(endPos + 1);
                        startPos += value == null ? "null".length() : value.length();
                    }
                }
                return text.replace("$$", "$");
            }

            private int findOpeningDollar(String text, String prefix, int start) {
                int ch;
                int open = -1;
                boolean escaping = false;
                for (int i = start; i < text.length(); i += Character.charCount(ch)) {
                    ch = text.codePointAt(i);
                    if (ch == 36) {
                        open = escaping ? -1 : i;
                        escaping = !escaping;
                    } else {
                        escaping = false;
                    }
                    if (open != -1 && ch != prefix.codePointAt(i - open)) {
                        open = -1;
                    }
                    if (open == -1 || i - open != prefix.length() - 1) continue;
                    return open;
                }
                return -1;
            }

            private int findClosingBrace(String text, int start) {
                int ch;
                int open = 1;
                boolean escaping = false;
                block5: for (int i = start; i < text.length(); i += Character.charCount(ch)) {
                    ch = text.codePointAt(i);
                    switch (ch) {
                        case 92: {
                            escaping = !escaping;
                            continue block5;
                        }
                        case 125: {
                            if (!escaping) {
                                --open;
                            }
                            if (open == 0) {
                                return i;
                            }
                            escaping = false;
                            continue block5;
                        }
                        case 123: {
                            if (!escaping) {
                                ++open;
                            }
                            escaping = false;
                            continue block5;
                        }
                        default: {
                            escaping = false;
                        }
                    }
                }
                return -1;
            }

            static interface ILookup {
                public String get(String var1);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class CommandUserObject
        implements IScope {
            private final IFactory factory;
            private Object instance;
            private Class<?> type;
            private CommandSpec commandSpec;

            private CommandUserObject(Object objectOrClass, IFactory factory) {
                this.factory = Assert.notNull(factory, "factory");
                this.type = objectOrClass == null ? null : objectOrClass.getClass();
                this.instance = objectOrClass;
                if (objectOrClass instanceof Class) {
                    this.type = (Class)objectOrClass;
                    this.instance = null;
                } else if (objectOrClass instanceof Method) {
                    this.type = null;
                }
            }

            public String toString() {
                if (this.type == null && this.instance == null) {
                    return "null";
                }
                if (this.type == null) {
                    return String.valueOf(this.instance);
                }
                if (this.instance == null) {
                    return String.valueOf(this.type);
                }
                return this.type.getName() + "@" + Integer.toHexString(System.identityHashCode(this.instance));
            }

            public CommandUserObject copy() {
                return new CommandUserObject(this.type == null ? this.instance : this.type, this.factory);
            }

            public static CommandUserObject create(Object userObject, IFactory factory) {
                if (userObject instanceof CommandUserObject) {
                    return (CommandUserObject)userObject;
                }
                return new CommandUserObject(userObject, factory);
            }

            public Object getInstance() {
                if (this.instance == null) {
                    Tracer t = new Tracer();
                    if (this.type == null) {
                        t.debug("Returning a null user object instance%n", new Object[0]);
                        return null;
                    }
                    try {
                        t.debug("Getting a %s instance from factory %s%n", this.type.getName(), this.factory);
                        this.instance = DefaultFactory.create(this.factory, this.type);
                        this.type = this.instance.getClass();
                        t.debug("Factory returned a %s instance (%s)%n", this.type.getName(), Integer.toHexString(System.identityHashCode(this.instance)));
                    }
                    catch (InitializationException ex) {
                        if (this.type.isInterface()) {
                            t.debug("%s. Creating Proxy for interface %s%n", ex.getCause(), this.type.getName());
                            this.instance = Proxy.newProxyInstance(this.type.getClassLoader(), new Class[]{this.type}, (InvocationHandler)new PicocliInvocationHandler());
                            t.debug("Created Proxy instance (%s)%n", Integer.toHexString(System.identityHashCode(this.instance)));
                        }
                        throw ex;
                    }
                    if (this.commandSpec != null) {
                        for (ArgSpec arg : this.commandSpec.args()) {
                            if (arg.group() != null || arg.hasInitialValue()) continue;
                            arg.initialValue();
                        }
                    }
                }
                return this.instance;
            }

            public Class<?> getType() {
                return this.type;
            }

            public boolean isMethod() {
                return this.instance instanceof Method;
            }

            @Override
            public <T> T get() {
                return (T)this.getInstance();
            }

            @Override
            public <T> T set(T value) {
                throw new UnsupportedOperationException();
            }

            public boolean isProxyClass() {
                if (this.type == null || !this.type.isInterface()) {
                    return false;
                }
                return Proxy.isProxyClass(this.getInstance().getClass());
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class ObjectScope
        implements IScope {
            private Object value;

            public ObjectScope(Object value) {
                this.value = value;
            }

            public static boolean isProxyClass(IScope scope) {
                if (scope instanceof CommandUserObject) {
                    return ((CommandUserObject)scope).isProxyClass();
                }
                Object obj = ObjectScope.tryGet(scope);
                return obj != null && Proxy.isProxyClass(obj.getClass());
            }

            public static boolean hasInstance(IScope scope) {
                if (scope instanceof CommandUserObject) {
                    return ((CommandUserObject)scope).instance != null;
                }
                return ObjectScope.tryGet(scope) != null;
            }

            @Override
            public <T> T get() {
                return (T)this.value;
            }

            @Override
            public <T> T set(T value) {
                Object old = this.value;
                this.value = value;
                return (T)old;
            }

            public static Object tryGet(IScope scope) {
                try {
                    return scope.get();
                }
                catch (Exception e) {
                    throw new InitializationException("Could not get scope value", e);
                }
            }

            static IScope asScope(Object scope) {
                return scope instanceof IScope ? (IScope)scope : new ObjectScope(scope);
            }

            public String toString() {
                return String.format("Scope(value=%s)", this.value);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        private static class ObjectBinding
        implements IGetter,
        ISetter {
            private Object value;

            private ObjectBinding() {
            }

            @Override
            public <T> T get() {
                return (T)this.value;
            }

            @Override
            public <T> T set(T value) {
                T result = value;
                this.value = value;
                return result;
            }

            public String toString() {
                return String.format("%s(value=%s)", this.getClass().getSimpleName(), this.value);
            }
        }

        private static class PicocliInvocationHandler
        implements InvocationHandler {
            final Map<String, Object> map = new HashMap<String, Object>();

            private PicocliInvocationHandler() {
            }

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return this.map.get(method.getName());
            }

            /*
             * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
             */
            class ProxyBinding
            implements IGetter,
            ISetter {
                private final Method method;

                ProxyBinding(Method method) {
                    this.method = Assert.notNull(method, "method");
                }

                @Override
                public <T> T get() {
                    return (T)PicocliInvocationHandler.this.map.get(this.method.getName());
                }

                @Override
                public <T> T set(T value) {
                    T result = this.get();
                    PicocliInvocationHandler.this.map.put(this.method.getName(), value);
                    return result;
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class MethodBinding
        implements IGetter,
        ISetter {
            private final IScope scope;
            private final Method method;
            private final CommandSpec spec;
            private Object currentValue;

            MethodBinding(IScope scope, Method method, CommandSpec spec) {
                this.scope = scope;
                this.method = method;
                this.spec = spec;
            }

            @Override
            public <T> T get() {
                return (T)this.currentValue;
            }

            @Override
            public <T> T set(T value) throws PicocliException {
                Object obj;
                try {
                    obj = this.scope.get();
                }
                catch (Exception ex) {
                    throw new PicocliException("Could not get scope for method " + this.method, ex);
                }
                try {
                    Object result = this.currentValue;
                    this.method.invoke(obj, value);
                    this.currentValue = value;
                    return (T)result;
                }
                catch (InvocationTargetException ex) {
                    if (ex.getTargetException() instanceof PicocliException) {
                        throw (PicocliException)ex.getTargetException();
                    }
                    throw this.createParameterException(value, ex.getTargetException());
                }
                catch (Exception ex) {
                    throw this.createParameterException(value, ex);
                }
            }

            private ParameterException createParameterException(Object value, Throwable t) {
                CommandLine cmd = this.spec.commandLine() == null ? new CommandLine(this.spec) : this.spec.commandLine();
                return new ParameterException(cmd, "Could not invoke " + this.method + " with " + value + " (" + t + ")", t);
            }

            public String toString() {
                return String.format("%s(%s)", this.getClass().getSimpleName(), this.method);
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class FieldBinding
        implements IGetter,
        ISetter {
            private final IScope scope;
            private final Field field;

            FieldBinding(Object scope, Field field) {
                this(ObjectScope.asScope(scope), field);
            }

            FieldBinding(IScope scope, Field field) {
                this.scope = scope;
                this.field = field;
            }

            @Override
            public <T> T get() throws PicocliException {
                Object obj;
                try {
                    obj = this.scope.get();
                }
                catch (Exception ex) {
                    throw new PicocliException("Could not get scope for field " + this.field, ex);
                }
                try {
                    Object result = this.field.get(obj);
                    return (T)result;
                }
                catch (Exception ex) {
                    throw new PicocliException("Could not get value for field " + this.field, ex);
                }
            }

            @Override
            public <T> T set(T value) throws PicocliException {
                Object obj;
                try {
                    obj = this.scope.get();
                }
                catch (Exception ex) {
                    throw new PicocliException("Could not get scope for field " + this.field, ex);
                }
                try {
                    Object result = this.field.get(obj);
                    this.field.set(obj, value);
                    return (T)result;
                }
                catch (Exception ex) {
                    throw new PicocliException("Could not set value for field " + this.field + " to " + value, ex);
                }
            }

            public String toString() {
                return String.format("%s(%s %s.%s)", this.getClass().getSimpleName(), this.field.getType().getName(), this.field.getDeclaringClass().getName(), this.field.getName());
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        private static class CommandReflection {
            private CommandReflection() {
            }

            static ArgGroupSpec extractArgGroupSpec(IAnnotatedElement member, IFactory factory, CommandSpec commandSpec, boolean annotationsAreMandatory) throws Exception {
                Class<Object> cls;
                Object instance = null;
                try {
                    instance = member.getter().get();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                Class<Object> clazz = cls = instance == null ? member.getTypeInfo().getType() : instance.getClass();
                if (member.isMultiValue()) {
                    cls = member.getTypeInfo().getAuxiliaryTypes()[0];
                }
                ObjectScope scope = new ObjectScope(instance);
                ArgGroupSpec.Builder builder = ArgGroupSpec.builder(member);
                builder.updateArgGroupAttributes(member.getAnnotation(ArgGroup.class));
                if (member.isOption() || member.isParameter()) {
                    if (member instanceof TypedMember) {
                        CommandReflection.validateArgSpecMember((TypedMember)member);
                    }
                    builder.addArg(CommandReflection.buildArgForMember(member, factory));
                }
                Stack<Class<Object>> hierarchy = new Stack<Class<Object>>();
                while (cls != null) {
                    hierarchy.add(cls);
                    cls = cls.getSuperclass();
                }
                boolean hasArgAnnotation = false;
                while (!hierarchy.isEmpty()) {
                    cls = (Class)hierarchy.pop();
                    hasArgAnnotation |= CommandReflection.initFromAnnotatedFields(scope, cls, commandSpec, builder, factory, null);
                }
                ArgGroupSpec result = builder.build();
                if (annotationsAreMandatory) {
                    CommandReflection.validateArgGroupSpec(result, hasArgAnnotation, cls.getName());
                }
                return result;
            }

            static CommandSpec extractCommandSpec(Object command, IFactory factory, boolean annotationsAreMandatory) {
                Assert.notNull(command, "command user object");
                Tracer t = new Tracer();
                if (command instanceof CommandSpec) {
                    t.debug("extractCommandSpec returning existing CommandSpec instance %s%n", command);
                    return (CommandSpec)command;
                }
                CommandUserObject userObject = CommandUserObject.create(command, factory);
                t.debug("Creating CommandSpec for %s with factory %s%n", userObject, factory.getClass().getName());
                CommandSpec result = CommandSpec.wrapWithoutInspection(userObject);
                boolean hasCommandAnnotation = false;
                if (userObject.isMethod()) {
                    Method method = (Method)command;
                    t.debug("Using method %s as command %n", method);
                    method.setAccessible(true);
                    Command cmd = method.getAnnotation(Command.class);
                    result.updateCommandAttributes(cmd, factory);
                    CommandReflection.injectSpecIntoVersionProvider(result, cmd, factory);
                    result.setAddMethodSubcommands(false);
                    hasCommandAnnotation = true;
                    CommandReflection.initSubcommands(cmd, null, result, factory, new Stack());
                    result.mixinStandardHelpOptions(cmd.mixinStandardHelpOptions());
                    CommandReflection.initFromMethodParameters(userObject, method, result, null, factory);
                    result.initName(((Method)command).getName());
                } else {
                    Class cls;
                    Stack<Class> hierarchy = new Stack<Class>();
                    for (cls = userObject.getType(); cls != null; cls = cls.getSuperclass()) {
                        hierarchy.add(cls);
                    }
                    Stack originalHierarchy = (Stack)hierarchy.clone();
                    boolean mixinStandardHelpOptions = false;
                    while (!hierarchy.isEmpty()) {
                        cls = (Class)hierarchy.pop();
                        Command cmd = cls.getAnnotation(Command.class);
                        if (cmd != null) {
                            result.updateCommandAttributes(cmd, factory);
                            CommandReflection.injectSpecIntoVersionProvider(result, cmd, factory);
                            hasCommandAnnotation = true;
                            mixinStandardHelpOptions |= cmd.mixinStandardHelpOptions();
                        }
                        CommandReflection.initSubcommands(cmd, cls, result, factory, originalHierarchy);
                        CommandReflection.initMethodSubcommands(cls, result, factory);
                        hasCommandAnnotation |= CommandReflection.initFromAnnotatedFields(userObject, cls, result, null, factory, null);
                    }
                    result.mixinStandardHelpOptions(mixinStandardHelpOptions);
                }
                result.updateArgSpecMessages();
                if (annotationsAreMandatory) {
                    CommandReflection.validateCommandSpec(result, hasCommandAnnotation, userObject.toString());
                }
                result.validate();
                return result;
            }

            private static void injectSpecIntoVersionProvider(CommandSpec result, Command cmd, IFactory factory) {
                if (result.versionProvider() == null) {
                    return;
                }
                CommandReflection.initFromAnnotatedFields(new ObjectScope(result.versionProvider()), cmd.versionProvider(), result, null, factory, new Predicate<TypedMember>(){

                    @Override
                    public boolean test(TypedMember tm) {
                        return tm.isSpec() && !tm.isArgGroup() && !tm.isUnmatched() && !tm.isMixin() && !tm.isOption() && !tm.isParameter() && !tm.isParentCommand();
                    }
                });
            }

            private static void initSubcommands(Command cmd, Class<?> cls, CommandSpec parent, IFactory factory, Stack<Class<?>> hierarchy) {
                if (cmd == null) {
                    return;
                }
                for (Class<?> sub : cmd.subcommands()) {
                    if (sub.equals(cls)) {
                        throw new InitializationException(cmd.name() + " (" + cls.getName() + ") cannot be a subcommand of itself");
                    }
                    if (hierarchy.contains(sub)) {
                        throw new InitializationException(cmd.name() + " (" + cls.getName() + ") has a subcommand (" + sub.getName() + ") that is a subclass of itself");
                    }
                    try {
                        if (Help.class == sub) {
                            throw new InitializationException(Help.class.getName() + " is not a valid subcommand. Did you mean " + HelpCommand.class.getName() + "?");
                        }
                        CommandLine subcommandLine = CommandLine.toCommandLine(sub, factory);
                        parent.addSubcommand(CommandReflection.subcommandName(sub), subcommandLine);
                        subcommandLine.getCommandSpec().injectParentCommand(parent.userObject);
                        for (CommandSpec mixin : subcommandLine.getCommandSpec().mixins().values()) {
                            mixin.injectParentCommand(parent.userObject);
                        }
                    }
                    catch (InitializationException ex) {
                        throw ex;
                    }
                    catch (Exception ex) {
                        throw new InitializationException("Could not instantiate and add subcommand " + sub.getName() + ": " + ex, ex);
                    }
                }
            }

            private static void initMethodSubcommands(Class<?> cls, CommandSpec parent, IFactory factory) {
                if (parent.isAddMethodSubcommands() && cls != null) {
                    for (CommandLine sub : CommandSpec.createMethodSubcommands(cls, factory, false)) {
                        parent.addSubcommand(sub.getCommandName(), sub);
                        for (CommandSpec mixin : sub.getCommandSpec().mixins().values()) {
                            mixin.injectParentCommand(parent.userObject);
                        }
                    }
                }
            }

            private static String subcommandName(Class<?> sub) {
                Command subCommand = sub.getAnnotation(Command.class);
                if (subCommand == null || "<main class>".equals(subCommand.name())) {
                    throw new InitializationException("Subcommand " + sub.getName() + " is missing the mandatory @Command annotation with a 'name' attribute");
                }
                return subCommand.name();
            }

            private static boolean initFromAnnotatedFields(IScope scope, Class<?> cls, CommandSpec receiver, ArgGroupSpec.Builder groupBuilder, IFactory factory, Predicate<TypedMember> predicate) {
                boolean result = false;
                for (Field field : cls.getDeclaredFields()) {
                    result |= CommandReflection.initFromAnnotatedTypedMembers(TypedMember.createIfAnnotated(field, scope), predicate, receiver, groupBuilder, factory);
                }
                for (AccessibleObject accessibleObject : cls.getDeclaredMethods()) {
                    result |= CommandReflection.initFromAnnotatedTypedMembers(TypedMember.createIfAnnotated((Method)accessibleObject, scope, receiver), predicate, receiver, groupBuilder, factory);
                }
                return result;
            }

            private static boolean initFromAnnotatedTypedMembers(TypedMember member, Predicate<TypedMember> predicate, CommandSpec commandSpec, ArgGroupSpec.Builder groupBuilder, IFactory factory) {
                if (member == null || predicate != null && !predicate.test(member)) {
                    return false;
                }
                boolean result = false;
                if (member.isMixin()) {
                    CommandReflection.assertNoDuplicateAnnotations(member, Mixin.class, Option.class, Parameters.class, Unmatched.class, Spec.class, ArgGroup.class);
                    if (groupBuilder != null) {
                        throw new InitializationException("@Mixins are not supported on @ArgGroups");
                    }
                    CommandSpec mixin = CommandReflection.buildMixinForMember(member, factory);
                    commandSpec.addMixin(member.getMixinName(), mixin, member);
                    for (IAnnotatedElement specElement : mixin.specElements) {
                        if (specElement.getAnnotation(Spec.class).value() != Spec.Target.MIXEE) continue;
                        try {
                            specElement.setter().set(commandSpec);
                        }
                        catch (Exception ex) {
                            throw new InitializationException("Could not inject MIXEE spec", ex);
                        }
                    }
                    result = true;
                }
                if (member.isArgGroup()) {
                    CommandReflection.assertNoDuplicateAnnotations(member, ArgGroup.class, Spec.class, Parameters.class, Option.class, Unmatched.class, Mixin.class);
                    if (groupBuilder != null) {
                        groupBuilder.addSubgroup(CommandReflection.buildArgGroupForMember(member, factory, commandSpec));
                    } else {
                        commandSpec.addArgGroup(CommandReflection.buildArgGroupForMember(member, factory, commandSpec));
                    }
                    return true;
                }
                if (member.isUnmatched()) {
                    CommandReflection.assertNoDuplicateAnnotations(member, Unmatched.class, Mixin.class, Option.class, Parameters.class, Spec.class, ArgGroup.class);
                    if (groupBuilder != null) {
                        throw new InitializationException("@Unmatched are not supported on @ArgGroups");
                    }
                    commandSpec.addUnmatchedArgsBinding(CommandReflection.buildUnmatchedForMember(member));
                }
                if (member.isArgSpec()) {
                    CommandReflection.validateArgSpecMember(member);
                    if (groupBuilder != null) {
                        groupBuilder.addArg(CommandReflection.buildArgForMember(member, factory));
                    } else {
                        commandSpec.add(CommandReflection.buildArgForMember(member, factory));
                    }
                    result = true;
                }
                if (member.isSpec()) {
                    CommandReflection.validateInjectSpec(member);
                    if (groupBuilder != null) {
                        groupBuilder.addSpecElement(member);
                    } else {
                        commandSpec.addSpecElement(member);
                        if (member.getAnnotation(Spec.class).value() == Spec.Target.SELF) {
                            try {
                                member.setter().set(commandSpec);
                            }
                            catch (Exception ex) {
                                throw new InitializationException("Could not inject spec", ex);
                            }
                        }
                    }
                }
                if (member.isParentCommand()) {
                    commandSpec.addParentCommandElement(member);
                }
                return result;
            }

            private static boolean initFromMethodParameters(IScope scope, Method method, CommandSpec receiver, ArgGroupSpec.Builder groupBuilder, IFactory factory) {
                boolean result = false;
                int optionCount = 0;
                TypedMember[] members = new TypedMember[method.getParameterTypes().length];
                int count = members.length;
                for (int i = 0; i < count; ++i) {
                    MethodParam param = new MethodParam(method, i);
                    if (param.isAnnotationPresent(Option.class) || param.isAnnotationPresent(Mixin.class) || param.isAnnotationPresent(ArgGroup.class)) {
                        ++optionCount;
                    } else {
                        param.position = i - optionCount;
                    }
                    members[i] = new TypedMember(param, scope);
                    result |= CommandReflection.initFromAnnotatedTypedMembers(members[i], null, receiver, groupBuilder, factory);
                }
                CommandSpec.access$15802(receiver, members);
                return result;
            }

            private static void validateArgSpecMember(TypedMember member) {
                if (!member.isArgSpec()) {
                    throw new IllegalStateException("Bug: validateArgSpecMember() should only be called with an @Option or @Parameters member");
                }
                if (member.isOption()) {
                    CommandReflection.assertNoDuplicateAnnotations(member, Option.class, Unmatched.class, Mixin.class, Parameters.class, Spec.class, ArgGroup.class);
                } else {
                    CommandReflection.assertNoDuplicateAnnotations(member, Parameters.class, Option.class, Unmatched.class, Mixin.class, Spec.class, ArgGroup.class);
                }
                if (!(member.accessible instanceof Field)) {
                    return;
                }
                Field field = (Field)member.accessible;
                if (Modifier.isFinal(field.getModifiers()) && (field.getType().isPrimitive() || String.class.isAssignableFrom(field.getType()))) {
                    throw new InitializationException("Constant (final) primitive and String fields like " + field + " cannot be used as " + (member.isOption() ? "an @Option" : "a @Parameter") + ": compile-time constant inlining may hide new values written to it.");
                }
            }

            private static void validateCommandSpec(CommandSpec result, boolean hasCommandAnnotation, String commandClassName) {
                if (!hasCommandAnnotation && result.positionalParameters.isEmpty() && result.optionsByNameMap.isEmpty() && result.unmatchedArgs.isEmpty()) {
                    throw new InitializationException(commandClassName + " is not a command: it has no @Command, @Option, @Parameters or @Unmatched annotations");
                }
            }

            private static void validateArgGroupSpec(ArgGroupSpec result, boolean hasArgAnnotation, String className) {
                if (!hasArgAnnotation && result.args().isEmpty()) {
                    throw new InitializationException(className + " is not a group: it has no @Option or @Parameters annotations");
                }
            }

            private static void validateInjectSpec(TypedMember member) {
                if (!member.isSpec()) {
                    throw new IllegalStateException("Bug: validateInjectSpec() should only be called with @Spec members");
                }
                CommandReflection.assertNoDuplicateAnnotations(member, Spec.class, Parameters.class, Option.class, Unmatched.class, Mixin.class, ArgGroup.class);
                if (!CommandSpec.class.getName().equals(member.getTypeInfo().getClassName())) {
                    throw new InitializationException("@picocli.CommandLine.Spec annotation is only supported on fields of type " + CommandSpec.class.getName());
                }
            }

            private static void assertNoDuplicateAnnotations(TypedMember member, Class<? extends Annotation> myAnnotation, Class<? extends Annotation> ... forbidden) {
                for (Class<? extends Annotation> annotation : forbidden) {
                    if (!member.isAnnotationPresent(annotation)) continue;
                    throw new DuplicateOptionAnnotationsException("A member cannot have both @" + myAnnotation.getSimpleName() + " and @" + annotation.getSimpleName() + " annotations, but '" + member + "' has both.");
                }
            }

            private static CommandSpec buildMixinForMember(IAnnotatedElement member, IFactory factory) {
                try {
                    Object userObject = member.getter().get();
                    if (userObject == null) {
                        userObject = factory.create(member.getTypeInfo().getType());
                        member.setter().set(userObject);
                    }
                    CommandSpec result = CommandSpec.forAnnotatedObject(userObject, factory);
                    return result.withToString(member.getToString());
                }
                catch (InitializationException ex) {
                    throw ex;
                }
                catch (Exception ex) {
                    throw new InitializationException("Could not access or modify mixin member " + member + ": " + ex, ex);
                }
            }

            private static ArgSpec buildArgForMember(IAnnotatedElement member, IFactory factory) {
                if (member.isOption()) {
                    return OptionSpec.builder(member, factory).build();
                }
                if (member.isParameter()) {
                    return PositionalParamSpec.builder(member, factory).build();
                }
                return PositionalParamSpec.builder(member, factory).build();
            }

            private static ArgGroupSpec buildArgGroupForMember(IAnnotatedElement member, IFactory factory, CommandSpec commandSpec) {
                try {
                    return CommandReflection.extractArgGroupSpec(member, factory, commandSpec, true);
                }
                catch (InitializationException ex) {
                    throw ex;
                }
                catch (Exception ex) {
                    throw new InitializationException("Could not access or modify ArgGroup member " + member + ": " + ex, ex);
                }
            }

            private static UnmatchedArgsBinding buildUnmatchedForMember(IAnnotatedElement member) {
                ITypeInfo info = member.getTypeInfo();
                if (!(info.getClassName().equals(String[].class.getName()) || info.isCollection() && info.getActualGenericTypeArguments().equals(Collections.singletonList(String.class.getName())))) {
                    throw new InitializationException("Invalid type for " + member + ": must be either String[] or List<String>");
                }
                if (info.getClassName().equals(String[].class.getName())) {
                    return UnmatchedArgsBinding.forStringArrayConsumer(member.setter());
                }
                return UnmatchedArgsBinding.forStringCollectionSupplier(member.getter());
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        private static interface Predicate<T> {
            public boolean test(T var1);
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class Messages {
            private final CommandSpec spec;
            private final String bundleBaseName;
            private final ResourceBundle rb;
            private final Set<String> keys;

            public Messages(CommandSpec spec, String baseName) {
                this(spec, baseName, Messages.createBundle(baseName));
            }

            public Messages(CommandSpec spec, ResourceBundle rb) {
                this(spec, Messages.extractName(rb), rb);
            }

            public Messages(CommandSpec spec, String baseName, ResourceBundle rb) {
                this.spec = Assert.notNull(spec, "CommandSpec");
                this.bundleBaseName = baseName;
                this.rb = rb;
                this.keys = Messages.keys(rb);
                if (rb != null) {
                    new Tracer().debug("Created Messages from resourceBundle[base=%s] for command '%s' (%s)%n", baseName, spec.name(), spec);
                }
            }

            private static ResourceBundle createBundle(String baseName) {
                return ResourceBundle.getBundle(baseName);
            }

            private static String extractName(ResourceBundle rb) {
                try {
                    return (String)ResourceBundle.class.getDeclaredMethod("getBaseBundleName", new Class[0]).invoke(rb, new Object[0]);
                }
                catch (Exception ignored) {
                    return "?";
                }
            }

            private static Set<String> keys(ResourceBundle rb) {
                if (rb == null) {
                    return Collections.emptySet();
                }
                LinkedHashSet<String> keys = new LinkedHashSet<String>();
                Enumeration<String> k = rb.getKeys();
                while (k.hasMoreElements()) {
                    keys.add(k.nextElement());
                }
                return keys;
            }

            public static Messages copy(CommandSpec spec, Messages original) {
                return original == null ? null : new Messages(spec, original.bundleBaseName, original.rb);
            }

            public static boolean empty(Messages messages) {
                return messages == null || messages.rb == null;
            }

            public String getString(String key, String defaultValue) {
                if (this.isEmpty()) {
                    return defaultValue;
                }
                String cmd = this.spec.qualifiedName(".");
                if (this.keys.contains(cmd + "." + key)) {
                    return this.rb.getString(cmd + "." + key);
                }
                if (this.keys.contains(key)) {
                    return this.rb.getString(key);
                }
                return defaultValue;
            }

            boolean isEmpty() {
                return this.rb == null || this.keys.isEmpty();
            }

            public String[] getStringArray(String key, String[] defaultValues) {
                if (this.isEmpty()) {
                    return defaultValues;
                }
                String cmd = this.spec.qualifiedName(".");
                List<String> result = Messages.addAllWithPrefix(this.rb, cmd + "." + key, this.keys, new ArrayList<String>());
                if (!result.isEmpty()) {
                    return result.toArray(new String[0]);
                }
                Messages.addAllWithPrefix(this.rb, key, this.keys, result);
                return result.isEmpty() ? defaultValues : result.toArray(new String[0]);
            }

            private static List<String> addAllWithPrefix(ResourceBundle rb, String key, Set<String> keys, List<String> result) {
                if (keys.contains(key)) {
                    result.add(rb.getString(key));
                }
                int i = 0;
                String elementKey;
                while (keys.contains(elementKey = key + "." + i)) {
                    result.add(rb.getString(elementKey));
                    ++i;
                }
                return result;
            }

            public static String resourceBundleBaseName(Messages messages) {
                return messages == null ? null : messages.resourceBundleBaseName();
            }

            public static ResourceBundle resourceBundle(Messages messages) {
                return messages == null ? null : messages.resourceBundle();
            }

            public String resourceBundleBaseName() {
                return this.bundleBaseName;
            }

            public ResourceBundle resourceBundle() {
                return this.rb;
            }

            public CommandSpec commandSpec() {
                return this.spec;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class TypedMember
        implements IAnnotatedElement,
        IExtensible {
            final AccessibleObject accessible;
            final String name;
            final ITypeInfo typeInfo;
            private final InitialValueState initialValueState;
            private IScope scope;
            private IGetter getter;
            private ISetter setter;

            static TypedMember createIfAnnotated(Field field, IScope scope) {
                return TypedMember.isAnnotated(field) ? new TypedMember(field, scope) : null;
            }

            static boolean isAnnotated(AnnotatedElement e) {
                return e.isAnnotationPresent(Option.class) || e.isAnnotationPresent(Parameters.class) || e.isAnnotationPresent(ArgGroup.class) || e.isAnnotationPresent(Unmatched.class) || e.isAnnotationPresent(Mixin.class) || e.isAnnotationPresent(Spec.class) || e.isAnnotationPresent(ParentCommand.class);
            }

            TypedMember(Field field) {
                this.accessible = Assert.notNull(field, "field");
                this.accessible.setAccessible(true);
                this.name = field.getName();
                this.typeInfo = this.createTypeInfo(field.getType(), field.getGenericType());
                this.initialValueState = InitialValueState.POSTPONED;
            }

            private TypedMember(Field field, IScope scope) {
                this(field);
                if (ObjectScope.isProxyClass(scope)) {
                    throw new InitializationException("Invalid picocli annotation on interface field");
                }
                FieldBinding binding = new FieldBinding(scope, field);
                this.getter = binding;
                this.setter = binding;
                this.scope = scope;
            }

            static TypedMember createIfAnnotated(Method method, IScope scope, CommandSpec spec) {
                return TypedMember.isAnnotated(method) ? new TypedMember(method, scope, spec) : null;
            }

            private TypedMember(Method method, IScope scope, CommandSpec spec) {
                boolean isSetter;
                this.scope = scope;
                this.accessible = Assert.notNull(method, "method");
                this.accessible.setAccessible(true);
                this.name = TypedMember.propertyName(method.getName());
                Class<?>[] parameterTypes = method.getParameterTypes();
                boolean isGetter = parameterTypes.length == 0 && method.getReturnType() != Void.TYPE && method.getReturnType() != Void.class;
                boolean bl = isSetter = parameterTypes.length > 0;
                if (isSetter == isGetter) {
                    throw new InitializationException("Invalid method, must be either getter or setter: " + method);
                }
                if (isGetter) {
                    this.typeInfo = this.createTypeInfo(method.getReturnType(), method.getGenericReturnType());
                    if (ObjectScope.isProxyClass(scope)) {
                        PicocliInvocationHandler handler;
                        Object proxy = ObjectScope.tryGet(scope);
                        PicocliInvocationHandler picocliInvocationHandler = handler = (PicocliInvocationHandler)Proxy.getInvocationHandler(proxy);
                        picocliInvocationHandler.getClass();
                        PicocliInvocationHandler.ProxyBinding binding = picocliInvocationHandler.new PicocliInvocationHandler.ProxyBinding(method);
                        this.getter = binding;
                        this.setter = binding;
                        this.initializeInitialValue(method);
                    } else {
                        MethodBinding binding = new MethodBinding(scope, method, spec);
                        this.getter = binding;
                        this.setter = binding;
                    }
                    this.initialValueState = InitialValueState.POSTPONED;
                } else {
                    this.typeInfo = this.createTypeInfo(parameterTypes[0], method.getGenericParameterTypes()[0]);
                    MethodBinding binding = new MethodBinding(scope, method, spec);
                    this.getter = binding;
                    this.setter = binding;
                    this.initialValueState = InitialValueState.UNAVAILABLE;
                }
            }

            TypedMember(MethodParam param, IScope scope) {
                this.scope = scope;
                this.accessible = Assert.notNull(param, "command method parameter");
                this.accessible.setAccessible(true);
                this.name = param.getName();
                this.typeInfo = this.createTypeInfo(param.getType(), param.getParameterizedType());
                ObjectBinding binding = new ObjectBinding();
                this.getter = binding;
                this.setter = binding;
                this.initializeInitialValue(param);
                this.initialValueState = InitialValueState.POSTPONED;
            }

            private ITypeInfo createTypeInfo(Class<?> type, Type genericType) {
                Range arity = null;
                if (this.isOption()) {
                    arity = Range.valueOf(this.getAnnotation(Option.class).arity());
                }
                if (this.isParameter()) {
                    arity = Range.valueOf(this.getAnnotation(Parameters.class).arity());
                }
                if (arity == null || arity.isUnspecified) {
                    arity = this.isOption() ? (type == null || CommandLine.isBoolean(type) ? Range.valueOf("0") : Range.valueOf("1")) : Range.valueOf("1");
                    arity = arity.unspecified(true);
                }
                return RuntimeTypeInfo.create(type, this.annotationTypes(), genericType, arity, this.isOption() ? Boolean.TYPE : String.class, this.isInteractive());
            }

            private void initializeInitialValue(Object arg) {
                Class<?> type = this.typeInfo.getType();
                try {
                    if (type == Boolean.TYPE) {
                        this.setter.set(false);
                    } else if (type == Byte.TYPE) {
                        this.setter.set((byte)0);
                    } else if (type == Character.TYPE) {
                        this.setter.set(Character.valueOf('\u0000'));
                    } else if (type == Short.TYPE) {
                        this.setter.set((short)0);
                    } else if (type == Integer.TYPE) {
                        this.setter.set(0);
                    } else if (type == Long.TYPE) {
                        this.setter.set(0L);
                    } else if (type == Float.TYPE) {
                        this.setter.set(Float.valueOf(0.0f));
                    } else if (type == Double.TYPE) {
                        this.setter.set(0.0);
                    } else {
                        this.setter.set(null);
                    }
                }
                catch (Exception ex) {
                    throw new InitializationException("Could not set initial value for " + arg + ": " + ex.toString(), ex);
                }
            }

            @Override
            public Object userObject() {
                return this.accessible;
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                return this.accessible.isAnnotationPresent(annotationClass);
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return this.accessible.getAnnotation(annotationClass);
            }

            @Override
            public String getName() {
                return this.name;
            }

            @Override
            public boolean isArgSpec() {
                return this.isOption() || this.isParameter() || this.isMethodParameter() && !this.isMixin();
            }

            @Override
            public boolean isOption() {
                return this.isAnnotationPresent(Option.class);
            }

            @Override
            public boolean isParameter() {
                return this.isAnnotationPresent(Parameters.class);
            }

            @Override
            public boolean isArgGroup() {
                return this.isAnnotationPresent(ArgGroup.class);
            }

            @Override
            public boolean isMixin() {
                return this.isAnnotationPresent(Mixin.class);
            }

            @Override
            public boolean isUnmatched() {
                return this.isAnnotationPresent(Unmatched.class);
            }

            @Override
            public boolean isSpec() {
                return this.isAnnotationPresent(Spec.class);
            }

            @Override
            public boolean isParentCommand() {
                return this.isAnnotationPresent(ParentCommand.class);
            }

            @Override
            public boolean isMultiValue() {
                return CommandLine.isMultiValue(this.getType());
            }

            @Override
            public boolean isInteractive() {
                return this.isOption() && this.getAnnotation(Option.class).interactive() || this.isParameter() && this.getAnnotation(Parameters.class).interactive();
            }

            @Override
            public IScope scope() {
                return this.scope;
            }

            @Override
            public IGetter getter() {
                return this.getter;
            }

            @Override
            public ISetter setter() {
                return this.setter;
            }

            @Override
            public ITypeInfo getTypeInfo() {
                return this.typeInfo;
            }

            public Class<?> getType() {
                return this.typeInfo.getType();
            }

            public Class<?>[] getAuxiliaryTypes() {
                return this.typeInfo.getAuxiliaryTypes();
            }

            private Class<?>[] annotationTypes() {
                if (this.isOption()) {
                    return this.getAnnotation(Option.class).type();
                }
                if (this.isParameter()) {
                    return this.getAnnotation(Parameters.class).type();
                }
                return new Class[0];
            }

            public String toString() {
                return this.accessible.toString();
            }

            @Override
            public String getToString() {
                if (this.isMixin()) {
                    return TypedMember.abbreviate("mixin from member " + this.toGenericString());
                }
                return (this.accessible instanceof Field ? "field " : (this.accessible instanceof Method ? "method " : this.accessible.getClass().getSimpleName() + " ")) + TypedMember.abbreviate(this.toGenericString());
            }

            public String toGenericString() {
                return this.accessible instanceof Field ? ((Field)this.accessible).toGenericString() : (this.accessible instanceof Method ? ((Method)this.accessible).toGenericString() : this.accessible.toString());
            }

            @Override
            public boolean hasInitialValue() {
                return this.initialValueState == InitialValueState.CACHED || this.initialValueState == InitialValueState.POSTPONED;
            }

            @Override
            public boolean isMethodParameter() {
                return this.accessible instanceof MethodParam;
            }

            @Override
            public int getMethodParamPosition() {
                return this.isMethodParameter() ? ((MethodParam)this.accessible).position : -1;
            }

            @Override
            public String getMixinName() {
                String annotationName = this.getAnnotation(Mixin.class).name();
                return CommandLine.empty(annotationName) ? this.getName() : annotationName;
            }

            static String propertyName(String methodName) {
                if (methodName.length() > 3 && (methodName.startsWith("get") || methodName.startsWith("set"))) {
                    return TypedMember.decapitalize(methodName.substring(3));
                }
                return TypedMember.decapitalize(methodName);
            }

            private static String decapitalize(String name) {
                if (name == null || name.length() == 0) {
                    return name;
                }
                char[] chars = name.toCharArray();
                chars[0] = Character.toLowerCase(chars[0]);
                return new String(chars);
            }

            static String abbreviate(String text) {
                return text.replace("private ", "").replace("protected ", "").replace("public ", "").replace("java.lang.", "");
            }

            @Override
            public <T> T getExtension(Class<T> cls) {
                if (cls == InitialValueState.class) {
                    return cls.cast((Object)this.initialValueState);
                }
                return null;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static interface IExtensible {
            public <T> T getExtension(Class<T> var1);
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static interface IAnnotatedElement {
            public Object userObject();

            public boolean isAnnotationPresent(Class<? extends Annotation> var1);

            public <T extends Annotation> T getAnnotation(Class<T> var1);

            public String getName();

            public String getMixinName();

            public boolean isArgSpec();

            public boolean isOption();

            public boolean isParameter();

            public boolean isArgGroup();

            public boolean isMixin();

            public boolean isUnmatched();

            public boolean isSpec();

            public boolean isParentCommand();

            public boolean isMultiValue();

            public boolean isInteractive();

            public boolean hasInitialValue();

            public boolean isMethodParameter();

            public int getMethodParamPosition();

            public IScope scope();

            public IGetter getter();

            public ISetter setter();

            public ITypeInfo getTypeInfo();

            public String getToString();
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class RuntimeTypeInfo
        implements ITypeInfo {
            static final String ERRORMSG = "Unsupported generic type %s. Only List<T>, Map<K,V>, Optional<T>, and Map<K, Optional<V>> are supported. Type parameters may be char[], a non-array type, or a wildcard type with an upper or lower bound.";
            private final Class<?> type;
            private final Class<?>[] auxiliaryTypes;
            private final List<String> actualGenericTypeArguments;

            RuntimeTypeInfo(Class<?> type, Class<?>[] auxiliaryTypes, List<String> actualGenericTypeArguments) {
                this.type = Assert.notNull(type, "type");
                this.auxiliaryTypes = (Class[])Assert.notNull(auxiliaryTypes, "auxiliaryTypes").clone();
                this.actualGenericTypeArguments = actualGenericTypeArguments == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<String>(actualGenericTypeArguments));
            }

            static ITypeInfo createForAuxType(Class<?> type) {
                return RuntimeTypeInfo.create(type, new Class[0], (Type)null, Range.valueOf("1"), String.class, false);
            }

            public static ITypeInfo create(Class<?> type, Class<?>[] annotationTypes, Type genericType, Range arity, Class<?> defaultType, boolean interactive) {
                Class<?>[] auxiliaryTypes = RuntimeTypeInfo.inferTypes(type, annotationTypes, genericType);
                ArrayList<String> actualGenericTypeArguments = new ArrayList<String>();
                if (genericType instanceof ParameterizedType) {
                    Class<?>[] declaredTypeParameters;
                    for (Class<?> c : declaredTypeParameters = RuntimeTypeInfo.extractTypeParameters((ParameterizedType)genericType)) {
                        actualGenericTypeArguments.add(c.getName());
                    }
                }
                return RuntimeTypeInfo.create(type, auxiliaryTypes, actualGenericTypeArguments, arity, defaultType, interactive);
            }

            public static ITypeInfo create(Class<?> type, Class<?>[] auxiliaryTypes, List<String> actualGenericTypeArguments, Range arity, Class<?> defaultType, boolean interactive) {
                if (type == null) {
                    type = auxiliaryTypes == null || auxiliaryTypes.length == 0 ? (interactive ? char[].class : (arity.isVariable || arity.max > 1 ? String[].class : (arity.max == 1 ? String.class : defaultType))) : auxiliaryTypes[0];
                }
                if (auxiliaryTypes == null || auxiliaryTypes.length == 0) {
                    auxiliaryTypes = type.isArray() ? (interactive && type.equals(char[].class) ? new Class[]{char[].class} : new Class[]{type.getComponentType()}) : (Collection.class.isAssignableFrom(type) ? new Class[]{interactive ? char[].class : String.class} : (Map.class.isAssignableFrom(type) ? new Class[]{String.class, String.class} : new Class[]{type}));
                }
                return new RuntimeTypeInfo(type, auxiliaryTypes, actualGenericTypeArguments);
            }

            static Class<?>[] inferTypes(Class<?> propertyType, Class<?>[] annotationTypes, Type genericType) {
                if (annotationTypes != null && annotationTypes.length > 0) {
                    return annotationTypes;
                }
                if (propertyType.isArray()) {
                    if (CommandLine.isOptional(propertyType.getComponentType())) {
                        throw new InitializationException(String.format(ERRORMSG, genericType));
                    }
                    return new Class[]{propertyType.getComponentType()};
                }
                if (CommandLine.isMultiValue(propertyType) || CommandLine.isOptional(propertyType)) {
                    if (genericType instanceof ParameterizedType) {
                        return RuntimeTypeInfo.extractTypeParameters((ParameterizedType)genericType);
                    }
                    return new Class[]{String.class, String.class};
                }
                return new Class[]{propertyType};
            }

            static Class<?>[] extractTypeParameters(ParameterizedType genericType) {
                Type[] paramTypes = genericType.getActualTypeArguments();
                ArrayList<Class<char[]>> result = new ArrayList<Class<char[]>>();
                for (int i = 0; i < paramTypes.length; ++i) {
                    GenericArrayType gat;
                    if (paramTypes[i] instanceof Class) {
                        result.add((Class)paramTypes[i]);
                        continue;
                    }
                    if (paramTypes[i] instanceof ParameterizedType) {
                        ParameterizedType parameterizedParamType = (ParameterizedType)paramTypes[i];
                        Type raw = parameterizedParamType.getRawType();
                        if (i == 1 && raw instanceof Class && CommandLine.isOptional((Class)raw)) {
                            result.add((Class)raw);
                            Class<?>[] aux = RuntimeTypeInfo.extractTypeParameters(parameterizedParamType);
                            if (aux.length == 1) {
                                result.add(aux[0]);
                                continue;
                            }
                        }
                    } else if (paramTypes[i] instanceof WildcardType) {
                        WildcardType wildcardType = (WildcardType)paramTypes[i];
                        Type[] lower = wildcardType.getLowerBounds();
                        if (lower.length > 0 && lower[0] instanceof Class) {
                            result.add((Class)lower[0]);
                            continue;
                        }
                        Type[] upper = wildcardType.getUpperBounds();
                        if (upper.length > 0 && upper[0] instanceof Class) {
                            result.add((Class)upper[0]);
                            continue;
                        }
                    } else if (paramTypes[i] instanceof GenericArrayType && Character.TYPE.equals((gat = (GenericArrayType)paramTypes[i]).getGenericComponentType())) {
                        result.add(char[].class);
                        continue;
                    }
                    throw new InitializationException(String.format(ERRORMSG, genericType));
                }
                return result.toArray(new Class[0]);
            }

            @Override
            public boolean isBoolean() {
                return this.auxiliaryTypes[0] == Boolean.TYPE || this.auxiliaryTypes[0] == Boolean.class;
            }

            @Override
            public boolean isMultiValue() {
                return CommandLine.isMultiValue(this.type);
            }

            @Override
            public boolean isArray() {
                return this.type.isArray();
            }

            @Override
            public boolean isCollection() {
                return Collection.class.isAssignableFrom(this.type);
            }

            @Override
            public boolean isMap() {
                return Map.class.isAssignableFrom(this.type);
            }

            @Override
            public boolean isOptional() {
                return CommandLine.isOptional(this.type);
            }

            @Override
            public boolean isEnum() {
                return this.auxiliaryTypes[0].isEnum();
            }

            @Override
            public String getClassName() {
                return this.type.getName();
            }

            @Override
            public String getClassSimpleName() {
                return this.type.getSimpleName();
            }

            @Override
            public Class<?> getType() {
                return this.type;
            }

            @Override
            public Class<?>[] getAuxiliaryTypes() {
                return this.auxiliaryTypes;
            }

            @Override
            public List<String> getActualGenericTypeArguments() {
                return this.actualGenericTypeArguments;
            }

            @Override
            public List<ITypeInfo> getAuxiliaryTypeInfos() {
                ArrayList<ITypeInfo> result = new ArrayList<ITypeInfo>();
                for (Class<?> c : this.auxiliaryTypes) {
                    result.add(RuntimeTypeInfo.createForAuxType(c));
                }
                return result;
            }

            @Override
            public List<String> getEnumConstantNames() {
                if (!this.isEnum()) {
                    return Collections.emptyList();
                }
                ArrayList<String> result = new ArrayList<String>();
                for (Object c : this.auxiliaryTypes[0].getEnumConstants()) {
                    result.add(c.toString());
                }
                return result;
            }

            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof RuntimeTypeInfo)) {
                    return false;
                }
                RuntimeTypeInfo other = (RuntimeTypeInfo)obj;
                return Arrays.equals(other.auxiliaryTypes, this.auxiliaryTypes) && this.type.equals(other.type);
            }

            public int hashCode() {
                return Arrays.hashCode(this.auxiliaryTypes) + 37 * Assert.hashCode(this.type);
            }

            public String toString() {
                return String.format("RuntimeTypeInfo(%s, aux=%s, collection=%s, map=%s)", this.type.getCanonicalName(), Arrays.toString(this.auxiliaryTypes), this.isCollection(), this.isMap());
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static interface ITypeInfo {
            public boolean isBoolean();

            public boolean isMultiValue();

            public boolean isOptional();

            public boolean isArray();

            public boolean isCollection();

            public boolean isMap();

            public boolean isEnum();

            public List<String> getEnumConstantNames();

            public String getClassName();

            public String getClassSimpleName();

            public List<ITypeInfo> getAuxiliaryTypeInfos();

            public List<String> getActualGenericTypeArguments();

            public Class<?> getType();

            public Class<?>[] getAuxiliaryTypes();
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class MethodParam
        extends AccessibleObject {
            final Method method;
            final int paramIndex;
            final String name;
            int position;

            public MethodParam(Method method, int paramIndex) {
                this.method = method;
                this.paramIndex = paramIndex;
                String tmp = "arg" + paramIndex;
                try {
                    Method getParameters = Method.class.getMethod("getParameters", new Class[0]);
                    Object parameters = getParameters.invoke(method, new Object[0]);
                    Object parameter = Array.get(parameters, paramIndex);
                    tmp = (String)Class.forName("java.lang.reflect.Parameter").getDeclaredMethod("getName", new Class[0]).invoke(parameter, new Object[0]);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                this.name = tmp;
            }

            public Type getParameterizedType() {
                return this.method.getGenericParameterTypes()[this.paramIndex];
            }

            public String getName() {
                return this.name;
            }

            public Class<?> getType() {
                return this.method.getParameterTypes()[this.paramIndex];
            }

            public Method getDeclaringExecutable() {
                return this.method;
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                for (Annotation annotation : this.getDeclaredAnnotations()) {
                    if (!annotationClass.isAssignableFrom(annotation.annotationType())) continue;
                    return (T)((Annotation)annotationClass.cast(annotation));
                }
                return null;
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return this.method.getParameterAnnotations()[this.paramIndex];
            }

            @Override
            public void setAccessible(boolean flag) throws SecurityException {
                this.method.setAccessible(flag);
            }

            @Override
            public boolean isAccessible() throws SecurityException {
                return this.method.isAccessible();
            }

            public String toString() {
                return this.method.toString() + ":" + this.getName();
            }
        }

        public static class UnmatchedArgsBinding {
            private final IGetter getter;
            private final ISetter setter;
            private Object initialValue;

            public static UnmatchedArgsBinding forStringArrayConsumer(ISetter setter) {
                return new UnmatchedArgsBinding(null, setter);
            }

            public static UnmatchedArgsBinding forStringCollectionSupplier(IGetter getter) {
                return new UnmatchedArgsBinding(getter, null);
            }

            private UnmatchedArgsBinding(IGetter getter, ISetter setter) {
                if (getter == null && setter == null) {
                    throw new IllegalArgumentException("Getter and setter cannot both be null");
                }
                this.setter = setter;
                this.getter = getter;
                IGetter initialValueHolder = setter instanceof IGetter ? (IGetter)((Object)setter) : getter;
                try {
                    this.initialValue = initialValueHolder.get();
                }
                catch (Exception ex) {
                    new Tracer().debug("Could not obtain initial value for unmatched from %s%n", initialValueHolder);
                }
            }

            public IGetter getter() {
                return this.getter;
            }

            public ISetter setter() {
                return this.setter;
            }

            void addAll(String[] unmatched) {
                if (this.setter != null) {
                    try {
                        this.setter.set(unmatched);
                    }
                    catch (Exception ex) {
                        throw new PicocliException(String.format("Could not invoke setter (%s) with unmatched argument array '%s': %s", this.setter, Arrays.toString(unmatched), ex), ex);
                    }
                }
                try {
                    ArrayList<String> collection = (ArrayList<String>)this.getter.get();
                    if (collection == null) {
                        collection = new ArrayList<String>();
                        ((ISetter)((Object)this.getter)).set(collection);
                    }
                    collection.addAll(Arrays.asList(unmatched));
                }
                catch (Exception ex) {
                    throw new PicocliException(String.format("Could not add unmatched argument array '%s' to collection returned by getter (%s): %s", Arrays.toString(unmatched), this.getter, ex), ex);
                }
            }

            void clear() {
                ISetter initialValueHolder = this.setter;
                if (initialValueHolder == null) {
                    if (this.getter instanceof ISetter) {
                        initialValueHolder = (ISetter)((Object)this.getter);
                    } else {
                        new Tracer().warn("Unable to clear %s: it does not implement ISetter", this.getter);
                        return;
                    }
                }
                try {
                    initialValueHolder.set(this.initialValue);
                }
                catch (Exception ex) {
                    throw new PicocliException(String.format("Could not invoke setter (%s) with initial value: %s", initialValueHolder, ex), ex);
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class ArgGroupSpec
        implements IOrdered {
            static final int DEFAULT_ORDER = -1;
            private static final String NO_HEADING = "__no_heading__";
            private static final String NO_HEADING_KEY = "__no_heading_key__";
            private final String heading;
            private final String headingKey;
            private final boolean exclusive;
            private final Range multiplicity;
            private final boolean validate;
            private final int order;
            private final IGetter getter;
            private final ISetter setter;
            private final IScope scope;
            private final ITypeInfo typeInfo;
            private final List<ArgGroupSpec> subgroups;
            private final Set<ArgSpec> args;
            private Messages messages;
            private ArgGroupSpec parentGroup;
            private String id = "1";
            private final List<IAnnotatedElement> specElements;

            ArgGroupSpec(Builder builder) {
                this.heading = NO_HEADING.equals(builder.heading) ? null : builder.heading;
                this.headingKey = NO_HEADING_KEY.equals(builder.headingKey) ? null : builder.headingKey;
                this.exclusive = builder.exclusive && builder.validate;
                this.multiplicity = builder.multiplicity;
                this.validate = builder.validate;
                this.order = builder.order;
                this.typeInfo = builder.typeInfo;
                this.getter = builder.getter;
                this.setter = builder.setter;
                this.scope = builder.scope;
                this.specElements = Collections.unmodifiableList(new ArrayList<IAnnotatedElement>(builder.specElements()));
                this.args = Collections.unmodifiableSet(new LinkedHashSet<ArgSpec>(builder.args()));
                this.subgroups = Collections.unmodifiableList(new ArrayList<ArgGroupSpec>(builder.subgroups()));
                if (this.args.isEmpty() && this.subgroups.isEmpty()) {
                    throw new InitializationException("ArgGroup has no options or positional parameters, and no subgroups: " + (this.getter == null ? this.setter : this.getter) + " in " + this.scope);
                }
                int i = 1;
                for (ArgGroupSpec sub : this.subgroups) {
                    sub.parentGroup = this;
                    sub.id = this.id + "." + i++;
                }
                for (ArgSpec arg : this.args) {
                    arg.group = this;
                }
                if (!this.validate && builder.exclusive) {
                    new Tracer().info("Setting exclusive=%s because %s is a non-validating group.%n", this.exclusive, this.synopsisUnit());
                }
                if (this.exclusive) {
                    String modifiedArgs = "";
                    String sep = "";
                    for (ArgSpec arg : this.args) {
                        if (arg.required()) continue;
                        modifiedArgs = modifiedArgs + sep + (arg.isOption() ? ((OptionSpec)arg).longestName() : arg.paramLabel() + "[" + ((PositionalParamSpec)arg).index() + "]");
                        sep = ",";
                        arg.required = true;
                    }
                    if (modifiedArgs.length() > 0) {
                        new Tracer().info("Made %s required in the group because %s is an exclusive group.%n", modifiedArgs, this.synopsisUnit());
                    }
                }
            }

            public static Builder builder() {
                return new Builder();
            }

            public static Builder builder(IAnnotatedElement annotatedElement) {
                return new Builder(Assert.notNull(annotatedElement, "annotatedElement"));
            }

            public boolean exclusive() {
                return this.exclusive;
            }

            public Range multiplicity() {
                return this.multiplicity;
            }

            public boolean validate() {
                return this.validate;
            }

            @Override
            public int order() {
                return this.order;
            }

            public String heading() {
                if (this.messages() == null) {
                    return this.heading;
                }
                String newValue = this.messages().getString(this.headingKey(), null);
                if (newValue != null) {
                    return newValue;
                }
                return this.heading;
            }

            public String headingKey() {
                return this.headingKey;
            }

            public ArgGroupSpec parentGroup() {
                return this.parentGroup;
            }

            public List<ArgGroupSpec> subgroups() {
                return this.subgroups;
            }

            public List<IAnnotatedElement> specElements() {
                return this.specElements;
            }

            public boolean isSubgroupOf(ArgGroupSpec group) {
                for (ArgGroupSpec sub : group.subgroups) {
                    if (this == sub) {
                        return true;
                    }
                    if (!this.isSubgroupOf(sub)) continue;
                    return true;
                }
                return false;
            }

            public ITypeInfo typeInfo() {
                return this.typeInfo;
            }

            public IGetter getter() {
                return this.getter;
            }

            public ISetter setter() {
                return this.setter;
            }

            public IScope scope() {
                return this.scope;
            }

            Object userObject() {
                try {
                    return this.getter.get();
                }
                catch (Exception ex) {
                    return ex.toString();
                }
            }

            Object userObjectOr(Object defaultValue) {
                try {
                    return this.getter.get();
                }
                catch (Exception ex) {
                    return defaultValue;
                }
            }

            String id() {
                return this.id;
            }

            int argCount() {
                int result = this.args.size();
                for (ArgGroupSpec sub : this.subgroups()) {
                    result += sub.argCount();
                }
                return result;
            }

            int localPositionalParamCount() {
                int result = 0;
                for (ArgSpec arg : this.args) {
                    if (!arg.isPositional()) continue;
                    result += ((PositionalParamSpec)arg).capacity().min();
                }
                return result;
            }

            public Set<ArgSpec> args() {
                return this.args;
            }

            public Set<ArgSpec> requiredArgs() {
                LinkedHashSet<ArgSpec> result = new LinkedHashSet<ArgSpec>(this.args);
                Iterator iter = result.iterator();
                while (iter.hasNext()) {
                    if (((ArgSpec)iter.next()).required()) continue;
                    iter.remove();
                }
                return Collections.unmodifiableSet(result);
            }

            public List<PositionalParamSpec> positionalParameters() {
                ArrayList<PositionalParamSpec> result = new ArrayList<PositionalParamSpec>();
                for (ArgSpec arg : this.args()) {
                    if (!(arg instanceof PositionalParamSpec)) continue;
                    result.add((PositionalParamSpec)arg);
                }
                return Collections.unmodifiableList(result);
            }

            public List<OptionSpec> options() {
                ArrayList<OptionSpec> result = new ArrayList<OptionSpec>();
                for (ArgSpec arg : this.args()) {
                    if (!(arg instanceof OptionSpec)) continue;
                    result.add((OptionSpec)arg);
                }
                return Collections.unmodifiableList(result);
            }

            public List<OptionSpec> allOptionsNested() {
                return this.addGroupOptionsToListRecursively(new ArrayList<OptionSpec>());
            }

            private List<OptionSpec> addGroupOptionsToListRecursively(List<OptionSpec> result) {
                result.addAll(this.options());
                for (ArgGroupSpec subGroup : this.subgroups()) {
                    subGroup.addGroupOptionsToListRecursively(result);
                }
                return result;
            }

            public List<PositionalParamSpec> allPositionalParametersNested() {
                return this.addGroupPositionalsToListRecursively(new ArrayList<PositionalParamSpec>());
            }

            private List<PositionalParamSpec> addGroupPositionalsToListRecursively(List<PositionalParamSpec> result) {
                result.addAll(this.positionalParameters());
                for (ArgGroupSpec subGroup : this.subgroups()) {
                    subGroup.addGroupPositionalsToListRecursively(result);
                }
                return result;
            }

            public String synopsis() {
                return this.synopsisText(new Help.ColorScheme.Builder(Help.Ansi.OFF).build(), new HashSet<ArgSpec>()).toString();
            }

            String synopsisUnit() {
                Help.ColorScheme colorScheme = new Help.ColorScheme.Builder(Help.Ansi.OFF).build();
                return this.synopsisUnitText(colorScheme, this.rawSynopsisUnitText(colorScheme, new HashSet<ArgSpec>())).toString();
            }

            public Help.Ansi.Text synopsisText(Help.ColorScheme colorScheme, Set<ArgSpec> outparam_groupArgs) {
                int i;
                Help.Ansi.Text synopsis = this.rawSynopsisUnitText(colorScheme, outparam_groupArgs);
                Help.Ansi.Text result = this.synopsisUnitText(colorScheme, synopsis);
                for (i = 1; i < this.multiplicity.min(); ++i) {
                    result = result.concat(" (").concat(synopsis).concat(")");
                }
                if (this.multiplicity().isVariable()) {
                    result = result.concat("...");
                } else {
                    while (i < this.multiplicity.max()) {
                        result = result.concat(" [").concat(synopsis).concat("]");
                        ++i;
                    }
                }
                return result;
            }

            private Help.Ansi.Text synopsisUnitText(Help.ColorScheme colorScheme, Help.Ansi.Text synopsis) {
                String prefix = this.multiplicity().min() > 0 ? "(" : "[";
                String postfix = this.multiplicity().min() > 0 ? ")" : "]";
                return colorScheme.text(prefix).concat(synopsis).concat(postfix);
            }

            private Help.Ansi.Text rawSynopsisUnitText(Help.ColorScheme colorScheme, Set<ArgSpec> outparam_groupArgs) {
                String infix = this.exclusive() ? " | " : " ";
                Help.Ansi ansi = colorScheme.ansi();
                ((Object)((Object)ansi)).getClass();
                Help.Ansi.Text synopsis = ansi.new Help.Ansi.Text(0);
                for (ArgSpec arg : this.args()) {
                    String prefix = synopsis.length > 0 ? infix : "";
                    synopsis = arg instanceof OptionSpec ? this.concatOptionText(prefix, synopsis, colorScheme, (OptionSpec)arg) : this.concatPositionalText(prefix, synopsis, colorScheme, (PositionalParamSpec)arg);
                    outparam_groupArgs.add(arg);
                }
                for (ArgGroupSpec subgroup : this.subgroups()) {
                    if (synopsis.length > 0) {
                        synopsis = synopsis.concat(infix);
                    }
                    synopsis = synopsis.concat(subgroup.synopsisText(colorScheme, outparam_groupArgs));
                }
                return synopsis;
            }

            private Help.Ansi.Text concatOptionText(String prefix, Help.Ansi.Text text, Help.ColorScheme colorScheme, OptionSpec option) {
                return Help.concatOptionText(prefix, text, colorScheme, option, this.createLabelRenderer(option.commandSpec));
            }

            private Help.Ansi.Text concatPositionalText(String prefix, Help.Ansi.Text text, Help.ColorScheme colorScheme, PositionalParamSpec positionalParam) {
                return Help.concatPositionalText(prefix, text, colorScheme, positionalParam, this.createLabelRenderer(positionalParam.commandSpec));
            }

            public Help.IParamLabelRenderer createLabelRenderer(CommandSpec commandSpec) {
                return new Help.DefaultParamLabelRenderer(commandSpec == null ? CommandSpec.create() : commandSpec);
            }

            public Messages messages() {
                return this.messages;
            }

            public ArgGroupSpec messages(Messages msgs) {
                this.messages = msgs;
                for (ArgGroupSpec sub : this.subgroups()) {
                    sub.messages(msgs);
                }
                return this;
            }

            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof ArgGroupSpec)) {
                    return false;
                }
                ArgGroupSpec other = (ArgGroupSpec)obj;
                return this.exclusive == other.exclusive && Assert.equals(this.multiplicity, other.multiplicity) && this.validate == other.validate && this.order == other.order && Assert.equals(this.heading, other.heading) && Assert.equals(this.headingKey, other.headingKey) && Assert.equals(this.subgroups, other.subgroups) && Assert.equals(this.args, other.args);
            }

            public int hashCode() {
                int result = 17;
                result += 37 * result + Assert.hashCode(this.exclusive);
                result += 37 * result + Assert.hashCode(this.multiplicity);
                result += 37 * result + Assert.hashCode(this.validate);
                result += 37 * result + this.order;
                result += 37 * result + Assert.hashCode(this.heading);
                result += 37 * result + Assert.hashCode(this.headingKey);
                result += 37 * result + Assert.hashCode(this.subgroups);
                result += 37 * result + Assert.hashCode(this.args);
                return result;
            }

            public String toString() {
                return "ArgGroup[exclusive=" + this.exclusive + ", multiplicity=" + this.multiplicity + ", validate=" + this.validate + ", order=" + this.order + ", args=[" + ArgSpec.describe(this.args()) + "], headingKey=" + ArgGroupSpec.quote(this.headingKey) + ", heading=" + ArgGroupSpec.quote(this.heading) + ", subgroups=" + this.subgroups + "]";
            }

            private static String quote(String s) {
                return s == null ? "null" : "'" + s + "'";
            }

            void initUserObject(CommandLine commandLine) {
                if (commandLine == null) {
                    new Tracer().debug("Could not create user object for %s with null CommandLine%n.", this);
                    return;
                }
                try {
                    this.tryInitUserObject(commandLine);
                }
                catch (PicocliException ex) {
                    throw ex;
                }
                catch (Exception ex) {
                    throw new InitializationException("Could not create user object for " + this, ex);
                }
            }

            void tryInitUserObject(CommandLine commandLine) throws Exception {
                Tracer tracer = commandLine.tracer;
                if (this.typeInfo() != null) {
                    tracer.debug("Creating new user object of type %s for group %s%n", this.typeInfo().getAuxiliaryTypes()[0], this.synopsis());
                    Object userObject = DefaultFactory.create(commandLine.factory, this.typeInfo().getAuxiliaryTypes()[0]);
                    tracer.debug("Created %s, invoking setter %s with scope %s%n", userObject, this.setter(), this.scope());
                    this.setUserObject(userObject, commandLine.factory);
                    for (ArgSpec arg : this.args()) {
                        tracer.debug("Initializing %s in group %s: setting scope to user object %s and initializing initial and default values%n", ArgSpec.describe(arg, "="), this.synopsis(), userObject);
                        arg.scope().set(userObject);
                        ((Interpreter)((CommandLine)commandLine).interpreter).parseResultBuilder.isInitializingDefaultValues = true;
                        arg.applyInitialValue(tracer);
                        commandLine.interpreter.applyDefault(commandLine.getCommandSpec().defaultValueProvider(), arg);
                        ((Interpreter)((CommandLine)commandLine).interpreter).parseResultBuilder.isInitializingDefaultValues = false;
                    }
                    for (ArgGroupSpec subgroup : this.subgroups()) {
                        tracer.debug("Setting scope for subgroup %s with setter=%s in group %s to user object %s%n", subgroup.synopsis(), subgroup.setter(), this.synopsis(), userObject);
                        subgroup.scope().set(userObject);
                    }
                    for (IAnnotatedElement specElement : this.specElements()) {
                        tracer.debug("Setting @Spec with setter=%s in user object %s to %s%n", specElement.setter(), userObject, commandLine.getCommandSpec());
                        specElement.scope().set(userObject);
                        specElement.setter().set(commandLine.getCommandSpec());
                    }
                } else {
                    tracer.debug("No type information available for group %s: cannot create new user object. Scope for arg setters is not changed.%n", this.synopsis());
                }
                tracer.debug("Initialization complete for group %s%n", this.synopsis());
            }

            void setUserObject(Object userObject, IFactory factory) throws Exception {
                if (this.typeInfo().isCollection()) {
                    Collection c = (Collection)this.getter().get();
                    if (c == null) {
                        Collection c2;
                        c = c2 = (Collection)DefaultFactory.create(factory, this.typeInfo.getType());
                        this.setter().set(c);
                    }
                    c.add(userObject);
                } else if (this.typeInfo().isArray()) {
                    Object old = this.getter().get();
                    int oldSize = old == null ? 0 : Array.getLength(old);
                    Object array = Array.newInstance(this.typeInfo().getAuxiliaryTypes()[0], oldSize + 1);
                    for (int i = 0; i < oldSize; ++i) {
                        Array.set(array, i, Array.get(old, i));
                    }
                    Array.set(array, oldSize, userObject);
                    this.setter().set(array);
                } else {
                    this.setter().set(userObject);
                }
            }

            ParseResult.GroupValidationResult validateArgs(CommandLine commandLine, Collection<ArgSpec> matchedArgs) {
                LinkedHashSet<ArgSpec> intersection = new LinkedHashSet<ArgSpec>(this.args());
                LinkedHashSet<ArgSpec> missing = new LinkedHashSet<ArgSpec>(this.requiredArgs());
                LinkedHashSet<ArgSpec> found = new LinkedHashSet<ArgSpec>(matchedArgs);
                missing.removeAll(matchedArgs);
                intersection.retainAll(found);
                int presentCount = intersection.size();
                boolean haveMissing = !missing.isEmpty() && !this.exclusive();
                boolean someButNotAllSpecified = haveMissing && !intersection.isEmpty();
                String exclusiveElements = ArgSpec.describe(intersection);
                String requiredElements = ArgSpec.describe(this.requiredArgs());
                String missingElements = ArgSpec.describe(missing);
                return this.validate(commandLine, presentCount, haveMissing, someButNotAllSpecified, exclusiveElements, requiredElements, missingElements);
            }

            private ParseResult.GroupValidationResult validate(CommandLine commandLine, int presentCount, boolean haveMissing, boolean someButNotAllSpecified, String exclusiveElements, String requiredElements, String missingElements) {
                if (this.exclusive()) {
                    if (presentCount > 1) {
                        return new ParseResult.GroupValidationResult(ParseResult.GroupValidationResult.Type.FAILURE_PRESENT, new MutuallyExclusiveArgsException(commandLine, "Error: " + exclusiveElements + " are mutually exclusive (specify only one)"));
                    }
                    if (this.multiplicity().min > 0 && haveMissing) {
                        return new ParseResult.GroupValidationResult(ParseResult.GroupValidationResult.Type.FAILURE_ABSENT, new MissingParameterException(commandLine, this.args(), "Error: Missing required argument (specify one of these): " + requiredElements));
                    }
                } else {
                    if (someButNotAllSpecified) {
                        return new ParseResult.GroupValidationResult(ParseResult.GroupValidationResult.Type.FAILURE_PARTIAL, new MissingParameterException(commandLine, this.args(), "Error: Missing required argument(s): " + missingElements));
                    }
                    if (this.multiplicity().min > 0 && haveMissing) {
                        return new ParseResult.GroupValidationResult(ParseResult.GroupValidationResult.Type.FAILURE_ABSENT, new MissingParameterException(commandLine, this.args(), "Error: Missing required argument(s): " + missingElements));
                    }
                }
                return presentCount > 0 ? ParseResult.GroupValidationResult.SUCCESS_PRESENT : ParseResult.GroupValidationResult.SUCCESS_ABSENT;
            }

            /*
             * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
             */
            public static class Builder {
                private IGetter getter;
                private ISetter setter;
                private IScope scope;
                private ITypeInfo typeInfo;
                private String heading;
                private String headingKey;
                private boolean exclusive = true;
                private Range multiplicity = Range.valueOf("0..1");
                private boolean validate = true;
                private int order = -1;
                private final List<ArgSpec> args = new ArrayList<ArgSpec>();
                private final List<ArgGroupSpec> subgroups = new ArrayList<ArgGroupSpec>();
                private final List<IAnnotatedElement> specElements = new ArrayList<IAnnotatedElement>();
                private Boolean topologicalSortDone;
                private final List<Builder> compositesReferencingMe = new ArrayList<Builder>();

                Builder() {
                }

                Builder(IAnnotatedElement source) {
                    this.typeInfo = source.getTypeInfo();
                    this.getter = source.getter();
                    this.setter = source.setter();
                    this.scope = source.scope();
                }

                public Builder updateArgGroupAttributes(ArgGroup group) {
                    return this.heading(group.heading()).headingKey(group.headingKey()).exclusive(group.exclusive()).multiplicity(group.multiplicity()).validate(group.validate()).order(group.order());
                }

                public ArgGroupSpec build() {
                    return new ArgGroupSpec(this);
                }

                public boolean exclusive() {
                    return this.exclusive;
                }

                public Builder exclusive(boolean newValue) {
                    this.exclusive = newValue;
                    return this;
                }

                public Range multiplicity() {
                    return this.multiplicity;
                }

                public Builder multiplicity(String newValue) {
                    return this.multiplicity(Range.valueOf(newValue));
                }

                public Builder multiplicity(Range newValue) {
                    this.multiplicity = newValue;
                    return this;
                }

                public boolean validate() {
                    return this.validate;
                }

                public Builder validate(boolean newValue) {
                    this.validate = newValue;
                    return this;
                }

                public int order() {
                    return this.order;
                }

                public Builder order(int order) {
                    this.order = order;
                    return this;
                }

                public String heading() {
                    return this.heading;
                }

                public Builder heading(String newValue) {
                    this.heading = newValue;
                    return this;
                }

                public String headingKey() {
                    return this.headingKey;
                }

                public Builder headingKey(String newValue) {
                    this.headingKey = newValue;
                    return this;
                }

                public ITypeInfo typeInfo() {
                    return this.typeInfo;
                }

                public Builder typeInfo(ITypeInfo newValue) {
                    this.typeInfo = newValue;
                    return this;
                }

                public IGetter getter() {
                    return this.getter;
                }

                public Builder getter(IGetter getter) {
                    this.getter = getter;
                    return this;
                }

                public ISetter setter() {
                    return this.setter;
                }

                public Builder setter(ISetter setter) {
                    this.setter = setter;
                    return this;
                }

                public IScope scope() {
                    return this.scope;
                }

                public Builder scope(IScope scope) {
                    this.scope = scope;
                    return this;
                }

                public Builder addArg(ArgSpec arg) {
                    this.args.add(arg);
                    return this;
                }

                public List<ArgSpec> args() {
                    return this.args;
                }

                public Builder addSubgroup(ArgGroupSpec group) {
                    this.subgroups.add(group);
                    return this;
                }

                public List<ArgGroupSpec> subgroups() {
                    return this.subgroups;
                }

                public Builder addSpecElement(IAnnotatedElement element) {
                    this.specElements.add(element);
                    return this;
                }

                public List<IAnnotatedElement> specElements() {
                    return this.specElements;
                }
            }
        }

        public static interface IOrdered {
            public int order();
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class PositionalParamSpec
        extends ArgSpec {
            private Range index;
            private Range capacity;
            private final Range builderCapacity;

            private PositionalParamSpec(Builder builder) {
                super(builder);
                this.index = builder.index == null ? Range.defaultParameterIndex(this.typeInfo) : builder.index;
                this.builderCapacity = builder.capacity;
                this.initCapacity();
                if (this.toString == null) {
                    this.toString = "positional parameter[" + this.index() + "]";
                }
            }

            private void initCapacity() {
                this.capacity = this.builderCapacity == null ? Range.parameterCapacity(this.arity(), this.index) : this.builderCapacity;
            }

            public static Builder builder() {
                return new Builder();
            }

            public static Builder builder(PositionalParamSpec original) {
                return new Builder(original);
            }

            public static Builder builder(IAnnotatedElement source, IFactory factory) {
                return new Builder(source, factory);
            }

            public Builder toBuilder() {
                return new Builder(this);
            }

            @Override
            public boolean isOption() {
                return false;
            }

            @Override
            public boolean isPositional() {
                return true;
            }

            @Override
            protected Collection<String> getAdditionalDescriptionKeys() {
                return Collections.singletonList(this.paramLabel() + "[" + this.index() + "]");
            }

            public Range index() {
                return this.index;
            }

            private Range capacity() {
                return this.capacity;
            }

            public int hashCode() {
                return super.hashCodeImpl() + 37 * Assert.hashCode(this.capacity) + 37 * Assert.hashCode(this.index);
            }

            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof PositionalParamSpec)) {
                    return false;
                }
                PositionalParamSpec other = (PositionalParamSpec)obj;
                return super.equalsImpl(other) && Assert.equals(this.capacity, other.capacity) && Assert.equals(this.index, other.index);
            }

            /*
             * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
             */
            public static class Builder
            extends ArgSpec.Builder<Builder> {
                private Range capacity;
                private Range index;

                private Builder() {
                }

                private Builder(PositionalParamSpec original) {
                    super(original);
                    this.index = original.index;
                    this.capacity = original.capacity;
                }

                private Builder(IAnnotatedElement member, IFactory factory) {
                    super(member.getAnnotation(Parameters.class), member, factory);
                    this.index = Range.parameterIndex(member);
                    this.capacity = Range.parameterCapacity(member);
                }

                @Override
                public PositionalParamSpec build() {
                    return new PositionalParamSpec(this);
                }

                @Override
                protected Builder self() {
                    return this;
                }

                public Range index() {
                    return this.index;
                }

                public Builder index(String range) {
                    return this.index(Range.valueOf(range));
                }

                public Builder index(Range index) {
                    this.index = index;
                    return this.self();
                }

                Range capacity() {
                    return this.capacity;
                }

                Builder capacity(Range capacity) {
                    this.capacity = capacity;
                    return this.self();
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class OptionSpec
        extends ArgSpec
        implements IOrdered {
            public static final String DEFAULT_FALLBACK_VALUE = "";
            static final int DEFAULT_ORDER = -1;
            private final String[] names;
            private final boolean help;
            private final boolean usageHelp;
            private final boolean versionHelp;
            private final boolean negatable;
            private final String fallbackValue;
            private final int order;

            public static Builder builder(String name, String ... names) {
                String[] copy = new String[Assert.notNull(names, "names").length + 1];
                copy[0] = Assert.notNull(name, "name");
                System.arraycopy((Object)names, 0, (Object)copy, 1, names.length);
                return new Builder(copy);
            }

            public static Builder builder(String[] names) {
                return new Builder(names);
            }

            public static Builder builder(IAnnotatedElement source, IFactory factory) {
                return new Builder(source, factory);
            }

            public static Builder builder(OptionSpec original) {
                return new Builder(original);
            }

            private OptionSpec(Builder builder) {
                super(builder);
                if (builder.names == null) {
                    throw new InitializationException("OptionSpec names cannot be null. Specify at least one option name.");
                }
                this.names = (String[])builder.names.clone();
                this.help = builder.help;
                this.usageHelp = builder.usageHelp;
                this.versionHelp = builder.versionHelp;
                this.order = builder.order;
                this.negatable = builder.negatable;
                this.fallbackValue = builder.fallbackValue;
                if (this.names.length == 0 || Arrays.asList(this.names).contains(DEFAULT_FALLBACK_VALUE)) {
                    throw new InitializationException("Invalid names: " + Arrays.toString(this.names));
                }
                if (this.toString == null) {
                    this.toString = "option " + this.longestName();
                }
            }

            public Builder toBuilder() {
                return new Builder(this);
            }

            @Override
            public boolean isOption() {
                return true;
            }

            @Override
            public boolean isPositional() {
                return false;
            }

            @Override
            protected boolean internalShowDefaultValue(boolean usageMessageShowDefaults) {
                return super.internalShowDefaultValue(usageMessageShowDefaults) && !this.help() && !this.versionHelp() && !this.usageHelp();
            }

            @Override
            protected Collection<String> getAdditionalDescriptionKeys() {
                LinkedHashSet<String> result = new LinkedHashSet<String>();
                for (String name : this.names()) {
                    result.add(CommandSpec.stripPrefix(name));
                }
                return result;
            }

            public String[] names() {
                return this.interpolate((String[])this.names.clone());
            }

            public String longestName() {
                return Help.ShortestFirst.longestFirst(this.names())[0];
            }

            public String shortestName() {
                return Help.ShortestFirst.sort(this.names())[0];
            }

            @Override
            public int order() {
                return this.order;
            }

            @Deprecated
            public boolean help() {
                return this.help;
            }

            public boolean usageHelp() {
                return this.usageHelp;
            }

            public boolean versionHelp() {
                return this.versionHelp;
            }

            public boolean negatable() {
                return this.negatable;
            }

            public String fallbackValue() {
                return this.interpolate(this.fallbackValue);
            }

            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof OptionSpec)) {
                    return false;
                }
                OptionSpec other = (OptionSpec)obj;
                return super.equalsImpl(other) && this.help == other.help && this.usageHelp == other.usageHelp && this.versionHelp == other.versionHelp && this.order == other.order && this.negatable == other.negatable && Assert.equals(this.fallbackValue, other.fallbackValue) && new HashSet<String>(Arrays.asList(this.names)).equals(new HashSet<String>(Arrays.asList(other.names)));
            }

            public int hashCode() {
                return super.hashCodeImpl() + 37 * Assert.hashCode(this.help) + 37 * Assert.hashCode(this.usageHelp) + 37 * Assert.hashCode(this.versionHelp) + 37 * Arrays.hashCode(this.names) + 37 * Assert.hashCode(this.negatable) + 37 * Assert.hashCode(this.fallbackValue) + 37 * this.order;
            }

            /*
             * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
             */
            public static class Builder
            extends ArgSpec.Builder<Builder> {
                private String[] names;
                private boolean help;
                private boolean usageHelp;
                private boolean versionHelp;
                private boolean negatable;
                private String fallbackValue = "";
                private int order = -1;

                private Builder(String[] names) {
                    this.names = names;
                }

                private Builder(OptionSpec original) {
                    super(original);
                    this.names = original.names;
                    this.help = original.help;
                    this.usageHelp = original.usageHelp;
                    this.versionHelp = original.versionHelp;
                    this.negatable = original.negatable;
                    this.fallbackValue = original.fallbackValue;
                    this.order = original.order;
                }

                private Builder(IAnnotatedElement member, IFactory factory) {
                    super(member.getAnnotation(Option.class), member, factory);
                    Option option = member.getAnnotation(Option.class);
                    this.names = option.names();
                    this.help = option.help();
                    this.usageHelp = option.usageHelp();
                    this.versionHelp = option.versionHelp();
                    this.negatable = option.negatable();
                    this.fallbackValue = "_NULL_".equals(option.fallbackValue()) ? null : option.fallbackValue();
                    this.order = option.order();
                }

                @Override
                public OptionSpec build() {
                    return new OptionSpec(this);
                }

                @Override
                protected Builder self() {
                    return this;
                }

                public String[] names() {
                    return (String[])this.names.clone();
                }

                @Deprecated
                public boolean help() {
                    return this.help;
                }

                public boolean usageHelp() {
                    return this.usageHelp;
                }

                public boolean versionHelp() {
                    return this.versionHelp;
                }

                public boolean negatable() {
                    return this.negatable;
                }

                public String fallbackValue() {
                    return this.fallbackValue;
                }

                public int order() {
                    return this.order;
                }

                public Builder names(String ... names) {
                    this.names = (String[])Assert.notNull(names, "names").clone();
                    return this.self();
                }

                public Builder help(boolean help) {
                    this.help = help;
                    return this.self();
                }

                public Builder usageHelp(boolean usageHelp) {
                    this.usageHelp = usageHelp;
                    return this.self();
                }

                public Builder versionHelp(boolean versionHelp) {
                    this.versionHelp = versionHelp;
                    return this.self();
                }

                public Builder negatable(boolean negatable) {
                    this.negatable = negatable;
                    return this.self();
                }

                public Builder fallbackValue(String fallbackValue) {
                    this.fallbackValue = fallbackValue;
                    return this.self();
                }

                public Builder order(int order) {
                    this.order = order;
                    return this.self();
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static abstract class ArgSpec {
            static final String NULL_VALUE = "_NULL_";
            static final String DESCRIPTION_VARIABLE_DEFAULT_VALUE = "${DEFAULT-VALUE}";
            static final String DESCRIPTION_VARIABLE_FALLBACK_VALUE = "${FALLBACK-VALUE}";
            static final String DESCRIPTION_VARIABLE_MAP_FALLBACK_VALUE = "${MAP-FALLBACK-VALUE}";
            static final String DESCRIPTION_VARIABLE_COMPLETION_CANDIDATES = "${COMPLETION-CANDIDATES}";
            private static final String NO_DEFAULT_VALUE = "__no_default_value__";
            private static final String UNSPECIFIED = "__unspecified__";
            private final boolean inherited;
            private final ArgSpec root;
            private final boolean hidden;
            private final String paramLabel;
            private final boolean hideParamSyntax;
            private final String[] description;
            private final String descriptionKey;
            private final Help.Visibility showDefaultValue;
            private Messages messages;
            CommandSpec commandSpec;
            private ArgGroupSpec group;
            private final Object userObject;
            private boolean required;
            private final boolean interactive;
            private final boolean echo;
            private final String prompt;
            private final String splitRegex;
            private final String splitRegexSynopsisLabel;
            protected final ITypeInfo typeInfo;
            private final ITypeConverter<?>[] converters;
            private final Iterable<String> completionCandidates;
            private final IParameterConsumer parameterConsumer;
            private final IParameterPreprocessor preprocessor;
            private final String mapFallbackValue;
            private final String defaultValue;
            private Object initialValue;
            private final boolean hasInitialValue;
            private InitialValueState initialValueState;
            protected final IAnnotatedElement annotatedElement;
            private final IGetter getter;
            private final ISetter setter;
            private final IScope scope;
            private final ScopeType scopeType;
            private Range arity;
            private List<String> stringValues = new ArrayList<String>();
            private List<String> originalStringValues = new ArrayList<String>();
            protected String toString;
            private final List<Object> typedValues = new ArrayList<Object>();
            Map<Integer, Object> typedValueAtPosition = new TreeMap<Integer, Object>();

            private <T extends Builder<T>> ArgSpec(Builder<T> builder) {
                this.userObject = ((Builder)builder).userObject;
                this.description = ((Builder)builder).description == null ? new String[]{} : ((Builder)builder).description;
                this.descriptionKey = ((Builder)builder).descriptionKey;
                this.splitRegex = ((Builder)builder).splitRegex == null ? "" : ((Builder)builder).splitRegex;
                this.splitRegexSynopsisLabel = ((Builder)builder).splitRegexSynopsisLabel == null ? "" : ((Builder)builder).splitRegexSynopsisLabel;
                this.paramLabel = CommandLine.empty(((Builder)builder).paramLabel) ? "PARAM" : ((Builder)builder).paramLabel;
                this.hideParamSyntax = ((Builder)builder).hideParamSyntax;
                this.converters = ((Builder)builder).converters == null ? new ITypeConverter[]{} : ((Builder)builder).converters;
                this.parameterConsumer = ((Builder)builder).parameterConsumer;
                this.preprocessor = ((Builder)builder).preprocessor != null ? ((Builder)builder).preprocessor : new NoOpParameterPreprocessor();
                this.showDefaultValue = ((Builder)builder).showDefaultValue == null ? Help.Visibility.ON_DEMAND : ((Builder)builder).showDefaultValue;
                this.hidden = ((Builder)builder).hidden;
                this.inherited = ((Builder)builder).inherited;
                this.root = ((Builder)builder).root == null && ScopeType.INHERIT.equals((Object)((Builder)builder).scopeType) ? this : ((Builder)builder).root;
                this.interactive = ((Builder)builder).interactive;
                this.echo = ((Builder)builder).echo;
                this.prompt = ((Builder)builder).prompt;
                this.initialValue = ((Builder)builder).initialValue;
                this.hasInitialValue = ((Builder)builder).hasInitialValue;
                this.initialValueState = ((Builder)builder).initialValueState;
                this.annotatedElement = ((Builder)builder).annotatedElement;
                this.defaultValue = NO_DEFAULT_VALUE.equals(((Builder)builder).defaultValue) ? null : ((Builder)builder).defaultValue;
                this.required = ((Builder)builder).required;
                this.toString = ((Builder)builder).toString;
                this.getter = ((Builder)builder).getter;
                this.setter = ((Builder)builder).setter;
                this.scope = ((Builder)builder).scope;
                this.scopeType = ((Builder)builder).scopeType;
                this.mapFallbackValue = ((Builder)builder).mapFallbackValue;
                Range tempArity = ((Builder)builder).arity;
                if (tempArity == null) {
                    tempArity = this.interactive ? Range.valueOf("0") : (this.isOption() ? (((Builder)builder).type == null || CommandLine.isBoolean(((Builder)builder).type) ? Range.valueOf("0") : Range.valueOf("1")) : Range.valueOf("1"));
                    tempArity = tempArity.unspecified(true);
                }
                this.arity = tempArity;
                this.typeInfo = ((Builder)builder).typeInfo == null ? RuntimeTypeInfo.create(((Builder)builder).type, ((Builder)builder).auxiliaryTypes, Collections.<String>emptyList(), this.arity, this.isOption() ? Boolean.TYPE : String.class, this.interactive) : ((Builder)builder).typeInfo;
                if (((Builder)builder).completionCandidates == null && this.typeInfo.isEnum()) {
                    ArrayList<String> list = new ArrayList<String>();
                    for (String c : this.typeInfo.getEnumConstantNames()) {
                        list.add(c.toString());
                    }
                    this.completionCandidates = Collections.unmodifiableList(list);
                } else {
                    this.completionCandidates = ((Builder)builder).completionCandidates;
                }
                if (this.interactive && !this.arity.isValidForInteractiveArgs()) {
                    throw new InitializationException("Interactive options and positional parameters are only supported for arity=0 and arity=0..1; not for arity=" + this.arity);
                }
                if (!CommandLine.empty(this.splitRegex) && !this.typeInfo.isMultiValue() && System.getProperty("picocli.ignore.invalid.split") == null) {
                    throw new InitializationException("Only multi-value options and positional parameters should have a split regex (this check can be disabled by setting system property 'picocli.ignore.invalid.split')");
                }
            }

            void applyInitialValue(Tracer tracer) {
                if (this.hasInitialValue()) {
                    try {
                        this.setter().set(this.initialValue());
                        tracer.debug("Set initial value for %s of type %s to %s.%n", this, this.type(), String.valueOf(this.initialValue()));
                    }
                    catch (Exception ex) {
                        tracer.warn("Could not set initial value for %s of type %s to %s: %s%n", this, this.type(), String.valueOf(this.initialValue()), ex);
                    }
                } else {
                    tracer.debug("Initial value not available for %s%n", this);
                }
            }

            public boolean required() {
                return this.required && this.defaultValue() == null && this.defaultValueFromProvider() == null;
            }

            public boolean interactive() {
                return this.interactive;
            }

            public boolean echo() {
                return this.echo;
            }

            public String prompt() {
                return this.prompt;
            }

            public String[] description() {
                String[] result = (String[])this.description.clone();
                if (this.messages() != null) {
                    String[] newValue = this.messages().getStringArray(this.descriptionKey(), null);
                    if (newValue == null) {
                        for (String name : this.getAdditionalDescriptionKeys()) {
                            newValue = this.messages().getStringArray(name, null);
                            if (newValue == null) continue;
                            result = newValue;
                            break;
                        }
                    } else {
                        result = newValue;
                    }
                }
                if (this.commandSpec == null || this.commandSpec.interpolateVariables()) {
                    result = this.expandVariables(result);
                }
                return result;
            }

            protected abstract Collection<String> getAdditionalDescriptionKeys();

            public String descriptionKey() {
                return this.interpolate(this.descriptionKey);
            }

            private String[] expandVariables(String[] desc) {
                Iterable<String> iter;
                if (desc.length == 0) {
                    return desc;
                }
                StringBuilder candidates = new StringBuilder();
                boolean isCompletionCandidatesUsed = false;
                for (String s : desc) {
                    if (!s.contains(DESCRIPTION_VARIABLE_COMPLETION_CANDIDATES)) continue;
                    isCompletionCandidatesUsed = true;
                    break;
                }
                if (isCompletionCandidatesUsed && (iter = this.completionCandidates()) != null) {
                    for (String c : iter) {
                        if (candidates.length() > 0) {
                            candidates.append(", ");
                        }
                        candidates.append(c);
                    }
                }
                String defaultValueString = this.defaultValueString(false);
                String fallbackValueString = this.isOption() ? ((OptionSpec)this).fallbackValue : "";
                String mapFallbackValueString = String.valueOf(this.mapFallbackValue);
                String[] result = new String[desc.length];
                for (int i = 0; i < desc.length; ++i) {
                    result[i] = CommandLine.format(desc[i].replace(DESCRIPTION_VARIABLE_DEFAULT_VALUE, defaultValueString.replace("%", "%%")).replace(DESCRIPTION_VARIABLE_FALLBACK_VALUE, fallbackValueString.replace("%", "%%")).replace(DESCRIPTION_VARIABLE_MAP_FALLBACK_VALUE, mapFallbackValueString.replace("%", "%%")).replace(DESCRIPTION_VARIABLE_COMPLETION_CANDIDATES, candidates.toString()), new Object[0]);
                }
                return this.interpolate(result);
            }

            @Deprecated
            public String[] renderedDescription() {
                return this.description();
            }

            public Range arity() {
                return this.arity;
            }

            public String paramLabel() {
                return this.interpolate(this.paramLabel);
            }

            public boolean hideParamSyntax() {
                return this.hideParamSyntax;
            }

            public Class<?>[] auxiliaryTypes() {
                return this.typeInfo.getAuxiliaryTypes();
            }

            public ITypeConverter<?>[] converters() {
                return (ITypeConverter[])this.converters.clone();
            }

            public String splitRegex() {
                return this.interpolate(this.splitRegex);
            }

            public String splitRegexSynopsisLabel() {
                return this.interpolate(this.splitRegexSynopsisLabel);
            }

            public boolean hidden() {
                return this.hidden;
            }

            public boolean inherited() {
                return this.inherited;
            }

            public ArgSpec root() {
                return this.root;
            }

            public Class<?> type() {
                return this.typeInfo.getType();
            }

            public ITypeInfo typeInfo() {
                return this.typeInfo;
            }

            public Object userObject() {
                return this.userObject;
            }

            public String mapFallbackValue() {
                String result = this.interpolate(this.mapFallbackValue);
                return NULL_VALUE.equals(result) ? null : result;
            }

            public String defaultValue() {
                return this.interpolate(this.defaultValue);
            }

            public Object initialValue() {
                if (this.initialValueState == InitialValueState.POSTPONED && this.annotatedElement != null) {
                    try {
                        this.initialValue = this.annotatedElement.getter().get();
                        this.initialValueState = InitialValueState.CACHED;
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                return this.initialValue;
            }

            public boolean hasInitialValue() {
                return this.hasInitialValue || this.initialValueState == InitialValueState.CACHED || this.initialValueState == InitialValueState.POSTPONED;
            }

            public Help.Visibility showDefaultValue() {
                return this.showDefaultValue;
            }

            public String defaultValueString() {
                return this.defaultValueString(false);
            }

            public String defaultValueString(boolean interpolateVariables) {
                Object value = this.calcDefaultValue(interpolateVariables);
                if (value != null && value.getClass().isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < Array.getLength(value); ++i) {
                        sb.append(i > 0 ? ", " : "").append(Array.get(value, i));
                    }
                    return sb.insert(0, "[").append("]").toString();
                }
                return String.valueOf(value);
            }

            private Object calcDefaultValue(boolean interpolate) {
                String result = this.defaultValueFromProvider();
                if (result == null) {
                    result = interpolate ? this.defaultValue() : this.defaultValue;
                }
                return result == null ? this.initialValue() : result;
            }

            private String defaultValueFromProvider() {
                if (this.commandSpec == null) {
                    return null;
                }
                String fromProvider = null;
                IDefaultValueProvider defaultValueProvider = null;
                try {
                    defaultValueProvider = this.commandSpec.defaultValueProvider();
                    fromProvider = defaultValueProvider == null ? null : defaultValueProvider.defaultValue(this);
                }
                catch (Exception ex) {
                    new Tracer().info("Error getting default value for %s from %s: %s%n", this, defaultValueProvider, ex);
                }
                return fromProvider;
            }

            public Iterable<String> completionCandidates() {
                return this.completionCandidates;
            }

            public IParameterConsumer parameterConsumer() {
                return this.parameterConsumer;
            }

            public IParameterPreprocessor preprocessor() {
                return this.preprocessor;
            }

            public IGetter getter() {
                return this.getter;
            }

            public ISetter setter() {
                return this.setter;
            }

            public IScope scope() {
                return this.scope;
            }

            public ScopeType scopeType() {
                return this.scopeType;
            }

            public <T> T getValue() throws PicocliException {
                try {
                    return this.getter.get();
                }
                catch (PicocliException ex) {
                    throw ex;
                }
                catch (Exception ex) {
                    throw new PicocliException("Could not get value for " + this + ": " + ex, ex);
                }
            }

            public <T> T setValue(T newValue) throws PicocliException {
                try {
                    return this.setter.set(newValue);
                }
                catch (PicocliException ex) {
                    throw ex;
                }
                catch (Exception ex) {
                    throw new PicocliException("Could not set value (" + newValue + ") for " + this + ": " + ex, ex);
                }
            }

            @Deprecated
            public <T> T setValue(T newValue, CommandLine commandLine) throws PicocliException {
                return this.setValue(newValue);
            }

            public boolean isMultiValue() {
                return this.typeInfo.isMultiValue();
            }

            public abstract boolean isOption();

            public abstract boolean isPositional();

            public ArgGroupSpec group() {
                return this.group;
            }

            public CommandSpec command() {
                return this.commandSpec;
            }

            public List<String> stringValues() {
                return Collections.unmodifiableList(this.stringValues);
            }

            public List<Object> typedValues() {
                return Collections.unmodifiableList(this.typedValues);
            }

            protected void resetStringValues() {
                this.stringValues = new ArrayList<String>();
            }

            public List<String> originalStringValues() {
                return Collections.unmodifiableList(this.originalStringValues);
            }

            protected void resetOriginalStringValues() {
                this.originalStringValues = new ArrayList<String>();
            }

            protected boolean internalShowDefaultValue(boolean usageHelpShowDefaults) {
                if (this.showDefaultValue() == Help.Visibility.ALWAYS) {
                    return true;
                }
                if (this.showDefaultValue() == Help.Visibility.NEVER) {
                    return false;
                }
                if (this.initialValue() == null && this.defaultValue() == null && this.defaultValueFromProvider() == null) {
                    return false;
                }
                return usageHelpShowDefaults && !CommandLine.isBoolean(this.type());
            }

            public Messages messages() {
                return this.messages;
            }

            public ArgSpec messages(Messages msgs) {
                this.messages = msgs;
                return this;
            }

            public String toString() {
                return this.toString;
            }

            private String scopeString() {
                try {
                    Object obj = this.scope.get();
                    if (obj == null) {
                        return "<no user object>";
                    }
                    return obj.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(obj));
                }
                catch (Exception ex) {
                    return "?: " + ex.toString();
                }
            }

            String[] splitValue(String value, ParserSpec parser, Range arity, int consumed) {
                int limit;
                if (this.splitRegex().length() == 0) {
                    return new String[]{value};
                }
                int n = limit = parser.limitSplit() ? Math.max(arity.max - consumed, 0) : 0;
                if (parser.splitQuotedStrings()) {
                    return this.debug(value.split(this.splitRegex(), limit), "Split (ignoring quotes)", value);
                }
                return this.debug(ArgSpec.splitRespectingQuotedStrings(value, limit, parser, this, this.splitRegex()), "Split", value);
            }

            private String[] debug(String[] result, String msg, String value) {
                Tracer t = new Tracer();
                if (t.isDebug()) {
                    t.debug("%s with regex '%s' resulted in %s parts: %s%n", msg, this.splitRegex(), result.length, Arrays.asList(result));
                }
                return result;
            }

            private static String[] splitRespectingQuotedStrings(String value, int limit, ParserSpec parser, ArgSpec argSpec, String splitRegex) {
                int i;
                int ch;
                LinkedList<String> quotedValues = new LinkedList<String>();
                StringBuilder splittable = new StringBuilder();
                StringBuilder temp = new StringBuilder();
                StringBuilder current = splittable;
                boolean escaping = false;
                boolean inQuote = false;
                block4: for (i = 0; i < value.length(); i += Character.charCount(ch)) {
                    ch = value.codePointAt(i);
                    switch (ch) {
                        case 92: {
                            escaping = !escaping;
                            break;
                        }
                        case 34: {
                            if (!escaping) {
                                inQuote = !inQuote;
                                StringBuilder stringBuilder = current = inQuote ? temp : splittable;
                                if (inQuote) {
                                    splittable.appendCodePoint(ch);
                                    continue block4;
                                }
                                quotedValues.add(temp.toString());
                                temp.setLength(0);
                            }
                            escaping = false;
                            break;
                        }
                        default: {
                            escaping = false;
                        }
                    }
                    current.appendCodePoint(ch);
                }
                if (temp.length() > 0) {
                    new Tracer().warn("Unbalanced quotes in [%s] for %s (value=%s)%n", temp, argSpec, value);
                    quotedValues.add(temp.toString());
                    temp.setLength(0);
                }
                String[] result = splittable.toString().split(splitRegex, limit);
                for (i = 0; i < result.length; ++i) {
                    result[i] = ArgSpec.restoreQuotedValues(result[i], quotedValues, parser);
                }
                if (!quotedValues.isEmpty()) {
                    new Tracer().warn("Unable to respect quotes while splitting value %s for %s (unprocessed remainder: %s)%n", value, argSpec, quotedValues);
                    return value.split(splitRegex, limit);
                }
                return result;
            }

            private static String restoreQuotedValues(String part, Queue<String> quotedValues, ParserSpec parser) {
                int ch;
                StringBuilder result = new StringBuilder();
                boolean escaping = false;
                boolean inQuote = false;
                boolean skip = false;
                for (int i = 0; i < part.length(); i += Character.charCount(ch)) {
                    ch = part.codePointAt(i);
                    switch (ch) {
                        case 92: {
                            escaping = !escaping;
                            break;
                        }
                        case 34: {
                            if (escaping) break;
                            boolean bl = inQuote = !inQuote;
                            if (inQuote) break;
                            result.append(quotedValues.remove());
                            break;
                        }
                        default: {
                            escaping = false;
                        }
                    }
                    if (!skip) {
                        result.appendCodePoint(ch);
                    }
                    skip = false;
                }
                return parser.trimQuotes() ? CommandLine.smartUnquote(result.toString()) : result.toString();
            }

            protected boolean equalsImpl(ArgSpec other) {
                return Assert.equals(this.defaultValue, other.defaultValue) && Assert.equals(this.mapFallbackValue, other.mapFallbackValue) && Assert.equals(this.arity, other.arity) && Assert.equals(this.hidden, other.hidden) && Assert.equals(this.inherited, other.inherited) && Assert.equals(this.paramLabel, other.paramLabel) && Assert.equals(this.hideParamSyntax, other.hideParamSyntax) && Assert.equals(this.required, other.required) && Assert.equals(this.splitRegex, other.splitRegex) && Assert.equals(this.splitRegexSynopsisLabel, other.splitRegexSynopsisLabel) && Arrays.equals(this.description, other.description) && Assert.equals(this.descriptionKey, other.descriptionKey) && Assert.equals(this.parameterConsumer, other.parameterConsumer) && Assert.equals(this.preprocessor, other.preprocessor) && this.typeInfo.equals(other.typeInfo) && this.scopeType.equals((Object)other.scopeType);
            }

            protected int hashCodeImpl() {
                return 17 + 37 * Assert.hashCode(this.defaultValue) + 37 * Assert.hashCode(this.mapFallbackValue) + 37 * Assert.hashCode(this.arity) + 37 * Assert.hashCode(this.hidden) + 37 * Assert.hashCode(this.inherited) + 37 * Assert.hashCode(this.paramLabel) + 37 * Assert.hashCode(this.hideParamSyntax) + 37 * Assert.hashCode(this.required) + 37 * Assert.hashCode(this.splitRegex) + 37 * Assert.hashCode(this.splitRegexSynopsisLabel) + 37 * Arrays.hashCode(this.description) + 37 * Assert.hashCode(this.descriptionKey) + 37 * Assert.hashCode(this.parameterConsumer) + 37 * Assert.hashCode(this.preprocessor) + 37 * this.typeInfo.hashCode() + 37 * this.scopeType.hashCode();
            }

            private static String describeTypes(Collection<ArgSpec> args) {
                if (args.isEmpty()) {
                    return "";
                }
                int optionCount = 0;
                int paramCount = 0;
                for (ArgSpec arg : args) {
                    if (arg.isOption()) {
                        ++optionCount;
                        continue;
                    }
                    ++paramCount;
                }
                if (optionCount == 0) {
                    return paramCount == 1 ? "parameter" : "parameters";
                }
                if (paramCount == 0) {
                    return optionCount == 1 ? "option" : "options";
                }
                return "options and parameters";
            }

            private static String describe(Collection<ArgSpec> args) {
                return ArgSpec.describe(args, ", ", "=", "", "");
            }

            private static String describe(Collection<ArgSpec> args, String separator, String optionParamSeparator, String openingQuote, String closingQuote) {
                StringBuilder sb = new StringBuilder();
                for (ArgSpec arg : args) {
                    if (sb.length() > 0) {
                        sb.append(separator);
                    }
                    if (arg.isPositional()) {
                        sb.append(openingQuote).append(arg.paramLabel()).append(closingQuote);
                        continue;
                    }
                    sb.append(openingQuote).append(((OptionSpec)arg).longestName());
                    if (arg.arity().min() > 0) {
                        sb.append(optionParamSeparator).append(arg.paramLabel());
                    }
                    sb.append(closingQuote);
                }
                return sb.toString();
            }

            private static String describe(ArgSpec argSpec, String separator) {
                return ArgSpec.describe(argSpec, separator, argSpec.paramLabel());
            }

            private static String describe(ArgSpec argSpec, String separator, String value) {
                String prefix = argSpec.isOption() ? ((OptionSpec)argSpec).longestName() : "params[" + ((PositionalParamSpec)argSpec).index() + "]";
                return argSpec.arity().min > 0 ? prefix + separator + value : prefix;
            }

            String interpolate(String value) {
                return this.commandSpec == null ? value : this.commandSpec.interpolator.interpolate(value);
            }

            String[] interpolate(String[] values) {
                return this.commandSpec == null ? values : this.commandSpec.interpolator.interpolate(values);
            }

            /*
             * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
             */
            static abstract class Builder<T extends Builder<T>> {
                private Object userObject;
                private Range arity;
                private String[] description;
                private String descriptionKey;
                private boolean required;
                private boolean interactive;
                private boolean echo;
                private String prompt;
                private String paramLabel;
                private boolean hideParamSyntax;
                private String splitRegex;
                private String splitRegexSynopsisLabel;
                private boolean hidden;
                private ArgSpec root;
                private boolean inherited;
                private Class<?> type;
                private Class<?>[] auxiliaryTypes;
                private ITypeInfo typeInfo;
                private ITypeConverter<?>[] converters;
                private String defaultValue;
                private Object initialValue;
                private boolean hasInitialValue = true;
                private InitialValueState initialValueState = InitialValueState.UNAVAILABLE;
                private Help.Visibility showDefaultValue;
                private Iterable<String> completionCandidates;
                private IParameterConsumer parameterConsumer;
                private IParameterPreprocessor preprocessor;
                private String toString;
                private IGetter getter = new ObjectBinding();
                private ISetter setter = (ISetter)((Object)this.getter);
                private IScope scope = new ObjectScope(null);
                private ScopeType scopeType = ScopeType.LOCAL;
                private IAnnotatedElement annotatedElement;
                private String mapFallbackValue = "__unspecified__";

                Builder() {
                }

                Builder(ArgSpec original) {
                    this.userObject = original.userObject;
                    this.arity = original.arity;
                    this.description = original.description;
                    this.descriptionKey = original.descriptionKey;
                    this.required = original.required;
                    this.interactive = original.interactive;
                    this.echo = original.echo;
                    this.prompt = original.prompt;
                    this.paramLabel = original.paramLabel;
                    this.hideParamSyntax = original.hideParamSyntax;
                    this.splitRegex = original.splitRegex;
                    this.splitRegexSynopsisLabel = original.splitRegexSynopsisLabel;
                    this.hidden = original.hidden;
                    this.inherited = original.inherited;
                    this.root = original.root;
                    this.setTypeInfo(original.typeInfo);
                    this.converters = original.converters;
                    this.defaultValue = original.defaultValue;
                    this.annotatedElement = original.annotatedElement;
                    this.initialValue = original.initialValue;
                    this.initialValueState = original.initialValueState;
                    this.hasInitialValue = original.hasInitialValue;
                    this.showDefaultValue = original.showDefaultValue;
                    this.completionCandidates = original.completionCandidates;
                    this.parameterConsumer = original.parameterConsumer;
                    this.preprocessor = original.preprocessor;
                    this.toString = original.toString;
                    this.getter = original.getter;
                    this.setter = original.setter;
                    this.scope = original.scope;
                    this.scopeType = original.scopeType;
                    this.mapFallbackValue = original.mapFallbackValue;
                }

                Builder(IAnnotatedElement annotatedElement) {
                    this.annotatedElement = annotatedElement;
                    this.userObject = annotatedElement.userObject();
                    this.setTypeInfo(annotatedElement.getTypeInfo());
                    this.toString = annotatedElement.getToString();
                    this.getter = annotatedElement.getter();
                    this.setter = annotatedElement.setter();
                    this.scope = annotatedElement.scope();
                    this.hasInitialValue = annotatedElement.hasInitialValue();
                    if (annotatedElement instanceof IExtensible) {
                        this.initialValueState = ((IExtensible)((Object)annotatedElement)).getExtension(InitialValueState.class);
                    }
                }

                Builder(Option option, IAnnotatedElement annotatedElement, IFactory factory) {
                    this(annotatedElement);
                    this.arity = Range.optionArity(annotatedElement);
                    this.required = option.required();
                    this.paramLabel = Builder.inferLabel(option.paramLabel(), annotatedElement.getName(), annotatedElement.getTypeInfo());
                    this.hideParamSyntax = option.hideParamSyntax();
                    this.interactive = option.interactive();
                    this.echo = option.echo();
                    this.prompt = option.prompt();
                    this.description = option.description();
                    this.descriptionKey = option.descriptionKey();
                    this.splitRegex = option.split();
                    this.splitRegexSynopsisLabel = option.splitSynopsisLabel();
                    this.hidden = option.hidden();
                    this.defaultValue = ArgSpec.NULL_VALUE.equals(option.defaultValue()) ? null : option.defaultValue();
                    this.mapFallbackValue = ArgSpec.NULL_VALUE.equals(option.mapFallbackValue()) ? null : option.mapFallbackValue();
                    this.showDefaultValue = option.showDefaultValue();
                    this.scopeType = option.scope();
                    this.inherited = false;
                    if (factory != null) {
                        this.converters = DefaultFactory.createConverter(factory, option.converter());
                        if (!NoCompletionCandidates.class.equals(option.completionCandidates())) {
                            this.completionCandidates = DefaultFactory.createCompletionCandidates(factory, option.completionCandidates());
                        }
                        if (!NullParameterConsumer.class.equals(option.parameterConsumer())) {
                            this.parameterConsumer = DefaultFactory.createParameterConsumer(factory, option.parameterConsumer());
                        }
                        if (!NoOpParameterPreprocessor.class.equals(option.preprocessor())) {
                            this.preprocessor = DefaultFactory.create(factory, option.preprocessor());
                        }
                    }
                }

                Builder(Parameters parameters, IAnnotatedElement annotatedElement, IFactory factory) {
                    this(annotatedElement);
                    this.arity = Range.parameterArity(annotatedElement);
                    boolean bl = this.required = this.arity.min > 0;
                    if (parameters == null) {
                        this.paramLabel = Builder.inferLabel(null, annotatedElement.getName(), annotatedElement.getTypeInfo());
                    } else {
                        this.paramLabel = Builder.inferLabel(parameters.paramLabel(), annotatedElement.getName(), annotatedElement.getTypeInfo());
                        this.hideParamSyntax = parameters.hideParamSyntax();
                        this.interactive = parameters.interactive();
                        this.echo = parameters.echo();
                        this.prompt = parameters.prompt();
                        this.description = parameters.description();
                        this.descriptionKey = parameters.descriptionKey();
                        this.splitRegex = parameters.split();
                        this.splitRegexSynopsisLabel = parameters.splitSynopsisLabel();
                        this.hidden = parameters.hidden();
                        this.defaultValue = ArgSpec.NULL_VALUE.equals(parameters.defaultValue()) ? null : parameters.defaultValue();
                        this.mapFallbackValue = ArgSpec.NULL_VALUE.equals(parameters.mapFallbackValue()) ? null : parameters.mapFallbackValue();
                        this.showDefaultValue = parameters.showDefaultValue();
                        this.scopeType = parameters.scope();
                        this.inherited = false;
                        if (factory != null) {
                            this.converters = DefaultFactory.createConverter(factory, parameters.converter());
                            if (!NoCompletionCandidates.class.equals(parameters.completionCandidates())) {
                                this.completionCandidates = DefaultFactory.createCompletionCandidates(factory, parameters.completionCandidates());
                            }
                            if (!NullParameterConsumer.class.equals(parameters.parameterConsumer())) {
                                this.parameterConsumer = DefaultFactory.createParameterConsumer(factory, parameters.parameterConsumer());
                            }
                            if (!NoOpParameterPreprocessor.class.equals(parameters.preprocessor())) {
                                this.preprocessor = DefaultFactory.create(factory, parameters.preprocessor());
                            }
                        }
                    }
                }

                private static String inferLabel(String label, String fieldName, ITypeInfo typeInfo) {
                    if (!CommandLine.empty(label)) {
                        return label.trim();
                    }
                    String name = fieldName;
                    if (typeInfo.isMap()) {
                        List<ITypeInfo> aux = typeInfo.getAuxiliaryTypeInfos();
                        name = aux.size() < 2 || aux.get(0) == null || aux.get(1) == null ? "String=String" : aux.get(0).getClassSimpleName() + "=" + aux.get(1).getClassSimpleName();
                    }
                    return "<" + name + ">";
                }

                public abstract ArgSpec build();

                protected abstract T self();

                public boolean required() {
                    return this.required;
                }

                public boolean interactive() {
                    return this.interactive;
                }

                public boolean echo() {
                    return this.echo;
                }

                public String prompt() {
                    return this.prompt;
                }

                public String[] description() {
                    return this.description;
                }

                public String descriptionKey() {
                    return this.descriptionKey;
                }

                public Range arity() {
                    return this.arity;
                }

                public String paramLabel() {
                    return this.paramLabel;
                }

                public boolean hideParamSyntax() {
                    return this.hideParamSyntax;
                }

                public Class<?>[] auxiliaryTypes() {
                    return this.auxiliaryTypes;
                }

                public ITypeConverter<?>[] converters() {
                    return this.converters;
                }

                public String splitRegex() {
                    return this.splitRegex;
                }

                public String splitRegexSynopsisLabel() {
                    return this.splitRegexSynopsisLabel;
                }

                public boolean hidden() {
                    return this.hidden;
                }

                public boolean inherited() {
                    return this.inherited;
                }

                public ArgSpec root() {
                    return this.root;
                }

                public Class<?> type() {
                    return this.type;
                }

                public ITypeInfo typeInfo() {
                    return this.typeInfo;
                }

                public Object userObject() {
                    return this.userObject;
                }

                public String mapFallbackValue() {
                    return this.mapFallbackValue;
                }

                public String defaultValue() {
                    return this.defaultValue;
                }

                public Object initialValue() {
                    return this.initialValue;
                }

                public boolean hasInitialValue() {
                    return this.hasInitialValue;
                }

                public Help.Visibility showDefaultValue() {
                    return this.showDefaultValue;
                }

                public Iterable<String> completionCandidates() {
                    return this.completionCandidates;
                }

                public IParameterConsumer parameterConsumer() {
                    return this.parameterConsumer;
                }

                public IParameterPreprocessor preprocessor() {
                    return this.preprocessor;
                }

                public IGetter getter() {
                    return this.getter;
                }

                public ISetter setter() {
                    return this.setter;
                }

                public IScope scope() {
                    return this.scope;
                }

                public ScopeType scopeType() {
                    return this.scopeType;
                }

                public String toString() {
                    return this.toString;
                }

                public T required(boolean required) {
                    this.required = required;
                    return this.self();
                }

                public T interactive(boolean interactive) {
                    this.interactive = interactive;
                    return this.self();
                }

                public T echo(boolean echo) {
                    this.echo = echo;
                    return this.self();
                }

                public T prompt(String prompt) {
                    this.prompt = prompt;
                    return this.self();
                }

                public T description(String ... description) {
                    this.description = (String[])Assert.notNull(description, "description").clone();
                    return this.self();
                }

                public T descriptionKey(String descriptionKey) {
                    this.descriptionKey = descriptionKey;
                    return this.self();
                }

                public T arity(String range) {
                    return this.arity(Range.valueOf(range));
                }

                public T arity(Range arity) {
                    this.arity = Assert.notNull(arity, "arity");
                    return this.self();
                }

                public T paramLabel(String paramLabel) {
                    this.paramLabel = Assert.notNull(paramLabel, "paramLabel");
                    return this.self();
                }

                public T hideParamSyntax(boolean hideParamSyntax) {
                    this.hideParamSyntax = hideParamSyntax;
                    return this.self();
                }

                public T auxiliaryTypes(Class<?> ... types) {
                    this.auxiliaryTypes = (Class[])Assert.notNull(types, "types").clone();
                    return this.self();
                }

                public T converters(ITypeConverter<?> ... cs) {
                    this.converters = (ITypeConverter[])Assert.notNull(cs, "type converters").clone();
                    return this.self();
                }

                public T splitRegex(String splitRegex) {
                    this.splitRegex = Assert.notNull(splitRegex, "splitRegex");
                    return this.self();
                }

                public T splitRegexSynopsisLabel(String splitRegexSynopsisLabel) {
                    this.splitRegexSynopsisLabel = Assert.notNull(splitRegexSynopsisLabel, "splitRegexSynopsisLabel");
                    return this.self();
                }

                public T showDefaultValue(Help.Visibility visibility) {
                    this.showDefaultValue = Assert.notNull(visibility, "visibility");
                    return this.self();
                }

                public T completionCandidates(Iterable<String> completionCandidates) {
                    this.completionCandidates = completionCandidates;
                    return this.self();
                }

                public T parameterConsumer(IParameterConsumer parameterConsumer) {
                    this.parameterConsumer = parameterConsumer;
                    return this.self();
                }

                public T preprocessor(IParameterPreprocessor preprocessor) {
                    this.preprocessor = preprocessor;
                    return this.self();
                }

                public T hidden(boolean hidden) {
                    this.hidden = hidden;
                    return this.self();
                }

                public T inherited(boolean inherited) {
                    this.inherited = inherited;
                    return this.self();
                }

                public T root(ArgSpec root) {
                    this.root = root;
                    return this.self();
                }

                public T type(Class<?> propertyType) {
                    this.type = Assert.notNull(propertyType, "type");
                    return this.self();
                }

                public T typeInfo(ITypeInfo typeInfo) {
                    this.setTypeInfo(Assert.notNull(typeInfo, "typeInfo"));
                    return this.self();
                }

                private void setTypeInfo(ITypeInfo newValue) {
                    this.typeInfo = newValue;
                    if (this.typeInfo != null) {
                        this.type = this.typeInfo.getType();
                        this.auxiliaryTypes = this.typeInfo.getAuxiliaryTypes();
                    }
                }

                public T userObject(Object userObject) {
                    this.userObject = Assert.notNull(userObject, "userObject");
                    return this.self();
                }

                public Builder mapFallbackValue(String fallbackValue) {
                    this.mapFallbackValue = fallbackValue;
                    return this.self();
                }

                public T defaultValue(String defaultValue) {
                    this.defaultValue = defaultValue;
                    return this.self();
                }

                public T initialValue(Object initialValue) {
                    this.initialValue = initialValue;
                    return this.self();
                }

                public T hasInitialValue(boolean hasInitialValue) {
                    this.hasInitialValue = hasInitialValue;
                    return this.self();
                }

                public T getter(IGetter getter) {
                    this.getter = getter;
                    return this.self();
                }

                public T setter(ISetter setter) {
                    this.setter = setter;
                    return this.self();
                }

                public T scope(IScope scope) {
                    this.scope = scope;
                    return this.self();
                }

                public T scopeType(ScopeType scopeType) {
                    this.scopeType = scopeType;
                    return this.self();
                }

                public T withToString(String toString) {
                    this.toString = toString;
                    return this.self();
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static enum InitialValueState {
            CACHED,
            POSTPONED,
            UNAVAILABLE;

        }

        public static class ParserSpec {
            static final String DEFAULT_SEPARATOR = "=";
            static final String DEFAULT_END_OF_OPTIONS_DELIMITER = "--";
            private boolean abbreviatedOptionsAllowed = false;
            private boolean abbreviatedSubcommandsAllowed = false;
            private boolean aritySatisfiedByAttachedOptionParam = false;
            private Character atFileCommentChar = Character.valueOf('#');
            private boolean caseInsensitiveEnumValuesAllowed = false;
            private boolean collectErrors = false;
            private String endOfOptionsDelimiter = "--";
            private boolean expandAtFiles = true;
            private boolean limitSplit = false;
            private boolean overwrittenOptionsAllowed = false;
            private boolean posixClusteredShortOptionsAllowed = true;
            private String separator;
            private boolean splitQuotedStrings = false;
            private boolean stopAtPositional = false;
            private boolean stopAtUnmatched = false;
            private boolean toggleBooleanFlags = false;
            private boolean trimQuotes = this.shouldTrimQuotes();
            private boolean unmatchedArgumentsAllowed = false;
            private boolean unmatchedOptionsAllowedAsOptionParameters = true;
            private boolean unmatchedOptionsArePositionalParams = false;
            private boolean useSimplifiedAtFiles = false;

            public String separator() {
                return this.separator == null ? DEFAULT_SEPARATOR : this.separator;
            }

            public boolean stopAtUnmatched() {
                return this.stopAtUnmatched;
            }

            public boolean stopAtPositional() {
                return this.stopAtPositional;
            }

            public String endOfOptionsDelimiter() {
                return this.endOfOptionsDelimiter;
            }

            public boolean toggleBooleanFlags() {
                return this.toggleBooleanFlags;
            }

            public boolean overwrittenOptionsAllowed() {
                return this.overwrittenOptionsAllowed;
            }

            public boolean unmatchedArgumentsAllowed() {
                return this.unmatchedArgumentsAllowed;
            }

            public boolean abbreviatedSubcommandsAllowed() {
                return this.abbreviatedSubcommandsAllowed;
            }

            public boolean abbreviatedOptionsAllowed() {
                return this.abbreviatedOptionsAllowed;
            }

            public boolean expandAtFiles() {
                return this.expandAtFiles;
            }

            public Character atFileCommentChar() {
                return this.atFileCommentChar;
            }

            public boolean useSimplifiedAtFiles() {
                String value = System.getProperty("picocli.useSimplifiedAtFiles");
                if (value != null) {
                    return "".equals(value) || Boolean.parseBoolean(value);
                }
                return this.useSimplifiedAtFiles;
            }

            public boolean posixClusteredShortOptionsAllowed() {
                return this.posixClusteredShortOptionsAllowed;
            }

            public boolean caseInsensitiveEnumValuesAllowed() {
                return this.caseInsensitiveEnumValuesAllowed;
            }

            public boolean trimQuotes() {
                return this.trimQuotes;
            }

            public boolean splitQuotedStrings() {
                return this.splitQuotedStrings;
            }

            public boolean unmatchedOptionsArePositionalParams() {
                return this.unmatchedOptionsArePositionalParams;
            }

            public boolean unmatchedOptionsAllowedAsOptionParameters() {
                return this.unmatchedOptionsAllowedAsOptionParameters;
            }

            private boolean splitFirst() {
                return this.limitSplit();
            }

            public boolean limitSplit() {
                return this.limitSplit;
            }

            public boolean aritySatisfiedByAttachedOptionParam() {
                return this.aritySatisfiedByAttachedOptionParam;
            }

            public boolean collectErrors() {
                return this.collectErrors;
            }

            public ParserSpec separator(String separator) {
                this.separator = separator;
                return this;
            }

            public ParserSpec stopAtUnmatched(boolean stopAtUnmatched) {
                this.stopAtUnmatched = stopAtUnmatched;
                return this;
            }

            public ParserSpec stopAtPositional(boolean stopAtPositional) {
                this.stopAtPositional = stopAtPositional;
                return this;
            }

            public ParserSpec endOfOptionsDelimiter(String delimiter) {
                this.endOfOptionsDelimiter = Assert.notNull(delimiter, "end-of-options delimiter");
                return this;
            }

            public ParserSpec toggleBooleanFlags(boolean toggleBooleanFlags) {
                this.toggleBooleanFlags = toggleBooleanFlags;
                return this;
            }

            public ParserSpec overwrittenOptionsAllowed(boolean overwrittenOptionsAllowed) {
                this.overwrittenOptionsAllowed = overwrittenOptionsAllowed;
                return this;
            }

            public ParserSpec unmatchedArgumentsAllowed(boolean unmatchedArgumentsAllowed) {
                this.unmatchedArgumentsAllowed = unmatchedArgumentsAllowed;
                return this;
            }

            public ParserSpec abbreviatedSubcommandsAllowed(boolean abbreviatedSubcommandsAllowed) {
                this.abbreviatedSubcommandsAllowed = abbreviatedSubcommandsAllowed;
                return this;
            }

            public ParserSpec abbreviatedOptionsAllowed(boolean abbreviatedOptionsAllowed) {
                this.abbreviatedOptionsAllowed = abbreviatedOptionsAllowed;
                return this;
            }

            public ParserSpec expandAtFiles(boolean expandAtFiles) {
                this.expandAtFiles = expandAtFiles;
                return this;
            }

            public ParserSpec atFileCommentChar(Character atFileCommentChar) {
                this.atFileCommentChar = atFileCommentChar;
                return this;
            }

            public ParserSpec useSimplifiedAtFiles(boolean useSimplifiedAtFiles) {
                this.useSimplifiedAtFiles = useSimplifiedAtFiles;
                return this;
            }

            public ParserSpec posixClusteredShortOptionsAllowed(boolean posixClusteredShortOptionsAllowed) {
                this.posixClusteredShortOptionsAllowed = posixClusteredShortOptionsAllowed;
                return this;
            }

            public ParserSpec caseInsensitiveEnumValuesAllowed(boolean caseInsensitiveEnumValuesAllowed) {
                this.caseInsensitiveEnumValuesAllowed = caseInsensitiveEnumValuesAllowed;
                return this;
            }

            public ParserSpec trimQuotes(boolean trimQuotes) {
                this.trimQuotes = trimQuotes;
                return this;
            }

            public ParserSpec splitQuotedStrings(boolean splitQuotedStrings) {
                this.splitQuotedStrings = splitQuotedStrings;
                return this;
            }

            public ParserSpec unmatchedOptionsAllowedAsOptionParameters(boolean unmatchedOptionsAllowedAsOptionParameters) {
                this.unmatchedOptionsAllowedAsOptionParameters = unmatchedOptionsAllowedAsOptionParameters;
                return this;
            }

            public ParserSpec unmatchedOptionsArePositionalParams(boolean unmatchedOptionsArePositionalParams) {
                this.unmatchedOptionsArePositionalParams = unmatchedOptionsArePositionalParams;
                return this;
            }

            public ParserSpec collectErrors(boolean collectErrors) {
                this.collectErrors = collectErrors;
                return this;
            }

            public ParserSpec aritySatisfiedByAttachedOptionParam(boolean newValue) {
                this.aritySatisfiedByAttachedOptionParam = newValue;
                return this;
            }

            public ParserSpec limitSplit(boolean limitSplit) {
                this.limitSplit = limitSplit;
                return this;
            }

            private boolean shouldTrimQuotes() {
                String value = System.getProperty("picocli.trimQuotes");
                if ("".equals(value)) {
                    value = "true";
                }
                return Boolean.parseBoolean(value);
            }

            void initSeparator(String value) {
                if (Model.initializable(this.separator, value, DEFAULT_SEPARATOR)) {
                    this.separator = value;
                }
            }

            void updateSeparator(String value) {
                if (Model.isNonDefault(value, DEFAULT_SEPARATOR)) {
                    this.separator = value;
                }
            }

            public String toString() {
                return String.format("abbreviatedOptionsAllowed=%s, abbreviatedSubcommandsAllowed=%s, aritySatisfiedByAttachedOptionParam=%s, atFileCommentChar=%s, caseInsensitiveEnumValuesAllowed=%s, collectErrors=%s, endOfOptionsDelimiter=%s, expandAtFiles=%s, limitSplit=%s, overwrittenOptionsAllowed=%s, posixClusteredShortOptionsAllowed=%s, separator=%s, splitQuotedStrings=%s, stopAtPositional=%s, stopAtUnmatched=%s, toggleBooleanFlags=%s, trimQuotes=%s, unmatchedArgumentsAllowed=%s, unmatchedOptionsAllowedAsOptionParameters=%s, unmatchedOptionsArePositionalParams=%s, useSimplifiedAtFiles=%s", this.abbreviatedOptionsAllowed, this.abbreviatedSubcommandsAllowed, this.aritySatisfiedByAttachedOptionParam, this.atFileCommentChar, this.caseInsensitiveEnumValuesAllowed, this.collectErrors, this.endOfOptionsDelimiter, this.expandAtFiles, this.limitSplit, this.overwrittenOptionsAllowed, this.posixClusteredShortOptionsAllowed, this.separator, this.splitQuotedStrings, this.stopAtPositional, this.stopAtUnmatched, this.toggleBooleanFlags, this.trimQuotes, this.unmatchedArgumentsAllowed, this.unmatchedOptionsAllowedAsOptionParameters, this.unmatchedOptionsArePositionalParams, this.useSimplifiedAtFiles);
            }

            void initFrom(ParserSpec settings) {
                this.abbreviatedOptionsAllowed = settings.abbreviatedOptionsAllowed;
                this.abbreviatedSubcommandsAllowed = settings.abbreviatedSubcommandsAllowed;
                this.aritySatisfiedByAttachedOptionParam = settings.aritySatisfiedByAttachedOptionParam;
                this.atFileCommentChar = settings.atFileCommentChar;
                this.caseInsensitiveEnumValuesAllowed = settings.caseInsensitiveEnumValuesAllowed;
                this.collectErrors = settings.collectErrors;
                this.endOfOptionsDelimiter = settings.endOfOptionsDelimiter;
                this.expandAtFiles = settings.expandAtFiles;
                this.limitSplit = settings.limitSplit;
                this.overwrittenOptionsAllowed = settings.overwrittenOptionsAllowed;
                this.posixClusteredShortOptionsAllowed = settings.posixClusteredShortOptionsAllowed;
                this.separator = settings.separator;
                this.splitQuotedStrings = settings.splitQuotedStrings;
                this.stopAtPositional = settings.stopAtPositional;
                this.stopAtUnmatched = settings.stopAtUnmatched;
                this.toggleBooleanFlags = settings.toggleBooleanFlags;
                this.trimQuotes = settings.trimQuotes;
                this.unmatchedArgumentsAllowed = settings.unmatchedArgumentsAllowed;
                this.unmatchedOptionsAllowedAsOptionParameters = settings.unmatchedOptionsAllowedAsOptionParameters;
                this.unmatchedOptionsArePositionalParams = settings.unmatchedOptionsArePositionalParams;
                this.useSimplifiedAtFiles = settings.useSimplifiedAtFiles;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class UsageMessageSpec {
            public static final String SECTION_KEY_HEADER_HEADING = "headerHeading";
            public static final String SECTION_KEY_HEADER = "header";
            public static final String SECTION_KEY_SYNOPSIS_HEADING = "synopsisHeading";
            public static final String SECTION_KEY_SYNOPSIS = "synopsis";
            public static final String SECTION_KEY_DESCRIPTION_HEADING = "descriptionHeading";
            public static final String SECTION_KEY_DESCRIPTION = "description";
            public static final String SECTION_KEY_PARAMETER_LIST_HEADING = "parameterListHeading";
            public static final String SECTION_KEY_AT_FILE_PARAMETER = "atFileParameterList";
            public static final String SECTION_KEY_PARAMETER_LIST = "parameterList";
            public static final String SECTION_KEY_OPTION_LIST_HEADING = "optionListHeading";
            public static final String SECTION_KEY_OPTION_LIST = "optionList";
            public static final String SECTION_KEY_END_OF_OPTIONS = "endOfOptionsList";
            public static final String SECTION_KEY_COMMAND_LIST_HEADING = "commandListHeading";
            public static final String SECTION_KEY_COMMAND_LIST = "commandList";
            public static final String SECTION_KEY_EXIT_CODE_LIST_HEADING = "exitCodeListHeading";
            public static final String SECTION_KEY_EXIT_CODE_LIST = "exitCodeList";
            public static final String SECTION_KEY_FOOTER_HEADING = "footerHeading";
            public static final String SECTION_KEY_FOOTER = "footer";
            public static final int DEFAULT_USAGE_WIDTH = 80;
            private static final int MINIMUM_USAGE_WIDTH = 55;
            static final int DEFAULT_USAGE_LONG_OPTIONS_WIDTH = 20;
            private static final int DEFAULT_SYNOPSIS_INDENT = -1;
            private static final double DEFAULT_SYNOPSIS_AUTO_INDENT_THRESHOLD = 0.5;
            private static final double MAX_SYNOPSIS_AUTO_INDENT_THRESHOLD = 0.9;
            static final Boolean DEFAULT_USAGE_AUTO_WIDTH = Boolean.FALSE;
            static final String DEFAULT_SYNOPSIS_HEADING = "Usage: ";
            static final String DEFAULT_SYNOPSIS_SUBCOMMANDS = "[COMMAND]";
            static final String DEFAULT_COMMAND_LIST_HEADING = "Commands:%n";
            static final char DEFAULT_REQUIRED_OPTION_MARKER = ' ';
            static final Boolean DEFAULT_ABBREVIATE_SYNOPSIS = Boolean.FALSE;
            static final Boolean DEFAULT_SORT_OPTIONS = Boolean.TRUE;
            static final Boolean DEFAULT_SHOW_AT_FILE = Boolean.FALSE;
            static final Boolean DEFAULT_SHOW_END_OF_OPTIONS = Boolean.FALSE;
            static final Boolean DEFAULT_SHOW_DEFAULT_VALUES = Boolean.FALSE;
            static final Boolean DEFAULT_HIDDEN = Boolean.FALSE;
            static final Boolean DEFAULT_ADJUST_CJK = Boolean.TRUE;
            static final String DEFAULT_SINGLE_VALUE = "";
            static final String[] DEFAULT_MULTI_LINE = new String[0];
            private IHelpFactory helpFactory;
            private List<String> sectionKeys = Collections.unmodifiableList(Arrays.asList("headerHeading", "header", "synopsisHeading", "synopsis", "descriptionHeading", "description", "parameterListHeading", "atFileParameterList", "parameterList", "optionListHeading", "optionList", "endOfOptionsList", "commandListHeading", "commandList", "exitCodeListHeading", "exitCodeList", "footerHeading", "footer"));
            private Map<String, IHelpSectionRenderer> helpSectionRendererMap = this.createHelpSectionRendererMap();
            private String[] description;
            private String[] customSynopsis;
            private String[] header;
            private String[] footer;
            private Boolean abbreviateSynopsis;
            private Boolean sortOptions;
            private Boolean showDefaultValues;
            private Boolean showAtFileInUsageHelp;
            private Boolean showEndOfOptionsDelimiterInUsageHelp;
            private Boolean hidden;
            private Boolean autoWidth;
            private Character requiredOptionMarker;
            private String headerHeading;
            private String synopsisHeading;
            private String synopsisSubcommandLabel;
            private Double synopsisAutoIndentThreshold;
            private Integer synopsisIndent;
            private String descriptionHeading;
            private String parameterListHeading;
            private String optionListHeading;
            private String commandListHeading;
            private String footerHeading;
            private String exitCodeListHeading;
            private String[] exitCodeListStrings;
            private Map<String, String> exitCodeList;
            private Integer width;
            private Integer longOptionsMaxWidth;
            private Integer cachedTerminalWidth;
            private final Interpolator interpolator;
            private Messages messages;
            private Boolean adjustLineBreaksForWideCJKCharacters;

            public UsageMessageSpec() {
                this(null);
            }

            UsageMessageSpec(Interpolator interpolator) {
                this.interpolator = interpolator;
            }

            public UsageMessageSpec width(int newValue) {
                if (newValue < 55) {
                    throw new InitializationException("Invalid usage message width " + newValue + ". Minimum value is " + 55);
                }
                this.width = newValue;
                return this;
            }

            public UsageMessageSpec longOptionsMaxWidth(int newValue) {
                if (newValue < 20) {
                    throw new InitializationException("Invalid usage long options max width " + newValue + ". Minimum value is " + 20);
                }
                if (newValue > this.width() - 20) {
                    throw new InitializationException("Invalid usage long options max width " + newValue + ". Value must not exceed width(" + this.width() + ") - " + 20);
                }
                this.longOptionsMaxWidth = newValue;
                return this;
            }

            private int getSysPropertyWidthOrDefault(int defaultWidth, boolean detectTerminalSize) {
                if (detectTerminalSize) {
                    if (this.cachedTerminalWidth == null) {
                        this.cachedTerminalWidth = UsageMessageSpec.getTerminalWidth();
                    }
                    return this.cachedTerminalWidth < 0 ? defaultWidth : Math.max(this.cachedTerminalWidth, 55);
                }
                String userValue = System.getProperty("picocli.usage.width");
                if (userValue == null) {
                    return defaultWidth;
                }
                try {
                    int width = Integer.parseInt(userValue);
                    if (width < 55) {
                        new Tracer().warn("Invalid picocli.usage.width value %d. Using minimum usage width %d.%n", width, 55);
                        return 55;
                    }
                    return width;
                }
                catch (NumberFormatException ex) {
                    new Tracer().warn("Invalid picocli.usage.width value '%s'. Using usage width %d.%n", userValue, defaultWidth);
                    return defaultWidth;
                }
            }

            private static boolean shouldDetectTerminalSize(boolean autoWidthEnabledInApplication) {
                String userValue = System.getProperty("picocli.usage.width");
                boolean sysPropAutoWidth = Arrays.asList("AUTO", "TERM", "TERMINAL").contains(String.valueOf(userValue).toUpperCase(Locale.ENGLISH));
                return sysPropAutoWidth || autoWidthEnabledInApplication && !UsageMessageSpec.isNumeric(userValue);
            }

            private static boolean isNumeric(String userValue) {
                try {
                    Integer.parseInt(userValue);
                    return true;
                }
                catch (Exception any) {
                    return false;
                }
            }

            private static int getTerminalWidth() {
                return Help.Ansi.isTTY() || Help.Ansi.isPseudoTTY() ? UsageMessageSpec.detectTerminalWidth() : -1;
            }

            private static int detectTerminalWidth() {
                String[] stringArray;
                long start = System.nanoTime();
                final Tracer tracer = new Tracer();
                final AtomicInteger size = new AtomicInteger(-1);
                if (Help.Ansi.isWindows() && !Help.Ansi.isPseudoTTY()) {
                    String[] stringArray2 = new String[3];
                    stringArray2[0] = "cmd.exe";
                    stringArray2[1] = "/c";
                    stringArray = stringArray2;
                    stringArray2[2] = "mode con";
                } else if (Help.Ansi.isMac()) {
                    String[] stringArray3 = new String[2];
                    stringArray3[0] = "tput";
                    stringArray = stringArray3;
                    stringArray3[1] = "cols";
                } else {
                    String[] stringArray4 = new String[4];
                    stringArray4[0] = "stty";
                    stringArray4[1] = "-a";
                    stringArray4[2] = "-F";
                    stringArray = stringArray4;
                    stringArray4[3] = "/dev/tty";
                }
                final String[] cmd = stringArray;
                Thread t = new Thread(new Runnable(){

                    /*
                     * WARNING - Removed try catching itself - possible behaviour change.
                     */
                    public void run() {
                        block9: {
                            BufferedReader reader;
                            block8: {
                                Process proc = null;
                                reader = null;
                                try {
                                    String line;
                                    ProcessBuilder pb = new ProcessBuilder(cmd);
                                    tracer.debug("getTerminalWidth() executing command %s%n", pb.command());
                                    Class<?> redirectClass = Class.forName("java.lang.ProcessBuilder$Redirect");
                                    Object INHERIT = redirectClass.getField("INHERIT").get(null);
                                    Method redirectError = ProcessBuilder.class.getDeclaredMethod("redirectError", redirectClass);
                                    redirectError.invoke(pb, INHERIT);
                                    proc = pb.start();
                                    reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                                    String txt = UsageMessageSpec.DEFAULT_SINGLE_VALUE;
                                    while ((line = reader.readLine()) != null) {
                                        txt = txt + " " + line;
                                    }
                                    tracer.debug("getTerminalWidth() parsing output: %s%n", txt);
                                    Pattern pattern = Help.Ansi.isWindows() && !Help.Ansi.isPseudoTTY() ? Pattern.compile(".*?:\\s*(\\d+)\\D.*?:\\s*(\\d+)\\D.*", 32) : (Help.Ansi.isMac() ? Pattern.compile("(\\s*)(\\d+)\\s*") : Pattern.compile(".*olumns(:)?\\s+(\\d+)\\D.*", 32));
                                    Matcher matcher = pattern.matcher(txt);
                                    if (matcher.matches()) {
                                        size.set(Integer.parseInt(matcher.group(2)));
                                    }
                                    if (proc == null) break block8;
                                    proc.destroy();
                                }
                                catch (Exception ignored) {
                                    tracer.debug("getTerminalWidth() ERROR: %s%n", ignored);
                                    break block9;
                                }
                                finally {
                                    if (proc != null) {
                                        proc.destroy();
                                    }
                                    CommandLine.close(reader);
                                }
                            }
                            CommandLine.close(reader);
                        }
                    }
                });
                t.start();
                long now = System.currentTimeMillis();
                while (size.intValue() < 0) {
                    try {
                        Thread.sleep(25L);
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                    if (System.currentTimeMillis() < now + 2000L && t.isAlive()) continue;
                }
                double duration = (double)(System.nanoTime() - start) / 1000000.0;
                tracer.debug("getTerminalWidth() returning: %s in %,.1fms%n", size, duration);
                return size.intValue();
            }

            public int width() {
                return this.getSysPropertyWidthOrDefault(this.width == null ? 80 : this.width, this.autoWidth());
            }

            public int longOptionsMaxWidth() {
                return this.longOptionsMaxWidth == null ? 20 : this.longOptionsMaxWidth;
            }

            public boolean autoWidth() {
                return UsageMessageSpec.shouldDetectTerminalSize(this.autoWidth == null ? DEFAULT_USAGE_AUTO_WIDTH : this.autoWidth);
            }

            public UsageMessageSpec autoWidth(boolean detectTerminalSize) {
                this.autoWidth = detectTerminalSize;
                return this;
            }

            static boolean isCharCJK(char c) {
                Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(c);
                return c == '\u00b1' || unicodeBlock == Character.UnicodeBlock.HIRAGANA || unicodeBlock == Character.UnicodeBlock.KATAKANA || unicodeBlock == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS || unicodeBlock == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO || unicodeBlock == Character.UnicodeBlock.HANGUL_JAMO || unicodeBlock == Character.UnicodeBlock.HANGUL_SYLLABLES || unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B || unicodeBlock == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS || unicodeBlock == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || unicodeBlock == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT || unicodeBlock == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || unicodeBlock == Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS || unicodeBlock == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS && c < '\uff61';
            }

            private Map<String, IHelpSectionRenderer> createHelpSectionRendererMap() {
                HashMap<String, IHelpSectionRenderer> result = new HashMap<String, IHelpSectionRenderer>();
                result.put(SECTION_KEY_HEADER_HEADING, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.headerHeading(new Object[0]);
                    }
                });
                result.put(SECTION_KEY_HEADER, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.header(new Object[0]);
                    }
                });
                result.put(SECTION_KEY_SYNOPSIS_HEADING, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.synopsisHeading(new Object[0]);
                    }
                });
                result.put(SECTION_KEY_SYNOPSIS, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.synopsis(help.synopsisHeadingLength());
                    }
                });
                result.put(SECTION_KEY_DESCRIPTION_HEADING, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.descriptionHeading(new Object[0]);
                    }
                });
                result.put(SECTION_KEY_DESCRIPTION, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.description(new Object[0]);
                    }
                });
                result.put(SECTION_KEY_PARAMETER_LIST_HEADING, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.parameterListHeading(new Object[0]);
                    }
                });
                result.put(SECTION_KEY_AT_FILE_PARAMETER, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.atFileParameterList();
                    }
                });
                result.put(SECTION_KEY_PARAMETER_LIST, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.parameterList();
                    }
                });
                result.put(SECTION_KEY_OPTION_LIST_HEADING, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.optionListHeading(new Object[0]);
                    }
                });
                result.put(SECTION_KEY_OPTION_LIST, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.optionList();
                    }
                });
                result.put(SECTION_KEY_END_OF_OPTIONS, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.endOfOptionsList();
                    }
                });
                result.put(SECTION_KEY_COMMAND_LIST_HEADING, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.commandListHeading(new Object[0]);
                    }
                });
                result.put(SECTION_KEY_COMMAND_LIST, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.commandList();
                    }
                });
                result.put(SECTION_KEY_EXIT_CODE_LIST_HEADING, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.exitCodeListHeading(new Object[0]);
                    }
                });
                result.put(SECTION_KEY_EXIT_CODE_LIST, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.exitCodeList();
                    }
                });
                result.put(SECTION_KEY_FOOTER_HEADING, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.footerHeading(new Object[0]);
                    }
                });
                result.put(SECTION_KEY_FOOTER, new IHelpSectionRenderer(){

                    public String render(Help help) {
                        return help.footer(new Object[0]);
                    }
                });
                return result;
            }

            public List<String> sectionKeys() {
                return this.sectionKeys;
            }

            public UsageMessageSpec sectionKeys(List<String> keys) {
                this.sectionKeys = Collections.unmodifiableList(new ArrayList<String>(keys));
                return this;
            }

            public Map<String, IHelpSectionRenderer> sectionMap() {
                return this.helpSectionRendererMap;
            }

            public UsageMessageSpec sectionMap(Map<String, IHelpSectionRenderer> map) {
                this.helpSectionRendererMap = new LinkedHashMap<String, IHelpSectionRenderer>(map);
                return this;
            }

            public IHelpFactory helpFactory() {
                if (this.helpFactory == null) {
                    this.helpFactory = new DefaultHelpFactory();
                }
                return this.helpFactory;
            }

            public UsageMessageSpec helpFactory(IHelpFactory helpFactory) {
                this.helpFactory = Assert.notNull(helpFactory, "helpFactory");
                return this;
            }

            private String interpolate(String value) {
                return this.interpolator == null ? value : this.interpolator.interpolate(value);
            }

            private String[] interpolate(String[] values) {
                return this.interpolator == null ? values : this.interpolator.interpolate(values);
            }

            private String str(String localized, String value, String defaultValue) {
                return this.interpolate(localized != null ? localized : (value != null ? value : defaultValue));
            }

            private String[] arr(String[] localized, String[] value, String[] defaultValue) {
                return this.interpolate(localized != null ? localized : (value != null ? (String[])value.clone() : defaultValue));
            }

            private String resourceStr(String key) {
                return this.messages == null ? null : this.messages.getString(key, null);
            }

            private String[] resourceArr(String key) {
                return this.messages == null ? null : this.messages.getStringArray(key, null);
            }

            public String headerHeading() {
                return this.str(this.resourceStr("usage.headerHeading"), this.headerHeading, DEFAULT_SINGLE_VALUE);
            }

            public String[] header() {
                return this.arr(this.resourceArr("usage.header"), this.header, DEFAULT_MULTI_LINE);
            }

            public String synopsisHeading() {
                return this.str(this.resourceStr("usage.synopsisHeading"), this.synopsisHeading, DEFAULT_SYNOPSIS_HEADING);
            }

            public String synopsisSubcommandLabel() {
                return this.str(this.resourceStr("usage.synopsisSubcommandLabel"), this.synopsisSubcommandLabel, DEFAULT_SYNOPSIS_SUBCOMMANDS);
            }

            public double synopsisAutoIndentThreshold() {
                return this.synopsisAutoIndentThreshold == null ? 0.5 : this.synopsisAutoIndentThreshold;
            }

            public int synopsisIndent() {
                return this.synopsisIndent == null ? -1 : this.synopsisIndent;
            }

            public boolean abbreviateSynopsis() {
                return this.abbreviateSynopsis == null ? DEFAULT_ABBREVIATE_SYNOPSIS : this.abbreviateSynopsis;
            }

            public String[] customSynopsis() {
                return this.arr(this.resourceArr("usage.customSynopsis"), this.customSynopsis, DEFAULT_MULTI_LINE);
            }

            public String descriptionHeading() {
                return this.str(this.resourceStr("usage.descriptionHeading"), this.descriptionHeading, DEFAULT_SINGLE_VALUE);
            }

            public String[] description() {
                return this.arr(this.resourceArr("usage.description"), this.description, DEFAULT_MULTI_LINE);
            }

            public String parameterListHeading() {
                return this.str(this.resourceStr("usage.parameterListHeading"), this.parameterListHeading, DEFAULT_SINGLE_VALUE);
            }

            public String optionListHeading() {
                return this.str(this.resourceStr("usage.optionListHeading"), this.optionListHeading, DEFAULT_SINGLE_VALUE);
            }

            public boolean sortOptions() {
                return this.sortOptions == null ? DEFAULT_SORT_OPTIONS : this.sortOptions;
            }

            public char requiredOptionMarker() {
                return this.requiredOptionMarker == null ? (char)' ' : this.requiredOptionMarker.charValue();
            }

            public boolean showDefaultValues() {
                return this.showDefaultValues == null ? DEFAULT_SHOW_DEFAULT_VALUES : this.showDefaultValues;
            }

            public boolean showAtFileInUsageHelp() {
                return this.showAtFileInUsageHelp == null ? DEFAULT_SHOW_AT_FILE : this.showAtFileInUsageHelp;
            }

            public boolean showEndOfOptionsDelimiterInUsageHelp() {
                return this.showEndOfOptionsDelimiterInUsageHelp == null ? DEFAULT_SHOW_END_OF_OPTIONS : this.showEndOfOptionsDelimiterInUsageHelp;
            }

            public boolean hidden() {
                return this.hidden == null ? DEFAULT_HIDDEN : this.hidden;
            }

            public String commandListHeading() {
                return this.str(this.resourceStr("usage.commandListHeading"), this.commandListHeading, DEFAULT_COMMAND_LIST_HEADING);
            }

            public String exitCodeListHeading() {
                return this.str(this.resourceStr("usage.exitCodeListHeading"), this.exitCodeListHeading, DEFAULT_SINGLE_VALUE);
            }

            public Map<String, String> exitCodeList() {
                String[] bundleValues = this.resourceArr("usage.exitCodeList");
                if (bundleValues == null && this.exitCodeList != null) {
                    return this.exitCodeList;
                }
                Map<String, String> result = UsageMessageSpec.keyValuesMap(this.arr(bundleValues, this.exitCodeListStrings, DEFAULT_MULTI_LINE));
                return Collections.unmodifiableMap(result);
            }

            public static Map<String, String> keyValuesMap(String ... entries) {
                LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
                if (entries == null) {
                    return result;
                }
                for (int i = 0; i < entries.length; ++i) {
                    int pos = entries[i].indexOf(58);
                    if (pos >= 0) {
                        result.put(entries[i].substring(0, pos), entries[i].substring(pos + 1));
                        continue;
                    }
                    new Tracer().info("Ignoring line at index %d: cannot split '%s' into 'key:value'%n", i, entries[i]);
                }
                return result;
            }

            public String footerHeading() {
                return this.str(this.resourceStr("usage.footerHeading"), this.footerHeading, DEFAULT_SINGLE_VALUE);
            }

            public String[] footer() {
                return this.arr(this.resourceArr("usage.footer"), this.footer, DEFAULT_MULTI_LINE);
            }

            public UsageMessageSpec headerHeading(String headerHeading) {
                this.headerHeading = headerHeading;
                return this;
            }

            public UsageMessageSpec header(String ... header) {
                this.header = header;
                return this;
            }

            public UsageMessageSpec synopsisHeading(String newValue) {
                this.synopsisHeading = newValue;
                return this;
            }

            public UsageMessageSpec synopsisSubcommandLabel(String newValue) {
                this.synopsisSubcommandLabel = newValue;
                return this;
            }

            public UsageMessageSpec synopsisAutoIndentThreshold(double newValue) {
                if (newValue < 0.0 || newValue > 0.9) {
                    throw new IllegalArgumentException("synopsisAutoIndentThreshold must be between 0.0 and 0.9 (inclusive), but was " + newValue);
                }
                this.synopsisAutoIndentThreshold = newValue;
                return this;
            }

            public UsageMessageSpec synopsisIndent(int newValue) {
                this.synopsisIndent = newValue;
                return this;
            }

            public UsageMessageSpec abbreviateSynopsis(boolean newValue) {
                this.abbreviateSynopsis = newValue;
                return this;
            }

            public UsageMessageSpec customSynopsis(String ... customSynopsis) {
                this.customSynopsis = customSynopsis;
                return this;
            }

            public UsageMessageSpec descriptionHeading(String newValue) {
                this.descriptionHeading = newValue;
                return this;
            }

            public UsageMessageSpec description(String ... description) {
                this.description = description;
                return this;
            }

            public UsageMessageSpec parameterListHeading(String newValue) {
                this.parameterListHeading = newValue;
                return this;
            }

            public UsageMessageSpec optionListHeading(String newValue) {
                this.optionListHeading = newValue;
                return this;
            }

            public UsageMessageSpec sortOptions(boolean newValue) {
                this.sortOptions = newValue;
                return this;
            }

            public UsageMessageSpec requiredOptionMarker(char newValue) {
                this.requiredOptionMarker = Character.valueOf(newValue);
                return this;
            }

            public UsageMessageSpec showDefaultValues(boolean newValue) {
                this.showDefaultValues = newValue;
                return this;
            }

            public UsageMessageSpec showAtFileInUsageHelp(boolean newValue) {
                this.showAtFileInUsageHelp = newValue;
                return this;
            }

            public UsageMessageSpec showEndOfOptionsDelimiterInUsageHelp(boolean newValue) {
                this.showEndOfOptionsDelimiterInUsageHelp = newValue;
                return this;
            }

            public UsageMessageSpec hidden(boolean value) {
                this.hidden = value;
                return this;
            }

            public UsageMessageSpec commandListHeading(String newValue) {
                this.commandListHeading = newValue;
                return this;
            }

            public UsageMessageSpec exitCodeListHeading(String newValue) {
                this.exitCodeListHeading = newValue;
                return this;
            }

            public UsageMessageSpec exitCodeList(Map<String, String> newValue) {
                this.exitCodeList = newValue == null ? null : Collections.unmodifiableMap(new LinkedHashMap<String, String>(newValue));
                return this;
            }

            public UsageMessageSpec footerHeading(String newValue) {
                this.footerHeading = newValue;
                return this;
            }

            public UsageMessageSpec footer(String ... footer) {
                this.footer = footer;
                return this;
            }

            public Messages messages() {
                return this.messages;
            }

            public UsageMessageSpec messages(Messages msgs) {
                this.messages = msgs;
                return this;
            }

            public boolean adjustLineBreaksForWideCJKCharacters() {
                return this.adjustLineBreaksForWideCJKCharacters == null ? DEFAULT_ADJUST_CJK : this.adjustLineBreaksForWideCJKCharacters;
            }

            public UsageMessageSpec adjustLineBreaksForWideCJKCharacters(boolean adjustForWideChars) {
                this.adjustLineBreaksForWideCJKCharacters = adjustForWideChars;
                return this;
            }

            void updateFromCommand(Command cmd, CommandSpec commandSpec, boolean loadResourceBundle) {
                if (!CommandLine.empty(cmd.resourceBundle())) {
                    if (loadResourceBundle) {
                        this.messages(new Messages(commandSpec, cmd.resourceBundle()));
                    } else {
                        ResourceBundle rb = null;
                        try {
                            rb = ResourceBundle.getBundle(cmd.resourceBundle());
                        }
                        catch (MissingResourceException missingResourceException) {
                            // empty catch block
                        }
                        this.messages(new Messages(commandSpec, cmd.resourceBundle(), rb));
                    }
                }
                if (Model.isNonDefault(cmd.abbreviateSynopsis(), UsageMessageSpec.DEFAULT_ABBREVIATE_SYNOPSIS)) {
                    this.abbreviateSynopsis = cmd.abbreviateSynopsis();
                }
                if (Model.isNonDefault(cmd.usageHelpAutoWidth(), UsageMessageSpec.DEFAULT_USAGE_AUTO_WIDTH)) {
                    this.autoWidth = cmd.usageHelpAutoWidth();
                }
                if (Model.isNonDefault(cmd.commandListHeading(), DEFAULT_COMMAND_LIST_HEADING)) {
                    this.commandListHeading = cmd.commandListHeading();
                }
                if (Model.isNonDefault(cmd.customSynopsis(), UsageMessageSpec.DEFAULT_MULTI_LINE)) {
                    this.customSynopsis = (String[])cmd.customSynopsis().clone();
                }
                if (Model.isNonDefault(cmd.description(), UsageMessageSpec.DEFAULT_MULTI_LINE)) {
                    this.description = (String[])cmd.description().clone();
                }
                if (Model.isNonDefault(cmd.descriptionHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.descriptionHeading = cmd.descriptionHeading();
                }
                if (Model.isNonDefault(cmd.exitCodeList(), UsageMessageSpec.DEFAULT_MULTI_LINE)) {
                    this.exitCodeListStrings = (String[])cmd.exitCodeList().clone();
                }
                if (Model.isNonDefault(cmd.exitCodeListHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.exitCodeListHeading = cmd.exitCodeListHeading();
                }
                if (Model.isNonDefault(cmd.footer(), UsageMessageSpec.DEFAULT_MULTI_LINE)) {
                    this.footer = (String[])cmd.footer().clone();
                }
                if (Model.isNonDefault(cmd.footerHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.footerHeading = cmd.footerHeading();
                }
                if (Model.isNonDefault(cmd.header(), UsageMessageSpec.DEFAULT_MULTI_LINE)) {
                    this.header = (String[])cmd.header().clone();
                }
                if (Model.isNonDefault(cmd.headerHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.headerHeading = cmd.headerHeading();
                }
                if (Model.isNonDefault(cmd.hidden(), UsageMessageSpec.DEFAULT_HIDDEN)) {
                    this.hidden = cmd.hidden();
                }
                if (Model.isNonDefault(cmd.optionListHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.optionListHeading = cmd.optionListHeading();
                }
                if (Model.isNonDefault(cmd.parameterListHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.parameterListHeading = cmd.parameterListHeading();
                }
                if (Model.isNonDefault(Character.valueOf(cmd.requiredOptionMarker()), Character.valueOf(' '))) {
                    this.requiredOptionMarker = Character.valueOf(cmd.requiredOptionMarker());
                }
                if (Model.isNonDefault(cmd.showAtFileInUsageHelp(), UsageMessageSpec.DEFAULT_SHOW_AT_FILE)) {
                    this.showAtFileInUsageHelp = cmd.showAtFileInUsageHelp();
                }
                if (Model.isNonDefault(cmd.showDefaultValues(), UsageMessageSpec.DEFAULT_SHOW_DEFAULT_VALUES)) {
                    this.showDefaultValues = cmd.showDefaultValues();
                }
                if (Model.isNonDefault(cmd.showEndOfOptionsDelimiterInUsageHelp(), UsageMessageSpec.DEFAULT_SHOW_END_OF_OPTIONS)) {
                    this.showEndOfOptionsDelimiterInUsageHelp = cmd.showEndOfOptionsDelimiterInUsageHelp();
                }
                if (Model.isNonDefault(cmd.sortOptions(), UsageMessageSpec.DEFAULT_SORT_OPTIONS)) {
                    this.sortOptions = cmd.sortOptions();
                }
                if (Model.isNonDefault(cmd.synopsisHeading(), DEFAULT_SYNOPSIS_HEADING)) {
                    this.synopsisHeading = cmd.synopsisHeading();
                }
                if (Model.isNonDefault(cmd.synopsisSubcommandLabel(), DEFAULT_SYNOPSIS_SUBCOMMANDS)) {
                    this.synopsisSubcommandLabel = cmd.synopsisSubcommandLabel();
                }
                if (Model.isNonDefault(cmd.usageHelpWidth(), 80)) {
                    this.width(cmd.usageHelpWidth());
                }
            }

            void initFromMixin(UsageMessageSpec mixin, CommandSpec commandSpec) {
                if (Model.initializable(this.abbreviateSynopsis, mixin.abbreviateSynopsis(), UsageMessageSpec.DEFAULT_ABBREVIATE_SYNOPSIS)) {
                    this.abbreviateSynopsis = mixin.abbreviateSynopsis();
                }
                if (Model.initializable(this.adjustLineBreaksForWideCJKCharacters, mixin.adjustLineBreaksForWideCJKCharacters(), UsageMessageSpec.DEFAULT_ADJUST_CJK)) {
                    this.adjustLineBreaksForWideCJKCharacters = mixin.adjustLineBreaksForWideCJKCharacters();
                }
                if (Model.initializable(this.autoWidth, mixin.autoWidth(), UsageMessageSpec.DEFAULT_USAGE_AUTO_WIDTH)) {
                    this.autoWidth = mixin.autoWidth();
                }
                if (Model.initializable(this.commandListHeading, mixin.commandListHeading(), DEFAULT_COMMAND_LIST_HEADING)) {
                    this.commandListHeading = mixin.commandListHeading();
                }
                if (Model.initializable(this.customSynopsis, mixin.customSynopsis(), UsageMessageSpec.DEFAULT_MULTI_LINE)) {
                    this.customSynopsis = (String[])mixin.customSynopsis().clone();
                }
                if (Model.initializable(this.description, mixin.description(), UsageMessageSpec.DEFAULT_MULTI_LINE)) {
                    this.description = (String[])mixin.description().clone();
                }
                if (Model.initializable(this.descriptionHeading, mixin.descriptionHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.descriptionHeading = mixin.descriptionHeading();
                }
                if (Model.initializable(this.exitCodeList, mixin.exitCodeList(), Collections.emptyMap()) && this.exitCodeListStrings == null) {
                    this.exitCodeList = Collections.unmodifiableMap(new LinkedHashMap<String, String>(mixin.exitCodeList()));
                }
                if (Model.initializable(this.exitCodeListHeading, mixin.exitCodeListHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.exitCodeListHeading = mixin.exitCodeListHeading();
                }
                if (Model.initializable(this.footer, mixin.footer(), UsageMessageSpec.DEFAULT_MULTI_LINE)) {
                    this.footer = (String[])mixin.footer().clone();
                }
                if (Model.initializable(this.footerHeading, mixin.footerHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.footerHeading = mixin.footerHeading();
                }
                if (Model.initializable(this.header, mixin.header(), UsageMessageSpec.DEFAULT_MULTI_LINE)) {
                    this.header = (String[])mixin.header().clone();
                }
                if (Model.initializable(this.headerHeading, mixin.headerHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.headerHeading = mixin.headerHeading();
                }
                if (Model.initializable(this.hidden, mixin.hidden(), UsageMessageSpec.DEFAULT_HIDDEN)) {
                    this.hidden = mixin.hidden();
                }
                if (Model.initializable(this.longOptionsMaxWidth, mixin.longOptionsMaxWidth(), 20)) {
                    this.longOptionsMaxWidth = mixin.longOptionsMaxWidth();
                }
                if (Messages.empty(this.messages) && Messages.resourceBundleBaseName(this.messages) == null) {
                    this.messages(Messages.copy(commandSpec, mixin.messages()));
                }
                if (Model.initializable(this.optionListHeading, mixin.optionListHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.optionListHeading = mixin.optionListHeading();
                }
                if (Model.initializable(this.parameterListHeading, mixin.parameterListHeading(), DEFAULT_SINGLE_VALUE)) {
                    this.parameterListHeading = mixin.parameterListHeading();
                }
                if (Model.initializable(this.requiredOptionMarker, Character.valueOf(mixin.requiredOptionMarker()), Character.valueOf(' '))) {
                    this.requiredOptionMarker = Character.valueOf(mixin.requiredOptionMarker());
                }
                if (Model.initializable(this.showAtFileInUsageHelp, mixin.showAtFileInUsageHelp(), UsageMessageSpec.DEFAULT_SHOW_AT_FILE)) {
                    this.showAtFileInUsageHelp = mixin.showAtFileInUsageHelp();
                }
                if (Model.initializable(this.showDefaultValues, mixin.showDefaultValues(), UsageMessageSpec.DEFAULT_SHOW_DEFAULT_VALUES)) {
                    this.showDefaultValues = mixin.showDefaultValues();
                }
                if (Model.initializable(this.showEndOfOptionsDelimiterInUsageHelp, mixin.showEndOfOptionsDelimiterInUsageHelp(), UsageMessageSpec.DEFAULT_SHOW_END_OF_OPTIONS)) {
                    this.showEndOfOptionsDelimiterInUsageHelp = mixin.showEndOfOptionsDelimiterInUsageHelp();
                }
                if (Model.initializable(this.sortOptions, mixin.sortOptions(), UsageMessageSpec.DEFAULT_SORT_OPTIONS)) {
                    this.sortOptions = mixin.sortOptions();
                }
                if (Model.initializable(this.synopsisHeading, mixin.synopsisHeading(), DEFAULT_SYNOPSIS_HEADING)) {
                    this.synopsisHeading = mixin.synopsisHeading();
                }
                if (Model.initializable(this.synopsisSubcommandLabel, mixin.synopsisSubcommandLabel(), DEFAULT_SYNOPSIS_SUBCOMMANDS)) {
                    this.synopsisSubcommandLabel = mixin.synopsisSubcommandLabel();
                }
                if (Model.initializable(this.width, mixin.width(), 80)) {
                    this.width = mixin.width();
                }
            }

            void initFrom(UsageMessageSpec settings, CommandSpec commandSpec) {
                this.abbreviateSynopsis = settings.abbreviateSynopsis;
                this.adjustLineBreaksForWideCJKCharacters = settings.adjustLineBreaksForWideCJKCharacters;
                this.autoWidth = settings.autoWidth;
                this.commandListHeading = settings.commandListHeading;
                this.customSynopsis = settings.customSynopsis;
                this.description = settings.description;
                this.descriptionHeading = settings.descriptionHeading;
                this.exitCodeList = settings.exitCodeList;
                this.exitCodeListHeading = settings.exitCodeListHeading;
                this.exitCodeListStrings = settings.exitCodeListStrings;
                this.footer = settings.footer;
                this.footerHeading = settings.footerHeading;
                this.header = settings.header;
                this.headerHeading = settings.headerHeading;
                this.helpFactory = settings.helpFactory;
                this.helpSectionRendererMap = settings.helpSectionRendererMap;
                this.hidden = settings.hidden;
                this.longOptionsMaxWidth = settings.longOptionsMaxWidth;
                this.messages = Messages.copy(commandSpec, settings.messages());
                this.optionListHeading = settings.optionListHeading;
                this.parameterListHeading = settings.parameterListHeading;
                this.requiredOptionMarker = settings.requiredOptionMarker;
                this.sectionKeys = settings.sectionKeys;
                this.showAtFileInUsageHelp = settings.showAtFileInUsageHelp;
                this.showDefaultValues = settings.showDefaultValues;
                this.showEndOfOptionsDelimiterInUsageHelp = settings.showEndOfOptionsDelimiterInUsageHelp;
                this.sortOptions = settings.sortOptions;
                this.synopsisAutoIndentThreshold = settings.synopsisAutoIndentThreshold;
                this.synopsisHeading = settings.synopsisHeading;
                this.synopsisIndent = settings.synopsisIndent;
                this.synopsisSubcommandLabel = settings.synopsisSubcommandLabel;
                this.width = settings.width;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static class CommandSpec {
            public static final String DEFAULT_COMMAND_NAME = "<main class>";
            static final Boolean DEFAULT_IS_HELP_COMMAND = false;
            static final Boolean DEFAULT_IS_ADD_METHOD_SUBCOMMANDS = true;
            static final Boolean DEFAULT_INTERPOLATE_VARIABLES = true;
            static final Boolean DEFAULT_SUBCOMMANDS_REPEATABLE = false;
            private final CaseAwareLinkedMap<String, CommandLine> commands = new CaseAwareLinkedMap();
            private final CaseAwareLinkedMap<String, OptionSpec> optionsByNameMap = new CaseAwareLinkedMap();
            private final CaseAwareLinkedMap<String, OptionSpec> negatedOptionsByNameMap = new CaseAwareLinkedMap();
            private final CaseAwareLinkedMap<Character, OptionSpec> posixOptionsByKeyMap = new CaseAwareLinkedMap();
            private final Map<String, CommandSpec> mixins = new LinkedHashMap<String, CommandSpec>();
            private final Map<String, IAnnotatedElement> mixinAnnotatedElements = new LinkedHashMap<String, IAnnotatedElement>();
            private final List<ArgSpec> requiredArgs = new ArrayList<ArgSpec>();
            private final List<ArgSpec> args = new ArrayList<ArgSpec>();
            private final List<OptionSpec> options = new ArrayList<OptionSpec>();
            private final List<PositionalParamSpec> positionalParameters = new ArrayList<PositionalParamSpec>();
            private final List<UnmatchedArgsBinding> unmatchedArgs = new ArrayList<UnmatchedArgsBinding>();
            private final List<IAnnotatedElement> specElements = new ArrayList<IAnnotatedElement>();
            private final List<IAnnotatedElement> parentCommandElements = new ArrayList<IAnnotatedElement>();
            private final List<ArgGroupSpec> groups = new ArrayList<ArgGroupSpec>();
            private final ParserSpec parser = new ParserSpec();
            private final Interpolator interpolator = new Interpolator(this);
            private final UsageMessageSpec usageMessage = new UsageMessageSpec(this.interpolator);
            private TypedMember[] methodParams;
            private final CommandUserObject userObject;
            private CommandLine commandLine;
            private CommandSpec parent;
            private Boolean isAddMethodSubcommands;
            private Boolean interpolateVariables;
            private String name;
            private Set<String> aliases = new LinkedHashSet<String>();
            private Boolean isHelpCommand;
            private IVersionProvider versionProvider;
            private IDefaultValueProvider defaultValueProvider;
            private INegatableOptionTransformer negatableOptionTransformer = RegexTransformer.createDefault();
            private Boolean subcommandsRepeatable;
            private String[] version;
            private String toString;
            private boolean inherited = false;
            private ScopeType scopeType = null;
            private Integer exitCodeOnSuccess;
            private Integer exitCodeOnUsageHelp;
            private Integer exitCodeOnVersionHelp;
            private Integer exitCodeOnInvalidInput;
            private Integer exitCodeOnExecutionException;
            private IModelTransformer modelTransformer = null;
            private IParameterPreprocessor preprocessor = new NoOpParameterPreprocessor();

            private CommandSpec(CommandUserObject userObject) {
                this.userObject = userObject;
                this.userObject.commandSpec = this;
            }

            private CommandSpec copy() {
                Object obj = this.userObject.type == null ? this.userObject.instance : this.userObject.type;
                CommandSpec result = obj == null ? CommandSpec.create() : CommandSpec.forAnnotatedObject(obj, this.userObject.factory);
                result.commandLine = this.commandLine;
                result.parent = this.parent;
                result.methodParams = this.methodParams;
                result.isAddMethodSubcommands = this.isAddMethodSubcommands;
                result.interpolateVariables = this.interpolateVariables;
                result.name = this.name;
                result.aliases = this.aliases;
                result.isHelpCommand = this.isHelpCommand;
                result.versionProvider = this.versionProvider;
                result.modelTransformer = this.modelTransformer;
                result.defaultValueProvider = this.defaultValueProvider;
                result.negatableOptionTransformer = this.negatableOptionTransformer;
                result.subcommandsRepeatable = this.subcommandsRepeatable;
                result.version = this.version;
                result.toString = this.toString;
                result.exitCodeOnSuccess = this.exitCodeOnSuccess;
                result.exitCodeOnUsageHelp = this.exitCodeOnUsageHelp;
                result.exitCodeOnVersionHelp = this.exitCodeOnVersionHelp;
                result.exitCodeOnInvalidInput = this.exitCodeOnInvalidInput;
                result.exitCodeOnExecutionException = this.exitCodeOnExecutionException;
                result.usageMessage.initFrom(this.usageMessage, this);
                result.parser(this.parser);
                result.inherited = this.inherited;
                result.scopeType = this.scopeType;
                return result;
            }

            public static CommandSpec create() {
                return CommandSpec.wrapWithoutInspection(null);
            }

            public static CommandSpec wrapWithoutInspection(Object userObject) {
                return new CommandSpec(CommandUserObject.create(userObject, CommandLine.defaultFactory()));
            }

            public static CommandSpec wrapWithoutInspection(Object userObject, IFactory factory) {
                return new CommandSpec(CommandUserObject.create(userObject, factory));
            }

            public static CommandSpec forAnnotatedObject(Object userObject) {
                return CommandSpec.forAnnotatedObject(userObject, new DefaultFactory());
            }

            public static CommandSpec forAnnotatedObject(Object userObject, IFactory factory) {
                return CommandReflection.extractCommandSpec(userObject, factory, true);
            }

            public static CommandSpec forAnnotatedObjectLenient(Object userObject) {
                return CommandSpec.forAnnotatedObjectLenient(userObject, new DefaultFactory());
            }

            public static CommandSpec forAnnotatedObjectLenient(Object userObject, IFactory factory) {
                return CommandReflection.extractCommandSpec(userObject, factory, false);
            }

            void validate() {
                Collections.sort(this.positionalParameters, new PositionalParametersSorter());
                CommandLine.validatePositionalParameters(this.positionalParameters);
                ArrayList<String> wrongUsageHelpAttr = new ArrayList<String>();
                ArrayList<String> wrongVersionHelpAttr = new ArrayList<String>();
                ArrayList<String> usageHelpAttr = new ArrayList<String>();
                ArrayList<String> versionHelpAttr = new ArrayList<String>();
                for (OptionSpec option : this.options()) {
                    if (option.usageHelp()) {
                        usageHelpAttr.add(option.longestName());
                        if (!CommandLine.isBoolean(option.type())) {
                            wrongUsageHelpAttr.add(option.longestName());
                        }
                    }
                    if (!option.versionHelp()) continue;
                    versionHelpAttr.add(option.longestName());
                    if (CommandLine.isBoolean(option.type())) continue;
                    wrongVersionHelpAttr.add(option.longestName());
                }
                String wrongType = "Non-boolean options like %s should not be marked as '%s=true'. Usually a command has one %s boolean flag that triggers display of the %s. Alternatively, consider using @Command(mixinStandardHelpOptions = true) on your command instead.";
                String multiple = "Multiple options %s are marked as '%s=true'. Usually a command has only one %s option that triggers display of the %s. Alternatively, consider using @Command(mixinStandardHelpOptions = true) on your command instead.%n";
                if (!wrongUsageHelpAttr.isEmpty()) {
                    throw new InitializationException(String.format(wrongType, wrongUsageHelpAttr, "usageHelp", "--help", "usage help message"));
                }
                if (!wrongVersionHelpAttr.isEmpty()) {
                    throw new InitializationException(String.format(wrongType, wrongVersionHelpAttr, "versionHelp", "--version", "version information"));
                }
                if (usageHelpAttr.size() > 1) {
                    new Tracer().warn(multiple, usageHelpAttr, "usageHelp", "--help", "usage help message");
                }
                if (versionHelpAttr.size() > 1) {
                    new Tracer().warn(multiple, versionHelpAttr, "versionHelp", "--version", "version information");
                }
            }

            public Object userObject() {
                return this.userObject.getInstance();
            }

            public CommandLine commandLine() {
                return this.commandLine;
            }

            protected CommandSpec commandLine(CommandLine commandLine) {
                this.commandLine = commandLine;
                for (CommandSpec mixedInSpec : this.mixins.values()) {
                    mixedInSpec.commandLine(commandLine);
                }
                for (CommandLine sub : this.commands.values()) {
                    sub.getCommandSpec().parent(this);
                }
                return this;
            }

            public ParserSpec parser() {
                return this.parser;
            }

            public CommandSpec parser(ParserSpec settings) {
                this.parser.initFrom(settings);
                return this;
            }

            public UsageMessageSpec usageMessage() {
                return this.usageMessage;
            }

            public CommandSpec usageMessage(UsageMessageSpec settings) {
                this.usageMessage.initFrom(settings, this);
                return this;
            }

            public boolean subcommandsCaseInsensitive() {
                return this.commands.isCaseInsensitive();
            }

            public CommandSpec subcommandsCaseInsensitive(boolean caseInsensitiveSubcommands) {
                if (this.subcommandsCaseInsensitive() == caseInsensitiveSubcommands) {
                    return this;
                }
                new Tracer().debug("Changing subcommandsCaseInsensitive to %s%n", caseInsensitiveSubcommands);
                this.commands.setCaseInsensitive(caseInsensitiveSubcommands);
                return this;
            }

            public boolean optionsCaseInsensitive() {
                return this.optionsByNameMap.isCaseInsensitive();
            }

            public CommandSpec optionsCaseInsensitive(boolean caseInsensitiveOptions) {
                if (this.optionsCaseInsensitive() == caseInsensitiveOptions) {
                    return this;
                }
                new Tracer().debug("Changing optionsCaseInsensitive to %s%n", caseInsensitiveOptions);
                this.optionsByNameMap.setCaseInsensitive(caseInsensitiveOptions);
                this.negatedOptionsByNameMap.setCaseInsensitive(caseInsensitiveOptions);
                this.posixOptionsByKeyMap.setCaseInsensitive(caseInsensitiveOptions);
                RegexTransformer transformer = caseInsensitiveOptions ? RegexTransformer.createCaseInsensitive() : RegexTransformer.createDefault();
                this.negatableOptionTransformer(transformer);
                return this;
            }

            public String resourceBundleBaseName() {
                return Messages.resourceBundleBaseName(this.usageMessage.messages());
            }

            public CommandSpec resourceBundleBaseName(String resourceBundleBaseName) {
                ResourceBundle bundle = CommandLine.empty(resourceBundleBaseName) ? null : ResourceBundle.getBundle(resourceBundleBaseName);
                this.setBundle(resourceBundleBaseName, bundle);
                return this;
            }

            public ResourceBundle resourceBundle() {
                return Messages.resourceBundle(this.usageMessage.messages());
            }

            public CommandSpec resourceBundle(ResourceBundle bundle) {
                this.setBundle(Messages.extractName(bundle), bundle);
                return this;
            }

            private void setBundle(String bundleBaseName, ResourceBundle bundle) {
                this.usageMessage().messages(new Messages(this, bundleBaseName, bundle));
                this.updateArgSpecMessages();
            }

            private void updateArgSpecMessages() {
                for (OptionSpec opt : this.options()) {
                    opt.messages(this.usageMessage().messages());
                }
                for (PositionalParamSpec pos : this.positionalParameters()) {
                    pos.messages(this.usageMessage().messages());
                }
                for (ArgGroupSpec group : this.argGroups()) {
                    group.messages(this.usageMessage().messages());
                }
            }

            public Map<String, CommandLine> subcommands() {
                return Collections.unmodifiableMap(this.commands);
            }

            public CommandSpec addSubcommand(String name, CommandSpec subcommand) {
                return this.addSubcommand(name, new CommandLine(subcommand));
            }

            public CommandSpec addSubcommand(String name, CommandLine subCommandLine) {
                CommandSpec subSpec = subCommandLine.getCommandSpec();
                String actualName = this.validateSubcommandName(this.interpolator.interpolateCommandName(name), subSpec);
                Tracer t = new Tracer();
                if (t.isDebug()) {
                    t.debug("Adding subcommand '%s' to '%s'%n", actualName, this.qualifiedName());
                }
                String previousName = this.commands.getCaseSensitiveKey(actualName);
                CommandLine previous = this.commands.put(actualName, subCommandLine);
                if (previous != null && previous != subCommandLine) {
                    throw new DuplicateNameException("Another subcommand named '" + previousName + "' already exists for command '" + this.name() + "'");
                }
                if (subSpec.name == null) {
                    subSpec.name(actualName);
                }
                subSpec.parent(this);
                for (String alias : subSpec.aliases()) {
                    if (t.isDebug()) {
                        t.debug("Adding alias '%s' for '%s'%n", (this.parent == null ? "" : this.parent.qualifiedName() + " ") + alias, this.qualifiedName());
                    }
                    if ((previous = this.commands.put(this.interpolator.interpolate(alias), subCommandLine)) == null || previous == subCommandLine) continue;
                    throw new DuplicateNameException("Alias '" + alias + "' for subcommand '" + actualName + "' is already used by another subcommand of '" + this.name() + "'");
                }
                subSpec.initCommandHierarchyWithResourceBundle(this.resourceBundleBaseName(), this.resourceBundle());
                if (this.scopeType() == ScopeType.INHERIT) {
                    subSpec.inheritAttributesFrom(this);
                } else if (this.inherited()) {
                    CommandSpec root;
                    for (root = this.parent(); root != null && root.scopeType() != ScopeType.INHERIT; root = root.parent()) {
                    }
                    if (root == null) {
                        throw new InitializationException("Cannot find scope=INHERIT root for " + this);
                    }
                    subSpec.inheritAttributesFrom(root);
                }
                for (ArgSpec arg : this.args()) {
                    if (arg.scopeType() != ScopeType.INHERIT) continue;
                    subSpec.add(arg.isOption() ? ((OptionSpec.Builder)OptionSpec.builder((OptionSpec)arg).inherited(true)).build() : ((PositionalParamSpec.Builder)PositionalParamSpec.builder((PositionalParamSpec)arg).inherited(true)).build());
                }
                return this;
            }

            private void inheritAttributesFrom(CommandSpec root) {
                this.inherited = true;
                this.initFrom(root);
                this.updatedSubcommandsToInheritFrom(root);
            }

            private void updatedSubcommandsToInheritFrom(CommandSpec root) {
                if (root != this && root.mixinStandardHelpOptions()) {
                    this.mixinStandardHelpOptions(true);
                }
                HashSet<CommandLine> subcommands = new HashSet<CommandLine>(this.subcommands().values());
                for (CommandLine sub : subcommands) {
                    sub.getCommandSpec().inheritAttributesFrom(root);
                }
            }

            public CommandLine removeSubcommand(String name) {
                Tracer t;
                String actualName = name;
                if (this.parser().abbreviatedSubcommandsAllowed()) {
                    actualName = AbbreviationMatcher.match(this.commands.keySet(), name, this.subcommandsCaseInsensitive(), this.commandLine);
                }
                TreeSet<String> removedNames = new TreeSet<String>();
                CommandLine result = this.commands.remove(actualName);
                if (result != null) {
                    removedNames.add(actualName);
                    this.commands.remove(result.getCommandName());
                    removedNames.add(result.getCommandName());
                    for (String alias : result.getCommandSpec().aliases()) {
                        this.commands.remove(alias);
                        removedNames.add(alias);
                    }
                }
                if ((t = new Tracer()).isDebug()) {
                    t.debug("Removed %d subcommand entries %s for key '%s' from '%s'%n", removedNames.size(), removedNames, name, this.qualifiedName());
                }
                return result;
            }

            private String validateSubcommandName(String name, CommandSpec subSpec) {
                String result;
                String string = result = name == null ? subSpec.name : name;
                if (result == null && !subSpec.aliases.isEmpty()) {
                    Iterator<String> iter = subSpec.aliases.iterator();
                    result = iter.next();
                    iter.remove();
                }
                if (result == null) {
                    throw new InitializationException("Cannot add subcommand with null name to " + this.qualifiedName());
                }
                return result;
            }

            private void initCommandHierarchyWithResourceBundle(String bundleBaseName, ResourceBundle rb) {
                if (this.resourceBundle() == null && this.resourceBundleBaseName() == null) {
                    this.setBundle(bundleBaseName, rb);
                }
                for (CommandLine sub : this.commands.values()) {
                    sub.getCommandSpec().initCommandHierarchyWithResourceBundle(bundleBaseName, rb);
                }
            }

            public boolean isAddMethodSubcommands() {
                return this.isAddMethodSubcommands == null ? DEFAULT_IS_ADD_METHOD_SUBCOMMANDS : this.isAddMethodSubcommands;
            }

            public CommandSpec setAddMethodSubcommands(Boolean addMethodSubcommands) {
                this.isAddMethodSubcommands = addMethodSubcommands;
                return this;
            }

            public boolean interpolateVariables() {
                return this.interpolateVariables == null ? DEFAULT_INTERPOLATE_VARIABLES : this.interpolateVariables;
            }

            public CommandSpec interpolateVariables(Boolean interpolate) {
                this.interpolateVariables = interpolate;
                return this;
            }

            public CommandSpec addMethodSubcommands() {
                return this.addMethodSubcommands(new DefaultFactory());
            }

            public CommandSpec addMethodSubcommands(IFactory factory) {
                if (this.userObject.isMethod()) {
                    throw new InitializationException("Cannot discover subcommand methods of this Command Method: " + this.userObject);
                }
                for (CommandLine sub : CommandSpec.createMethodSubcommands(this.userObject.getType(), factory, true)) {
                    this.addSubcommand(sub.getCommandName(), sub);
                }
                this.isAddMethodSubcommands = true;
                return this;
            }

            static List<CommandLine> createMethodSubcommands(Class<?> cls, IFactory factory, boolean includeInherited) {
                ArrayList<CommandLine> result = new ArrayList<CommandLine>();
                for (Method method : CommandLine.getCommandMethods(cls, null, includeInherited)) {
                    result.add(new CommandLine(method, factory));
                }
                return result;
            }

            public CommandSpec parent() {
                return this.parent;
            }

            public CommandSpec root() {
                CommandSpec root = this;
                while (root.parent != null) {
                    root = root.parent;
                }
                return root;
            }

            public CommandSpec parent(CommandSpec parent) {
                this.parent = parent;
                this.injectParentCommand(parent.userObject);
                return this;
            }

            public CommandSpec add(ArgSpec arg) {
                return arg.isOption() ? this.addOption((OptionSpec)arg) : this.addPositional((PositionalParamSpec)arg);
            }

            public CommandSpec addOption(OptionSpec option) {
                Tracer tracer = new Tracer();
                for (String name : this.interpolator.interpolate(option.names())) {
                    String existingName = this.optionsByNameMap.getCaseSensitiveKey(name);
                    OptionSpec existing = this.optionsByNameMap.put(name, option);
                    if (existing != null) {
                        throw DuplicateOptionAnnotationsException.create(existingName, option, existing);
                    }
                    String existingNegatedName = this.negatedOptionsByNameMap.getCaseSensitiveKey(name);
                    OptionSpec existingNegated = this.negatedOptionsByNameMap.get(name);
                    if (existingNegated != null && existingNegated != option) {
                        throw DuplicateOptionAnnotationsException.create(existingNegatedName, option, existingNegated);
                    }
                    if (name.length() != 2 || !name.startsWith("-")) continue;
                    this.posixOptionsByKeyMap.put(Character.valueOf(name.charAt(1)), option);
                }
                this.options.add(option);
                this.addOptionNegative(option, tracer);
                if (option.scopeType() == ScopeType.INHERIT) {
                    HashSet<CommandLine> done = new HashSet<CommandLine>();
                    for (CommandLine sub : this.subcommands().values()) {
                        if (done.contains(sub)) continue;
                        sub.getCommandSpec().addOption(((OptionSpec.Builder)OptionSpec.builder(option).inherited(true)).build());
                        done.add(sub);
                    }
                }
                return this.addArg(option);
            }

            private void addOptionNegative(OptionSpec option, Tracer tracer) {
                if (option.negatable()) {
                    if (!option.typeInfo().isBoolean() && !option.typeInfo().isOptional()) {
                        throw new InitializationException("Only boolean options can be negatable, but " + option + " is of type " + option.typeInfo().getClassName());
                    }
                    for (String name : this.interpolator.interpolate(option.names())) {
                        String negatedName = this.negatableOptionTransformer().makeNegative(name, this);
                        if (name.equals(negatedName)) {
                            tracer.debug("Option %s is negatable, but has no negative form.%n", name);
                            continue;
                        }
                        tracer.debug("Option %s is negatable, registering negative name %s.%n", name, negatedName);
                        String existingName = this.negatedOptionsByNameMap.getCaseSensitiveKey(negatedName);
                        OptionSpec existing = this.negatedOptionsByNameMap.put(negatedName, option);
                        if (existing == null) {
                            existingName = this.optionsByNameMap.getCaseSensitiveKey(negatedName);
                            existing = this.optionsByNameMap.get(negatedName);
                        }
                        if (existing == null) continue;
                        throw DuplicateOptionAnnotationsException.create(existingName, option, existing);
                    }
                }
            }

            private void resetNegativeOptionNames() {
                Tracer tracer = new Tracer();
                tracer.debug("Clearing negatedOptionsByNameMap...%n", new Object[0]);
                this.negatedOptionsByNameMap.clear();
                for (OptionSpec option : this.options) {
                    this.addOptionNegative(option, tracer);
                }
            }

            public CommandSpec addPositional(PositionalParamSpec positional) {
                this.positionalParameters.add(positional);
                this.addArg(positional);
                if (positional.index().isUnresolved()) {
                    positional.index = Range.valueOf(this.interpolator.interpolate(positional.index().originalValue));
                    positional.initCapacity();
                }
                this.adjustRelativeIndices(positional);
                if (positional.scopeType() == ScopeType.INHERIT) {
                    HashSet<CommandLine> subCmds = new HashSet<CommandLine>(this.subcommands().values());
                    for (CommandLine sub : subCmds) {
                        sub.getCommandSpec().addPositional(((PositionalParamSpec.Builder)PositionalParamSpec.builder(positional).inherited(true)).build());
                    }
                }
                return this;
            }

            private void adjustRelativeIndices(PositionalParamSpec newlyAdded) {
                Collections.sort(this.positionalParameters, new PositionalParametersSorter());
                for (int i = this.positionalParameters.indexOf(newlyAdded); i < this.positionalParameters.size(); ++i) {
                    int previousMax;
                    PositionalParamSpec adjust = this.positionalParameters.get(i);
                    Range index = adjust.index();
                    if (!index.isRelative()) continue;
                    int n = previousMax = i == 0 ? -1 : this.positionalParameters.get(i - 1).index().max();
                    int max = i == 0 ? 0 : (previousMax == Integer.MAX_VALUE ? previousMax : previousMax + 1);
                    max = index.isRelativeToAnchor() ? Math.max(max, index.anchor()) : max;
                    adjust.index = new Range(max, max, index.isVariable(), index.isUnspecified, index.originalValue);
                    adjust.initCapacity();
                }
            }

            private CommandSpec addArg(ArgSpec arg) {
                this.args.add(arg);
                arg.messages(this.usageMessage().messages());
                arg.commandSpec = this;
                if (arg.arity().isUnresolved()) {
                    arg.arity = Range.valueOf(this.interpolator.interpolate(arg.arity().originalValue));
                }
                if (arg.required() && arg.group() == null && !arg.inherited()) {
                    this.requiredArgs.add(arg);
                }
                return this;
            }

            public CommandSpec remove(ArgSpec arg) {
                if (arg.group() != null) {
                    throw new UnsupportedOperationException("Cannot remove ArgSpec that is part of an ArgGroup");
                }
                int removed = CommandSpec.remove(arg, this.optionsByNameMap);
                removed += CommandSpec.remove(arg, this.posixOptionsByKeyMap);
                removed += CommandSpec.remove(arg, this.negatedOptionsByNameMap);
                this.requiredArgs.remove(arg);
                this.options.remove(arg);
                if (this.positionalParameters.remove(arg)) {
                    ++removed;
                }
                this.args.remove(arg);
                if (removed == 0) {
                    throw new NoSuchElementException(String.valueOf(arg));
                }
                arg.commandSpec = null;
                arg.messages(null);
                return this;
            }

            private static <T> int remove(ArgSpec arg, Map<T, OptionSpec> map) {
                int result = 0;
                Iterator<Map.Entry<T, OptionSpec>> iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<T, OptionSpec> entry = iterator.next();
                    if (entry.getValue() != arg) continue;
                    iterator.remove();
                    ++result;
                }
                return result;
            }

            public CommandSpec addArgGroup(ArgGroupSpec group) {
                return this.addArgGroup(group, new HashSet<OptionSpec>(), new HashSet<PositionalParamSpec>());
            }

            private CommandSpec addArgGroup(ArgGroupSpec group, Set<OptionSpec> groupOptions, Set<PositionalParamSpec> groupPositionals) {
                Assert.notNull(group, "group");
                if (group.parentGroup() != null) {
                    throw new InitializationException("Groups that are part of another group should not be added to a command. Add only the top-level group.");
                }
                this.check(group, this.flatten(this.groups, new HashSet<ArgGroupSpec>()));
                this.groups.add(group);
                this.addGroupArgsToCommand(group, new HashMap<String, ArgGroupSpec>(), groupOptions, groupPositionals);
                return this;
            }

            private void addGroupArgsToCommand(ArgGroupSpec group, Map<String, ArgGroupSpec> added, Set<OptionSpec> groupOptions, Set<PositionalParamSpec> groupPositionals) {
                HashMap<String, OptionSpec> options = new HashMap<String, OptionSpec>();
                for (ArgSpec arg : group.args()) {
                    if (arg.isOption()) {
                        String[] names;
                        for (String name : names = this.interpolator.interpolate(((OptionSpec)arg).names())) {
                            ArgGroupSpec other = added.get(name);
                            if (other == null) continue;
                            if (other == group) {
                                throw DuplicateOptionAnnotationsException.create(name, arg, (ArgSpec)options.get(name));
                            }
                            throw new DuplicateNameException("An option cannot be in multiple groups but " + name + " is in " + group.synopsisUnit() + " and " + added.get(name).synopsisUnit() + ". Refactor to avoid this. For example, (-a | (-a -b)) can be rewritten as (-a [-b]), and (-a -b | -a -c) can be rewritten as (-a (-b | -c)).");
                        }
                        for (String name : names) {
                            added.put(name, group);
                        }
                        for (String name : names) {
                            options.put(name, (OptionSpec)arg);
                        }
                        groupOptions.add((OptionSpec)arg);
                    } else {
                        groupPositionals.add((PositionalParamSpec)arg);
                    }
                    this.add(arg);
                }
                for (ArgGroupSpec sub : group.subgroups()) {
                    this.addGroupArgsToCommand(sub, added, groupOptions, groupPositionals);
                }
            }

            private Set<ArgGroupSpec> flatten(Collection<ArgGroupSpec> groups, Set<ArgGroupSpec> result) {
                for (ArgGroupSpec group : groups) {
                    this.flatten(group, result);
                }
                return result;
            }

            private Set<ArgGroupSpec> flatten(ArgGroupSpec group, Set<ArgGroupSpec> result) {
                result.add(group);
                for (ArgGroupSpec sub : group.subgroups()) {
                    this.flatten(sub, result);
                }
                return result;
            }

            private void check(ArgGroupSpec group, Set<ArgGroupSpec> existing) {
                if (existing.contains(group)) {
                    throw new InitializationException("The specified group " + group.synopsisUnit() + " has already been added to the " + this.qualifiedName() + " command.");
                }
                for (ArgGroupSpec sub : group.subgroups()) {
                    this.check(sub, existing);
                }
            }

            public CommandSpec addMixin(String name, CommandSpec mixin, IAnnotatedElement annotatedElement) {
                CommandSpec result = this.addMixin(name, mixin);
                this.mixinAnnotatedElements.put(this.interpolator.interpolate(name), annotatedElement);
                return result;
            }

            public CommandSpec addMixin(String name, CommandSpec mixin) {
                this.mixins.put(this.interpolator.interpolate(name), mixin);
                this.initName(this.interpolator.interpolateCommandName(mixin.name()));
                this.initFrom(mixin);
                for (Map.Entry<String, CommandLine> entry : mixin.subcommands().entrySet()) {
                    this.addSubcommand(entry.getKey(), entry.getValue());
                }
                LinkedHashSet<OptionSpec> options = new LinkedHashSet<OptionSpec>(mixin.options());
                LinkedHashSet<PositionalParamSpec> positionals = new LinkedHashSet<PositionalParamSpec>(mixin.positionalParameters());
                for (ArgGroupSpec argGroupSpec : mixin.argGroups()) {
                    HashSet<OptionSpec> groupOptions = new HashSet<OptionSpec>();
                    HashSet<PositionalParamSpec> groupPositionals = new HashSet<PositionalParamSpec>();
                    this.addArgGroup(argGroupSpec, groupOptions, groupPositionals);
                    options.removeAll(groupOptions);
                    positionals.removeAll(groupPositionals);
                }
                for (OptionSpec optionSpec : options) {
                    this.addOption(optionSpec);
                }
                for (PositionalParamSpec paramSpec : positionals) {
                    this.addPositional(paramSpec);
                }
                return this;
            }

            private void initFrom(CommandSpec spec) {
                this.initExitCodeOnSuccess(spec.exitCodeOnSuccess());
                this.initExitCodeOnUsageHelp(spec.exitCodeOnUsageHelp());
                this.initExitCodeOnVersionHelp(spec.exitCodeOnVersionHelp());
                this.initExitCodeOnInvalidInput(spec.exitCodeOnInvalidInput());
                this.initExitCodeOnExecutionException(spec.exitCodeOnExecutionException());
                this.parser.initSeparator(spec.parser.separator());
                this.initVersion(spec.version());
                this.initHelpCommand(spec.helpCommand());
                this.initVersionProvider(spec.versionProvider());
                this.initDefaultValueProvider(spec.defaultValueProvider());
                this.initModelTransformer(spec.modelTransformer());
                this.initPreprocessor(spec.preprocessor());
                this.usageMessage.initFromMixin(spec.usageMessage, this);
                this.initSubcommandsRepeatable(spec.subcommandsRepeatable());
            }

            public CommandSpec addUnmatchedArgsBinding(UnmatchedArgsBinding spec) {
                this.unmatchedArgs.add(spec);
                this.parser().unmatchedArgumentsAllowed(true);
                return this;
            }

            public CommandSpec addSpecElement(IAnnotatedElement spec) {
                this.specElements.add(spec);
                return this;
            }

            public CommandSpec addParentCommandElement(IAnnotatedElement spec) {
                this.parentCommandElements.add(spec);
                return this;
            }

            void injectParentCommand(CommandUserObject commandUserObject) {
                try {
                    for (IAnnotatedElement injectionTarget : this.parentCommandElements()) {
                        injectionTarget.setter().set(commandUserObject.getInstance());
                    }
                }
                catch (Exception ex) {
                    throw new InitializationException("Unable to initialize @ParentCommand field: " + ex, ex);
                }
            }

            public Map<String, CommandSpec> mixins() {
                return Collections.unmodifiableMap(this.mixins);
            }

            public Map<String, IAnnotatedElement> mixinAnnotatedElements() {
                return Collections.unmodifiableMap(this.mixinAnnotatedElements);
            }

            public List<OptionSpec> options() {
                return Collections.unmodifiableList(this.options);
            }

            public List<PositionalParamSpec> positionalParameters() {
                return Collections.unmodifiableList(this.positionalParameters);
            }

            public List<ArgGroupSpec> argGroups() {
                return Collections.unmodifiableList(this.groups);
            }

            public Map<String, OptionSpec> optionsMap() {
                return Collections.unmodifiableMap(this.optionsByNameMap);
            }

            public Map<String, OptionSpec> negatedOptionsMap() {
                return Collections.unmodifiableMap(this.negatedOptionsByNameMap);
            }

            public Map<Character, OptionSpec> posixOptionsMap() {
                return Collections.unmodifiableMap(this.posixOptionsByKeyMap);
            }

            public List<ArgSpec> requiredArgs() {
                return Collections.unmodifiableList(this.requiredArgs);
            }

            public List<UnmatchedArgsBinding> unmatchedArgsBindings() {
                return Collections.unmodifiableList(this.unmatchedArgs);
            }

            public List<IAnnotatedElement> specElements() {
                return Collections.unmodifiableList(this.specElements);
            }

            public List<IAnnotatedElement> parentCommandElements() {
                return Collections.unmodifiableList(this.parentCommandElements);
            }

            public String name() {
                return this.interpolator.interpolateCommandName(this.name == null ? DEFAULT_COMMAND_NAME : this.name);
            }

            public String[] aliases() {
                return this.interpolator.interpolate(this.aliases.toArray(new String[0]));
            }

            public Set<String> names() {
                LinkedHashSet<String> result = new LinkedHashSet<String>();
                result.add(this.name());
                result.addAll(Arrays.asList(this.aliases()));
                return result;
            }

            public List<ArgSpec> args() {
                return Collections.unmodifiableList(this.args);
            }

            Object[] commandMethodParamValues() {
                Object[] values = new Object[this.methodParams.length];
                CommandSpec autoHelpMixin = this.mixins.get("mixinStandardHelpOptions");
                int argIndex = autoHelpMixin == null || autoHelpMixin.inherited() ? 0 : 2;
                block0: for (int i = 0; i < this.methodParams.length; ++i) {
                    if (this.methodParams[i].isAnnotationPresent(Mixin.class)) {
                        String name = this.methodParams[i].getAnnotation(Mixin.class).name();
                        CommandSpec mixin = this.mixins.get(CommandLine.empty(name) ? this.methodParams[i].name : name);
                        values[i] = mixin.userObject.getInstance();
                        argIndex += mixin.args.size();
                        continue;
                    }
                    if (this.methodParams[i].isAnnotationPresent(ArgGroup.class)) {
                        values[i] = null;
                        for (ArgGroupSpec group : this.groups) {
                            if (!group.typeInfo.equals(this.methodParams[i].typeInfo)) continue;
                            values[i] = group.userObjectOr(null);
                            argIndex += group.argCount();
                            continue block0;
                        }
                        continue;
                    }
                    values[i] = this.args.get(argIndex++).getValue();
                }
                return values;
            }

            public String qualifiedName() {
                return this.qualifiedName(" ");
            }

            public String qualifiedName(String separator) {
                String result = this.name();
                if (this.parent() != null) {
                    result = this.parent().qualifiedName(separator) + separator + result;
                }
                return result;
            }

            public String[] version() {
                if (this.versionProvider != null) {
                    try {
                        return this.interpolator.interpolate(this.versionProvider.getVersion());
                    }
                    catch (Exception ex) {
                        String msg = "Could not get version info from " + this.versionProvider + ": " + ex;
                        throw new ExecutionException(this.commandLine, msg, ex);
                    }
                }
                return this.interpolator.interpolate(this.version == null ? UsageMessageSpec.DEFAULT_MULTI_LINE : this.version);
            }

            public IVersionProvider versionProvider() {
                return this.versionProvider;
            }

            public boolean helpCommand() {
                return this.isHelpCommand == null ? DEFAULT_IS_HELP_COMMAND : this.isHelpCommand;
            }

            public int exitCodeOnSuccess() {
                return this.exitCodeOnSuccess == null ? 0 : this.exitCodeOnSuccess;
            }

            public int exitCodeOnUsageHelp() {
                return this.exitCodeOnUsageHelp == null ? 0 : this.exitCodeOnUsageHelp;
            }

            public int exitCodeOnVersionHelp() {
                return this.exitCodeOnVersionHelp == null ? 0 : this.exitCodeOnVersionHelp;
            }

            public int exitCodeOnInvalidInput() {
                return this.exitCodeOnInvalidInput == null ? 2 : this.exitCodeOnInvalidInput;
            }

            public int exitCodeOnExecutionException() {
                return this.exitCodeOnExecutionException == null ? 1 : this.exitCodeOnExecutionException;
            }

            public INegatableOptionTransformer negatableOptionTransformer() {
                return this.negatableOptionTransformer;
            }

            public boolean mixinStandardHelpOptions() {
                return this.mixins.containsKey("mixinStandardHelpOptions");
            }

            public boolean subcommandsRepeatable() {
                return this.subcommandsRepeatable == null ? DEFAULT_SUBCOMMANDS_REPEATABLE : this.subcommandsRepeatable;
            }

            public String toString() {
                return this.toString == null ? "command '" + this.name + "' (user object: " + this.userObject + ")" : this.toString;
            }

            public CommandSpec name(String name) {
                this.name = name;
                return this;
            }

            public CommandSpec aliases(String ... aliases) {
                this.aliases = new LinkedHashSet<String>(Arrays.asList(aliases == null ? new String[]{} : aliases));
                return this;
            }

            public IDefaultValueProvider defaultValueProvider() {
                return this.defaultValueProvider;
            }

            public CommandSpec defaultValueProvider(IDefaultValueProvider defaultValueProvider) {
                this.defaultValueProvider = defaultValueProvider;
                return this;
            }

            public CommandSpec version(String ... version) {
                this.version = version;
                return this;
            }

            public CommandSpec versionProvider(IVersionProvider versionProvider) {
                this.versionProvider = versionProvider;
                return this;
            }

            public CommandSpec helpCommand(boolean newValue) {
                this.isHelpCommand = newValue;
                return this;
            }

            public CommandSpec exitCodeOnSuccess(int newValue) {
                this.exitCodeOnSuccess = newValue;
                return this;
            }

            public CommandSpec exitCodeOnUsageHelp(int newValue) {
                this.exitCodeOnUsageHelp = newValue;
                return this;
            }

            public CommandSpec exitCodeOnVersionHelp(int newValue) {
                this.exitCodeOnVersionHelp = newValue;
                return this;
            }

            public CommandSpec exitCodeOnInvalidInput(int newValue) {
                this.exitCodeOnInvalidInput = newValue;
                return this;
            }

            public CommandSpec exitCodeOnExecutionException(int newValue) {
                this.exitCodeOnExecutionException = newValue;
                return this;
            }

            public boolean inherited() {
                return this.inherited;
            }

            public ScopeType scopeType() {
                return this.scopeType == null ? ScopeType.LOCAL : this.scopeType;
            }

            public CommandSpec scopeType(ScopeType scopeType) {
                if (this.scopeType == ScopeType.INHERIT && scopeType != ScopeType.INHERIT && !this.subcommands().isEmpty()) {
                    throw new IllegalStateException("Cannot un-inherit: subcommands have already been initialized with values from this command");
                }
                this.scopeType = scopeType;
                if (scopeType == ScopeType.INHERIT) {
                    this.updatedSubcommandsToInheritFrom(this);
                }
                return this;
            }

            public IModelTransformer modelTransformer() {
                return this.modelTransformer;
            }

            public CommandSpec modelTransformer(IModelTransformer modelTransformer) {
                this.modelTransformer = modelTransformer;
                return this;
            }

            public IParameterPreprocessor preprocessor() {
                return this.preprocessor;
            }

            public CommandSpec preprocessor(IParameterPreprocessor preprocessor) {
                this.preprocessor = Assert.notNull(preprocessor, "preprocessor");
                return this;
            }

            public CommandSpec negatableOptionTransformer(INegatableOptionTransformer newValue) {
                Tracer tracer = new Tracer();
                tracer.debug("Replacing negatableOptionTransformer %s with %s%n", this.negatableOptionTransformer, newValue);
                this.negatableOptionTransformer = newValue;
                this.resetNegativeOptionNames();
                return this;
            }

            public CommandSpec mixinStandardHelpOptions(boolean newValue) {
                block7: {
                    block6: {
                        if (!newValue) break block6;
                        CommandSpec mixin = CommandSpec.forAnnotatedObject(new AutoHelpMixin(), new DefaultFactory());
                        boolean overlap = false;
                        for (String key : mixin.optionsMap().keySet()) {
                            if (!this.optionsMap().containsKey(key)) continue;
                            overlap = true;
                            break;
                        }
                        if (!overlap) {
                            mixin.inherited = this.inherited();
                            this.addMixin("mixinStandardHelpOptions", mixin);
                        }
                        if (this.scopeType() != ScopeType.INHERIT && !this.inherited()) break block7;
                        for (CommandLine sub : new HashSet<CommandLine>(this.subcommands().values())) {
                            sub.getCommandSpec().mixinStandardHelpOptions(newValue);
                        }
                        break block7;
                    }
                    CommandSpec helpMixin = this.mixins.remove("mixinStandardHelpOptions");
                    if (helpMixin != null) {
                        this.options.removeAll(helpMixin.options);
                        for (OptionSpec option : helpMixin.options()) {
                            for (String name : this.interpolator.interpolate(option.names())) {
                                this.optionsByNameMap.remove(name);
                                if (name.length() != 2 || !name.startsWith("-")) continue;
                                this.posixOptionsByKeyMap.remove(Character.valueOf(name.charAt(1)));
                            }
                        }
                    }
                }
                return this;
            }

            public CommandSpec subcommandsRepeatable(boolean subcommandsRepeatable) {
                this.subcommandsRepeatable = subcommandsRepeatable;
                return this;
            }

            public CommandSpec withToString(String newValue) {
                this.toString = newValue;
                return this;
            }

            public void updateCommandAttributes(Command cmd, IFactory factory) {
                this.parser().updateSeparator(this.interpolator.interpolate(cmd.separator()));
                this.updateExitCodeOnSuccess(cmd.exitCodeOnSuccess());
                this.updateExitCodeOnUsageHelp(cmd.exitCodeOnUsageHelp());
                this.updateExitCodeOnVersionHelp(cmd.exitCodeOnVersionHelp());
                this.updateExitCodeOnInvalidInput(cmd.exitCodeOnInvalidInput());
                this.updateExitCodeOnExecutionException(cmd.exitCodeOnExecutionException());
                this.aliases(cmd.aliases());
                this.updateName(cmd.name());
                this.updateVersion(cmd.version());
                this.updateHelpCommand(cmd.helpCommand());
                this.updateSubcommandsRepeatable(cmd.subcommandsRepeatable());
                this.updateAddMethodSubcommands(cmd.addMethodSubcommands());
                this.usageMessage().updateFromCommand(cmd, this, factory != null);
                this.updateScopeType(cmd.scope());
                if (factory != null) {
                    this.updateModelTransformer(cmd.modelTransformer(), factory);
                    this.updateVersionProvider(cmd.versionProvider(), factory);
                    this.initDefaultValueProvider(cmd.defaultValueProvider(), factory);
                    this.updatePreprocessor(cmd.preprocessor(), factory);
                }
            }

            void initName(String value) {
                if (Model.initializable(this.name, value, DEFAULT_COMMAND_NAME)) {
                    this.name = value;
                }
            }

            void initHelpCommand(boolean value) {
                if (Model.initializable(this.isHelpCommand, value, CommandSpec.DEFAULT_IS_HELP_COMMAND)) {
                    this.isHelpCommand = value;
                }
            }

            void initVersion(String[] value) {
                if (Model.initializable(this.version, value, UsageMessageSpec.DEFAULT_MULTI_LINE)) {
                    this.version = (String[])value.clone();
                }
            }

            void initVersionProvider(IVersionProvider value) {
                if (this.versionProvider == null) {
                    this.versionProvider = value;
                }
            }

            void initDefaultValueProvider(IDefaultValueProvider value) {
                if (this.defaultValueProvider == null) {
                    this.defaultValueProvider = value;
                }
            }

            void initDefaultValueProvider(Class<? extends IDefaultValueProvider> value, IFactory factory) {
                if (Model.initializable(this.defaultValueProvider, value, NoDefaultProvider.class)) {
                    this.defaultValueProvider = DefaultFactory.createDefaultValueProvider(factory, value);
                }
            }

            void initSubcommandsRepeatable(boolean value) {
                if (Model.initializable(this.subcommandsRepeatable, value, CommandSpec.DEFAULT_SUBCOMMANDS_REPEATABLE)) {
                    this.subcommandsRepeatable = value;
                }
            }

            void initExitCodeOnSuccess(int exitCode) {
                if (Model.initializable(this.exitCodeOnSuccess, exitCode, 0)) {
                    this.exitCodeOnSuccess = exitCode;
                }
            }

            void initExitCodeOnUsageHelp(int exitCode) {
                if (Model.initializable(this.exitCodeOnUsageHelp, exitCode, 0)) {
                    this.exitCodeOnUsageHelp = exitCode;
                }
            }

            void initExitCodeOnVersionHelp(int exitCode) {
                if (Model.initializable(this.exitCodeOnVersionHelp, exitCode, 0)) {
                    this.exitCodeOnVersionHelp = exitCode;
                }
            }

            void initExitCodeOnInvalidInput(int exitCode) {
                if (Model.initializable(this.exitCodeOnInvalidInput, exitCode, 2)) {
                    this.exitCodeOnInvalidInput = exitCode;
                }
            }

            void initExitCodeOnExecutionException(int exitCode) {
                if (Model.initializable(this.exitCodeOnExecutionException, exitCode, 1)) {
                    this.exitCodeOnExecutionException = exitCode;
                }
            }

            void updateName(String value) {
                if (Model.isNonDefault(value, DEFAULT_COMMAND_NAME)) {
                    this.name = value;
                }
            }

            void initModelTransformer(IModelTransformer value) {
                if (this.modelTransformer == null) {
                    this.modelTransformer = value;
                }
            }

            void updateModelTransformer(Class<? extends IModelTransformer> value, IFactory factory) {
                if (Model.isNonDefault(value, NoOpModelTransformer.class)) {
                    this.modelTransformer = DefaultFactory.create(factory, value);
                }
            }

            void initPreprocessor(IParameterPreprocessor value) {
                if (this.preprocessor instanceof NoOpParameterPreprocessor) {
                    this.preprocessor = Assert.notNull(value, "preprocessor");
                }
            }

            void updatePreprocessor(Class<? extends IParameterPreprocessor> value, IFactory factory) {
                if (Model.isNonDefault(value, NoOpParameterPreprocessor.class)) {
                    this.preprocessor = DefaultFactory.create(factory, value);
                }
            }

            void updateHelpCommand(boolean value) {
                if (Model.isNonDefault(value, CommandSpec.DEFAULT_IS_HELP_COMMAND)) {
                    this.isHelpCommand = value;
                }
            }

            void updateSubcommandsRepeatable(boolean value) {
                if (Model.isNonDefault(value, CommandSpec.DEFAULT_SUBCOMMANDS_REPEATABLE)) {
                    this.subcommandsRepeatable = value;
                }
            }

            void updateAddMethodSubcommands(boolean value) {
                if (Model.isNonDefault(value, CommandSpec.DEFAULT_IS_ADD_METHOD_SUBCOMMANDS)) {
                    this.isAddMethodSubcommands = value;
                }
            }

            void updateVersion(String[] value) {
                if (Model.isNonDefault(value, UsageMessageSpec.DEFAULT_MULTI_LINE)) {
                    this.version = (String[])value.clone();
                }
            }

            void updateVersionProvider(Class<? extends IVersionProvider> value, IFactory factory) {
                if (Model.isNonDefault(value, NoVersionProvider.class)) {
                    this.versionProvider = DefaultFactory.createVersionProvider(factory, value);
                }
            }

            void updateExitCodeOnSuccess(int exitCode) {
                if (Model.isNonDefault(exitCode, 0)) {
                    this.exitCodeOnSuccess = exitCode;
                }
            }

            void updateExitCodeOnUsageHelp(int exitCode) {
                if (Model.isNonDefault(exitCode, 0)) {
                    this.exitCodeOnUsageHelp = exitCode;
                }
            }

            void updateExitCodeOnVersionHelp(int exitCode) {
                if (Model.isNonDefault(exitCode, 0)) {
                    this.exitCodeOnVersionHelp = exitCode;
                }
            }

            void updateExitCodeOnInvalidInput(int exitCode) {
                if (Model.isNonDefault(exitCode, 2)) {
                    this.exitCodeOnInvalidInput = exitCode;
                }
            }

            void updateExitCodeOnExecutionException(int exitCode) {
                if (Model.isNonDefault(exitCode, 1)) {
                    this.exitCodeOnExecutionException = exitCode;
                }
            }

            void updateScopeType(ScopeType scopeType) {
                if (this.scopeType == null) {
                    this.scopeType(scopeType);
                }
            }

            public OptionSpec findOption(char shortName) {
                return CommandSpec.findOption(shortName, this.options());
            }

            public OptionSpec findOption(String name) {
                return CommandSpec.findOption(name, this.options());
            }

            static OptionSpec findOption(char shortName, Iterable<OptionSpec> options) {
                for (OptionSpec option : options) {
                    for (String name : option.names()) {
                        if (name.length() == 2 && name.charAt(0) == '-' && name.charAt(1) == shortName) {
                            return option;
                        }
                        if (name.length() != 1 || name.charAt(0) != shortName) continue;
                        return option;
                    }
                }
                return null;
            }

            static OptionSpec findOption(String name, List<OptionSpec> options) {
                for (OptionSpec option : options) {
                    for (String prefixed : option.names()) {
                        if (!prefixed.equals(name) && !CommandSpec.stripPrefix(prefixed).equals(name)) continue;
                        return option;
                    }
                }
                return null;
            }

            static String stripPrefix(String prefixed) {
                for (int i = 0; i < prefixed.length(); ++i) {
                    if (!Character.isJavaIdentifierPart(prefixed.charAt(i))) continue;
                    return prefixed.substring(i);
                }
                return prefixed;
            }

            List<String> findVisibleOptionNamesWithPrefix(String prefix) {
                ArrayList<String> result = new ArrayList<String>();
                for (OptionSpec option : this.options()) {
                    for (String name : option.names()) {
                        if (option.hidden() || !CommandSpec.stripPrefix(name).startsWith(prefix)) continue;
                        result.add(name);
                    }
                }
                return result;
            }

            boolean resemblesOption(String arg, Tracer tracer) {
                if (arg == null) {
                    return false;
                }
                if (arg.length() == 1) {
                    if (tracer != null && tracer.isDebug()) {
                        tracer.debug("Single-character arguments that don't match known options are considered positional parameters%n", arg);
                    }
                    return false;
                }
                try {
                    Long.decode(arg);
                    return false;
                }
                catch (NumberFormatException numberFormatException) {
                    try {
                        Double.parseDouble(arg);
                        return false;
                    }
                    catch (NumberFormatException numberFormatException2) {
                        boolean result;
                        if (this.options().isEmpty()) {
                            boolean result2 = arg.startsWith("-");
                            if (tracer != null && tracer.isDebug()) {
                                tracer.debug("'%s' %s an option%n", arg, result2 ? "resembles" : "doesn't resemble");
                            }
                            return result2;
                        }
                        int count = 0;
                        for (String optionName : this.optionsMap().keySet()) {
                            for (int i = 0; i < arg.length() && optionName.length() > i && arg.charAt(i) == optionName.charAt(i); ++i) {
                                ++count;
                            }
                        }
                        boolean bl = result = count > 0 && count * 10 >= this.optionsMap().size() * 9;
                        if (tracer != null && tracer.isDebug()) {
                            tracer.debug("'%s' %s an option: %d matching prefix chars out of %d option names%n", arg, result ? "resembles" : "doesn't resemble", count, this.optionsMap().size());
                        }
                        return result;
                    }
                }
            }

            static /* synthetic */ TypedMember[] access$15802(CommandSpec x0, TypedMember[] x1) {
                x0.methodParams = x1;
                return x1;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class CaseAwareLinkedMap<K, V>
        extends AbstractMap<K, V> {
            private final LinkedHashMap<K, V> targetMap = new LinkedHashMap();
            private final HashMap<K, K> keyMap = new HashMap();
            private final Set<K> keySet = new CaseAwareKeySet();
            private boolean caseInsensitive = false;
            private final Locale locale;

            public CaseAwareLinkedMap() {
                this(Locale.ENGLISH);
            }

            public CaseAwareLinkedMap(Locale locale) {
                this.locale = locale;
            }

            public CaseAwareLinkedMap(CaseAwareLinkedMap<? extends K, ? extends V> map) {
                this.targetMap.putAll(map.targetMap);
                this.keyMap.putAll(map.keyMap);
                this.caseInsensitive = map.caseInsensitive;
                this.locale = map.locale;
            }

            static boolean isCaseConvertible(Class<?> clazz) {
                return clazz == String.class || clazz == Character.class;
            }

            private K toLowerCase(Object caseSensitiveKey) {
                if (caseSensitiveKey.getClass() == String.class) {
                    return (K)((String)caseSensitiveKey).toLowerCase(this.locale);
                }
                if (caseSensitiveKey.getClass() == Character.class) {
                    return (K)Character.valueOf(Character.toLowerCase(((Character)caseSensitiveKey).charValue()));
                }
                throw new UnsupportedOperationException("Unsupported case-conversion for key " + caseSensitiveKey.getClass());
            }

            public boolean isCaseInsensitive() {
                return this.caseInsensitive;
            }

            public void setCaseInsensitive(boolean caseInsensitive) {
                if (!this.isCaseInsensitive() && caseInsensitive) {
                    for (K key : this.targetMap.keySet()) {
                        K duplicatedKey = this.keyMap.put(key != null ? (Object)this.toLowerCase(key) : null, key);
                        if (duplicatedKey == null) continue;
                        throw new DuplicateNameException("Duplicated keys: " + duplicatedKey + " and " + key);
                    }
                } else if (this.isCaseInsensitive()) {
                    this.keyMap.clear();
                }
                this.caseInsensitive = caseInsensitive;
            }

            public Locale getLocale() {
                return this.locale;
            }

            public K getCaseSensitiveKey(K caseInsensitiveKey) {
                if (caseInsensitiveKey != null && this.caseInsensitive) {
                    return this.keyMap.get(this.toLowerCase(caseInsensitiveKey));
                }
                return caseInsensitiveKey;
            }

            @Override
            public int size() {
                return this.targetMap.size();
            }

            @Override
            public boolean containsKey(Object key) {
                if (key != null && this.caseInsensitive) {
                    if (!CaseAwareLinkedMap.isCaseConvertible(key.getClass())) {
                        return false;
                    }
                    return this.keyMap.containsKey(this.toLowerCase(key));
                }
                return this.targetMap.containsKey(key);
            }

            @Override
            public boolean containsValue(Object value) {
                return this.targetMap.containsValue(value);
            }

            @Override
            public V get(Object key) {
                if (key != null && CaseAwareLinkedMap.isCaseConvertible(key.getClass()) && this.caseInsensitive) {
                    K caseSensitiveKey = this.keyMap.get(this.toLowerCase(key));
                    if (caseSensitiveKey == null) {
                        return null;
                    }
                    return this.targetMap.get(caseSensitiveKey);
                }
                return this.targetMap.get(key);
            }

            @Override
            public V put(K key, V value) {
                K caseSensitiveKey;
                if (key != null && this.caseInsensitive && (caseSensitiveKey = this.keyMap.put(this.toLowerCase(key), key)) != null) {
                    Object removedValue = this.targetMap.remove(caseSensitiveKey);
                    this.targetMap.put(key, value);
                    return removedValue;
                }
                return this.targetMap.put(key, value);
            }

            @Override
            public V remove(Object key) {
                if (key != null && this.caseInsensitive) {
                    K caseSensitiveKey = this.keyMap.remove(this.toLowerCase(key));
                    if (caseSensitiveKey == null) {
                        return null;
                    }
                    return this.targetMap.remove(caseSensitiveKey);
                }
                return this.targetMap.remove(key);
            }

            @Override
            public void clear() {
                this.targetMap.clear();
                this.keyMap.clear();
            }

            @Override
            public Set<K> keySet() {
                return this.keySet;
            }

            @Override
            public Collection<V> values() {
                return this.targetMap.values();
            }

            @Override
            public Set<Map.Entry<K, V>> entrySet() {
                return this.targetMap.entrySet();
            }

            /*
             * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
             */
            class CaseAwareKeySet
            extends AbstractSet<K> {
                CaseAwareKeySet() {
                }

                @Override
                public boolean contains(Object o) {
                    return CaseAwareLinkedMap.this.containsKey(o);
                }

                @Override
                public Iterator<K> iterator() {
                    return CaseAwareLinkedMap.this.targetMap.keySet().iterator();
                }

                @Override
                public int size() {
                    return CaseAwareLinkedMap.this.targetMap.keySet().size();
                }
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static interface ISetter {
            public <T> T set(T var1) throws Exception;
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static interface IGetter {
            public <T> T get() throws Exception;
        }

        public static interface IScope
        extends IGetter,
        ISetter {
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static class Range
    implements Comparable<Range> {
        @Deprecated
        public final int min;
        @Deprecated
        public final int max;
        @Deprecated
        public final boolean isVariable;
        private final boolean isUnspecified;
        private final String originalValue;
        private final boolean relative;
        private final int anchor;

        public Range(int min, int max, boolean variable, boolean unspecified, String originalValue) {
            if (min < 0 || max < 0) {
                throw new InitializationException("Invalid negative range (min=" + min + ", max=" + max + ")");
            }
            if (min > max) {
                throw new InitializationException("Invalid range (min=" + min + ", max=" + max + ")");
            }
            this.min = min;
            this.max = max;
            this.isVariable = variable;
            this.isUnspecified = unspecified;
            this.originalValue = originalValue;
            boolean bl = this.relative = originalValue != null && originalValue.contains("+");
            this.anchor = this.relative ? ("+".equals(originalValue) ? Integer.MAX_VALUE : Range.parseInt(originalValue, Integer.MAX_VALUE)) : min;
        }

        public static Range optionArity(Field field) {
            return Range.optionArity(new Model.TypedMember(field));
        }

        private static Range optionArity(Model.IAnnotatedElement member) {
            return member.isAnnotationPresent(Option.class) ? Range.adjustForType(Range.valueOf(member.getAnnotation(Option.class).arity()), member) : new Range(0, 0, false, true, "0");
        }

        public static Range parameterArity(Field field) {
            return Range.parameterArity(new Model.TypedMember(field));
        }

        private static Range parameterArity(Model.IAnnotatedElement member) {
            if (member.isAnnotationPresent(Parameters.class)) {
                return Range.adjustForType(Range.valueOf(member.getAnnotation(Parameters.class).arity()), member);
            }
            return member.isMethodParameter() ? Range.adjustForType(Range.valueOf(""), member) : new Range(0, 0, false, true, "0");
        }

        public static Range parameterIndex(Field field) {
            return Range.parameterIndex(new Model.TypedMember(field));
        }

        private static Range parameterIndex(Model.IAnnotatedElement member) {
            if (member.isAnnotationPresent(Parameters.class)) {
                Range result = Range.valueOf(member.getAnnotation(Parameters.class).index());
                if (!result.isUnspecified) {
                    return result;
                }
            }
            if (member.isMethodParameter()) {
                int min = member.getMethodParamPosition();
                int max = member.isMultiValue() ? Integer.MAX_VALUE : min;
                return new Range(min, max, member.isMultiValue(), false, null);
            }
            return Range.defaultParameterIndex(member.getTypeInfo());
        }

        private static Range defaultParameterIndex(Model.ITypeInfo typeInfo) {
            return Range.valueOf(typeInfo.isMultiValue() ? "*" : "0+");
        }

        static Range adjustForType(Range result, Model.IAnnotatedElement member) {
            return result.isUnspecified ? Range.defaultArity(member) : result;
        }

        public static Range defaultArity(Field field) {
            return Range.defaultArity(new Model.TypedMember(field));
        }

        private static Range defaultArity(Model.IAnnotatedElement member) {
            if (member.isInteractive()) {
                return Range.valueOf("0").unspecified(true);
            }
            Model.ITypeInfo info = member.getTypeInfo();
            if (member.isAnnotationPresent(Option.class)) {
                boolean zeroArgs = info.isBoolean() || info.isMultiValue() && info.getAuxiliaryTypeInfos().get(0).isBoolean();
                return zeroArgs ? Range.valueOf("0").unspecified(true) : Range.valueOf("1").unspecified(true);
            }
            if (info.isMultiValue()) {
                return Range.valueOf("0..1").unspecified(true);
            }
            return Range.valueOf("1").unspecified(true);
        }

        @Deprecated
        public static Range defaultArity(Class<?> type) {
            return CommandLine.isBoolean(type) ? Range.valueOf("0").unspecified(true) : Range.valueOf("1").unspecified(true);
        }

        private int size() {
            return this.isRelative() ? 1 : 1 + this.max - this.min;
        }

        static Range parameterCapacity(Model.IAnnotatedElement member) {
            Range arity = Range.parameterArity(member);
            if (!member.isMultiValue()) {
                return arity;
            }
            Range index = Range.parameterIndex(member);
            return Range.parameterCapacity(arity, index);
        }

        private static Range parameterCapacity(Range arity, Range index) {
            if (arity.max == 0) {
                return arity;
            }
            if (index.size() == 1) {
                return arity;
            }
            if (index.isVariable) {
                return Range.valueOf(arity.min + "..*");
            }
            if (arity.size() == 1) {
                return Range.valueOf(arity.min * index.size() + "");
            }
            if (arity.isVariable) {
                return Range.valueOf(arity.min * index.size() + "..*");
            }
            return Range.valueOf(arity.min * index.size() + ".." + arity.max * index.size());
        }

        public static Range valueOf(String range) {
            boolean variable;
            int max;
            int min;
            if (range.contains("${")) {
                return new Range(0, 0, false, false, range);
            }
            boolean unspecified = (range = range.trim()).length() == 0 || range.startsWith("..");
            int dots = range.indexOf("..");
            if (dots >= 0) {
                min = Range.parseInt(range.substring(0, dots), 0);
                max = Range.parseInt(range.substring(dots + 2), Integer.MAX_VALUE);
                variable = max == Integer.MAX_VALUE;
            } else {
                max = Range.parseInt(range, Integer.MAX_VALUE);
                variable = !range.contains("+") && max == Integer.MAX_VALUE;
                min = variable ? 0 : max;
            }
            return new Range(min, max, variable, unspecified, unspecified ? null : range);
        }

        private static int parseInt(String str, int defaultValue) {
            try {
                int pos = str.indexOf(43);
                return Integer.parseInt(pos < 0 ? str : str.substring(0, str.indexOf(43)));
            }
            catch (Exception ex) {
                return defaultValue;
            }
        }

        public Range min(int newMin) {
            return new Range(newMin, Math.max(newMin, this.max), this.isVariable, this.isUnspecified, this.originalValue);
        }

        public Range max(int newMax) {
            return new Range(Math.min(this.min, newMax), newMax, this.isVariable, this.isUnspecified, this.originalValue);
        }

        public Range unspecified(boolean unspecified) {
            return new Range(this.min, this.max, this.isVariable, unspecified, this.originalValue);
        }

        public boolean isUnspecified() {
            return this.isUnspecified;
        }

        public boolean isUnresolved() {
            return this.originalValue != null && this.originalValue.contains("${");
        }

        public boolean isRelative() {
            return this.relative;
        }

        int anchor() {
            return this.anchor;
        }

        boolean isRelativeToAnchor() {
            return this.anchor != Integer.MAX_VALUE && this.isRelative();
        }

        public String originalValue() {
            return this.originalValue;
        }

        public int min() {
            return this.min;
        }

        public int max() {
            return this.max;
        }

        public boolean isVariable() {
            return this.isVariable;
        }

        public boolean contains(int value) {
            return this.min <= value && this.max >= value;
        }

        public boolean equals(Object object) {
            if (!(object instanceof Range)) {
                return false;
            }
            Range other = (Range)object;
            return other.max == this.max && other.min == this.min && other.isVariable == this.isVariable;
        }

        public int hashCode() {
            return ((629 + this.max) * 37 + this.min) * 37 + (this.isVariable ? 1 : 0);
        }

        public String toString() {
            if (this.isUnresolved()) {
                return this.originalValue;
            }
            if (this.min == this.max) {
                return this.relative && this.min == Integer.MAX_VALUE ? "+" : String.valueOf(this.min);
            }
            return this.min + ".." + (this.isVariable ? "*" : Integer.valueOf(this.max));
        }

        String internalToString() {
            if (this.isUnresolved()) {
                return this.originalValue;
            }
            return this.isRelative() ? this.originalValue + " (" + this.toString() + ")" : this.toString();
        }

        @Override
        public int compareTo(Range other) {
            int result;
            if (this.originalValue != null && other.originalValue != null && this.originalValue.equals(other.originalValue)) {
                return 0;
            }
            int n = this.anchor() < other.anchor() ? -1 : (result = this.anchor() == other.anchor() ? 0 : 1);
            if (result == 0) {
                int n2 = this.max < other.max ? -1 : (result = this.max == other.max ? 0 : 1);
            }
            if (result == 0 && this.isRelative() != other.isRelative()) {
                result = this.isRelative() ? 1 : -1;
            }
            return result;
        }

        boolean isValidForInteractiveArgs() {
            return this.min == 0 && (this.max == 0 || this.max == 1);
        }

        boolean overlaps(Range index) {
            return this.contains(index.min) || this.contains(index.max) || index.contains(this.min) || index.contains(this.max);
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class DefaultFactory
    implements IFactory {
        static Class<?> GROOVY_CLOSURE_CLASS = DefaultFactory.loadClosureClass();

        private DefaultFactory() {
        }

        private static Class<?> loadClosureClass() {
            try {
                return Class.forName("groovy.lang.Closure");
            }
            catch (Exception ignored) {
                return null;
            }
        }

        public <T> T create(Class<T> cls) throws Exception {
            if (GROOVY_CLOSURE_CLASS != null && GROOVY_CLOSURE_CLASS.isAssignableFrom(cls)) {
                Callable callable = (Callable)Callable.class.cast(cls.getConstructor(Object.class, Object.class).newInstance(null, null));
                try {
                    return (T)callable.call();
                }
                catch (Exception ex) {
                    throw new InitializationException("Error in Groovy closure: " + ex);
                }
            }
            if (cls.isInterface() && Collection.class.isAssignableFrom(cls)) {
                if (List.class.isAssignableFrom(cls)) {
                    return cls.cast(new ArrayList());
                }
                if (SortedSet.class.isAssignableFrom(cls)) {
                    return cls.cast(new TreeSet());
                }
                if (Set.class.isAssignableFrom(cls)) {
                    return cls.cast(new LinkedHashSet());
                }
                if (Queue.class.isAssignableFrom(cls)) {
                    return cls.cast(new LinkedList());
                }
                return cls.cast(new ArrayList());
            }
            if (Map.class.isAssignableFrom(cls)) {
                try {
                    return cls.cast(cls.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]));
                }
                catch (Exception callable) {
                    return cls.cast(new LinkedHashMap());
                }
            }
            try {
                T result = cls.newInstance();
                return result;
            }
            catch (Exception ex) {
                Constructor<T> constructor = cls.getDeclaredConstructor(new Class[0]);
                constructor.setAccessible(true);
                return constructor.newInstance(new Object[0]);
            }
        }

        private static ITypeConverter<?>[] createConverter(IFactory factory, Class<? extends ITypeConverter<?>>[] classes) {
            ITypeConverter[] result = new ITypeConverter[classes.length];
            for (int i = 0; i < classes.length; ++i) {
                result[i] = DefaultFactory.create(factory, classes[i]);
            }
            return result;
        }

        static IVersionProvider createVersionProvider(IFactory factory, Class<? extends IVersionProvider> cls) {
            return DefaultFactory.create(factory, cls);
        }

        static IDefaultValueProvider createDefaultValueProvider(IFactory factory, Class<? extends IDefaultValueProvider> cls) {
            return DefaultFactory.create(factory, cls);
        }

        static Iterable<String> createCompletionCandidates(IFactory factory, Class<? extends Iterable<String>> cls) {
            return DefaultFactory.create(factory, cls);
        }

        static IParameterConsumer createParameterConsumer(IFactory factory, Class<? extends IParameterConsumer> cls) {
            return DefaultFactory.create(factory, cls);
        }

        static <T> T create(IFactory factory, Class<T> cls) {
            try {
                return factory.create(cls);
            }
            catch (NoSuchMethodException ex) {
                throw new InitializationException("Cannot instantiate " + cls.getName() + ": the class has no constructor", ex);
            }
            catch (InitializationException ex) {
                throw ex;
            }
            catch (Exception ex) {
                throw new InitializationException("Could not instantiate " + cls + ": " + ex, ex);
            }
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static interface IFactory {
        public <K> K create(Class<K> var1) throws Exception;
    }

    private static class DefaultHelpFactory
    implements IHelpFactory {
        private DefaultHelpFactory() {
        }

        public Help create(Model.CommandSpec commandSpec, Help.ColorScheme colorScheme) {
            return new Help(commandSpec, colorScheme);
        }
    }

    public static interface IHelpFactory {
        public Help create(Model.CommandSpec var1, Help.ColorScheme var2);
    }

    public static class RegexTransformer
    implements INegatableOptionTransformer {
        final Map<Pattern, String> replacements;
        final Map<Pattern, String> synopsis;

        RegexTransformer(Builder builder) {
            this.replacements = Collections.unmodifiableMap(new LinkedHashMap<Pattern, String>(builder.replacements));
            this.synopsis = Collections.unmodifiableMap(new LinkedHashMap<Pattern, String>(builder.synopsis));
        }

        public static RegexTransformer createDefault() {
            RegexTransformer transformer = new Builder().addPattern("^--no-(\\w(-|\\w)*)$", "--$1", "--[no-]$1").addPattern("^--(\\w(-|\\w)*)$", "--no-$1", "--[no-]$1").addPattern("^(-|--)(\\w*:)\\+(\\w(-|\\w)*)$", "$1$2-$3", "$1$2(+|-)$3").addPattern("^(-|--)(\\w*:)\\-(\\w(-|\\w)*)$", "$1$2+$3", "$1$2(+|-)$3").build();
            return transformer;
        }

        public static RegexTransformer createCaseInsensitive() {
            RegexTransformer transformer = new Builder().addPattern("^--((?i)no)-(\\w(-|\\w)*)$", "--$2", "--[$1-]$2").addPattern("^--(\\w(-|\\w)*)$", "--no-$1", "--[no-]$1").addPattern("^(-|--)(\\w*:)\\+(\\w(-|\\w)*)$", "$1$2-$3", "$1$2(+|-)$3").addPattern("^(-|--)(\\w*:)\\-(\\w(-|\\w)*)$", "$1$2+$3", "$1$2(+|-)$3").build();
            return transformer;
        }

        public String makeNegative(String optionName, Model.CommandSpec cmd) {
            for (Map.Entry<Pattern, String> entry : this.replacements.entrySet()) {
                Matcher matcher = entry.getKey().matcher(optionName);
                if (!matcher.find()) continue;
                return matcher.replaceAll(entry.getValue());
            }
            return optionName;
        }

        public String makeSynopsis(String optionName, Model.CommandSpec cmd) {
            for (Map.Entry<Pattern, String> entry : this.synopsis.entrySet()) {
                Matcher matcher = entry.getKey().matcher(optionName);
                if (!matcher.find()) continue;
                return matcher.replaceAll(entry.getValue());
            }
            return optionName;
        }

        public String toString() {
            return this.getClass().getName() + "[replacements=" + this.replacements + ", synopsis=" + this.synopsis + "]@" + System.identityHashCode(this);
        }

        public static class Builder {
            Map<Pattern, String> replacements = new LinkedHashMap<Pattern, String>();
            Map<Pattern, String> synopsis = new LinkedHashMap<Pattern, String>();

            public Builder() {
            }

            public Builder(RegexTransformer old) {
                this.replacements.putAll(old.replacements);
                this.synopsis.putAll(old.synopsis);
            }

            public Builder addPattern(String regex, String negativeReplacement, String synopsisReplacement) {
                Pattern pattern = Pattern.compile(regex);
                this.replacements.put(pattern, negativeReplacement);
                this.synopsis.put(pattern, synopsisReplacement);
                return this;
            }

            public Builder removePattern(String regex) {
                Iterator<Pattern> iter = this.replacements.keySet().iterator();
                while (iter.hasNext()) {
                    Pattern pattern = iter.next();
                    if (!pattern.toString().equals(regex)) continue;
                    iter.remove();
                    this.synopsis.remove(pattern);
                }
                return this;
            }

            public RegexTransformer build() {
                return new RegexTransformer(this);
            }
        }
    }

    public static interface INegatableOptionTransformer {
        public String makeNegative(String var1, Model.CommandSpec var2);

        public String makeSynopsis(String var1, Model.CommandSpec var2);
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class NoOpParameterPreprocessor
    implements IParameterPreprocessor {
        private NoOpParameterPreprocessor() {
        }

        @Override
        public boolean preprocess(Stack<String> args, Model.CommandSpec commandSpec, Model.ArgSpec argSpec, Map<String, Object> info) {
            return false;
        }

        public boolean equals(Object obj) {
            return obj instanceof NoOpParameterPreprocessor;
        }

        public int hashCode() {
            return NoOpParameterPreprocessor.class.hashCode() + 7;
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static interface IParameterPreprocessor {
        public boolean preprocess(Stack<String> var1, Model.CommandSpec var2, Model.ArgSpec var3, Map<String, Object> var4);
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class NullParameterConsumer
    implements IParameterConsumer {
        private NullParameterConsumer() {
        }

        @Override
        public void consumeParameters(Stack<String> args, Model.ArgSpec argSpec, Model.CommandSpec commandSpec) {
            throw new UnsupportedOperationException();
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static interface IParameterConsumer {
        public void consumeParameters(Stack<String> var1, Model.ArgSpec var2, Model.CommandSpec var3);
    }

    private static class NoDefaultProvider
    implements IDefaultValueProvider {
        private NoDefaultProvider() {
        }

        public String defaultValue(Model.ArgSpec argSpec) {
            throw new UnsupportedOperationException();
        }
    }

    public static interface IDefaultValueProvider {
        public String defaultValue(Model.ArgSpec var1) throws Exception;
    }

    private static class NoOpModelTransformer
    implements IModelTransformer {
        private NoOpModelTransformer() {
        }

        public Model.CommandSpec transform(Model.CommandSpec commandSpec) {
            return commandSpec;
        }
    }

    public static interface IModelTransformer {
        public Model.CommandSpec transform(Model.CommandSpec var1);
    }

    private static class NoVersionProvider
    implements IVersionProvider {
        private NoVersionProvider() {
        }

        public String[] getVersion() throws Exception {
            throw new UnsupportedOperationException();
        }
    }

    public static interface IVersionProvider {
        public String[] getVersion() throws Exception;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static interface ITypeConverter<K> {
        public K convert(String var1) throws Exception;
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
    public static @interface ArgGroup {
        public String heading() default "__no_heading__";

        public String headingKey() default "__no_heading_key__";

        public boolean exclusive() default true;

        public String multiplicity() default "0..1";

        public boolean validate() default true;

        public int order() default -1;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.TYPE, ElementType.LOCAL_VARIABLE, ElementType.FIELD, ElementType.PACKAGE, ElementType.METHOD})
    public static @interface Command {
        public String name() default "<main class>";

        public String[] aliases() default {};

        public Class<?>[] subcommands() default {};

        public boolean subcommandsRepeatable() default false;

        public boolean addMethodSubcommands() default true;

        public String separator() default "=";

        public String[] version() default {};

        public Class<? extends IVersionProvider> versionProvider() default NoVersionProvider.class;

        public boolean mixinStandardHelpOptions() default false;

        public boolean helpCommand() default false;

        public String headerHeading() default "";

        public String[] header() default {};

        public String synopsisHeading() default "Usage: ";

        public boolean abbreviateSynopsis() default false;

        public String[] customSynopsis() default {};

        public String synopsisSubcommandLabel() default "[COMMAND]";

        public String descriptionHeading() default "";

        public String[] description() default {};

        public String parameterListHeading() default "";

        public String optionListHeading() default "";

        public boolean sortOptions() default true;

        public char requiredOptionMarker() default 32;

        public Class<? extends IDefaultValueProvider> defaultValueProvider() default NoDefaultProvider.class;

        public boolean showDefaultValues() default false;

        public boolean showAtFileInUsageHelp() default false;

        public boolean showEndOfOptionsDelimiterInUsageHelp() default false;

        public String commandListHeading() default "Commands:%n";

        public String footerHeading() default "";

        public String[] footer() default {};

        public boolean hidden() default false;

        public String resourceBundle() default "";

        public int usageHelpWidth() default 80;

        public boolean usageHelpAutoWidth() default false;

        public int exitCodeOnSuccess() default 0;

        public int exitCodeOnUsageHelp() default 0;

        public int exitCodeOnVersionHelp() default 0;

        public int exitCodeOnInvalidInput() default 2;

        public int exitCodeOnExecutionException() default 1;

        public String exitCodeListHeading() default "";

        public String[] exitCodeList() default {};

        public ScopeType scope() default ScopeType.LOCAL;

        public Class<? extends IModelTransformer> modelTransformer() default NoOpModelTransformer.class;

        public Class<? extends IParameterPreprocessor> preprocessor() default NoOpParameterPreprocessor.class;
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(value={ElementType.FIELD, ElementType.METHOD})
    public static @interface Spec {
        public Target value() default Target.SELF;

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static enum Target {
            SELF,
            MIXEE;

        }
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.FIELD, ElementType.PARAMETER})
    public static @interface Mixin {
        public String name() default "";
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.FIELD})
    public static @interface Unmatched {
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.FIELD})
    public static @interface ParentCommand {
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
    public static @interface Parameters {
        public static final String NULL_VALUE = "_NULL_";

        public String index() default "";

        public String[] description() default {};

        public String arity() default "";

        public String paramLabel() default "";

        public boolean hideParamSyntax() default false;

        public Class<?>[] type() default {};

        public Class<? extends ITypeConverter<?>>[] converter() default {};

        public String split() default "";

        public String splitSynopsisLabel() default "";

        public boolean hidden() default false;

        public String defaultValue() default "__no_default_value__";

        public Help.Visibility showDefaultValue() default Help.Visibility.ON_DEMAND;

        public Class<? extends Iterable<String>> completionCandidates() default NoCompletionCandidates.class;

        public boolean interactive() default false;

        public boolean echo() default false;

        public String prompt() default "";

        public String descriptionKey() default "";

        public ScopeType scope() default ScopeType.LOCAL;

        public Class<? extends IParameterConsumer> parameterConsumer() default NullParameterConsumer.class;

        public String mapFallbackValue() default "__unspecified__";

        public Class<? extends IParameterPreprocessor> preprocessor() default NoOpParameterPreprocessor.class;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
    public static @interface Option {
        public static final String NULL_VALUE = "_NULL_";

        public String[] names();

        public boolean required() default false;

        public boolean help() default false;

        public boolean usageHelp() default false;

        public boolean versionHelp() default false;

        public String[] description() default {};

        public String arity() default "";

        public String paramLabel() default "";

        public boolean hideParamSyntax() default false;

        public Class<?>[] type() default {};

        public Class<? extends ITypeConverter<?>>[] converter() default {};

        public String split() default "";

        public String splitSynopsisLabel() default "";

        public boolean hidden() default false;

        public String defaultValue() default "__no_default_value__";

        public Help.Visibility showDefaultValue() default Help.Visibility.ON_DEMAND;

        public Class<? extends Iterable<String>> completionCandidates() default NoCompletionCandidates.class;

        public boolean interactive() default false;

        public boolean echo() default false;

        public String prompt() default "";

        public String descriptionKey() default "";

        public int order() default -1;

        public boolean negatable() default false;

        public ScopeType scope() default ScopeType.LOCAL;

        public String fallbackValue() default "";

        public String mapFallbackValue() default "__unspecified__";

        public Class<? extends IParameterConsumer> parameterConsumer() default NullParameterConsumer.class;

        public Class<? extends IParameterPreprocessor> preprocessor() default NoOpParameterPreprocessor.class;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum ScopeType {
        LOCAL,
        INHERIT;

    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class NoCompletionCandidates
    implements Iterable<String> {
        private NoCompletionCandidates() {
        }

        @Override
        public Iterator<String> iterator() {
            throw new UnsupportedOperationException();
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static class RunAll
    extends AbstractParseResultHandler<List<Object>>
    implements IParseResultHandler {
        @Override
        public int execute(ParseResult parseResult) throws ExecutionException {
            return super.execute(parseResult);
        }

        @Override
        public List<Object> handleParseResult(List<CommandLine> parsedCommands, PrintStream out, Help.Ansi ansi) {
            if (CommandLine.printHelpIfRequested(parsedCommands, out, this.err(), ansi)) {
                return this.returnResultOrExit(Collections.emptyList());
            }
            ArrayList result = new ArrayList();
            for (CommandLine parsed : parsedCommands) {
                CommandLine.executeUserObject(parsed, result);
            }
            return this.returnResultOrExit(result);
        }

        @Override
        protected List<Object> handle(ParseResult parseResult) throws ExecutionException {
            return this.returnResultOrExit(this.recursivelyExecuteUserObject(parseResult, new ArrayList<Object>()));
        }

        private List<Object> recursivelyExecuteUserObject(ParseResult parseResult, List<Object> result) throws ExecutionException {
            CommandLine.executeUserObject(parseResult.commandSpec().commandLine(), result);
            for (ParseResult pr : parseResult.subcommands()) {
                this.recursivelyExecuteUserObject(pr, result);
            }
            return result;
        }

        @Override
        protected List<IExitCodeGenerator> extractExitCodeGenerators(ParseResult parseResult) {
            return this.recursivelyExtractExitCodeGenerators(parseResult, new ArrayList<IExitCodeGenerator>());
        }

        private List<IExitCodeGenerator> recursivelyExtractExitCodeGenerators(ParseResult parseResult, List<IExitCodeGenerator> result) throws ExecutionException {
            if (parseResult.commandSpec().userObject() instanceof IExitCodeGenerator) {
                result.add((IExitCodeGenerator)parseResult.commandSpec().userObject());
            }
            for (ParseResult pr : parseResult.subcommands()) {
                this.recursivelyExtractExitCodeGenerators(pr, result);
            }
            return result;
        }

        @Override
        protected RunAll self() {
            return this;
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static class RunLast
    extends AbstractParseResultHandler<List<Object>>
    implements IParseResultHandler {
        @Override
        public int execute(ParseResult parseResult) throws ExecutionException {
            return super.execute(parseResult);
        }

        @Override
        public List<Object> handleParseResult(List<CommandLine> parsedCommands, PrintStream out, Help.Ansi ansi) {
            if (CommandLine.printHelpIfRequested(parsedCommands, out, this.err(), ansi)) {
                return this.returnResultOrExit(Collections.emptyList());
            }
            return this.returnResultOrExit(RunLast.executeUserObjectOfLastSubcommandWithSameParent(parsedCommands));
        }

        @Override
        protected List<Object> handle(ParseResult parseResult) throws ExecutionException {
            return RunLast.executeUserObjectOfLastSubcommandWithSameParent(parseResult.asCommandLineList());
        }

        private static List<Object> executeUserObjectOfLastSubcommandWithSameParent(List<CommandLine> parsedCommands) {
            int start = RunLast.indexOfLastSubcommandWithSameParent(parsedCommands);
            ArrayList<Object> result = new ArrayList<Object>();
            for (int i = start; i < parsedCommands.size(); ++i) {
                CommandLine.executeUserObject(parsedCommands.get(i), result);
            }
            return result;
        }

        private static int indexOfLastSubcommandWithSameParent(List<CommandLine> parsedCommands) {
            int start = parsedCommands.size() - 1;
            int i = parsedCommands.size() - 2;
            while (i >= 0 && parsedCommands.get(i).getParent() == parsedCommands.get(i + 1).getParent()) {
                start = i--;
            }
            return start;
        }

        @Override
        protected List<IExitCodeGenerator> extractExitCodeGenerators(ParseResult parseResult) {
            List<CommandLine> parsedCommands = parseResult.asCommandLineList();
            int start = RunLast.indexOfLastSubcommandWithSameParent(parsedCommands);
            ArrayList<IExitCodeGenerator> result = new ArrayList<IExitCodeGenerator>();
            for (int i = start; i < parsedCommands.size(); ++i) {
                Object userObject = parsedCommands.get(i).getCommandSpec().userObject();
                if (!(userObject instanceof IExitCodeGenerator)) continue;
                result.add((IExitCodeGenerator)userObject);
            }
            return result;
        }

        @Override
        protected RunLast self() {
            return this;
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static class RunFirst
    extends AbstractParseResultHandler<List<Object>>
    implements IParseResultHandler {
        @Override
        public int execute(ParseResult parseResult) throws ExecutionException {
            return super.execute(parseResult);
        }

        @Override
        public List<Object> handleParseResult(List<CommandLine> parsedCommands, PrintStream out, Help.Ansi ansi) {
            if (CommandLine.printHelpIfRequested(parsedCommands, out, this.err(), ansi)) {
                return this.returnResultOrExit(Collections.emptyList());
            }
            return this.returnResultOrExit(CommandLine.executeUserObject(parsedCommands.get(0), new ArrayList()));
        }

        @Override
        protected List<Object> handle(ParseResult parseResult) throws ExecutionException {
            return CommandLine.executeUserObject(parseResult.commandSpec().commandLine(), new ArrayList());
        }

        @Override
        protected List<IExitCodeGenerator> extractExitCodeGenerators(ParseResult parseResult) {
            if (parseResult.commandSpec().userObject() instanceof IExitCodeGenerator) {
                return Collections.singletonList((IExitCodeGenerator)parseResult.commandSpec().userObject());
            }
            return Collections.emptyList();
        }

        @Override
        protected RunFirst self() {
            return this;
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Deprecated
    public static abstract class AbstractParseResultHandler<R>
    extends AbstractHandler<R, AbstractParseResultHandler<R>>
    implements IParseResultHandler2<R>,
    IExecutionStrategy {
        @Override
        public R handleParseResult(ParseResult parseResult) throws ExecutionException {
            if (CommandLine.printHelpIfRequested(parseResult.asCommandLineList(), this.out(), this.err(), this.colorScheme())) {
                return this.returnResultOrExit(null);
            }
            return this.returnResultOrExit(this.handle(parseResult));
        }

        @Override
        public int execute(ParseResult parseResult) throws ExecutionException {
            Integer helpExitCode = CommandLine.executeHelpRequest(parseResult);
            if (helpExitCode != null) {
                return helpExitCode;
            }
            R executionResult = this.handle(parseResult);
            List<IExitCodeGenerator> exitCodeGenerators = this.extractExitCodeGenerators(parseResult);
            return this.resolveExitCode(parseResult.commandSpec().exitCodeOnSuccess(), executionResult, exitCodeGenerators);
        }

        private int resolveExitCode(int exitCodeOnSuccess, R executionResult, List<IExitCodeGenerator> exitCodeGenerators) {
            int result = 0;
            for (IExitCodeGenerator generator : exitCodeGenerators) {
                try {
                    int exitCode = generator.getExitCode();
                    if ((exitCode <= 0 || exitCode <= result) && (exitCode >= result || result > 0)) continue;
                    result = exitCode;
                }
                catch (Exception ex) {
                    result = result == 0 ? 1 : result;
                    ex.printStackTrace();
                }
            }
            if (executionResult instanceof List) {
                List resultList = (List)executionResult;
                for (Object obj : resultList) {
                    int exitCode;
                    if (!(obj instanceof Integer) || ((exitCode = ((Integer)obj).intValue()) <= 0 || exitCode <= result) && (exitCode >= result || result > 0)) continue;
                    result = exitCode;
                }
            }
            return result == 0 ? exitCodeOnSuccess : result;
        }

        protected abstract R handle(ParseResult var1) throws ExecutionException;

        protected List<IExitCodeGenerator> extractExitCodeGenerators(ParseResult parseResult) {
            return Collections.emptyList();
        }
    }

    static class ColoredStackTraceWriter
    extends StringWriter {
        Help.ColorScheme colorScheme;

        public ColoredStackTraceWriter(Help.ColorScheme colorScheme) {
            this.colorScheme = colorScheme;
        }

        public void write(String str, int off, int len) {
            List<Help.Ansi.IStyle> styles = str.startsWith("\t") ? this.colorScheme.stackTraceStyles() : this.colorScheme.errorStyles();
            super.write(this.colorScheme.apply(str.substring(off, len), styles).toString());
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Deprecated
    public static class DefaultExceptionHandler<R>
    extends AbstractHandler<R, DefaultExceptionHandler<R>>
    implements IExceptionHandler,
    IExceptionHandler2<R> {
        @Override
        public List<Object> handleException(ParameterException ex, PrintStream out, Help.Ansi ansi, String ... args) {
            DefaultExceptionHandler.internalHandleParseException(ex, CommandLine.newPrintWriter(out, CommandLine.getStdoutEncoding()), Help.defaultColorScheme(ansi));
            return Collections.emptyList();
        }

        @Override
        public R handleParseException(ParameterException ex, String[] args) {
            DefaultExceptionHandler.internalHandleParseException(ex, CommandLine.newPrintWriter(this.err(), CommandLine.getStderrEncoding()), this.colorScheme());
            return this.returnResultOrExit(null);
        }

        static void internalHandleParseException(ParameterException ex, PrintWriter writer, Help.ColorScheme colorScheme) {
            Tracer tracer;
            writer.println(colorScheme.errorText(ex.getMessage()));
            if (!UnmatchedArgumentException.printSuggestions(ex, writer)) {
                ex.getCommandLine().usage(writer, colorScheme);
            }
            if ((tracer = new Tracer()).isDebug()) {
                ex.printStackTrace(tracer.stream);
            }
        }

        @Override
        public R handleExecutionException(ExecutionException ex, ParseResult parseResult) {
            return this.throwOrExit(ex);
        }

        @Override
        protected DefaultExceptionHandler<R> self() {
            return this;
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Deprecated
    public static abstract class AbstractHandler<R, T extends AbstractHandler<R, T>> {
        private Help.ColorScheme colorScheme = Help.defaultColorScheme(Help.Ansi.AUTO);
        private Integer exitCode;
        private PrintStream out = System.out;
        private PrintStream err = System.err;

        public PrintStream out() {
            return this.out;
        }

        public PrintStream err() {
            return this.err;
        }

        @Deprecated
        public Help.Ansi ansi() {
            return this.colorScheme.ansi();
        }

        public Help.ColorScheme colorScheme() {
            return this.colorScheme;
        }

        public Integer exitCode() {
            return this.exitCode;
        }

        public boolean hasExitCode() {
            return this.exitCode != null;
        }

        protected R returnResultOrExit(R result) {
            if (this.hasExitCode()) {
                this.exit(this.exitCode());
            }
            return result;
        }

        protected R throwOrExit(ExecutionException ex) {
            if (this.hasExitCode()) {
                ex.printStackTrace(this.err());
                this.exit(this.exitCode());
                return null;
            }
            throw ex;
        }

        protected void exit(int exitCode) {
            System.exit(exitCode);
        }

        protected abstract T self();

        @Deprecated
        public T useOut(PrintStream out) {
            this.out = Assert.notNull(out, "out");
            return this.self();
        }

        @Deprecated
        public T useErr(PrintStream err) {
            this.err = Assert.notNull(err, "err");
            return this.self();
        }

        @Deprecated
        public T useAnsi(Help.Ansi ansi) {
            this.colorScheme = Help.defaultColorScheme(Assert.notNull(ansi, "ansi"));
            return this.self();
        }

        @Deprecated
        public T andExit(int exitCode) {
            this.exitCode = exitCode;
            return this.self();
        }
    }

    public static interface IExecutionExceptionHandler {
        public int handleExecutionException(Exception var1, CommandLine var2, ParseResult var3) throws Exception;
    }

    public static interface IParameterExceptionHandler {
        public int handleParseException(ParameterException var1, String[] var2) throws Exception;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Deprecated
    public static interface IExceptionHandler2<R> {
        public R handleParseException(ParameterException var1, String[] var2);

        public R handleExecutionException(ExecutionException var1, ParseResult var2);
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Deprecated
    public static interface IExceptionHandler {
        public List<Object> handleException(ParameterException var1, PrintStream var2, Help.Ansi var3, String ... var4);
    }

    public static interface IExecutionStrategy {
        public int execute(ParseResult var1) throws ExecutionException, ParameterException;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Deprecated
    public static interface IParseResultHandler2<R> {
        public R handleParseResult(ParseResult var1) throws ExecutionException;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Deprecated
    public static interface IParseResultHandler {
        public List<Object> handleParseResult(List<CommandLine> var1, PrintStream var2, Help.Ansi var3) throws ExecutionException;
    }

    public static interface IExitCodeExceptionMapper {
        public int getExitCode(Throwable var1);
    }

    public static interface IExitCodeGenerator {
        public int getExitCode();
    }

    public static final class ExitCode {
        public static final int OK = 0;
        public static final int SOFTWARE = 1;
        public static final int USAGE = 2;

        private ExitCode() {
        }
    }
}

