/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionValues;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.SandboxPolicy;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleStackTrace;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.InitErrorObjectNodeFactory;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryOperationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.DeclareTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.EvalCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.InputNodeTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ObjectAllocationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableTag;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSEngine;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSLanguageOptions;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JobCallback;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.interop.JavaScriptLanguageView;
import com.oracle.truffle.js.runtime.objects.AsyncContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

@ProvidedTags({
                StandardTags.StatementTag.class,
                StandardTags.RootTag.class,
                StandardTags.RootBodyTag.class,
                StandardTags.ExpressionTag.class,
                StandardTags.CallTag.class,
                StandardTags.ReadVariableTag.class,
                StandardTags.WriteVariableTag.class,
                StandardTags.TryBlockTag.class,
                DebuggerTags.AlwaysHalt.class,
                // Expressions
                ObjectAllocationTag.class,
                BinaryOperationTag.class,
                UnaryOperationTag.class,
                WriteVariableTag.class,
                ReadElementTag.class,
                WriteElementTag.class,
                ReadPropertyTag.class,
                WritePropertyTag.class,
                ReadVariableTag.class,
                LiteralTag.class,
                FunctionCallTag.class,
                // Statements and builtins
                BuiltinRootTag.class,
                EvalCallTag.class,
                ControlFlowRootTag.class,
                ControlFlowBlockTag.class,
                ControlFlowBranchTag.class,
                DeclareTag.class,
                // Other
                InputNodeTag.class,
})

@TruffleLanguage.Registration(id = JavaScriptLanguage.ID, name = JavaScriptLanguage.NAME, implementationName = JavaScriptLanguage.IMPLEMENTATION_NAME, characterMimeTypes = {
                JavaScriptLanguage.APPLICATION_MIME_TYPE,
                JavaScriptLanguage.TEXT_MIME_TYPE,
                JavaScriptLanguage.MODULE_MIME_TYPE}, defaultMimeType = JavaScriptLanguage.APPLICATION_MIME_TYPE, //
                contextPolicy = TruffleLanguage.ContextPolicy.SHARED, //
                dependentLanguages = "regex", //
                fileTypeDetectors = JSFileTypeDetector.class, //
                website = "https://www.graalvm.org/javascript/", sandbox = SandboxPolicy.UNTRUSTED)
public final class JavaScriptLanguage extends TruffleLanguage<JSRealm> {
    public static final String TEXT_MIME_TYPE = "text/javascript";
    public static final String APPLICATION_MIME_TYPE = "application/javascript";
    public static final String MODULE_MIME_TYPE = "application/javascript+module";
    public static final String JSON_MIME_TYPE = "application/json";
    public static final String SCRIPT_SOURCE_NAME_SUFFIX = ".js";
    public static final String MODULE_SOURCE_NAME_SUFFIX = ".mjs";
    public static final String JSON_SOURCE_NAME_SUFFIX = ".json";

    public static final String WASM_LANGUAGE_ID = "wasm";
    public static final String WASM_MIME_TYPE = "application/wasm";
    public static final String WASM_SOURCE_NAME_SUFFIX = ".wasm";

    public static final String INTERNAL_SOURCE_URL_PREFIX = "internal:";
    public static final String NODE_ENV_PARSE_TOKEN = "%NODE_ENV_PARSE_TOKEN%";

    public static final String NAME = "JavaScript";
    public static final String IMPLEMENTATION_NAME = "GraalVM JavaScript";
    public static final String ID = "js";

    @CompilationFinal private volatile JSContext languageContext;
    private volatile boolean multiContext;

    private final Assumption promiseJobsQueueEmptyAssumption;

    private static final LanguageReference<JavaScriptLanguage> REFERENCE = LanguageReference.create(JavaScriptLanguage.class);

