/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionValues;
import org.graalvm.polyglot.Context;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.EvalCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.InputNodeTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ObjectAllocationExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.parser.env.DebugEnvironment;
import com.oracle.truffle.js.parser.env.Environment;
import com.oracle.truffle.js.parser.foreign.InteropBoundFunctionForeign;
import com.oracle.truffle.js.parser.foreign.JSForeignAccessFactoryForeign;
import com.oracle.truffle.js.parser.foreign.JSMetaObject;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSInteropRuntime;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.InteropBoundFunction;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

@ProvidedTags({StandardTags.CallTag.class,
                StandardTags.StatementTag.class,
                DebuggerTags.AlwaysHalt.class,
                StandardTags.RootTag.class,
                StandardTags.ExpressionTag.class,
                // Expressions
                ObjectAllocationExpressionTag.class,
                BinaryExpressionTag.class,
                UnaryExpressionTag.class,
                WriteVariableExpressionTag.class,
                ReadElementExpressionTag.class,
                WriteElementExpressionTag.class,
                ReadPropertyExpressionTag.class,
                WritePropertyExpressionTag.class,
                ReadVariableExpressionTag.class,
                LiteralExpressionTag.class,
                FunctionCallExpressionTag.class,
                // Statements and builtins
                BuiltinRootTag.class,
                EvalCallTag.class,
                ControlFlowRootTag.class,
                ControlFlowBlockTag.class,
                ControlFlowBranchTag.class,
                // Other
                InputNodeTag.class,
})

@TruffleLanguage.Registration(id = JavaScriptLanguage.ID, name = JavaScriptLanguage.NAME, version = JavaScriptLanguage.VERSION_NUMBER, mimeType = {JavaScriptLanguage.APPLICATION_MIME_TYPE,
                JavaScriptLanguage.TEXT_MIME_TYPE}, contextPolicy = TruffleLanguage.ContextPolicy.REUSE, dependentLanguages = "regex")
public class JavaScriptLanguage extends AbstractJavaScriptLanguage {
    private static final int MAX_TOSTRING_DEPTH = 10;

    private volatile JSContext languageContext;

    public static final OptionDescriptors OPTION_DESCRIPTORS;
    static {
        ArrayList<OptionDescriptor> options = new ArrayList<>();
        GraalJSParserOptions.describeOptions(options);
        JSContextOptions.describeOptions(options);
        OPTION_DESCRIPTORS = OptionDescriptors.create(options);
        ensureErrorClassesInitialized();
    }

    @Override
    public boolean isObjectOfLanguage(Object o) {
        return JSObject.isJSObject(o) || o instanceof Symbol || o instanceof JSLazyString || o instanceof InteropBoundFunction || o instanceof JSMetaObject;
    }

