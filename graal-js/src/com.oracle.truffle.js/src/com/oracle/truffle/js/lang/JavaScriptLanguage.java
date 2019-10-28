/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionValues;
import org.graalvm.polyglot.Context;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleStackTrace;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
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
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSEngine;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSMetaObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSScope;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.InteropFunction;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

@ProvidedTags({
                StandardTags.StatementTag.class,
                StandardTags.RootTag.class,
                StandardTags.RootBodyTag.class,
                StandardTags.ExpressionTag.class,
                StandardTags.CallTag.class,
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
                JavaScriptLanguage.MODULE_MIME_TYPE}, defaultMimeType = JavaScriptLanguage.APPLICATION_MIME_TYPE, contextPolicy = TruffleLanguage.ContextPolicy.SHARED, dependentLanguages = "regex", fileTypeDetectors = JSFileTypeDetector.class)
public class JavaScriptLanguage extends AbstractJavaScriptLanguage {
    public static final String TEXT_MIME_TYPE = "text/javascript";
    public static final String APPLICATION_MIME_TYPE = "application/javascript";
    public static final String MODULE_MIME_TYPE = "application/javascript+module";
    public static final String SCRIPT_SOURCE_NAME_SUFFIX = ".js";
    public static final String MODULE_SOURCE_NAME_SUFFIX = ".mjs";
    public static final String INTERNAL_SOURCE_URL_PREFIX = "internal:";

    public static final String NAME = "JavaScript";
    public static final String IMPLEMENTATION_NAME = "GraalVM JavaScript";
    public static final String ID = "js";

    private volatile JSContext languageContext;
    private volatile boolean multiContext;

    private final Assumption promiseJobsQueueEmptyAssumption;

    public static final OptionDescriptors OPTION_DESCRIPTORS;
    static {
        ArrayList<OptionDescriptor> options = new ArrayList<>();
        JSContextOptions.describeOptions(options);
        OPTION_DESCRIPTORS = OptionDescriptors.create(options);
        ensureErrorClassesInitialized();
    }

    public JavaScriptLanguage() {
        this.promiseJobsQueueEmptyAssumption = Truffle.getRuntime().createAssumption("PromiseJobsQueueEmpty");
    }

    @Override
    public boolean isObjectOfLanguage(Object o) {
        return JSObject.isJSObject(o) || o instanceof Symbol || o instanceof BigInt || o instanceof JSLazyString || o instanceof LargeInteger || o instanceof InteropFunction ||
                        o instanceof JSMetaObject;
    }

    @TruffleBoundary
    @Override
    public CallTarget parse(ParsingRequest parsingRequest) {
        Source source = parsingRequest.getSource();
        List<String> argumentNames = parsingRequest.getArgumentNames();
        if (argumentNames == null || argumentNames.isEmpty()) {
            final JSContext context = getJSContext();

            if (context.isOptionParseOnly()) {
                parseInContext(source, context);
                return createEmptyScript(context).getCallTarget();
            }

            final ScriptNode program = parseInContext(source, context);

            RootNode rootNode = new RootNode(this) {
                @Child private DirectCallNode directCallNode = DirectCallNode.create(program.getCallTarget());
                @Child private ExportValueNode exportValueNode = ExportValueNode.create();
                @CompilationFinal private ContextReference<JSRealm> contextReference;

                @Override
                public Object execute(VirtualFrame frame) {
                    if (contextReference == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        contextReference = lookupContextReference(JavaScriptLanguage.class);
                    }
                    JSRealm realm = contextReference.get();
                    assert realm.getContext() == context : "unexpected JSContext";
                    try {
                        interopBoundaryEnter(realm);
                        Object result = directCallNode.call(program.argumentsToRun(realm));
                        return exportValueNode.execute(result);
                    } finally {
                        interopBoundaryExit(realm);
                    }
                }

                @Override
                public boolean isInternal() {
                    return true;
                }
            };
            return Truffle.getRuntime().createCallTarget(rootNode);
        } else {
            RootNode rootNode = parseWithArgumentNames(source, argumentNames);
            return Truffle.getRuntime().createCallTarget(rootNode);
        }
    }

    @TruffleBoundary
    private static ScriptNode createEmptyScript(JSContext context) {
        return ScriptNode.fromFunctionData(context, JSFunction.createEmptyFunctionData(context));
    }