    public static final OptionDescriptors OPTION_DESCRIPTORS;
    static {
        OPTION_DESCRIPTORS = JSContextOptions.optionDescriptorsWithDefaultValues();
        ensureErrorClassesInitialized();
    }

    private int realmCount;

    public JavaScriptLanguage() {
        this.promiseJobsQueueEmptyAssumption = Truffle.getRuntime().createAssumption("PromiseJobsQueueEmpty");
    }

    @Override
    protected void finalizeContext(JSRealm realm) {
        // Terminate spawned agent threads
        realm.getAgent().terminate();
    }

    @TruffleBoundary
    @Override
    public CallTarget parse(ParsingRequest parsingRequest) {
        Source source = parsingRequest.getSource();
        List<String> argumentNames = parsingRequest.getArgumentNames();
        final JSContext context = getJSContext();

        final ScriptNode program;
        assert argumentNames != null;
        if (argumentNames.size() == 4 && argumentNames.get(0).equals(NODE_ENV_PARSE_TOKEN)) {
            String prolog = argumentNames.get(1);
            String epilog = argumentNames.get(2);
            boolean strict = Boolean.parseBoolean(argumentNames.get(3));
            program = parseScript(context, source, prolog, epilog, strict, new ArrayList<>());
        } else {
            program = parseScript(context, source, "", "", context.getParserOptions().strict(), argumentNames);
        }

        if (context.isOptionParseOnly()) {
            return createEmptyScript(context).getCallTarget();
        }

        return new ParsedProgramRoot(this, context, program).getCallTarget();
    }

    private final class ParsedProgramRoot extends RootNode {
        private final JSContext context;
        private final ScriptNode program;
        @Child private DirectCallNode directCallNode;
        @Child private ExportValueNode exportValueNode = ExportValueNode.create();
        @Child private ImportValueNode importValueNode = ImportValueNode.create();

        private ParsedProgramRoot(TruffleLanguage<?> language, JSContext context, ScriptNode program) {
            super(language);
            this.context = context;
            this.program = program;
            this.directCallNode = DirectCallNode.create(program.getCallTarget());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            JSRealm realm = JSRealm.get(this);
            assert realm.getContext() == context : "unexpected JSContext";
            try {
                interopBoundaryEnter(realm);
                Object[] arguments = frame.getArguments();
                for (int i = 0; i < arguments.length; i++) {
                    arguments[i] = importValueNode.executeWithTarget(arguments[i]);
                }
                arguments = program.argumentsToRunWithArguments(realm, arguments);
                Object result = directCallNode.call(arguments);
                return exportValueNode.execute(result);
            } finally {
                interopBoundaryExit(realm);
            }
        }

        @Override
        public boolean isInternal() {
            return true;
        }

        @Override
        protected boolean isInstrumentable() {
            return false;
        }
    }

    public static CallTarget getParsedProgramCallTarget(RootNode rootNode) {
        return ((ParsedProgramRoot) rootNode).directCallNode.getCallTarget();
    }

    @TruffleBoundary
    private static ScriptNode createEmptyScript(JSContext context) {
        return ScriptNode.fromFunctionData(JSFunction.createEmptyFunctionData(context));
    }

    @Override
    protected ExecutableNode parse(InlineParsingRequest request) throws Exception {
        final Source source = request.getSource();
        final MaterializedFrame requestFrame = request.getFrame();
        final JSContext context = getJSContext();
        final Node locationNode = request.getLocation();
        final boolean strict = isStrictLocation(locationNode);
        final ExecutableNode executableNode = new ExecutableNode(this) {
            @Child private JavaScriptNode expression = insert(parseInlineScript(context, source, requestFrame, strict, locationNode));
            @Child private ExportValueNode exportValueNode = ExportValueNode.create();

            @Override
            public Object execute(VirtualFrame frame) {
                assert JavaScriptLanguage.get(this).getJSContext() == context : "unexpected JSContext";
                Object result = expression.execute(frame);
                return exportValueNode.execute(result);
            }
        };
        return executableNode;
    }