    @TruffleBoundary
    @Override
    public CallTarget parse(ParsingRequest parsingRequest) {
        Source source = parsingRequest.getSource();
        List<String> argumentNames = parsingRequest.getArgumentNames();
        if (argumentNames == null || argumentNames.isEmpty()) {
            final JSContext context = getContextReference().get().getContext();

            if (context.isOptionParseOnly()) {
                parseInContext(source, context);
                return createEmptyScript(context).getCallTarget();
            }

            final ScriptNode program = parseInContext(source, context);

            RootNode rootNode = new RootNode(this) {
                @Child private DirectCallNode directCallNode = DirectCallNode.create(program.getCallTarget());
                @Child private ExportValueNode exportValueNode = ExportValueNode.create(context);

                @Override
                public Object execute(VirtualFrame frame) {
                    JSRealm realm = getContextReference().get();
                    assert realm.getContext() == context : "unexpected JSContext";
                    try {
                        context.interopBoundaryEnter();
                        Object result = directCallNode.call(program.argumentsToRun(realm));
                        return exportValueNode.executeWithTarget(result, Undefined.instance);
                    } finally {
                        context.interopBoundaryExit();
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
        final JSContext context = getContextReference().get().getContext();
        final ExecutableNode executableNode = new ExecutableNode(this) {
            @Child private JavaScriptNode expression = insert(parseInline(source, context, requestFrame));
            @Child private ExportValueNode exportValueNode = ExportValueNode.create(context);

            @Override
            public Object execute(VirtualFrame frame) {
                assert getContextReference().get().getContext() == context : "unexpected JSContext";
                Object result = expression.execute(frame);
                return exportValueNode.executeWithTarget(result, Undefined.instance);
            }
        };
        return executableNode;
    }

    private RootNode parseWithArgumentNames(Source source, List<String> argumentNames) {
        return new RootNode(this) {
            @Override
            public Object execute(VirtualFrame frame) {
                return executeImpl(getContextReference().get(), frame.getArguments());
            }

            @TruffleBoundary
            private Object executeImpl(JSRealm realm, Object[] arguments) {
                // (GR-2039) only works for simple expressions at the moment. needs parser support.
                StringBuilder code = new StringBuilder();
                code.append("(function");
                code.append(" (");
                assert !argumentNames.isEmpty();
                code.append(argumentNames.get(0));
                for (int i = 1; i < argumentNames.size(); i++) {
                    code.append(", ");
                    code.append(argumentNames.get(i));
                }
                code.append(") {\n");
                code.append("return ");
                code.append(source.getCharacters());
                code.append("\n})");
                Source wrappedSource = Source.newBuilder(code.toString()).name(Evaluator.FUNCTION_SOURCE_NAME).language(ID).build();
                Object function = parseInContext(wrappedSource, realm.getContext()).run(realm);
                return JSRuntime.jsObjectToJavaObject(JSFunction.call(JSArguments.create(Undefined.instance, function, arguments)));
            }
        };
    }

    @TruffleBoundary
    @Override
    protected String toString(JSRealm realm, Object value) {
        return toStringIntl(realm, value, 0);
    }

    protected String toStringIntl(JSRealm realm, Object value, int inDepth) {
        int depth = inDepth + 1;
        if (depth >= MAX_TOSTRING_DEPTH) {
            return "..."; // bail-out from recursions or deep nesting
        }
        if (value == null) {
            return "null";
        } else if (value instanceof JSMetaObject) {
            String type = ((JSMetaObject) value).getClassName();
            if (type == null) {
                type = ((JSMetaObject) value).getType();
            }
            return type;
        } else if (value instanceof Symbol) {
            return value.toString();
        } else if (value instanceof JSLazyString) {
            return value.toString();
        } else if (value instanceof BigInt) {
            return value.toString() + "n";
        } else if (value instanceof TruffleObject && !JSObject.isJSObject(value)) {
            TruffleObject truffleObject = (TruffleObject) value;
            Env env = realm.getEnv();
            try {
                if (env.isHostObject(truffleObject)) {
                    Object hostObject = env.asHostObject(truffleObject);
                    Class<?> clazz = hostObject.getClass();
                    if (clazz == Class.class) {
                        clazz = (Class<?>) hostObject;
                        return "JavaClass[" + clazz.getTypeName() + "]";
                    } else {
                        return "JavaObject[" + clazz.getTypeName() + "]";
                    }
                } else if (ForeignAccess.sendIsNull(Message.IS_NULL.createNode(), truffleObject)) {
                    return "null";
                } else if (ForeignAccess.sendIsPointer(Message.IS_POINTER.createNode(), truffleObject)) {
                    long pointer = ForeignAccess.sendAsPointer(Message.AS_POINTER.createNode(), truffleObject);
                    return "Pointer[0x" + Long.toHexString(pointer) + "]";
                } else if (ForeignAccess.sendHasSize(Message.HAS_SIZE.createNode(), truffleObject)) {
                    return "Array" + foreignArrayToString(realm, truffleObject, depth);
                } else if (ForeignAccess.sendIsExecutable(Message.IS_EXECUTABLE.createNode(), truffleObject)) {
                    return "Executable";
                } else if (ForeignAccess.sendIsBoxed(Message.IS_BOXED.createNode(), truffleObject)) {
                    return toStringIntl(realm, ForeignAccess.sendUnbox(Message.UNBOX.createNode(), truffleObject), depth);
                } else {
                    return "Object" + foreignObjectToString(realm, truffleObject, depth);
                }
            } catch (Exception e) {
                return "Object";
            }
        }
        if (value instanceof Double && ((Double) value) == 0d) {
            if (Double.doubleToLongBits((Double) value) != 0) {
                return "-0";
            } else {
                return "0";
            }
        }
        return JSRuntime.safeToString(value);
    }

    @TruffleBoundary
    protected static ScriptNode parseInContext(Source code, JSContext context) {
        long startTime = JSTruffleOptions.ProfileTime ? System.nanoTime() : 0L;
        try {
            return ((JSParser) context.getEvaluator()).parseScriptNode(context, code);
        } finally {
            if (JSTruffleOptions.ProfileTime) {
                context.getTimeProfiler().printElapsed(startTime, "parsing " + code.getName());
            }
        }
    }

    @TruffleBoundary
    protected static JavaScriptNode parseInline(Source code, JSContext context, MaterializedFrame lexicalContextFrame) {
        long startTime = JSTruffleOptions.ProfileTime ? System.nanoTime() : 0L;
        try {
            Environment env = assembleDebugEnvironment(context, lexicalContextFrame);
            return ((JSParser) context.getEvaluator()).parseInlineExpression(context, code, env, true);
        } finally {
            if (JSTruffleOptions.ProfileTime) {
                context.getTimeProfiler().printElapsed(startTime, "parsing " + code.getName());
            }
        }
    }

    private static Environment assembleDebugEnvironment(JSContext context, MaterializedFrame lexicalContextFrame) {
        Environment env = null;
        ArrayList<FrameDescriptor> frameDescriptors = new ArrayList<>();
        Frame frame = lexicalContextFrame;
        while (frame != null && frame != JSFrameUtil.NULL_MATERIALIZED_FRAME) {
            assert isJSArgumentsArray(frame.getArguments());
            FrameSlot parentSlot;
            while ((parentSlot = frame.getFrameDescriptor().findFrameSlot(ScopeFrameNode.PARENT_SCOPE_IDENTIFIER)) != null) {
                frameDescriptors.add(frame.getFrameDescriptor());
                frame = (Frame) FrameUtil.getObjectSafe(frame, parentSlot);
            }
            frameDescriptors.add(frame.getFrameDescriptor());
            frame = JSArguments.getEnclosingFrame(frame.getArguments());
        }

        for (int i = frameDescriptors.size() - 1; i >= 0; i--) {
            env = new DebugEnvironment(env, NodeFactory.getInstance(context), context, frameDescriptors.get(i));
        }
        return env;
    }

    private static boolean isJSArgumentsArray(Object[] arguments) {
        return arguments != null && arguments.length >= JSArguments.RUNTIME_ARGUMENT_COUNT && JSFunction.isJSFunction(JSArguments.getFunctionObject(arguments));
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
            assert curContext.getContextOptions().equals(toContextOptions(env.getOptions()));
            return curContext;
        }
        JSContext newContext = newOrParentJSContext(env);
        languageContext = newContext;
        return newContext;
    }

    private JSContext newOrParentJSContext(Env env) {
        TruffleContext parent = env.getContext().getParent();
        if (parent == null) {
            return newJSContext(env);
        } else {
            Object prev = parent.enter();
            try {
                return getCurrentContext(JavaScriptLanguage.class).getContext();
            } finally {
                parent.leave(prev);
            }
        }
    }

    private JSContext newJSContext(Env env) {
        JSContext context = JSEngine.createJSContext(this, env);

        if (JSContextOptions.TIME_ZONE.hasBeenSet(env.getOptions())) {
            context.setLocalTimeZoneId(TimeZone.getTimeZone(JSContextOptions.TIME_ZONE.getValue(env.getOptions())).toZoneId());
        }

        context.setInteropRuntime(new JSInteropRuntime(JSForeignAccessFactoryForeign.ACCESS, InteropBoundFunctionForeign.ACCESS));
        return context;
    }

    @Override
    protected void initializeContext(JSRealm realm) {
        realm.setArguments(realm.getEnv().getApplicationArguments());

        if (((GraalJSParserOptions) realm.getContext().getParserOptions()).isScripting()) {
            realm.addScriptingObjects();
        }
    }

    @Override
    protected boolean patchContext(JSRealm realm, Env newEnv) {
        JSContext context = realm.getContext();
        if (!JSContextOptions.optionsAllowPreInitializedContext(realm.getEnv(), newEnv)) {
            languageContext = null;
            return false;
        }

        assert context.getLanguage() == this;
        realm.patchTruffleLanguageEnv(newEnv);

        if (newEnv.out() != realm.getOutputStream()) {
            realm.setOutputWriter(null, newEnv.out());
        }
        if (newEnv.err() != realm.getErrorStream()) {
            realm.setErrorWriter(null, newEnv.err());
        }

        if (JSContextOptions.TIME_ZONE.hasBeenSet(newEnv.getOptions())) {
            context.setLocalTimeZoneId(TimeZone.getTimeZone(JSContextOptions.TIME_ZONE.getValue(newEnv.getOptions())).toZoneId());
        }

        context.setInteropRuntime(new JSInteropRuntime(JSForeignAccessFactoryForeign.ACCESS, InteropBoundFunctionForeign.ACCESS));
        realm.setArguments(newEnv.getApplicationArguments());

        if (((GraalJSParserOptions) context.getParserOptions()).isScripting()) {
            realm.addScriptingObjects();
        }
        return true;
    }

    @Override
    protected void disposeContext(JSRealm realm) {
    }

    @Override
    protected boolean areOptionsCompatible(OptionValues firstOptions, OptionValues newOptions) {
        return firstOptions.equals(newOptions) || toContextOptions(firstOptions).equals(toContextOptions(newOptions));
    }

    private static JSContextOptions toContextOptions(OptionValues optionValues) {
        JSContextOptions newOptions = new JSContextOptions(new GraalJSParserOptions());
        newOptions.setOptionValues(optionValues);
        return newOptions;
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
        String description;

        if (value instanceof JSMetaObject) {
            return "metaobject";
        } else if (JSObject.isJSObject(value)) {
            DynamicObject obj = (DynamicObject) value;
            type = "object";
            description = JSObject.safeToString(obj);
            className = obj == Undefined.instance ? "undefined" : JSRuntime.getConstructorName(obj);

            if (JSFunction.isJSFunction(obj)) {
                DynamicObject func = obj;
                if (JSFunction.isBoundFunction(func)) {
                    func = JSFunction.getBoundTargetFunction(func);
                }
                description = JSObject.safeToString(func);
                type = "function";
            } else if (JSArray.isJSArray(obj)) {
                subtype = "array";
                description = JSArray.CLASS_NAME + "[" + JSArray.arrayGetLength(obj) + "]";
            } else if (JSDate.isJSDate(obj)) {
                subtype = "date";
                description = JSDate.formatUTC(JSDate.getJSDateUTCFormat(), JSDate.getTimeMillisField(obj));
            } else if (JSSymbol.isJSSymbol(obj)) {
                Symbol sym = JSSymbol.getSymbolData(obj);
                type = "symbol";
                description = "Symbol(" + sym.getName() + ")";
            } else if (value == Undefined.instance) {
                type = "undefined";
                description = "undefined";
            } else if (value == Null.instance) {
                subtype = "null";
                description = "null";
            } else if (JSUserObject.isJSUserObject(obj)) {
                description = className;
            }
        } else if (value instanceof TruffleObject && !(value instanceof Symbol) && !(value instanceof JSLazyString)) {
            assert !JSObject.isJSObject(value);
            TruffleObject truffleObject = (TruffleObject) value;
            if (JSInteropNodeUtil.isBoxed(truffleObject)) {
                return findMetaObject(realm, JSInteropNodeUtil.unbox(truffleObject));
            } else if (value instanceof InteropBoundFunction) {
                return findMetaObject(realm, ((InteropBoundFunction) value).getFunction());
            }
            type = "object";
            className = "Foreign";
            description = "foreign TruffleObject";
        } else if (value == null) {
            type = "null";
            description = "null";
        } else {
            // primitive
            type = JSRuntime.typeof(value);
            if (value instanceof Symbol) {
                description = "Symbol(" + ((Symbol) value).getName() + ")";
            } else {
                description = toString(realm, value);
            }
        }

        return new JSMetaObject(type, subtype, className, description, realm.getEnv());
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

    private String foreignArrayToString(JSRealm realm, TruffleObject truffleObject, int depth) throws InteropException {
        CompilerAsserts.neverPartOfCompilation();
        assert ForeignAccess.sendHasSize(JSInteropUtil.createHasSize(), truffleObject);
        int size = ((Number) ForeignAccess.sendGetSize(JSInteropUtil.createGetSize(), truffleObject)).intValue();
        if (size == 0) {
            return "[]";
        }
        Node readNode = JSInteropUtil.createRead();
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < size; i++) {
            Object value = ForeignAccess.sendRead(readNode, truffleObject, i);
            sb.append(value == truffleObject ? "(this)" : toStringIntl(realm, value, depth));
            if (i + 1 < size) {
                sb.append(',').append(' ');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private String foreignObjectToString(JSRealm realm, TruffleObject truffleObject, int depth) throws InteropException {
        CompilerAsserts.neverPartOfCompilation();
        if (!ForeignAccess.sendHasKeys(JSInteropUtil.createHasKeys(), truffleObject)) {
            return "";
        }
        TruffleObject keys = ForeignAccess.sendKeys(JSInteropUtil.createKeys(), truffleObject);
        int keyCount = ((Number) ForeignAccess.sendGetSize(JSInteropUtil.createGetSize(), keys)).intValue();
        if (keyCount == 0) {
            return "{}";
        }
        Node readNode = JSInteropUtil.createRead();
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (int i = 0; i < keyCount; i++) {
            Object key = ForeignAccess.sendRead(readNode, keys, i);
            Object value = ForeignAccess.sendRead(readNode, truffleObject, key);
            sb.append(toStringIntl(realm, key, depth));
            sb.append('=');
            sb.append(value == truffleObject ? "(this)" : toStringIntl(realm, value, depth));
            if (i + 1 < keyCount) {
                sb.append(',').append(' ');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static void ensureErrorClassesInitialized() {
        if (JSTruffleOptions.SubstrateVM) {
            return;
        }
        // Ensure error-related classes are initialized to avoid NoClassDefFoundError
        // during conversion of StackOverflowError to RangeError
        TruffleStackTraceElement.getStackTrace(Errors.createRangeError(""));
    }
}