    @Override
    protected ExecutableNode parse(InlineParsingRequest request) throws Exception {
        final Source source = request.getSource();
        final MaterializedFrame requestFrame = request.getFrame();
        final JSContext context = getJSContext();
        final boolean strict = isStrictLocation(request.getLocation());
        final ExecutableNode executableNode = new ExecutableNode(this) {
            @Child private JavaScriptNode expression = insert(parseInline(source, context, requestFrame, strict));
            @Child private ExportValueNode exportValueNode = ExportValueNode.create();

            @Override
            public Object execute(VirtualFrame frame) {
                assert JavaScriptLanguage.getCurrentJSRealm().getContext() == context : "unexpected JSContext";
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

    private RootNode parseWithArgumentNames(Source source, List<String> argumentNames) {
        return new RootNode(this) {
            @CompilationFinal private ContextReference<JSRealm> contextReference;

            @Override
            public Object execute(VirtualFrame frame) {
                if (contextReference == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    contextReference = lookupContextReference(JavaScriptLanguage.class);
                }
                JSRealm realm = contextReference.get();
                return executeImpl(realm, frame.getArguments());
            }

            @TruffleBoundary
            private Object executeImpl(JSRealm realm, Object[] arguments) {
                StringBuilder code = new StringBuilder();
                code.append("'use strict';");
                code.append("(function");
                code.append(" (");
                assert !argumentNames.isEmpty();
                code.append(argumentNames.get(0));
                for (int i = 1; i < argumentNames.size(); i++) {
                    code.append(", ");
                    code.append(argumentNames.get(i));
                }
                code.append(") {\n");
                code.append("return eval(").append(JSRuntime.quote(source.getCharacters().toString())).append(");\n");
                code.append("})");
                Source wrappedSource = Source.newBuilder(ID, code.toString(), Evaluator.FUNCTION_SOURCE_NAME).build();
                Object function = parseInContext(wrappedSource, realm.getContext()).run(realm);
                return JSRuntime.jsObjectToJavaObject(JSFunction.call(JSArguments.create(Undefined.instance, function, arguments)));
            }
        };
    }

    @TruffleBoundary
    @Override
    protected String toString(JSRealm realm, Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof JSMetaObject) {
            JSMetaObject metaObject = (JSMetaObject) value;
            String type = metaObject.getClassName();
            if (type == null) {
                String subType = metaObject.getSubtype();
                if ("null".equals(subType)) {
                    type = "null";
                } else {
                    type = metaObject.getType();
                }
            }
            return type;
        }
        return JSRuntime.safeToString(value);
    }

    @TruffleBoundary
    protected static ScriptNode parseInContext(Source code, JSContext context) {
        long startTime = JSTruffleOptions.ProfileTime ? System.nanoTime() : 0L;
        try {
            return context.getEvaluator().parseScriptNode(context, code);
        } finally {
            if (JSTruffleOptions.ProfileTime) {
                context.getTimeProfiler().printElapsed(startTime, "parsing " + code.getName());
            }
        }
    }

    @TruffleBoundary
    protected static JavaScriptNode parseInline(Source code, JSContext context, MaterializedFrame lexicalContextFrame, boolean strict) {
        long startTime = JSTruffleOptions.ProfileTime ? System.nanoTime() : 0L;
        try {
            return context.getEvaluator().parseInlineScript(context, code, lexicalContextFrame, strict);
        } finally {
            if (JSTruffleOptions.ProfileTime) {
                context.getTimeProfiler().printElapsed(startTime, "parsing " + code.getName());
            }
        }
    }

    @Override
    protected JSRealm createContext(Env env) {
        JSContext context = languageContext;
        if (context == null) {
            context = initLanguageContext(env);
        }
        JSRealm realm = context.createRealm(env);

        if (env.out() != realm.getOutputStream()) {
            realm.setOutputWriter(null, env.out());
        }
        if (env.err() != realm.getErrorStream()) {
            realm.setErrorWriter(null, env.err());
        }

        return realm;
    }

    private synchronized JSContext initLanguageContext(Env env) {
        JSContext curContext = languageContext;
        if (curContext != null) {
            assert curContext.getContextOptions().equals(JSContextOptions.fromOptionValues(env.getOptions()));
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
        realm.initialize();
    }

    @Override
    protected boolean patchContext(JSRealm realm, Env newEnv) {
        assert realm.getContext().getLanguage() == this;

        if (optionsAllowPreInitializedContext(realm.getEnv(), newEnv) && realm.patchContext(newEnv)) {
            return true;
        } else {
            languageContext = null;
            return false;
        }
    }

    /**
     * Options which can be patched without throwing away the pre-initialized context.
     */
    private static final OptionKey<?>[] PREINIT_CONTEXT_PATCHABLE_OPTIONS = {
                    JSContextOptions.ARRAY_SORT_INHERITED,
                    JSContextOptions.TIMER_RESOLUTION,
                    JSContextOptions.SHELL,
                    JSContextOptions.V8_COMPATIBILITY_MODE,
                    JSContextOptions.GLOBAL_PROPERTY,
                    JSContextOptions.SCRIPTING,
                    JSContextOptions.DIRECT_BYTE_BUFFER,
                    JSContextOptions.INTL_402,
                    JSContextOptions.LOAD,
                    JSContextOptions.PRINT,
                    JSContextOptions.CONSOLE,
                    JSContextOptions.PERFORMANCE,
    };

    /**
     * Check for options that differ from the expected options and do not support patching, in which
     * case we cannot use the pre-initialized context for faster startup.
     */
    private static boolean optionsAllowPreInitializedContext(Env preinitEnv, Env env) {
        OptionValues preinitOptions = preinitEnv.getOptions();
        OptionValues options = env.getOptions();
        if (!preinitOptions.hasSetOptions() && !options.hasSetOptions()) {
            return true;
        } else if (preinitOptions.equals(options)) {
            return true;
        } else {
            assert preinitOptions.getDescriptors().equals(options.getDescriptors());
            Collection<OptionKey<?>> ignoredOptions = Arrays.asList(PREINIT_CONTEXT_PATCHABLE_OPTIONS);
            for (OptionDescriptor descriptor : options.getDescriptors()) {
                OptionKey<?> key = descriptor.getKey();
                if (preinitOptions.hasBeenSet(key) || options.hasBeenSet(key)) {
                    if (ignoredOptions.contains(key)) {
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
        realm.setGlobalObject(Undefined.instance);
    }

    @Override
    protected void initializeMultipleContexts() {
        multiContext = true;
    }

    @Override
    public boolean isMultiContext() {
        return multiContext;
    }

    @Override
    protected boolean areOptionsCompatible(OptionValues firstOptions, OptionValues newOptions) {
        return firstOptions.equals(newOptions);
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return OPTION_DESCRIPTORS;
    }

    @TruffleBoundary
    @Override
    protected Object findMetaObject(JSRealm realm, Object value) {
        String type;
        String subtype = null;
        String className = null;

        if (value instanceof JSMetaObject) {
            return "metaobject";
        } else if (value == Undefined.instance) {
            type = "undefined";
        } else if (value == Null.instance) {
            type = "object";
            subtype = "null";
        } else if (JSRuntime.isObject(value)) {
            DynamicObject obj = (DynamicObject) value;
            type = "object";
            className = JSRuntime.getConstructorName(obj);

            if (JSRuntime.isCallable(obj)) {
                type = "function";
            } else if (JSArray.isJSArray(obj)) {
                subtype = "array";
            } else if (JSDate.isJSDate(obj)) {
                subtype = "date";
            } else if (JSSymbol.isJSSymbol(obj)) {
                type = "symbol";
            }
        } else if (value instanceof InteropFunction) {
            return findMetaObject(realm, ((InteropFunction) value).getFunction());
        } else if (JSRuntime.isForeignObject(value)) {
            assert !JSObject.isJSObject(value);
            InteropLibrary interop = InteropLibrary.getFactory().getUncached(value);
            if (interop.isNull(value) || interop.isBoolean(value) || interop.isString(value) || interop.isNumber(value)) {
                Object unboxed = JSInteropUtil.toPrimitiveOrDefault(value, Null.instance, interop, null);
                assert !JSRuntime.isForeignObject(unboxed);
                return findMetaObject(realm, unboxed);
            }
            type = "object";
            className = "Foreign";
        } else if (value == null) {
            type = "null";
        } else {
            // primitive
            type = JSRuntime.typeof(value);
        }

        return new JSMetaObject(type, subtype, className, realm.getEnv());
    }

    @Override
    protected SourceSection findSourceLocation(JSRealm realm, Object value) {
        if (JSFunction.isJSFunction(value)) {
            DynamicObject func = (DynamicObject) value;
            CallTarget ct = JSFunction.getCallTarget(func);
            if (JSFunction.isBoundFunction(func)) {
                func = JSFunction.getBoundTargetFunction(func);
                ct = JSFunction.getCallTarget(func);
            }

            if (ct instanceof RootCallTarget) {
                return ((RootCallTarget) ct).getRootNode().getSourceSection();
            }
        }
        return null;
    }

    @Override
    protected boolean isVisible(JSRealm realm, Object value) {
        return (value != Undefined.instance);
    }

    @Override
    protected Iterable<Scope> findLocalScopes(JSRealm realm, Node node, Frame frame) {
        return JSScope.createLocalScopes(node, frame == null ? null : frame.materialize());
    }

    @Override
    protected Iterable<Scope> findTopScopes(JSRealm realm) {
        return JSScope.createGlobalScopes(realm);
    }

    public static JSContext getJSContext(Context context) {
        return getJSRealm(context).getContext();
    }

    public static JSRealm getJSRealm(Context context) {
        context.enter();
        try {
            context.initialize(ID);
            return getCurrentContext(JavaScriptLanguage.class);
        } finally {
            context.leave();
        }
    }

    public void interopBoundaryEnter(JSRealm realm) {
        realm.getAgent().interopBoundaryEnter();
    }

    public void interopBoundaryExit(JSRealm realm) {
        JSAgent agent = realm.getAgent();
        if (agent.interopBoundaryExit()) {
            if (!promiseJobsQueueEmptyAssumption.isValid()) {
                agent.processAllPromises();
            }
        }
    }

    public Assumption getPromiseJobsQueueEmptyAssumption() {
        return promiseJobsQueueEmptyAssumption;
    }

    public JSContext getJSContext() {
        return languageContext;
    }

    private static void ensureErrorClassesInitialized() {
        if (JSTruffleOptions.SubstrateVM) {
            return;
        }
        // Ensure error-related classes are initialized to avoid NoClassDefFoundError
        // during conversion of StackOverflowError to RangeError
        TruffleStackTrace.getStackTrace(Errors.createRangeError(""));
    }
}