    private static boolean isStrictLocation(Node location) {
        if (location != null) {
            RootNode rootNode = location.getRootNode();
            if (rootNode instanceof FunctionRootNode) {
                return ((FunctionRootNode) rootNode).getFunctionData().isStrict();
            }
        }
        return true;
    }

    @TruffleBoundary
    private static ScriptNode parseScript(JSContext context, Source code, String prolog, String epilog, boolean strict, List<String> argumentNames) {
        boolean profileTime = context.getLanguageOptions().profileTime();
        long startTime = profileTime ? System.nanoTime() : 0L;
        try {
            return context.getEvaluator().parseScript(context, code, prolog, epilog, strict, argumentNames.isEmpty() ? null : argumentNames);
        } finally {
            if (profileTime) {
                context.getTimeProfiler().printElapsed(startTime, "parsing " + code.getName());
            }
        }
    }

    @TruffleBoundary
    protected static JavaScriptNode parseInlineScript(JSContext context, Source code, MaterializedFrame lexicalContextFrame, boolean strict, Node locationNode) {
        boolean profileTime = context.getLanguageOptions().profileTime();
        long startTime = profileTime ? System.nanoTime() : 0L;
        try {
            return context.getEvaluator().parseInlineScript(context, code, lexicalContextFrame, strict, locationNode);
        } finally {
            if (profileTime) {
                context.getTimeProfiler().printElapsed(startTime, "parsing " + code.getName());
            }
        }
    }

    @Override
    protected JSRealm createContext(Env env) {
        validateSandbox(env);
        CompilerAsserts.neverPartOfCompilation();
        JSContext context = languageContext;
        if (context == null) {
            context = initLanguageContext(env);
        }
        return context.createRealm(env);
    }

    private synchronized JSContext initLanguageContext(Env env) {
        CompilerAsserts.neverPartOfCompilation();
        JSContext curContext = languageContext;
        if (curContext != null) {
            assert curContext.getLanguageOptions().equals(JSLanguageOptions.fromOptionValues(env.getSandboxPolicy(), env.getOptions()));
            return curContext;
        }
        JSContext newContext = newJSContext(env);
        languageContext = newContext;
        return newContext;
    }

    private JSContext newJSContext(Env env) {
        return JSEngine.createJSContext(this, env);
    }

    @Override
    protected void initializeContext(JSRealm realm) {
        synchronized (this) {
            realmCount++;
        }
        realm.initialize();
    }

    @Override
    protected boolean patchContext(JSRealm realm, Env newEnv) {
        CompilerAsserts.neverPartOfCompilation();
        validateSandbox(newEnv);
        assert realm.getContext().getLanguage() == this;

        if (optionsAllowPreInitializedContext(realm.getEnv(), newEnv) && realm.patchContext(newEnv)) {
            return true;
        } else {
            languageContext = null;
            return false;
        }
    }

    private static void validateSandbox(Env env) {
        SandboxPolicy policy = env.getSandboxPolicy();
        OptionValues optionValues = env.getOptions();
        if (policy.isStricterThan(SandboxPolicy.TRUSTED)) {
            if (optionValues.hasBeenSet(JSContextOptions.ECMASCRIPT_VERSION)) {
                if (optionValues.get(JSContextOptions.ECMASCRIPT_VERSION) >= JSConfig.StagingECMAScriptVersion) {
                    throw new SandboxValidationError(String.format("""
                                    The validation for the given sandbox policy %s failed. \
                                    The js.ecmascript-version option may only be set to "staging" in sandbox policy TRUSTED. \
                                    In order to resolve this, switch to a lower ecmascript version or a less strict sandbox policy.""",
                                    policy));
                }
            }
        }
        if (policy.isStricterOrEqual(SandboxPolicy.UNTRUSTED)) {
            if (optionValues.hasBeenSet(JSContextOptions.TIMER_RESOLUTION)) {
                long timerResolution = optionValues.get(JSContextOptions.TIMER_RESOLUTION);
                long minValue = TimeUnit.MILLISECONDS.toNanos(100);
                if (timerResolution != 0 && timerResolution < minValue) {
                    throw new SandboxValidationError(String.format("""
                                    The validation for the given sandbox policy %s failed. \
                                    The js.timer-resolution option is set to %d, but must be set to at least %d. \
                                    In order to resolve this use the default value by removing Builder.option("js.timer-resolution", "%d") or increase its value to at least %d \
                                    or switch to a less strict sandbox policy using Builder.sandbox(SandboxPolicy).""",
                                    policy, timerResolution, minValue, timerResolution, minValue));
                }
            }
        }
    }

    /**
     * Options which can be patched without throwing away the pre-initialized context.
     */
    private static final Set<OptionKey<?>> PREINIT_CONTEXT_PATCHABLE_OPTIONS = Set.of(
                    JSContextOptions.TIMER_RESOLUTION,
                    JSContextOptions.SHELL,
                    JSContextOptions.V8_COMPATIBILITY_MODE,
                    JSContextOptions.GLOBAL_PROPERTY,
                    JSContextOptions.GLOBAL_ARGUMENTS,
                    JSContextOptions.DIRECT_BYTE_BUFFER,
                    JSContextOptions.LOAD,
                    JSContextOptions.PRINT,
                    JSContextOptions.CONSOLE,
                    JSContextOptions.PERFORMANCE,
                    JSContextOptions.REGEXP_STATIC_RESULT,
                    JSContextOptions.TIME_ZONE);

    /**
     * Check for options that differ from the expected options and do not support patching, in which
     * case we cannot use the pre-initialized context for faster startup.
     */
    private static boolean optionsAllowPreInitializedContext(Env preinitEnv, Env env) {
        OptionValues preinitOptions = preinitEnv.getOptions();
        OptionValues options = env.getOptions();
        if (areOptionsEqual(preinitOptions, options)) {
            return true;
        } else {
            assert preinitOptions.getDescriptors().equals(options.getDescriptors());
            for (OptionDescriptor descriptor : options.getDescriptors()) {
                OptionKey<?> key = descriptor.getKey();
                if (preinitOptions.hasBeenSet(key) || options.hasBeenSet(key)) {
                    if (PREINIT_CONTEXT_PATCHABLE_OPTIONS.contains(key)) {
                        continue;
                    }
                    if (!preinitOptions.get(key).equals(options.get(key))) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    @Override
    protected void disposeContext(JSRealm realm) {
        CompilerAsserts.neverPartOfCompilation();
        var options = realm.getContextOptions();
        if (options.isProfileTime() && options.isProfileTimePrintCumulative()) {
            realm.getContext().getTimeProfiler().printCumulative();
        }
        realm.dispose();
        synchronized (this) {
            if (--realmCount == 0) {
                // Clear inverted maps that are no longer needed to ensure
                // that they are not stored in aux engine cache
                realm.getContext().clearSymbolInvertedMaps();
            }
        }
    }

    @Override
    protected void initializeMultipleContexts() {
        multiContext = true;
    }

    public boolean isMultiContext() {
        return multiContext;
    }

    @Override
    protected boolean areOptionsCompatible(OptionValues firstOptions, OptionValues newOptions) {
        if (areOptionsEqual(firstOptions, newOptions)) {
            return true;
        } else {
            return JSLanguageOptions.fromOptionValues(SandboxPolicy.TRUSTED, firstOptions).equals(JSLanguageOptions.fromOptionValues(SandboxPolicy.TRUSTED, newOptions));
        }
    }

    private static boolean areOptionsEqual(OptionValues prevOptions, OptionValues newOptions) {
        if (!prevOptions.hasSetOptions() && !newOptions.hasSetOptions()) {
            return true;
        } else if (prevOptions.equals(newOptions)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return OPTION_DESCRIPTORS;
    }

    @Override
    protected boolean isVisible(JSRealm realm, Object value) {
        return (value != Undefined.instance);
    }

    @Override
    protected Object getLanguageView(JSRealm context, Object value) {
        return JavaScriptLanguageView.create(value);
    }

    @Override
    protected Object getScope(JSRealm context) {
        return context.getTopScopeObject();
    }

    public static JSRealm getCurrentJSRealm() {
        return JSRealm.get(null);
    }

    public static JavaScriptLanguage getCurrentLanguage() {
        return JavaScriptLanguage.get(null);
    }

    public static TruffleLanguage.Env getCurrentEnv() {
        return getCurrentJSRealm().getEnv();
    }

    public static JSContext getJSContext(Context context) {
        return getJSRealm(context).getContext();
    }

    public static JSRealm getJSRealm(Context context) {
        context.enter();
        try {
            context.initialize(ID);
            return JSRealm.get(null);
        } finally {
            context.leave();
        }
    }

    @SuppressWarnings("static-method")
    public void interopBoundaryEnter(JSRealm realm) {
        realm.getAgent().interopBoundaryEnter();
    }

    public void interopBoundaryExit(JSRealm realm) {
        JSAgent agent = realm.getAgent();
        if (agent.interopBoundaryExit()) {
            if (!promiseJobsQueueEmptyAssumption.isValid()) {
                agent.processAllPromises(true);
            }
            if (getJSContext().getLanguageOptions().testV8Mode()) {
                processTimeoutCallbacks(realm);
            }
        }
    }

    @TruffleBoundary
    @SuppressWarnings("unchecked")
    private static void processTimeoutCallbacks(JSRealm realm) {
        JSAgent agent = realm.getAgent();
        List<JobCallback> callbackList;
        while ((callbackList = (List<JobCallback>) realm.getEmbedderData()) != null && !callbackList.isEmpty()) {
            realm.setEmbedderData(null);
            for (JobCallback callback : callbackList) {
                AsyncContext previousContextMapping = agent.asyncContextSwap(callback.asyncContextSnapshot());
                try {
                    JSRuntime.call(callback.callback(), Undefined.instance, JSArguments.EMPTY_ARGUMENTS_ARRAY);
                } finally {
                    agent.asyncContextSwap(previousContextMapping);
                }
            }
            agent.processAllPromises(true);
        }
    }

    public Assumption getPromiseJobsQueueEmptyAssumption() {
        return promiseJobsQueueEmptyAssumption;
    }

    @Idempotent
    @NeverDefault
    public JSContext getJSContext() {
        return Objects.requireNonNull(languageContext);
    }

    @Idempotent
    public static JavaScriptLanguage get(Node node) {
        return REFERENCE.get(node);
    }

    public boolean bindMemberFunctions() {
        return getJSContext().getLanguageOptions().bindMemberFunctions();
    }

    public int getAsyncStackDepth() {
        return super.getAsynchronousStackDepth();
    }

    private static void ensureErrorClassesInitialized() {
        if (JSConfig.SubstrateVM) {
            return;
        }
        // Ensure error-related classes are initialized to avoid NoClassDefFoundError
        // during conversion of StackOverflowError to RangeError
        try {
            JSException.ensureInitialized();
            JSErrorObject.ensureInitialized();
            Class.forName(Errors.class.getName());
            Class.forName(TruffleStackTrace.class.getName());
            Class.forName(TruffleStackTraceElement.class.getName());
            Class.forName(InitErrorObjectNodeFactory.DefineStackPropertyNodeGen.class.getName());
            Class.forName(TryCatchNode.GetErrorObjectNode.class.getName());
        } catch (ClassNotFoundException ex) {
            throw Errors.shouldNotReachHere(ex);
        }
    }
}
