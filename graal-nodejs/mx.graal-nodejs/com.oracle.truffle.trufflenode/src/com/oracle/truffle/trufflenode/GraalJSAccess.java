/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode;

import static com.oracle.truffle.js.runtime.util.BufferUtil.asBaseBuffer;
import static com.oracle.truffle.trufflenode.ValueType.ARRAY_BUFFER_VIEW_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.BIG_INT_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.BOOLEAN_VALUE_FALSE;
import static com.oracle.truffle.trufflenode.ValueType.BOOLEAN_VALUE_TRUE;
import static com.oracle.truffle.trufflenode.ValueType.DATA_VIEW_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DATE_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_ARRAY_BUFFER_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_BIGINT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_BIGUINT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_FLOAT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_FLOAT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_INT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_INT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_INT8ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_UINT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_UINT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_UINT8ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_UINT8CLAMPEDARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.EXTERNAL_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.FUNCTION_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_ARRAY_BUFFER_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_BIGINT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_BIGUINT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_FLOAT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_FLOAT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_INT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_INT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_INT8ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_UINT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_UINT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_UINT8ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_UINT8CLAMPEDARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.LAZY_STRING_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.MAP_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.NULL_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.NUMBER_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.ORDINARY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.PROMISE_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.PROXY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.REGEXP_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.SET_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.STRING_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.SYMBOL_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.UNDEFINED_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.UNKNOWN_TYPE;
import static com.oracle.truffle.trufflenode.buffer.NIOBuffer.NIO_BUFFER_MODULE_NAME;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;

import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.InstrumentInfo;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.Breakpoint;
import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.SuspendedCallback;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.builtins.JSONBuiltins;
import com.oracle.truffle.js.builtins.helper.TruffleJSONParser;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.function.ConstructorRootNode;
import com.oracle.truffle.js.parser.GraalJSEvaluator;
import com.oracle.truffle.js.parser.GraalJSParserHelper;
import com.oracle.truffle.js.parser.JSParser;
import com.oracle.truffle.js.parser.JavaScriptTranslator;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.ExitException;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.ImportMetaInitializer;
import com.oracle.truffle.js.runtime.ImportModuleDynamicallyCallback;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSParserOptions;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.PrepareStackTraceCallback;
import com.oracle.truffle.js.runtime.PromiseHook;
import com.oracle.truffle.js.runtime.PromiseRejectionTracker;
import com.oracle.truffle.js.runtime.RegexCompilerInterface;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSUncheckedProxyHandler;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.builtins.JSWeakSet;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSCopyableObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSModuleData;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord.Status;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;
import com.oracle.truffle.js.runtime.util.JSHashMap;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.trufflenode.buffer.NIOBuffer;
import com.oracle.truffle.trufflenode.info.Accessor;
import com.oracle.truffle.trufflenode.info.FunctionTemplate;
import com.oracle.truffle.trufflenode.info.ObjectTemplate;
import com.oracle.truffle.trufflenode.info.PropertyHandler;
import com.oracle.truffle.trufflenode.info.Script;
import com.oracle.truffle.trufflenode.info.UnboundScript;
import com.oracle.truffle.trufflenode.info.Value;
import com.oracle.truffle.trufflenode.node.ArrayBufferGetContentsNode;
import com.oracle.truffle.trufflenode.node.ExecuteNativeFunctionNode;
import com.oracle.truffle.trufflenode.node.ExecuteNativePropertyHandlerNode;
import com.oracle.truffle.trufflenode.node.debug.SetBreakPointNode;
import com.oracle.truffle.trufflenode.serialization.Deserializer;
import com.oracle.truffle.trufflenode.serialization.Serializer;
import com.oracle.truffle.trufflenode.threading.JavaMessagePortData;
import com.oracle.truffle.trufflenode.threading.SharedMemMessagingBindings;
import com.oracle.truffle.trufflenode.threading.SharedMemMessagingManager;

/**
 * Entry point for any access to the JavaScript engine from the native code.
 */
@SuppressWarnings("static-method")
public final class GraalJSAccess {

    private static final boolean VERBOSE = Boolean.getBoolean("truffle.node.js.verbose");
    private static final boolean USE_NIO_BUFFER = !"false".equals(System.getProperty("node.buffer.nio"));
    private static final boolean USE_SNAPSHOTS = !"false".equalsIgnoreCase(System.getProperty("truffle.node.js.snapshots"));

    private static final HiddenKey PRIVATE_VALUES_KEY = new HiddenKey("PrivateValues");
    private static final HiddenKey FUNCTION_TEMPLATE_DATA_KEY = new HiddenKey("FunctionTemplateData");
    public static final HiddenKey INTERNAL_FIELD_COUNT_KEY = new HiddenKey("InternalFieldCount");
    private static final Map<Integer, HiddenKey> INTERNAL_FIELD_KEYS_MAP = new ConcurrentHashMap<>();
    private static final HiddenKey[] INTERNAL_FIELD_KEYS_ARRAY = createInternalFieldKeysArray(10);

    private static final Symbol RESOLVER_RESOLVE = Symbol.create("Resolve");
    private static final Symbol RESOLVER_REJECT = Symbol.create("Reject");

    public static final HiddenKey HOLDER_KEY = new HiddenKey("Holder");

    public static final HiddenKey EXTERNALIZED_KEY = new HiddenKey("Externalized");

    // Placeholders returned by a native function when a primitive value
    // written into a shared buffer should be returned instead
    private static final Object INT_PLACEHOLDER = new Object();
    private static final Object SAFE_INT_PLACEHOLDER = new Object();
    private static final Object DOUBLE_PLACEHOLDER = new Object();

    private final Context evaluator;
    private final JSContext mainJSContext;
    private final JSRealm mainJSRealm;
    private final NodeJSAgent agent;
    private final Deallocator deallocator;
    private ESModuleLoader moduleLoader;

    /** Env that can be used for accessing instruments when no context is active anymore. */
    private final TruffleLanguage.Env envForInstruments;

    /**
     * Direct {@code ByteBuffer} shared with the native code and used to pass additional data from
     * Java. Use it with care: reset the buffer before you use it, make sure that you read the same
     * data in the same order as you write them and perform the reading as soon as possible (as the
     * buffer may be used by any subsequent transition from Java to C).
     */
    private final ByteBuffer sharedBuffer = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());

    /**
     * Caching file content, as used by e.g. the require function. While Node.js currently caches
     * the same file being loaded from the identical location, loading the same content from
     * different locations does not trigger any caching on the Node.js side.
     */
    private final Map<String, Reference<String>> sourceCodeCache = new WeakHashMap<>();

    /**
     * HostDefinedOptions, see v8::ScriptOrModule::GetHostDefinedOptions() and v8::PrimitiveArray.
     */
    private final Map<Source, Object> hostDefinedOptionsMap = new WeakHashMap<>();

    private final boolean exposeGC;

    /**
     * @see Options.OptionsParser#preprocessArguments
     */
    private GraalJSAccess(String[] args) throws Exception {
        try {
            Options options = Options.parseArguments(prepareArguments(args));
            Context.Builder contextBuilder = options.getContextBuilder();

            contextBuilder.option(JSContextOptions.DIRECT_BYTE_BUFFER_NAME, "true");
            contextBuilder.option(JSContextOptions.V8_COMPATIBILITY_MODE_NAME, "true");
            contextBuilder.option(JSContextOptions.INTL_402_NAME, "true");
            contextBuilder.option(JSContextOptions.CLASS_FIELDS_NAME, "true");
            // Node.js does not have global load property
            contextBuilder.option(JSContextOptions.LOAD_NAME, "false");
            // Node.js provides its own console
            contextBuilder.option(JSContextOptions.CONSOLE_NAME, "false");
            // Node.js does not have global arguments property
            contextBuilder.option(JSContextOptions.GLOBAL_ARGUMENTS_NAME, "false");

            exposeGC = options.isGCExposed();
            evaluator = contextBuilder.build();
            mainJSRealm = JavaScriptLanguage.getJSRealm(evaluator);
        } catch (IllegalArgumentException iaex) {
            System.err.printf("ERROR: %s", iaex.getMessage());
            System.exit(1);
            throw iaex; // avoids compiler complaints that final fields are not initialized
        } catch (PolyglotException pex) {
            int exitCode = 1;
            String message = pex.getMessage();
            boolean emptyMessage = message == null || message.isEmpty();
            if (pex.isExit()) {
                exitCode = pex.getExitStatus();
                if (exitCode != 0 && !emptyMessage) {
                    System.err.println(message);
                }
            } else if (pex.isInternalError() || emptyMessage) {
                pex.printStackTrace();
            } else {
                System.err.println("ERROR: " + message);
            }
            System.exit(exitCode);
            throw pex;
        }

        mainJSContext = mainJSRealm.getContext();
        assert mainJSContext != null : "JSContext initialized";
        agent = new NodeJSAgent();
        mainJSRealm.setAgent(agent);
        agent.interopBoundaryEnter();
        deallocator = new Deallocator();
        envForInstruments = mainJSRealm.getEnv();
        // Disallow importing dynamically unless ESM Loader (--experimental-modules) is enabled.
        isolateEnableImportModuleDynamically(false);
    }

    private static String[] prepareArguments(String[] args) {
        String options = System.getenv("NODE_POLYGLOT_OPTIONS");
        if (options == null) {
            return args;
        }
        String[] additionalArgs = options.split(" ");
        String[] mergedArgs = new String[args.length + additionalArgs.length];
        System.arraycopy(args, 0, mergedArgs, 0, args.length);
        System.arraycopy(additionalArgs, 0, mergedArgs, args.length, additionalArgs.length);
        return mergedArgs;
    }

    public static Object create(String[] args) throws Exception {
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        return new GraalJSAccess(args);
    }

    public Object undefinedInstance() {
        return Undefined.instance;
    }

    public Object nullInstance() {
        return Null.instance;
    }

    public void resetSharedBuffer() {
        asBaseBuffer(sharedBuffer).clear();
    }

    public ByteBuffer getSharedBuffer() {
        return DirectByteBufferHelper.cast(sharedBuffer);
    }

    public int valueType(Object value) {
        return valueType(value, false);
    }

    @TruffleBoundary
    public int valueType(Object value, boolean useSharedBuffer) {
        if (value == Undefined.instance) {
            return UNDEFINED_VALUE;
        } else if (value == Null.instance) {
            return NULL_VALUE;
        } else if (value == Boolean.TRUE) {
            return BOOLEAN_VALUE_TRUE;
        } else if (value == Boolean.FALSE) {
            return BOOLEAN_VALUE_FALSE;
        } else if (value instanceof String) {
            return STRING_VALUE;
        } else if (JSRuntime.isNumber(value)) {
            if (useSharedBuffer) {
                sharedBuffer.putDouble(((Number) value).doubleValue());
            }
            return NUMBER_VALUE;
        } else if (JSDynamicObject.isJSDynamicObject(value)) {
            return valueTypeJSObject((DynamicObject) value, useSharedBuffer);
        } else if (JSRuntime.isForeignObject(value)) {
            return valueTypeForeignObject((TruffleObject) value, useSharedBuffer);
        } else if (JSRuntime.isString(value)) { // JSLazyString
            return LAZY_STRING_VALUE;
        } else if (value instanceof Symbol) {
            return SYMBOL_VALUE;
        } else if (value instanceof BigInt) {
            return BIG_INT_VALUE;
        } else if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? BOOLEAN_VALUE_TRUE : BOOLEAN_VALUE_FALSE;
        }
        if (value instanceof Throwable) {
            valueTypeError(value);
        }
        return UNKNOWN_TYPE;
    }

    private int valueTypeForeignObject(TruffleObject value, boolean useSharedBuffer) {
        InteropLibrary interop = InteropLibrary.getFactory().getUncached(value);
        if (interop.isExecutable(value) || interop.isInstantiable(value)) {
            return FUNCTION_OBJECT;
        } else if (interop.isNull(value)) {
            return NULL_VALUE;
        } else if (interop.isBoolean(value)) {
            try {
                return interop.asBoolean(value) ? BOOLEAN_VALUE_TRUE : BOOLEAN_VALUE_FALSE;
            } catch (UnsupportedMessageException e) {
                valueTypeError(value);
                return UNKNOWN_TYPE;
            }
        } else if (interop.isString(value)) {
            return STRING_VALUE;
        } else if (interop.isNumber(value)) {
            return valueTypeForeignNumber(value, interop, useSharedBuffer);
        } else {
            return ORDINARY_OBJECT;
        }
    }

    public int valueTypeForeignNumber(TruffleObject value, InteropLibrary interop, boolean useSharedBuffer) {
        try {
            return valueType(interop.asDouble(value), useSharedBuffer);
        } catch (UnsupportedMessageException e) {
            if (interop.fitsInLong(value)) {
                try {
                    return valueType(interop.asLong(value), useSharedBuffer);
                } catch (UnsupportedMessageException ignore) {
                    // fall through to error case
                }
            }
        }
        valueTypeError(value);
        return UNKNOWN_TYPE;
    }

    private int valueTypeJSObject(DynamicObject obj, boolean useSharedBuffer) {
        if (JSExternal.isJSExternalObject(obj)) {
            return EXTERNAL_OBJECT;
        } else if (JSFunction.isJSFunction(obj)) {
            return FUNCTION_OBJECT;
        } else if (JSArray.isJSArray(obj)) {
            return ARRAY_OBJECT;
        } else if (JSDate.isJSDate(obj)) {
            return DATE_OBJECT;
        } else if (JSRegExp.isJSRegExp(obj)) {
            return REGEXP_OBJECT;
        } else if (JSArrayBufferView.isJSArrayBufferView(obj)) {
            return valueTypeArrayBufferView(obj, useSharedBuffer);
        } else if (JSArrayBuffer.isJSDirectArrayBuffer(obj)) {
            return DIRECT_ARRAY_BUFFER_OBJECT;
        } else if (JSArrayBuffer.isJSInteropArrayBuffer(obj)) {
            return INTEROP_ARRAY_BUFFER_OBJECT;
        } else if (JSDataView.isJSDataView(obj)) {
            if (useSharedBuffer) {
                JSContext context = JSObject.getJSContext(obj);
                sharedBuffer.putInt(arrayBufferViewByteLength(context, obj));
                sharedBuffer.putInt(arrayBufferViewByteOffset(context, obj));
            }
            return DATA_VIEW_OBJECT;
        } else if (JSMap.isJSMap(obj)) {
            return MAP_OBJECT;
        } else if (JSSet.isJSSet(obj)) {
            return SET_OBJECT;
        } else if (JSPromise.isJSPromise(obj)) {
            return PROMISE_OBJECT;
        } else if (JSProxy.isJSProxy(obj)) {
            return PROXY_OBJECT;
        } else {
            return ORDINARY_OBJECT;
        }
    }

    private int valueTypeArrayBufferView(DynamicObject obj, boolean useSharedBuffer) {
        if (useSharedBuffer) {
            JSContext context = JSObject.getJSContext(obj);
            sharedBuffer.putInt(arrayBufferViewByteLength(context, obj));
            sharedBuffer.putInt(arrayBufferViewByteOffset(context, obj));
        }
        ScriptArray array = JSObject.getArray(obj);
        if (array instanceof TypedArray.DirectUint8Array) {
            return DIRECT_UINT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint8ClampedArray) {
            return DIRECT_UINT8CLAMPEDARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt8Array) {
            return DIRECT_INT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint16Array) {
            return DIRECT_UINT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt16Array) {
            return DIRECT_INT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint32Array) {
            return DIRECT_UINT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt32Array) {
            return DIRECT_INT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectFloat32Array) {
            return DIRECT_FLOAT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectFloat64Array) {
            return DIRECT_FLOAT64ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectBigInt64Array) {
            return DIRECT_BIGINT64ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectBigUint64Array) {
            return DIRECT_BIGUINT64ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropUint8Array) {
            return INTEROP_UINT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropUint8ClampedArray) {
            return INTEROP_UINT8CLAMPEDARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropInt8Array) {
            return INTEROP_INT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropUint16Array) {
            return INTEROP_UINT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropInt16Array) {
            return INTEROP_INT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropUint32Array) {
            return INTEROP_UINT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropInt32Array) {
            return INTEROP_INT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropFloat32Array) {
            return INTEROP_FLOAT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropFloat64Array) {
            return INTEROP_FLOAT64ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropBigInt64Array) {
            return INTEROP_BIGINT64ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropBigUint64Array) {
            return INTEROP_BIGUINT64ARRAY_OBJECT;
        } else {
            return ARRAY_BUFFER_VIEW_OBJECT;
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static void valueTypeError(Object value) {
        System.err.println("unknown type: " + ((value == null) ? null : value.getClass().getSimpleName()));
    }

    public double valueDouble(Object obj) {
        return JSRuntime.toDouble(obj);
    }

    public long valueExternal(Object obj) {
        return ((JSExternalObject) obj).getPointer();
    }

    public String valueUnknown(Object obj) {
        return obj.toString();
    }

    public Object valueToObject(Object context, Object value) {
        return JSRuntime.toObject(((JSRealm) context).getContext(), value);
    }

    public Object valueToInteger(Object value) {
        if (value instanceof Double) {
            double doubleValue = (Double) value;
            if (doubleValue < Long.MIN_VALUE || Long.MAX_VALUE < doubleValue) {
                return value; // Integer already
            }
        }
        long integer = JSRuntime.toInteger(value);
        if (JSRuntime.longIsRepresentableAsInt(integer)) {
            return (int) integer;
        } else {
            return (double) integer;
        }
    }

    public Object valueToInt32(Object value) {
        return JSRuntime.toInt32(value);
    }

    public Object valueToUint32(Object value) {
        return (double) JSRuntime.toUInt32(value);
    }

    public String valueToString(Object value) {
        return JSRuntime.toString(value);
    }

    public boolean valueToBoolean(Object value) {
        return JSRuntime.toBoolean(value);
    }

    public Object valueToNumber(Object value) {
        return JSRuntime.toNumber(value);
    }

    public Object valueToArrayIndex(Object value) {
        if (JSRuntime.isArrayIndex(value)) {
            double index = JSRuntime.toDouble(value);
            if (index == 0) {
                index = 0; // handles the negative zero
            }
            resetSharedBuffer();
            sharedBuffer.putDouble(index);
            return index;
        } else {
            return null;
        }
    }

    public int valueInt32Value(Object value) {
        return JSRuntime.toInt32(value);
    }

    public double valueUint32Value(Object value) {
        return JSRuntime.toUInt32(value);
    }

    public long valueIntegerValue(Object value) {
        return JSRuntime.toInteger(value);
    }

    public boolean valueIsNativeError(Object object) {
        return JSError.isJSError(object);
    }

    public boolean valueIsSetIterator(Object object) {
        if (JSRuntime.isObject(object)) {
            DynamicObject dynamicObject = (DynamicObject) object;
            Object iteratedObj = JSObjectUtil.getHiddenProperty(dynamicObject, JSRuntime.ITERATED_OBJECT_ID);
            return JSSet.isJSSet(iteratedObj);
        }
        return false;
    }

    public boolean valueIsMapIterator(Object object) {
        if (JSRuntime.isObject(object)) {
            DynamicObject dynamicObject = (DynamicObject) object;
            Object iteratedObj = JSObjectUtil.getHiddenProperty(dynamicObject, JSRuntime.ITERATED_OBJECT_ID);
            return JSMap.isJSMap(iteratedObj);
        }
        return false;
    }

    public boolean valueIsSharedArrayBuffer(Object object) {
        return JSSharedArrayBuffer.isJSSharedArrayBuffer(object);
    }

    public boolean valueIsArgumentsObject(Object object) {
        return JSArgumentsArray.isJSArgumentsObject(object);
    }

    public boolean valueIsBooleanObject(Object object) {
        return JSBoolean.isJSBoolean(object);
    }

    public boolean valueIsNumberObject(Object object) {
        return JSNumber.isJSNumber(object);
    }

    public boolean valueIsStringObject(Object object) {
        return JSString.isJSString(object);
    }

    public boolean valueIsSymbolObject(Object object) {
        return JSSymbol.isJSSymbol(object);
    }

    public boolean valueIsBigIntObject(Object object) {
        return JSBigInt.isJSBigInt(object);
    }

    public boolean valueIsWeakMap(Object object) {
        return JSWeakMap.isJSWeakMap(object);
    }

    public boolean valueIsWeakSet(Object object) {
        return JSWeakSet.isJSWeakSet(object);
    }

    public boolean valueIsAsyncFunction(Object object) {
        return JSFunction.isJSFunction(object) ? JSFunction.getFunctionData((DynamicObject) object).isAsync() : false;
    }

    public boolean valueIsGeneratorFunction(Object object) {
        return JSFunction.isJSFunction(object) ? JSFunction.getFunctionData((DynamicObject) object).isGenerator() : false;
    }

    public boolean valueIsGeneratorObject(Object object) {
        return (object instanceof DynamicObject) && DynamicObjectLibrary.getUncached().containsKey((DynamicObject) object, JSFunction.GENERATOR_STATE_ID);
    }

    public boolean valueIsModuleNamespaceObject(Object object) {
        return JSModuleNamespace.isJSModuleNamespace(object);
    }

    public boolean valueEquals(Object left, Object right) {
        return JSRuntime.equal(left, right);
    }

    public boolean valueStrictEquals(Object left, Object right) {
        return JSRuntime.identical(left, right);
    }

    public boolean valueInstanceOf(Object left, Object right) {
        if (left instanceof DynamicObject) {
            DynamicObject function = (DynamicObject) right;
            Object hasInstance = JSObject.get(function, Symbol.SYMBOL_HAS_INSTANCE);
            if (hasInstance == Undefined.instance) {
                Object prototype = JSObject.get(function, JSObject.PROTOTYPE);
                if (prototype instanceof DynamicObject) {
                    return JSRuntime.isPrototypeOf((DynamicObject) left, (DynamicObject) prototype);
                } else {
                    throw Errors.createTypeError("prototype is not an Object");
                }
            } else {
                return JSRuntime.toBoolean(JSRuntime.call(hasInstance, function, new Object[]{left}));
            }
        } else {
            return false;
        }
    }

    public String valueTypeOf(Object value) {
        return JSRuntime.typeof(value);
    }

    public Object objectNew(Object context) {
        JSRealm jsRealm = (JSRealm) context;
        return JSOrdinary.create(jsRealm.getContext(), jsRealm);
    }

    public boolean objectSet(Object object, Object key, Object value) {
        assert !(key instanceof HiddenKey);
        Object propertyKey = JSRuntime.toPropertyKey(key);
        JSObject.set((DynamicObject) object, propertyKey, value);
        return true;
    }

    public boolean objectSetIndex(Object object, int index, Object value) {
        JSObject.set((DynamicObject) object, index, value);
        return true;
    }

    public boolean objectForceSet(Object object, Object key, Object value, int attributes) {
        Object propertyKey = JSRuntime.toPropertyKey(key);
        PropertyDescriptor descriptor = propertyDescriptor(attributes, value);
        return JSObject.defineOwnProperty((DynamicObject) object, propertyKey, descriptor);
    }

    public boolean objectSetPrivate(Object context, Object object, Object key, Object value) {
        if (JSRuntime.isObject(object)) {
            DynamicObject dynamicObject = (DynamicObject) object;
            Object privateValues = JSObjectUtil.getHiddenProperty(dynamicObject, PRIVATE_VALUES_KEY);
            if (privateValues == null) {
                privateValues = objectNew(context);
                JSObjectUtil.putHiddenProperty(dynamicObject, PRIVATE_VALUES_KEY, privateValues);
            }
            JSObject.set((DynamicObject) privateValues, key, value);
        }
        return true;
    }

    public Object objectGetPrivate(Object object, Object key) {
        if (!JSDynamicObject.isJSDynamicObject(object)) {
            return null;
        }
        DynamicObject dynamicObject = (DynamicObject) object;
        DynamicObject privateValues = (DynamicObject) JSObjectUtil.getHiddenProperty(dynamicObject, PRIVATE_VALUES_KEY);
        if (privateValues == null) {
            return null;
        } else if (JSObject.hasOwnProperty(privateValues, key)) {
            return JSObject.get(privateValues, key);
        } else {
            return null;
        }
    }

    public boolean objectDeletePrivate(Object object, Object key) {
        if (!JSDynamicObject.isJSDynamicObject(object)) {
            return true;
        }
        DynamicObject dynamicObject = (DynamicObject) object;
        Object privateValues = JSObjectUtil.getHiddenProperty(dynamicObject, PRIVATE_VALUES_KEY);
        if (privateValues == null) {
            return true;
        } else {
            return JSObject.delete((DynamicObject) privateValues, key);
        }
    }

    public Object objectGet(Object object, Object key) {
        assert !(key instanceof HiddenKey);
        Object propertyKey = JSRuntime.toPropertyKey(key);
        Object value;
        if (object instanceof JSDynamicObject) {
            value = JSObject.get((JSDynamicObject) object, propertyKey);
        } else {
            TruffleObject truffleObject;
            if (object instanceof TruffleObject) {
                truffleObject = (TruffleObject) object;
            } else {
                truffleObject = JSRuntime.toObject(mainJSContext, object);
            }
            value = JSInteropUtil.get(truffleObject, propertyKey);
        }
        return processReturnValue(value);
    }

    public Object objectGetIndex(Object object, int index) {
        Object value = JSObject.get((DynamicObject) object, index);
        return processReturnValue(value);
    }

    private Object processReturnValue(Object value) {
        Object flatten = valueFlatten(value);
        resetSharedBuffer();
        asBaseBuffer(sharedBuffer).position(4);
        sharedBuffer.putInt(0, valueType(flatten, true));
        return flatten;
    }

    public Object objectGetOwnPropertyDescriptor(Object object, Object key) {
        DynamicObject dynamicObject = (DynamicObject) object;
        JSContext context = JSObject.getJSContext(dynamicObject);
        PropertyDescriptor desc = JSObject.getOwnProperty(dynamicObject, key);
        return JSRuntime.fromPropertyDescriptor(desc, context);
    }

    public boolean objectDefineProperty(Object object, Object key,
                    Object value, Object get, Object set,
                    boolean hasEnumerable, boolean enumerable,
                    boolean hasConfigurable, boolean configurable,
                    boolean hasWritable, boolean writable) {
        DynamicObject dynamicObject = (DynamicObject) object;
        PropertyDescriptor descriptor = PropertyDescriptor.createEmpty();
        if (value != null) {
            descriptor.setValue(value);
        }
        if (get != null) {
            descriptor.setGet((DynamicObject) get);
        }
        if (set != null) {
            descriptor.setSet((DynamicObject) set);
        }
        if (hasEnumerable) {
            descriptor.setEnumerable(enumerable);
        }
        if (hasConfigurable) {
            descriptor.setConfigurable(configurable);
        }
        if (hasWritable) {
            descriptor.setWritable(writable);
        }
        return JSObject.defineOwnProperty(dynamicObject, key, descriptor);
    }

    public Object valueFlatten(Object value) {
        if (value instanceof String) {
            return value;
        } else if (value instanceof JSLazyString) {
            return ((JSLazyString) value).toString();
        } else if (JSRuntime.isForeignObject(value)) {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached(value);
            if (interop.isString(value)) {
                try {
                    return interop.asString(value);
                } catch (UnsupportedMessageException e) {
                }
            }
            return value;
        } else {
            return value;
        }
    }

    public boolean objectHas(Object object, Object key) {
        return JSObject.hasProperty((DynamicObject) object, JSRuntime.toPropertyKey(key));
    }

    public boolean objectHasOwnProperty(Object object, Object key) {
        return JSObject.hasOwnProperty((DynamicObject) object, key);
    }

    public boolean objectHasRealNamedProperty(Object object, Object key) {
        Object obj = object;
        if (JSProxy.isJSProxy(obj)) {
            obj = JSProxy.getTarget((DynamicObject) obj);
        }
        return objectHasOwnProperty(obj, key);
    }

    public boolean objectDelete(Object object, Object key) {
        return JSObject.delete((DynamicObject) object, JSRuntime.toPropertyKey(key));
    }

    public boolean objectDelete(Object object, long index) {
        return JSObject.delete((DynamicObject) object, index);
    }

    @CompilerDirectives.TruffleBoundary
    public boolean objectSetAccessor(Object object, Object name, long getterPtr, long setterPtr, Object data, int attributes) {
        DynamicObject dynamicObject = (DynamicObject) object;
        JSContext context = JSObject.getJSContext(dynamicObject);
        int flags = propertyAttributes(attributes);
        Accessor accessor = new Accessor(this, name, getterPtr, setterPtr, data, null, flags);
        Pair<JSFunctionData, JSFunctionData> accessorFunctions = accessor.getFunctions(context);
        JSRealm realm = getCurrentRealm();
        DynamicObject getter = functionFromFunctionData(realm, accessorFunctions.getFirst(), dynamicObject);
        DynamicObject setter = functionFromFunctionData(realm, accessorFunctions.getSecond(), dynamicObject);
        JSObjectUtil.putAccessorProperty(context, dynamicObject, accessor.getName(), getter, setter, flags);
        return true;
    }

    public Object objectClone(Object object) {
        // According to Factory::CopyJSObject (v8/src/heap/factory.cc):
        // > We can only clone regexps, normal objects, api objects, errors or arrays.
        // > Copying anything else will break invariants.
        if (object instanceof JSCopyableObject) {
            return ((JSCopyableObject) object).copy();
        } else {
            throw Errors.createTypeErrorFormat("Cannot copy %s", JSRuntime.safeToString(object));
        }
    }

    public boolean objectSetPrototype(Object object, Object prototype) {
        return JSObject.setPrototype((DynamicObject) object, (DynamicObject) prototype);
    }

    public Object objectGetPrototype(Object object) {
        return JSObject.getPrototype((DynamicObject) object);
    }

    public String objectGetConstructorName(Object object) {
        String name = "Object";
        if (JSDynamicObject.isJSDynamicObject(object)) {
            DynamicObject dynamicObject = (DynamicObject) object;
            Object constructor = JSObject.get(dynamicObject, JSObject.CONSTRUCTOR);
            if (JSFunction.isJSFunction(constructor)) {
                name = JSFunction.getName((DynamicObject) constructor);
            }
        } else {
            InteropLibrary interop = InteropLibrary.getUncached(object);
            if (interop.hasMetaObject(object)) {
                try {
                    Object metaObject = interop.getMetaObject(object);
                    Object interopName = InteropLibrary.getUncached(metaObject).getMetaSimpleName(metaObject);
                    name = InteropLibrary.getUncached(interopName).asString(interopName);
                } catch (UnsupportedMessageException ex) {
                    throw Errors.shouldNotReachHere(ex);
                }
            }
        }
        return name;
    }

    private static String foreignStringToString(Object foreignString) throws UnsupportedMessageException {
        if (foreignString instanceof String) {
            return (String) foreignString;
        } else {
            return InteropLibrary.getFactory().getUncached(foreignString).asString(foreignString);
        }
    }

    private static JSRealm getCurrentRealm() {
        return JSRealm.get(null);
    }

    public Object objectGetOwnPropertyNames(Object object) {
        Object[] namesArray;
        if (JSDynamicObject.isJSDynamicObject(object)) {
            DynamicObject dynamicObject = (DynamicObject) object;
            List<String> names = JSObject.enumerableOwnNames(dynamicObject);
            namesArray = names.toArray();
            convertArrayIndicesToNumbers(namesArray);
        } else {
            assert JSRuntime.isForeignObject(object);
            try {
                List<Object> names = new ArrayList<>();
                InteropLibrary library = InteropLibrary.getFactory().getUncached(object);
                if (library.hasMembers(object)) {
                    Object members = library.getMembers(object);
                    InteropLibrary membersLibrary = InteropLibrary.getFactory().getUncached(members);
                    long size = membersLibrary.getArraySize(members);
                    for (long i = 0; i < size; i++) {
                        Object key = membersLibrary.readArrayElement(members, i);
                        names.add(foreignStringToString(key));
                    }
                }
                if (library.hasArrayElements(object)) {
                    long size = library.getArraySize(object);
                    for (long i = 0; i < size; i++) {
                        if (library.isArrayElementExisting(object, i)) {
                            names.add(JSRuntime.longToIntOrDouble(i));
                        }
                    }
                }
                namesArray = names.toArray();
            } catch (UnsupportedMessageException | InvalidArrayIndexException ex) {
                namesArray = new Object[0];
            }
        }
        return JSArray.createConstantObjectArray(mainJSContext, getCurrentRealm(), namesArray);
    }

    public Object objectGetPropertyNames(Object object, boolean ownOnly,
                    boolean enumerableOnly, boolean configurableOnly, boolean writableOnly,
                    boolean skipIndices, boolean skipSymbols, boolean skipStrings,
                    boolean keepNumbers) {
        Object[] propertyNames;
        if (JSDynamicObject.isJSDynamicObject(object)) {
            Set<Object> keys = new LinkedHashSet<>();
            DynamicObject dynamicObject = (DynamicObject) object;
            do {
                JSClass jsclass = JSObject.getJSClass(dynamicObject);
                Iterable<Object> ownKeys = jsclass.ownPropertyKeys(dynamicObject);
                for (Object key : ownKeys) {
                    Object keyToStore = key;
                    if (key instanceof String) {
                        if (skipStrings) {
                            continue;
                        }
                        boolean index = JSRuntime.isArrayIndex((String) key);
                        if (index) {
                            if (skipIndices) {
                                continue;
                            }
                            if (keepNumbers) {
                                keyToStore = JSRuntime.stringToNumber((String) key);
                            }
                        }
                    } else {
                        assert key instanceof Symbol;
                        if (skipSymbols) {
                            continue;
                        }
                    }
                    PropertyDescriptor desc = jsclass.getOwnProperty(dynamicObject, key);
                    if ((enumerableOnly && (desc == null || !desc.getEnumerable())) || (configurableOnly && (desc == null || !desc.getConfigurable())) ||
                                    (writableOnly && (desc == null || !desc.getWritable()))) {
                        continue;
                    }
                    keys.add(keyToStore);
                }
                dynamicObject = JSObject.getPrototype(dynamicObject);
            } while (!ownOnly && dynamicObject != Null.instance);
            propertyNames = keys.toArray();
        } else {
            assert JSRuntime.isForeignObject(object);
            try {
                List<Object> keys = new ArrayList<>();
                InteropLibrary library = InteropLibrary.getFactory().getUncached(object);
                if (!skipStrings && library.hasMembers(object)) {
                    Object members = library.getMembers(object);
                    InteropLibrary membersLibrary = InteropLibrary.getFactory().getUncached(members);
                    long size = membersLibrary.getArraySize(members);
                    for (long i = 0; i < size; i++) {
                        Object key = membersLibrary.readArrayElement(members, i);
                        String stringKey = foreignStringToString(key);
                        if (!writableOnly || library.isMemberWritable(object, stringKey)) {
                            keys.add(stringKey);
                        }
                    }
                }
                if (!skipIndices && library.hasArrayElements(object)) {
                    long size = library.getArraySize(object);
                    for (long i = 0; i < size; i++) {
                        if (library.isArrayElementExisting(object, i)) {
                            Object key;
                            if (keepNumbers) {
                                key = JSRuntime.longToIntOrDouble(i);
                            } else {
                                key = String.valueOf(i);
                            }
                            if (!writableOnly || library.isArrayElementWritable(object, i)) {
                                keys.add(key);
                            }
                        }
                    }
                }
                propertyNames = keys.toArray();
            } catch (UnsupportedMessageException | InvalidArrayIndexException ex) {
                propertyNames = new Object[0];
            }
        }
        return JSArray.createConstantObjectArray(mainJSContext, getCurrentRealm(), propertyNames);
    }

    private void convertArrayIndicesToNumbers(Object[] namesArray) {
        for (int i = 0; i < namesArray.length; i++) {
            Object name = namesArray[i];
            if (name instanceof String && JSRuntime.isArrayIndex((String) name)) {
                namesArray[i] = JSRuntime.stringToNumber((String) name);
            }
        }
    }

    public Object objectGetRealNamedProperty(Object object, Object key) {
        Object propertyKey = JSRuntime.toPropertyKey(key);
        Object current = object;
        while (JSRuntime.isObject(current)) {
            DynamicObject currentDO = (DynamicObject) current;
            if (JSProxy.isJSProxy(currentDO)) {
                current = JSProxy.getTarget(currentDO);
            } else {
                if (JSObject.hasOwnProperty(currentDO, propertyKey)) {
                    return JSObject.get(currentDO, propertyKey);
                }
                current = JSObject.getPrototype(currentDO);
            }
        }
        return null;
    }

    public int objectGetRealNamedPropertyAttributes(Object object, Object key) {
        Object propertyKey = JSRuntime.toPropertyKey(key);
        Object current = object;
        while (JSRuntime.isObject(current)) {
            DynamicObject currentDO = (DynamicObject) current;
            if (JSProxy.isJSProxy(currentDO)) {
                current = JSProxy.getTarget(currentDO);
            } else {
                PropertyDescriptor descriptor = JSObject.getOwnProperty(currentDO, propertyKey);
                if (descriptor != null) {
                    int attributes = 0;
                    if (!descriptor.isAccessorDescriptor() && !descriptor.getWritable()) {
                        attributes |= 1; /* v8::PropertyAttribute::ReadOnly */
                    }
                    if (!descriptor.getEnumerable()) {
                        attributes |= 2; /* v8::PropertyAttribute::DontEnum */
                    }
                    if (!descriptor.getConfigurable()) {
                        attributes |= 4; /* v8::PropertyAttribute::DontDelete */
                    }
                    return attributes;
                }
                current = JSObject.getPrototype(currentDO);
            }
        }
        return -1;
    }

    public Object objectCreationContext(Object object) {
        DynamicObject dynamicObject = (DynamicObject) object;
        if (JSFunction.isJSFunction(object)) {
            return JSFunction.getRealm(dynamicObject);
        } else {
            return objectCreationContextFromConstructor(dynamicObject);
        }
    }

    private Object objectCreationContextFromConstructor(DynamicObject object) {
        // V8 has a link to the constructor in the object's map which is used to get the context.
        // We try to get the constructor via the object's prototype instead.
        if (!JSProxy.isJSProxy(object)) {
            DynamicObject prototype = JSObject.getPrototype(object);
            if (prototype != Null.instance) {
                Object constructor = JSRuntime.getDataProperty(prototype, JSObject.CONSTRUCTOR);
                if (JSFunction.isJSFunction(constructor)) {
                    return JSFunction.getRealm((DynamicObject) constructor);
                }
            }
        } else if (JSRuntime.isCallableProxy(object)) {
            // Callable Proxy: get the creation context from the target function.
            return objectCreationContext(JSProxy.getTarget(object));
        }
        return mainJSRealm;
    }

    public void objectSetIntegrityLevel(Object object, boolean freeze) {
        if (JSDynamicObject.isJSDynamicObject(object)) {
            JSObject.setIntegrityLevel((DynamicObject) object, freeze, true);
        }
    }

    public Object arrayNew(Object context, int length) {
        JSRealm realm = (JSRealm) context;
        return JSArray.createConstantEmptyArray(realm.getContext(), realm, length);
    }

    public Object arrayNewFromElements(Object context, Object[] elements) {
        JSRealm realm = (JSRealm) context;
        return JSArray.createConstantObjectArray(realm.getContext(), realm, elements);
    }

    public long arrayLength(Object object) {
        return JSArray.arrayGetLength((DynamicObject) object);
    }

    public Object arrayBufferNew(Object context, Object buffer, long pointer) {
        ByteBuffer byteBuffer = (ByteBuffer) buffer;
        if (pointer != 0) {
            deallocator.register(byteBuffer, pointer);
        }
        JSRealm realm = (JSRealm) context;
        DynamicObject arrayBuffer = JSArrayBuffer.createDirectArrayBuffer(realm.getContext(), realm, byteBuffer);
        JSObjectUtil.putHiddenProperty(arrayBuffer, EXTERNALIZED_KEY, pointer == 0);
        return arrayBuffer;
    }

    public Object arrayBufferNew(Object context, int byteLength) {
        JSRealm realm = (JSRealm) context;
        return JSArrayBuffer.createDirectArrayBuffer(realm.getContext(), realm, byteLength);
    }

    public Object arrayBufferNewBackingStore(int byteLength) {
        return DirectByteBufferHelper.allocateDirect(byteLength);
    }

    public long arrayBufferByteLength(Object arrayBuffer) {
        if (JSArrayBuffer.isJSInteropArrayBuffer(arrayBuffer)) {
            return ((JSArrayBufferObject.Interop) arrayBuffer).getByteLength();
        } else {
            return JSArrayBuffer.getDirectByteLength(arrayBuffer);
        }
    }

    public Object arrayBufferGetContents(Object arrayBuffer) {
        if (JSArrayBuffer.isJSInteropArrayBuffer(arrayBuffer)) {
            return interopArrayBufferGetContents(arrayBuffer);
        } else {
            return JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
        }
    }

    private Object interopArrayBufferGetContents(Object arrayBuffer) {
        assert JSArrayBuffer.isJSInteropArrayBuffer(arrayBuffer);
        Object interopBuffer = JSArrayBufferObject.getInteropBuffer(arrayBuffer);
        RealmData realmEmbedderData = getRealmEmbedderData(mainJSRealm);
        DynamicObject function = realmEmbedderData.getArrayBufferGetContentsFunction();
        if (function == null) {
            function = JSFunction.create(mainJSRealm, getContextEmbedderData(mainJSContext).getOrCreateFunctionData(
                            ContextData.FunctionKey.ArrayBufferGetContents, GraalJSAccess::createInteropBufferGetContents));
            realmEmbedderData.setArrayBufferGetContentsFunction(function);
        }
        return JSFunction.call(JSArguments.createOneArg(Undefined.instance, function, interopBuffer));
    }

    private static JSFunctionData createInteropBufferGetContents(JSContext context) {
        class InteropArrayBufferGetContents extends JavaScriptRootNode {
            @Child private JavaScriptNode valueNode = AccessIndexedArgumentNode.create(0);
            @Child private ArrayBufferGetContentsNode bufferGetContents = ArrayBufferGetContentsNode.create();

            @Override
            public Object execute(VirtualFrame frame) {
                Object value = valueNode.execute(frame);
                return bufferGetContents.execute(value);
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new InteropArrayBufferGetContents());
        return JSFunctionData.createCallOnly(context, callTarget, 1, "ArrayBufferGetContents");
    }

    public Object arrayBufferViewBuffer(Object arrayBufferView) {
        DynamicObject dynamicObject = (DynamicObject) arrayBufferView;
        if (JSDataView.isJSDataView(arrayBufferView)) {
            return JSDataView.getArrayBuffer(dynamicObject);
        } else {
            return JSArrayBufferView.getArrayBuffer(dynamicObject);
        }
    }

    public int arrayBufferViewByteLength(Object arrayBufferView) {
        DynamicObject dynamicObject = (DynamicObject) arrayBufferView;
        return arrayBufferViewByteLength(JSObject.getJSContext(dynamicObject), dynamicObject);
    }

    public boolean arrayBufferIsExternal(Object arrayBuffer) {
        return JSObjectUtil.getHiddenProperty((DynamicObject) arrayBuffer, EXTERNALIZED_KEY) == Boolean.TRUE;
    }

    public void arrayBufferExternalize(Object arrayBuffer) {
        DynamicObject dynamicObject = (DynamicObject) arrayBuffer;
        JSObjectUtil.putHiddenProperty(dynamicObject, EXTERNALIZED_KEY, true);
    }

    public void arrayBufferDetach(Object arrayBuffer) {
        JSArrayBuffer.detachArrayBuffer((DynamicObject) arrayBuffer);
    }

    public static int arrayBufferViewByteLength(JSContext context, DynamicObject arrayBufferView) {
        if (JSDataView.isJSDataView(arrayBufferView)) {
            return JSDataView.typedArrayGetLength(arrayBufferView);
        } else {
            return JSArrayBufferView.getByteLength(arrayBufferView, context);
        }
    }

    public int arrayBufferViewByteOffset(Object arrayBufferView) {
        DynamicObject dynamicObject = (DynamicObject) arrayBufferView;
        return arrayBufferViewByteOffset(JSObject.getJSContext(dynamicObject), dynamicObject);
    }

    public static int arrayBufferViewByteOffset(JSContext context, DynamicObject arrayBufferView) {
        if (JSDataView.isJSDataView(arrayBufferView)) {
            return JSDataView.typedArrayGetOffset(arrayBufferView);
        } else {
            return JSArrayBufferView.getByteOffset(arrayBufferView, context);
        }
    }

    private static final ReferenceQueue<JSAgentWaiterList> agentWaiterListQueue = new ReferenceQueue<>();
    private static final Map<Long, WeakReference<JSAgentWaiterList>> agentWaiterListMap = new HashMap<>();

    private static class WeakAgentWaiterList extends WeakReference<JSAgentWaiterList> {
        long pointer;

        WeakAgentWaiterList(JSAgentWaiterList wl, long pointer) {
            super(wl, agentWaiterListQueue);
            this.pointer = pointer;
        }

    }

    private static void pollAgentWaiterListQueue() {
        WeakAgentWaiterList ref;
        while ((ref = (WeakAgentWaiterList) agentWaiterListQueue.poll()) != null) {
            if (agentWaiterListMap.get(ref.pointer).get() == null) {
                agentWaiterListMap.remove(ref.pointer);
            }
        }
    }

    private static void updateWaiterList(DynamicObject sharedArrayBuffer, long pointer) {
        synchronized (agentWaiterListMap) {
            pollAgentWaiterListQueue();
            assert JSSharedArrayBuffer.isJSSharedArrayBuffer(sharedArrayBuffer);
            assert pointer != 0;
            WeakReference<JSAgentWaiterList> ref = agentWaiterListMap.get(pointer);
            JSAgentWaiterList wl = (ref == null) ? null : ref.get();
            if (wl == null) {
                // new data block => save wl
                ref = new WeakAgentWaiterList(JSSharedArrayBuffer.getWaiterList(sharedArrayBuffer), pointer);
                agentWaiterListMap.put(pointer, ref);
            } else {
                // known data block => share wl
                JSSharedArrayBuffer.setWaiterList(sharedArrayBuffer, wl);
            }
        }
    }

    public Object sharedArrayBufferNew(Object context, Object buffer, long pointer, boolean externalized) {
        ByteBuffer byteBuffer = (ByteBuffer) buffer;
        JSRealm realm = (JSRealm) context;
        DynamicObject sharedArrayBuffer = JSSharedArrayBuffer.createSharedArrayBuffer(realm.getContext(), realm, byteBuffer);
        JSObjectUtil.putHiddenProperty(sharedArrayBuffer, EXTERNALIZED_KEY, externalized);
        if (externalized) {
            updateWaiterList(sharedArrayBuffer, pointer);
        } else {
            deallocator.register(byteBuffer, pointer);
        }
        return sharedArrayBuffer;
    }

    public boolean sharedArrayBufferIsExternal(Object sharedArrayBuffer) {
        return JSObjectUtil.getHiddenProperty((DynamicObject) sharedArrayBuffer, EXTERNALIZED_KEY) == Boolean.TRUE;
    }

    public Object sharedArrayBufferGetContents(Object sharedArrayBuffer) {
        return JSSharedArrayBuffer.getDirectByteBuffer((DynamicObject) sharedArrayBuffer);
    }

    public void sharedArrayBufferExternalize(Object sharedArrayBuffer, long pointer) {
        if (!sharedArrayBufferIsExternal(sharedArrayBuffer)) {
            DynamicObject dynamicObject = (DynamicObject) sharedArrayBuffer;
            JSObjectUtil.putHiddenProperty(dynamicObject, EXTERNALIZED_KEY, true);
            updateWaiterList(dynamicObject, pointer);
        }
    }

    public int typedArrayLength(Object typedArray) {
        return JSArrayBufferView.typedArrayGetLength((DynamicObject) typedArray);
    }

    private Object typedArrayNew(Object arrayBuffer, int offset, int length, TypedArrayFactory factory) {
        TypedArray arrayType = factory.createArrayType(true, offset != 0);
        DynamicObject dynamicObject = (DynamicObject) arrayBuffer;
        JSContext context = JSObject.getJSContext(dynamicObject);
        JSRealm realm = getCurrentRealm();
        boolean detached = JSArrayBuffer.isDetachedBuffer(dynamicObject);
        if (detached) {
            dynamicObject = JSArrayBuffer.createDirectArrayBuffer(context, realm, 0);
        }
        DynamicObject result = JSArrayBufferView.createArrayBufferView(context, realm, dynamicObject, arrayType, offset, length);
        if (detached) {
            JSArrayBuffer.detachArrayBuffer(dynamicObject);
        }
        return result;
    }

    public Object uint8ArrayNew(Object arrayBuffer, int offset, int length) {
        return typedArrayNew(arrayBuffer, offset, length, TypedArrayFactory.Uint8Array);
    }

    public Object uint8ClampedArrayNew(Object arrayBuffer, int offset, int length) {
        return typedArrayNew(arrayBuffer, offset, length, TypedArrayFactory.Uint8ClampedArray);
    }

    public Object int8ArrayNew(Object arrayBuffer, int offset, int length) {
        return typedArrayNew(arrayBuffer, offset, length, TypedArrayFactory.Int8Array);
    }

    public Object uint16ArrayNew(Object arrayBuffer, int offset, int length) {
        return typedArrayNew(arrayBuffer, offset, length, TypedArrayFactory.Uint16Array);
    }

    public Object int16ArrayNew(Object arrayBuffer, int offset, int length) {
        return typedArrayNew(arrayBuffer, offset, length, TypedArrayFactory.Int16Array);
    }

    public Object uint32ArrayNew(Object arrayBuffer, int offset, int length) {
        return typedArrayNew(arrayBuffer, offset, length, TypedArrayFactory.Uint32Array);
    }

    public Object int32ArrayNew(Object arrayBuffer, int offset, int length) {
        return typedArrayNew(arrayBuffer, offset, length, TypedArrayFactory.Int32Array);
    }

    public Object float32ArrayNew(Object arrayBuffer, int offset, int length) {
        return typedArrayNew(arrayBuffer, offset, length, TypedArrayFactory.Float32Array);
    }

    public Object float64ArrayNew(Object arrayBuffer, int offset, int length) {
        return typedArrayNew(arrayBuffer, offset, length, TypedArrayFactory.Float64Array);
    }

    public Object bigInt64ArrayNew(Object arrayBuffer, int offset, int length) {
        return typedArrayNew(arrayBuffer, offset, length, TypedArrayFactory.BigInt64Array);
    }

    public Object bigUint64ArrayNew(Object arrayBuffer, int offset, int length) {
        return typedArrayNew(arrayBuffer, offset, length, TypedArrayFactory.BigUint64Array);
    }

    public Object dataViewNew(Object arrayBuffer, int offset, int length) {
        DynamicObject dynamicObject = (DynamicObject) arrayBuffer;
        JSRealm realm = getCurrentRealm();
        return JSDataView.createDataView(realm.getContext(), realm, dynamicObject, offset, length);
    }

    public Object externalNew(Object context, long pointer) {
        return JSExternal.create(((JSRealm) context).getContext(), pointer);
    }

    public Object integerNew(long value) {
        return (double) value;
    }

    public Object numberNew(double value) {
        return value;
    }

    public Object dateNew(Object context, double value) {
        JSRealm realm = (JSRealm) context;
        return JSDate.create(realm.getContext(), realm, value);
    }

    public double dateValueOf(Object date) {
        return JSDate.getTimeMillisField((DynamicObject) date);
    }

    public Object symbolNew(Object name) {
        return Symbol.create((String) name);
    }

    public Object symbolName(Object symbol) {
        return ((Symbol) symbol).getDescription();
    }

    public Object symbolGetIterator() {
        return Symbol.SYMBOL_ITERATOR;
    }

    public Object functionNewInstance(Object function, Object[] arguments) {
        DynamicObject functionObject = (DynamicObject) function;
        JSFunctionData functionData = JSFunction.getFunctionData(functionObject);
        Object[] callArguments = JSArguments.create(null, function, arguments);
        return functionData.getConstructTarget().call(callArguments);
    }

    public void functionSetName(Object function, String name) {
        DynamicObject functionObject = (DynamicObject) function;
        JSFunctionData functionData = JSFunction.getFunctionData(functionObject);
        functionData.setName(name);
    }

    public String functionGetName(Object function) {
        DynamicObject functionObject = (DynamicObject) function;
        JSFunctionData functionData = JSFunction.getFunctionData(functionObject);
        return functionData.getName();
    }

    public Object functionCall(Object function, Object receiver, Object[] arguments) {
        Object value = JSRuntime.call(function, receiver, arguments);
        return processReturnValue(value);
    }

    public Object functionCall0(Object function, Object receiver) {
        return functionCall(function, receiver, JSArguments.EMPTY_ARGUMENTS_ARRAY);
    }

    public Object functionCall1(Object function, Object receiver, Object arg0) {
        return functionCall(function, receiver, new Object[]{arg0});
    }

    public Object functionCall2(Object function, Object receiver, Object arg0, Object arg1) {
        return functionCall(function, receiver, new Object[]{arg0, arg1});
    }

    public Object functionCall3(Object function, Object receiver, Object arg0, Object arg1, Object arg2) {
        return functionCall(function, receiver, new Object[]{arg0, arg1, arg2});
    }

    public Object functionCall4(Object function, Object receiver, Object arg0, Object arg1, Object arg2, Object arg3) {
        return functionCall(function, receiver, new Object[]{arg0, arg1, arg2, arg3});
    }

    public Object functionCall5(Object function, Object receiver, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
        return functionCall(function, receiver, new Object[]{arg0, arg1, arg2, arg3, arg4});
    }

    public Object functionCall6(Object function, Object receiver, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        return functionCall(function, receiver, new Object[]{arg0, arg1, arg2, arg3, arg4, arg5});
    }

    public Object functionCall7(Object function, Object receiver, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
        return functionCall(function, receiver, new Object[]{arg0, arg1, arg2, arg3, arg4, arg5, arg6});
    }

    public Object functionCall8(Object function, Object receiver, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
        return functionCall(function, receiver, new Object[]{arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7});
    }

    public Object functionCall9(Object function, Object receiver, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
        return functionCall(function, receiver, new Object[]{arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8});
    }

    private static SourceSection functionGetSourceSection(Object function) {
        if (JSFunction.isJSFunction(function)) {
            CallTarget callTarget = JSFunction.getCallTarget((DynamicObject) function);
            if (callTarget instanceof RootCallTarget) {
                RootNode rootNode = ((RootCallTarget) callTarget).getRootNode();
                return rootNode.getSourceSection();
            }
        }
        return null;
    }

    public Object functionResourceName(Object function) {
        SourceSection sourceSection = functionGetSourceSection(function);
        if (sourceSection == null) {
            return null;
        } else {
            Source source = sourceSection.getSource();
            return source.getName();
        }
    }

    public int functionGetScriptLineNumber(Object function) {
        SourceSection sourceSection = functionGetSourceSection(function);
        if (sourceSection == null) {
            return -1; // Function::kLineOffsetNotFound
        } else {
            return sourceSection.getStartLine() - 1;
        }
    }

    public int functionGetScriptColumnNumber(Object function) {
        SourceSection sourceSection = functionGetSourceSection(function);
        if (sourceSection == null) {
            return -1; // Function::kLineOffsetNotFound
        } else {
            String code = sourceSection.getCharacters().toString();
            int idx = code.indexOf('(');
            int delta = (idx == -1) ? 0 : idx;
            return sourceSection.getStartColumn() + delta - 1;
        }
    }

    public Object exceptionError(Object context, Object object) {
        return exceptionCreate((JSRealm) context, JSErrorType.Error, object);
    }

    public Object exceptionTypeError(Object context, Object object) {
        return exceptionCreate((JSRealm) context, JSErrorType.TypeError, object);
    }

    public Object exceptionSyntaxError(Object context, Object object) {
        return exceptionCreate((JSRealm) context, JSErrorType.SyntaxError, object);
    }

    public Object exceptionRangeError(Object context, Object object) {
        return exceptionCreate((JSRealm) context, JSErrorType.RangeError, object);
    }

    public Object exceptionReferenceError(Object context, Object object) {
        return exceptionCreate((JSRealm) context, JSErrorType.ReferenceError, object);
    }

    private Object exceptionCreate(JSRealm realm, JSErrorType errorType, Object message) {
        DynamicObject error = JSError.create(errorType, realm, message);
        assert JSError.getException(error) != null;
        return error;
    }

    public Object exceptionCreateMessage(Object exceptionObject) {
        return exceptionObjectToException(exceptionObject);
    }

    private GraalJSException exceptionObjectToException(Object exceptionObject) {
        if (JSDynamicObject.isJSDynamicObject(exceptionObject)) {
            GraalJSException exception = JSError.getException((DynamicObject) exceptionObject);
            if (exception != null) {
                return exception;
            }
        }
        return UserScriptException.create(exceptionObject);
    }

    public void isolateThrowException(Object exceptionObject) {
        throw exceptionObjectToException(exceptionObject);
    }

    public void templateSet(Object templateObj, Object name, Object value, int attributes) {
        ObjectTemplate template;
        if (templateObj instanceof FunctionTemplate) {
            template = ((FunctionTemplate) templateObj).getFunctionObjectTemplate();
        } else {
            template = (ObjectTemplate) templateObj;
        }
        template.addValue(new Value(name, value, propertyAttributes(attributes)));
    }

    public void templateSetAccessorProperty(Object templateObj, Object name, Object getter, Object setter, int attributes) {
        templateSet(templateObj, name, new Pair<>(getter, setter), attributes);
    }

    public Object functionTemplateNew(int id, long pointer, Object additionalData, Object signature, int length, boolean isConstructor, boolean singleFunctionTemplate) {
        FunctionTemplate template = new FunctionTemplate(id, pointer, additionalData, (FunctionTemplate) signature, length, isConstructor, singleFunctionTemplate);
        template.getInstanceTemplate().setParentFunctionTemplate(template);
        return template;
    }

    public void functionTemplateSetCallHandler(Object templateObj, long funcPointer, Object additionalData) {
        FunctionTemplate functionTemplate = (FunctionTemplate) templateObj;
        functionTemplate.setFunctionPointer(funcPointer);
        functionTemplate.setAdditionalData(additionalData);
    }

    public void functionTemplateInherit(Object templateObj, Object parent) {
        FunctionTemplate functionTemplate = (FunctionTemplate) templateObj;
        functionTemplate.setParent((FunctionTemplate) parent);
    }

    public void functionTemplateSetClassName(Object templateObj, Object name) {
        FunctionTemplate functionTemplate = (FunctionTemplate) templateObj;
        functionTemplate.setClassName((String) name);
        ObjectTemplate instanceTemplate = functionTemplate.getInstanceTemplate();
        instanceTemplate.addValue(new Value(Symbol.SYMBOL_TO_STRING_TAG, name, JSAttributes.configurableNotEnumerableNotWritable()));
    }

    public Object functionTemplateInstanceTemplate(Object templateObj) {
        FunctionTemplate functionTemplate = (FunctionTemplate) templateObj;
        return functionTemplate.getInstanceTemplate();
    }

    public Object functionTemplatePrototypeTemplate(Object templateObj) {
        FunctionTemplate functionTemplate = (FunctionTemplate) templateObj;
        return functionTemplate.getPrototypeTemplate();
    }

    public Object functionTemplateGetFunction(Object realm, Object templateObj) {
        JSRealm jsRealm = (JSRealm) realm;
        JSContext jsContext = jsRealm.getContext();
        FunctionTemplate template = (FunctionTemplate) templateObj;

        if (template.getFunctionObject(jsRealm) == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            DynamicObject obj = functionTemplateCreateCallback(jsContext, jsRealm, template);
            objectTemplateInstantiate(jsRealm, template.getFunctionObjectTemplate(), obj);

            ObjectTemplate prototypeTemplate = template.getPrototypeTemplate();
            if (prototypeTemplate != null) {
                DynamicObject proto = JSOrdinary.create(jsContext, jsRealm);
                objectTemplateInstantiate(jsRealm, prototypeTemplate, proto);
                JSObjectUtil.putConstructorProperty(jsContext, proto, obj);
                JSObject.set(obj, JSObject.PROTOTYPE, proto);

                FunctionTemplate parentTemplate = template.getParent();
                if (parentTemplate != null) {
                    DynamicObject parentFunction = (DynamicObject) functionTemplateGetFunction(realm, parentTemplate);
                    DynamicObject parentProto = (DynamicObject) JSObject.get(parentFunction, JSObject.PROTOTYPE);
                    JSObject.setPrototype(proto, parentProto);
                }
            }

            if (template.hasReadOnlyPrototype()) {
                PropertyDescriptor desc = PropertyDescriptor.createEmpty();
                desc.setWritable(false);
                JSObject.defineOwnProperty(obj, JSObject.PROTOTYPE, desc);
            }
        }

        return template.getFunctionObject(jsRealm);
    }

    private DynamicObject functionTemplateCreateCallback(JSContext context, JSRealm realm, FunctionTemplate template) {
        CompilerAsserts.neverPartOfCompilation("do not create function template in compiled code");

        JSFunctionData functionData = template.getFunctionData();
        if (functionData == null) {
            JSOrdinary instanceLayout = template.getInstanceTemplate().getInternalFieldCount() > 0 ? JSOrdinary.INTERNAL_FIELD_INSTANCE : JSOrdinary.INSTANCE;
            functionData = JSFunctionData.create(context, template.getLength(), template.getClassName(), template.getPrototypeTemplate() != null, false, false, false);
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new ExecuteNativeFunctionNode.NativeFunctionRootNode(this, context, template, false, false));
            CallTarget newCallTarget = Truffle.getRuntime().createCallTarget(new ExecuteNativeFunctionNode.NativeFunctionRootNode(this, context, template, true, false));
            CallTarget newTargetCallTarget = Truffle.getRuntime().createCallTarget(new ExecuteNativeFunctionNode.NativeFunctionRootNode(this, context, template, true, true));
            CallTarget constructTarget = Truffle.getRuntime().createCallTarget(ConstructorRootNode.create(functionData, newCallTarget, false, instanceLayout));
            CallTarget constructNewTarget = Truffle.getRuntime().createCallTarget(ConstructorRootNode.create(functionData, newTargetCallTarget, true, instanceLayout));
            functionData.setCallTarget(callTarget);
            functionData.setConstructTarget(constructTarget);
            functionData.setConstructNewTarget(constructNewTarget);
            template.setFunctionData(functionData);
        }

        DynamicObject functionObject = JSFunction.create(realm, functionData);
        template.setFunctionObject(realm, functionObject);

        // Additional data are held weakly from C => we have to ensure that
        // they are not GCed before the corresponding function is GCed
        JSObjectUtil.putHiddenProperty(functionObject, FUNCTION_TEMPLATE_DATA_KEY, template.getAdditionalData());

        return functionObject;
    }

    public boolean functionTemplateHasInstance(Object functionTemplate, Object instance) {
        if (instance instanceof DynamicObject) {
            Object constructor = JSObjectUtil.getHiddenProperty((DynamicObject) instance, FunctionTemplate.CONSTRUCTOR);
            if (!(constructor instanceof FunctionTemplate)) {
                return false; // not created from FunctionTemplate
            }
            FunctionTemplate instanceTemplate = (FunctionTemplate) constructor;
            while (instanceTemplate != null) {
                if (instanceTemplate == functionTemplate) {
                    return true;
                } else {
                    instanceTemplate = instanceTemplate.getParent();
                }
            }
        }
        return false;
    }

    public void functionTemplateReadOnlyPrototype(Object templateObj) {
        FunctionTemplate template = (FunctionTemplate) templateObj;
        template.markPrototypeReadOnly();
    }

    public Object objectTemplateNew() {
        return new ObjectTemplate();
    }

    @CompilerDirectives.TruffleBoundary
    public Object objectTemplateNewInstance(Object realm, Object templateObj) {
        JSRealm jsRealm = (JSRealm) realm;
        JSContext jsContext = jsRealm.getContext();
        ObjectTemplate template = (ObjectTemplate) templateObj;
        FunctionTemplate functionHandler = template.getFunctionHandler();
        DynamicObject instance;
        if (functionHandler == null) {
            FunctionTemplate parentFunctionTemplate = template.getParentFunctionTemplate();
            if (parentFunctionTemplate == null) {
                instance = JSOrdinary.create(jsContext, jsRealm);
            } else {
                DynamicObject function = (DynamicObject) functionTemplateGetFunction(realm, parentFunctionTemplate);
                DynamicObject prototype = (DynamicObject) JSObject.get(function, JSObject.PROTOTYPE);
                instance = JSOrdinary.createWithPrototype(prototype, jsContext, template.getInternalFieldCount() > 0 ? JSOrdinary.INTERNAL_FIELD_INSTANCE : JSOrdinary.INSTANCE);
                JSObjectUtil.putHiddenProperty(instance, FunctionTemplate.CONSTRUCTOR, parentFunctionTemplate);
            }
        } else {
            instance = functionTemplateCreateCallback(jsContext, jsRealm, functionHandler);
        }
        objectTemplateInstantiate(jsRealm, templateObj, instance);
        if (template.hasPropertyHandler()) {
            instance = propertyHandlerInstantiate(jsContext, jsRealm, template, instance, false);
        }
        return instance;
    }

    @CompilerDirectives.TruffleBoundary
    public DynamicObject propertyHandlerInstantiate(JSContext context, JSRealm realm, ObjectTemplate template, DynamicObject target, boolean global) {
        DynamicObject handler = JSUncheckedProxyHandler.create(context, realm);
        DynamicObject proxy = JSProxy.create(context, realm, target, handler);

        DynamicObject getter = functionFromRootNode(context, realm, new ExecuteNativePropertyHandlerNode(
                        this,
                        context,
                        template,
                        proxy,
                        ExecuteNativePropertyHandlerNode.Mode.GETTER), proxy);
        JSObject.set(handler, JSProxy.GET, getter);

        DynamicObject setter = functionFromRootNode(context, realm, new ExecuteNativePropertyHandlerNode(
                        this,
                        context,
                        template,
                        proxy,
                        ExecuteNativePropertyHandlerNode.Mode.SETTER), proxy);
        JSObject.set(handler, JSProxy.SET, setter);

        DynamicObject query = functionFromRootNode(context, realm, new ExecuteNativePropertyHandlerNode(
                        this,
                        context,
                        template,
                        proxy,
                        ExecuteNativePropertyHandlerNode.Mode.QUERY), proxy);
        JSObject.set(handler, JSProxy.HAS, query);

        DynamicObject deleter = functionFromRootNode(context, realm, new ExecuteNativePropertyHandlerNode(
                        this,
                        context,
                        template,
                        proxy,
                        ExecuteNativePropertyHandlerNode.Mode.DELETER), proxy);
        JSObject.set(handler, JSProxy.DELETE_PROPERTY, deleter);

        DynamicObject ownKeys = functionFromRootNode(context, realm, new ExecuteNativePropertyHandlerNode(
                        this,
                        context,
                        template,
                        proxy,
                        ExecuteNativePropertyHandlerNode.Mode.OWN_KEYS), proxy);
        JSObject.set(handler, JSProxy.OWN_KEYS, ownKeys);

        DynamicObject getOwnPropertyDescriptor = functionFromRootNode(context, realm, new ExecuteNativePropertyHandlerNode(
                        this,
                        context,
                        template,
                        proxy,
                        ExecuteNativePropertyHandlerNode.Mode.GET_OWN_PROPERTY_DESCRIPTOR), proxy);
        JSObject.set(handler, JSProxy.GET_OWN_PROPERTY_DESCRIPTOR, getOwnPropertyDescriptor);

        DynamicObject defineProperty = functionFromRootNode(context, realm, new ExecuteNativePropertyHandlerNode(
                        this,
                        context,
                        template,
                        proxy,
                        ExecuteNativePropertyHandlerNode.Mode.DEFINE_PROPERTY), proxy);
        JSObject.set(handler, JSProxy.DEFINE_PROPERTY, defineProperty);

        DynamicObject getPrototypeOf = functionFromRootNode(context, realm, new PropertyHandlerPrototypeNode(global), null);
        JSObject.set(handler, JSProxy.GET_PROTOTYPE_OF, getPrototypeOf);

        for (Value value : template.getValues()) {
            Object name = value.getName();
            if (name instanceof HiddenKey) {
                JSObjectUtil.putHiddenProperty(proxy, name, value.getValue());
            } // else set on target (in objectTemplateInstantiate) already
        }

        int internalFieldCount = template.getInternalFieldCount();
        if (internalFieldCount > 0) {
            JSObjectUtil.putHiddenProperty(proxy, INTERNAL_FIELD_COUNT_KEY, internalFieldCount);
        }

        return proxy;
    }

    @CompilerDirectives.TruffleBoundary
    public void objectTemplateInstantiate(JSRealm realm, Object templateObj, Object targetObject) {
        JSContext context = realm.getContext();
        ObjectTemplate template = (ObjectTemplate) templateObj;
        DynamicObject obj = (DynamicObject) targetObject;

        for (Accessor accessor : template.getAccessors()) {
            Pair<JSFunctionData, JSFunctionData> accessorFunctions = accessor.getFunctions(context);
            DynamicObject getter = functionFromFunctionData(realm, accessorFunctions.getFirst(), obj);
            DynamicObject setter = functionFromFunctionData(realm, accessorFunctions.getSecond(), obj);
            JSObjectUtil.putAccessorProperty(context, obj, accessor.getName(), getter, setter, accessor.getAttributes());
        }

        for (Value value : template.getValues()) {
            Object name = value.getName();
            Object processedValue = value.getValue();
            int attributes = value.getAttributes();
            if (processedValue instanceof FunctionTemplate) {
                // process all found FunctionTemplates, recursively
                FunctionTemplate functionTempl = (FunctionTemplate) processedValue;
                processedValue = functionTemplateGetFunction(realm, functionTempl);
            }
            if (processedValue instanceof Pair) {
                Pair<?, ?> pair = (Pair<?, ?>) processedValue;
                Object getterTemplate = pair.getFirst();
                Object setterTemplate = pair.getSecond();
                Object getter = (getterTemplate == null) ? Undefined.instance : functionTemplateGetFunction(realm, getterTemplate);
                Object setter = (setterTemplate == null) ? Undefined.instance : functionTemplateGetFunction(realm, setterTemplate);
                JSObjectUtil.putAccessorProperty(context, obj, name, (DynamicObject) getter, (DynamicObject) setter, attributes);
            } else {
                if (name instanceof HiddenKey) {
                    if (!template.hasPropertyHandler()) {
                        JSObjectUtil.putHiddenProperty(obj, name, processedValue);
                    } // else set on the proxy/handler
                } else {
                    JSObject.defineOwnProperty(obj, name, PropertyDescriptor.createData(processedValue, attributes));
                }
            }
        }
        if (template.getInternalFieldCount() > 0) {
            if (targetObject instanceof JSOrdinaryObject.InternalFieldLayout) {
                ((JSOrdinaryObject.InternalFieldLayout) targetObject).setInternalFieldCount(template.getInternalFieldCount());
            } else {
                JSObjectUtil.putHiddenProperty(obj, INTERNAL_FIELD_COUNT_KEY, template.getInternalFieldCount());
            }
        }
    }

    public void objectTemplateSetAccessor(Object templateObj, Object name, long getterPtr, long setterPtr, Object data, Object signature, int attributes) {
        ObjectTemplate template = (ObjectTemplate) templateObj;
        template.addAccessor(new Accessor(this, name, getterPtr, setterPtr, data, (FunctionTemplate) signature, propertyAttributes(attributes)));
    }

    public void objectTemplateSetHandler(Object templateObj, long getter, long setter, long query, long deleter, long enumerator, long definer, long descriptor, Object data, boolean named,
                    boolean stringKeysOnly) {
        ObjectTemplate template = (ObjectTemplate) templateObj;
        PropertyHandler handler = new PropertyHandler(getter, setter, query, deleter, enumerator, definer, descriptor, data);
        if (named) {
            template.setNamedPropertyHandler(handler, stringKeysOnly);
        } else {
            template.setIndexedPropertyHandler(handler);
        }
    }

    public void objectTemplateSetCallAsFunctionHandler(Object templateObj, int id, long functionPointer, Object additionalData) {
        ObjectTemplate template = (ObjectTemplate) templateObj;
        FunctionTemplate functionHandler = (FunctionTemplate) functionTemplateNew(id, functionPointer, additionalData, null, 0, true, false);
        template.setFunctionHandler(functionHandler);
    }

    public void objectTemplateSetInternalFieldCount(Object templateObj, int count) {
        ObjectTemplate template = (ObjectTemplate) templateObj;
        template.setInternalFieldCount(count);
    }

    public Object scriptCompilerCompileFunctionInContext(Object context, String sourceName, String body, Object[] arguments, Object[] exts, Object hostDefinedOptions) {
        if (VERBOSE) {
            System.err.println("FUNCTION IN CONTEXT: " + sourceName);
        }
        JSRealm realm = (JSRealm) context;
        JSContext jsContext = realm.getContext();
        Evaluator nodeEvaluator = jsContext.getEvaluator();
        Object extraArgument = getExtraArgumentOfInternalScript(sourceName, realm);
        Object[] extensions;
        ByteBuffer snapshot = null;
        if (extraArgument == null) {
            extensions = exts;
            if (USE_SNAPSHOTS && hostDefinedOptions == null) {
                assert exts.length == 0 && UnboundScript.isCoreModule(sourceName);
                snapshot = getCoreModuleBinarySnapshot(sourceName);
            }
        } else {
            assert exts.length == 0;
            DynamicObject graalExtension = JSOrdinary.create(jsContext, realm);
            JSObject.set(graalExtension, "graalExtension", extraArgument);
            extensions = new Object[]{graalExtension};
        }

        StringBuilder params = new StringBuilder();
        for (int i = 0; i < arguments.length; i++) {
            if (i != 0) {
                params.append(", ");
            }
            params.append(arguments[i]);
        }
        String parameterList = params.toString();

        try {
            GraalJSParserHelper.checkFunctionSyntax(jsContext, jsContext.getParserOptions(), parameterList, body, false, false, sourceName);
        } catch (com.oracle.js.parser.ParserException ex) {
            // throw the correct JS error
            nodeEvaluator.parseFunction(jsContext, parameterList, body, false, false, sourceName);
        }

        StringBuilder code = new StringBuilder();

        boolean anyExtension = extensions.length > 0;
        if (anyExtension) {
            code.append("(function () {");

            for (int i = 0; i < extensions.length; i++) {
                code.append("with (arguments[").append(i).append("]) {");
            }

            code.append("return ");
        }

        code.append("(function (");
        code.append(parameterList);
        code.append(") {");

        // hashbang would result in SyntaxError => comment it out
        if (body.startsWith("#!")) {
            code.append("//");
        }

        String prefix = code.toString();

        code = new StringBuilder();
        code.append("\n});");

        if (anyExtension) {
            for (int i = 0; i < extensions.length; i++) {
                code.append("}"); // with (arguments[i]) {
            }

            code.append(";})");
        }

        String suffix = code.toString();

        Source source = null;
        if (hostDefinedOptions == null) {
            // sources of built-in modules
            source = Source.newBuilder(JavaScriptLanguage.ID, body, sourceName).internal(isBootstrapSource(sourceName)).build();
        } else {
            if (!sourceName.isEmpty()) {
                try {
                    TruffleFile truffleFile = realm.getEnv().getPublicTruffleFile(sourceName);
                    source = Source.newBuilder(JavaScriptLanguage.ID, truffleFile).content(body).name(sourceName).build();
                } catch (InvalidPathException e) {
                    if (VERBOSE) {
                        System.err.println("INVALID PATH: " + sourceName);
                    }
                }
            }
            if (source == null) {
                source = Source.newBuilder(JavaScriptLanguage.ID, body, sourceName).build();
            }
            hostDefinedOptionsMap.put(source, hostDefinedOptions);
        }

        Object function;
        if (snapshot == null) {
            ScriptNode scriptNode = nodeEvaluator.parseScript(jsContext, source, prefix, suffix);
            DynamicObject fn = (DynamicObject) scriptNode.run(realm);
            function = anyExtension ? JSFunction.call(fn, Undefined.instance, extensions) : fn;
        } else {
            ScriptNode scriptNode = parseScriptFromSnapshot(jsContext, source, prefix, suffix, snapshot);
            function = scriptNode.run(realm);
        }
        assert JSFunction.isJSFunction(function);

        NodeScriptOrModule scriptOrModule = new NodeScriptOrModule(jsContext, source);
        // function should keep scriptOrModule alive
        JSObjectUtil.putHiddenProperty((DynamicObject) function, NodeScriptOrModule.SCRIPT_OR_MODULE, scriptOrModule);

        return new Object[]{function, scriptOrModule};
    }

    private static boolean isBootstrapSource(String sourceName) {
        return sourceName.startsWith("internal/per_context") || sourceName.startsWith("internal/bootstrap");
    }

    public Object scriptCompile(Object context, Object sourceCode, Object fileName, Object hostDefinedOptions) {
        UnboundScript unboundScript = (UnboundScript) unboundScriptCompile(sourceCode, fileName, hostDefinedOptions);
        return unboundScriptBindToContext(context, unboundScript);
    }

    @CompilerDirectives.TruffleBoundary
    public Object scriptRun(Object script) {
        Script boundScript = (Script) script;
        ScriptNode scriptNode = boundScript.getScriptNode();
        if (VERBOSE) {
            Source source = scriptNode.getRootNode().getSourceSection().getSource();
            System.err.println("EXECUTING: " + source.getName());
        }
        JSRealm realm = boundScript.getRealm();
        Object[] arguments = scriptNode.argumentsToRun(realm);
        Object prev = realm.getTruffleContext().enter(null);
        try {
            Object result;
            if (boundScript.isGraalInternal()) {
                result = scriptRunInternal(scriptNode, realm, arguments);
            } else {
                result = scriptNode.run(arguments);
            }
            return result;
        } finally {
            realm.getTruffleContext().leave(null, prev);
        }
    }

    private Object scriptRunInternal(ScriptNode scriptNode, JSRealm realm, Object[] arguments) {
        // Internal scripts are allowed to access implementation classes
        JSContext context = scriptNode.getContext();
        final DynamicObject moduleFunction = (DynamicObject) scriptNode.run(arguments);

        JavaScriptRootNode wrapperNode = new JavaScriptRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                DynamicObject thisObject = (DynamicObject) JSArguments.getThisObject(args);
                Object result = JSFunction.call(moduleFunction, thisObject, getInternalModuleUserArguments(args, scriptNode, getRealm()));
                return result;
            }
        };
        JSFunctionData functionData = JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(wrapperNode), 5, "");
        return JSFunction.create(realm, functionData);
    }

    private Object getExtraArgumentOfInternalScript(String moduleName, JSRealm realm) {
        Object extraArgument = null;
        JSContext context = realm.getContext();
        if (NIO_BUFFER_MODULE_NAME.equals(moduleName)) {
            // NIO-based buffer APIs in internal/graal/buffer.js are initialized by passing one
            // extra argument to the module loading function.
            extraArgument = USE_NIO_BUFFER ? NIOBuffer.createInitFunction(realm) : Null.instance;
        } else if ("internal/graal/debug.js".equals(moduleName)) {
            CallTarget setBreakPointCallTarget = Truffle.getRuntime().createCallTarget(new SetBreakPointNode(this));
            JSFunctionData setBreakPointData = JSFunctionData.createCallOnly(context, setBreakPointCallTarget, 3, SetBreakPointNode.NAME);
            DynamicObject setBreakPoint = JSFunction.create(realm, setBreakPointData);
            extraArgument = setBreakPoint;
        } else if ("internal/worker/io.js".equals(moduleName) || "internal/main/worker_thread.js".equals(moduleName)) {
            // The Shared-mem channel initialization is similar to NIO-based buffers.
            extraArgument = SharedMemMessagingBindings.createInitFunction(this, realm);
        } else if ("inspector.js".equals(moduleName)) {
            TruffleObject inspector = lookupInstrument("inspect", TruffleObject.class);
            extraArgument = (inspector == null) ? Null.instance : inspector;
        }
        return extraArgument;
    }

    @TruffleBoundary
    private Object[] getInternalModuleUserArguments(Object[] args, ScriptNode node, JSRealm realm) {
        Object[] userArgs = JSArguments.extractUserArguments(args);
        String moduleName = node.getRootNode().getSourceSection().getSource().getName();
        Object extraArgument = getExtraArgumentOfInternalScript(moduleName, realm);
        if (extraArgument == null) {
            return userArgs;
        }
        Object[] extendedArgs = new Object[userArgs.length + 1];
        System.arraycopy(userArgs, 0, extendedArgs, 0, userArgs.length);
        extendedArgs[userArgs.length] = extraArgument;
        return extendedArgs;
    }

    public Object scriptGetUnboundScript(Object script) {
        return new UnboundScript((Script) script);
    }

    private static FunctionNode parseSource(Source source, JSContext context) {
        ContextData contextData = (ContextData) context.getEmbedderData();
        String content = source.getCharacters().toString();
        FunctionNode parseResult = contextData.getFunctionNodeCache().get(content);
        if (parseResult == null) {
            parseResult = GraalJSParserHelper.parseScript(context, source, context.getParserOptions());
            contextData.getFunctionNodeCache().put(content, parseResult);
        }
        return parseResult;
    }

    public Object unboundScriptCompile(Object sourceCode, Object fileName, Object hostDefinedOptions) {
        String sourceCodeStr = (String) sourceCode;
        String fileNameStr = (String) fileName;
        Source source = UnboundScript.createSource(internSourceCode(sourceCodeStr), fileNameStr);

        hostDefinedOptionsMap.put(source, hostDefinedOptions);

        // Needed to generate potential syntax errors, see node --check
        FunctionNode functionNode = parseSource(source, mainJSContext);

        return new UnboundScript(source, functionNode);
    }

    private static ByteBuffer getCoreModuleBinarySnapshot(String modulePath) {
        ByteBuffer snapshotBinary = NativeAccess.getCoreModuleBinarySnapshot(modulePath);
        if (VERBOSE) {
            if (snapshotBinary == null) {
                System.err.printf("no snapshot for %s\n", modulePath);
            } else {
                System.err.printf("successfully read snapshot for %s\n", modulePath);
            }
        }
        return snapshotBinary;
    }

    public Object unboundScriptBindToContext(Object context, Object script) {
        JSRealm jsRealm = (JSRealm) context;
        JSContext jsContext = jsRealm.getContext();
        UnboundScript unboundScript = (UnboundScript) script;
        Source source = unboundScript.getSource();
        Object parseResult = unboundScript.getParseResult();
        ScriptNode scriptNode;
        if (parseResult instanceof FunctionNode) {
            ContextData contextData = (ContextData) jsContext.getEmbedderData();
            scriptNode = contextData.getScriptNodeCache().get(source);
            if (scriptNode == null) {
                JSParserOptions options = jsContext.getParserOptions();
                NodeFactory factory = NodeFactory.getInstance(jsContext);
                Object prev = jsRealm.getTruffleContext().enter(null);
                try {
                    scriptNode = JavaScriptTranslator.translateFunction(factory, jsContext, null, source, options.isStrict(), (FunctionNode) parseResult);
                } finally {
                    jsRealm.getTruffleContext().leave(null, prev);
                }
                if (!"repl".equals(source.getName())) {
                    contextData.getScriptNodeCache().put(source, scriptNode);
                }
            }
        } else {
            scriptNode = parseScriptFromSnapshot(jsContext, source, "", "", (ByteBuffer) parseResult);
        }
        return new Script(scriptNode, parseResult, jsRealm, unboundScript.getId());
    }

    public String unboundScriptGetContent(Object script) {
        return ((UnboundScript) script).getSource().getCharacters().toString();
    }

    private String internSourceCode(String sourceCode) {
        Reference<String> cacheEntry = sourceCodeCache.get(sourceCode);
        String entry = null;
        if (cacheEntry == null || (entry = cacheEntry.get()) == null) {
            sourceCodeCache.put(sourceCode, new WeakReference<>(sourceCode));
            return sourceCode;
        }
        return entry;
    }

    private ScriptNode parseScriptFromSnapshot(JSContext context, Source source, String prefix, String suffix, ByteBuffer snapshotBinary) {
        JSParser parser = (JSParser) context.getEvaluator();
        try {
            return parser.parseScript(context, source, snapshotBinary);
        } catch (IllegalArgumentException e) {
            if (VERBOSE) {
                String moduleName = source.getName();
                System.err.printf("error when parsing binary snapshot for %s: %s\n", moduleName, e);
                System.err.printf("falling back to parsing %s at runtime\n", moduleName);
            }
            return parser.parseScript(context, source, prefix, suffix);
        }
    }

    public int unboundScriptGetId(Object script) {
        return ((UnboundScript) script).getId();
    }

    public Object contextGlobal(Object realm) {
        return ((JSRealm) realm).getGlobalObject();
    }

    private static int propertyAttributes(int attributes) {
        int flags = 0;
        if ((attributes & 1 /* v8::PropertyAttribute::ReadOnly */) != 0) {
            flags |= JSAttributes.NOT_WRITABLE;
        }
        if ((attributes & 2 /* v8::PropertyAttribute::DontEnum */) != 0) {
            flags |= JSAttributes.NOT_ENUMERABLE;
        }
        if ((attributes & 4 /* v8::PropertyAttribute::DontDelete */) != 0) {
            flags |= JSAttributes.NOT_CONFIGURABLE;
        }
        return flags;
    }

    public static PropertyDescriptor propertyDescriptor(int v8Attributes, Object value) {
        boolean writable = ((v8Attributes & 1 /* v8::PropertyAttribute::ReadOnly */) == 0);
        boolean enumerable = ((v8Attributes & 2 /* v8::PropertyAttribute::DontEnum */) == 0);
        boolean configurable = ((v8Attributes & 4 /* v8::PropertyAttribute::DontDelete */) == 0);
        return PropertyDescriptor.createData(value, enumerable, writable, configurable);
    }

    private static DynamicObject functionFromRootNode(JSContext context, JSRealm realm, JavaScriptRootNode rootNode, Object holder) {
        return functionFromFunctionData(realm, functionDataFromRootNode(context, rootNode), holder);
    }

    public static JSFunctionData functionDataFromRootNode(JSContext context, JavaScriptRootNode rootNode) {
        CallTarget callbackCallTarget = Truffle.getRuntime().createCallTarget(rootNode);
        return JSFunctionData.create(context, callbackCallTarget, callbackCallTarget, 0, "", false, false, false, true);
    }

    private static DynamicObject functionFromFunctionData(JSRealm realm, JSFunctionData functionData, Object holder) {
        if (functionData == null) {
            return null;
        }
        DynamicObject function = JSFunction.create(realm, functionData);
        if (holder != null) {
            JSObjectUtil.putHiddenProperty(function, HOLDER_KEY, holder);
        }
        JSObject.preventExtensions(function);
        return function;
    }

    @CompilerDirectives.TruffleBoundary
    public Object tryCatchException(Object context, Object exception) {
        Throwable throwable = (Throwable) exception;
        if (exception instanceof ExitException) {
            int exitCode = ((ExitException) exception).getStatus();
            exit(exitCode);
        } else if (throwable instanceof OutOfMemoryError) {
            throwable.printStackTrace();
            exit(1);
        }
        JSRealm jsRealm = (JSRealm) context;
        if (!(throwable instanceof GraalJSException)) {
            isolateInternalErrorCheck(throwable);
            throwable = JSException.create(JSErrorType.Error, throwable.getMessage(), throwable, null);
        }
        GraalJSException truffleException = (GraalJSException) throwable;
        Object exceptionObject = truffleException.getErrorObjectEager(jsRealm);
        if (JSRuntime.isObject(exceptionObject) && JSError.getException((DynamicObject) exceptionObject) == null) {
            JSObjectUtil.putHiddenProperty((DynamicObject) exceptionObject, JSError.EXCEPTION_PROPERTY_NAME, truffleException);
        }
        // Patch stack property of SyntaxErrors so that it looks like the one from V8
        Matcher matcher = messageSyntaxErrorMatcher(exception);
        if (matcher != null) {
            String stack = JSRuntime.toString(JSObject.get((DynamicObject) exceptionObject, JSError.STACK_NAME));
            if (stack.startsWith(truffleException.getMessage())) {
                String message = matcher.group(SYNTAX_ERROR_MESSAGE_GROUP);
                stack = "SyntaxError: " + message + stack.substring(truffleException.getMessage().length());
                JSObject.set((DynamicObject) exceptionObject, JSError.STACK_NAME, stack);
            }
        }
        return exceptionObject;
    }

    @SuppressWarnings("deprecation")
    public boolean tryCatchHasTerminated(Object exception) {
        return (exception instanceof GraalJSKillException) || (exception instanceof com.oracle.truffle.api.TruffleException && ((com.oracle.truffle.api.TruffleException) exception).isCancelled());
    }

    private static GraalJSException.JSStackTraceElement messageGraalJSExceptionStackFrame(Object exception) {
        if (exception instanceof GraalJSException) {
            GraalJSException truffleException = (GraalJSException) exception;
            GraalJSException.JSStackTraceElement[] stackTrace = truffleException.getJSStackTrace();
            GraalJSException.JSStackTraceElement stackFrame;
            int index = 0;
            do {
                if (index == stackTrace.length) {
                    stackFrame = null;
                    break;
                } else {
                    stackFrame = stackTrace[index++];
                }
            } while (JSFunction.BUILTIN_SOURCE_NAME.equals(stackFrame.getFileName()));
            return stackFrame;
        }
        return null;
    }

    private static final Pattern SYNTAX_ERROR_PATTERN = Pattern.compile("(.+):(\\d+):(\\d+) ([^\r\n]+)\r?\n([^\r\n]+)(?:\r?\n(?:.|(?:\r?\n))*)?");
    private static final int SYNTAX_ERROR_RESOURCE_NAME_GROUP = 1;
    private static final int SYNTAX_ERROR_LINE_NUMBER_GROUP = 2;
    private static final int SYNTAX_ERROR_COLUMN_NUMBER_GROUP = 3;
    private static final int SYNTAX_ERROR_MESSAGE_GROUP = 4;
    private static final int SYNTAX_ERROR_SOURCE_LINE_GROUP = 5;

    private static Matcher messageSyntaxErrorMatcher(Object exception) {
        if (exception instanceof JSException) {
            JSException jsException = (JSException) exception;
            if (jsException.getErrorType() == JSErrorType.SyntaxError) {
                String message = jsException.getRawMessage();
                Matcher matcher = SYNTAX_ERROR_PATTERN.matcher(message);
                if (matcher.matches()) {
                    return matcher;
                }
            }
        }
        return null;
    }

    private static String messageSyntaxErrorResourceName(Object exception) {
        Matcher matcher = messageSyntaxErrorMatcher(exception);
        if (matcher != null) {
            return matcher.group(SYNTAX_ERROR_RESOURCE_NAME_GROUP);
        }
        return null;
    }

    private static int messageSyntaxErrorLineNumber(Object exception) {
        Matcher matcher = messageSyntaxErrorMatcher(exception);
        if (matcher != null) {
            String lineNumber = matcher.group(SYNTAX_ERROR_LINE_NUMBER_GROUP);
            return Integer.parseInt(lineNumber);
        }
        return -1;
    }

    private static int messageSyntaxErrorColumnNumber(Object exception) {
        Matcher matcher = messageSyntaxErrorMatcher(exception);
        if (matcher != null) {
            String columnNumber = matcher.group(SYNTAX_ERROR_COLUMN_NUMBER_GROUP);
            return Integer.parseInt(columnNumber);
        }
        return -1;
    }

    private static String messageSyntaxErrorSourceLine(Object exception) {
        Matcher matcher = messageSyntaxErrorMatcher(exception);
        if (matcher != null) {
            return matcher.group(SYNTAX_ERROR_SOURCE_LINE_GROUP);
        }
        return null;
    }

    public Object messageGetScriptResourceName(Object exception) {
        String resourceName = messageSyntaxErrorResourceName(exception);
        if (resourceName != null) {
            return resourceName;
        }
        GraalJSException.JSStackTraceElement stackFrame = messageGraalJSExceptionStackFrame(exception);
        if (stackFrame != null) {
            return stackFrame.getFileName();
        }
        return "unknown";
    }

    public int messageGetLineNumber(Object exception) {
        int lineNumber = messageSyntaxErrorLineNumber(exception);
        if (lineNumber != -1) {
            return lineNumber;
        }
        GraalJSException.JSStackTraceElement stackFrame = messageGraalJSExceptionStackFrame(exception);
        if (stackFrame != null) {
            return stackFrame.getLineNumber();
        }
        return 0;
    }

    public int messageGetStartColumn(Object exception) {
        int columnNumber = messageSyntaxErrorColumnNumber(exception);
        if (columnNumber != -1) {
            return columnNumber;
        }
        GraalJSException.JSStackTraceElement stackFrame = messageGraalJSExceptionStackFrame(exception);
        if (stackFrame != null) {
            return stackFrame.getColumnNumber() - 1;
        }
        return 0;
    }

    public Object messageGetSourceLine(Object exception) {
        String sourceLine = messageSyntaxErrorSourceLine(exception);
        if (sourceLine != null) {
            return sourceLine;
        }
        GraalJSException.JSStackTraceElement stackFrame = messageGraalJSExceptionStackFrame(exception);
        if (stackFrame != null) {
            return stackFrame.getLine();
        }
        return "unknown";
    }

    public Object messageGetStackTrace(Object exception) {
        if (exception instanceof GraalJSException) {
            GraalJSException truffleException = (GraalJSException) exception;
            return truffleException.getJSStackTrace();
        }
        return new GraalJSException.JSStackTraceElement[0];
    }

    public Object messageGet(Object exception) {
        return "Uncaught " + ((Throwable) exception).getMessage();
    }

    public Object stackTraceCurrentStackTrace() {
        return GraalJSException.getJSStackTrace(null);
    }

    public int stackFrameGetLineNumber(Object stackFrame) {
        GraalJSException.JSStackTraceElement element = (GraalJSException.JSStackTraceElement) stackFrame;
        return element.getLineNumber();
    }

    public int stackFrameGetColumn(Object stackFrame) {
        GraalJSException.JSStackTraceElement element = (GraalJSException.JSStackTraceElement) stackFrame;
        return element.getColumnNumber();
    }

    public Object stackFrameGetScriptName(Object stackFrame) {
        GraalJSException.JSStackTraceElement element = (GraalJSException.JSStackTraceElement) stackFrame;
        return element.getFileName();
    }

    public Object stackFrameGetFunctionName(Object stackFrame) {
        GraalJSException.JSStackTraceElement element = (GraalJSException.JSStackTraceElement) stackFrame;
        return element.getFunctionName();
    }

    public boolean stackFrameIsEval(Object stackFrame) {
        GraalJSException.JSStackTraceElement element = (GraalJSException.JSStackTraceElement) stackFrame;
        return element.isEval();
    }

    /**
     * Key for a weak callback.
     */
    private static final HiddenKey HIDDEN_WEAK_CALLBACK = new HiddenKey("WeakCallback");
    private static final HiddenKey HIDDEN_WEAK_CALLBACK_CONTEXT = new HiddenKey("WeakCallbackContext");

    /**
     * Reference queue associated with weak references to objects that require invocation of a
     * callback when the object is no longer used.
     */
    private final ReferenceQueue<Object> weakCallbackQueue = new ReferenceQueue<>();

    /**
     * Collection holding weak references to objects that require invocation of a callback when the
     * object is no longer used.
     */
    private final Set<WeakCallback> weakCallbacks = new HashSet<>();

    @SuppressWarnings("unchecked")
    private WeakCallback updateWeakCallback(Object object, long reference, long data, long callbackPointer, int type) {
        Map<Long, WeakCallback> map;
        if (object instanceof NodeScriptOrModule) {
            map = ((NodeScriptOrModule) object).getWeakCallbackMap();
        } else {
            DynamicObject target;
            HiddenKey key;
            if (object instanceof JSRealm) {
                target = ((JSRealm) object).getGlobalObject();
                key = HIDDEN_WEAK_CALLBACK_CONTEXT;
            } else if (object instanceof DynamicObject) {
                target = (DynamicObject) object;
                key = HIDDEN_WEAK_CALLBACK;
            } else {
                System.err.println("Weak references not supported for " + object);
                return null;
            }
            map = (Map<Long, WeakCallback>) JSObjectUtil.getHiddenProperty(target, key);
            if (map == null) {
                map = new HashMap<>();
                JSObjectUtil.putHiddenProperty(target, key, map);
            }
        }

        WeakCallback weakCallback = map.get(reference);
        if (weakCallback == null) {
            weakCallback = new WeakCallback(object, data, callbackPointer, type, weakCallbackQueue);
            map.put(reference, weakCallback);
        } else {
            weakCallback.data = data;
            weakCallback.callback = callbackPointer;
            weakCallback.type = type;
        }
        if (callbackPointer == 0) {
            weakCallbacks.remove(weakCallback);
        } else {
            weakCallbacks.add(weakCallback);
        }
        return weakCallback;
    }

    private void pollWeakCallbackQueue(boolean canBlock) {
        WeakCallback callback;
        if (canBlock) {
            try {
                // System.gc() may not enqueue references synchronously,
                // give them some time to appear in the queue
                callback = (WeakCallback) weakCallbackQueue.remove(10);
                if (callback != null) {
                    processWeakCallback(callback);
                }
            } catch (InterruptedException iex) {
            }
        }
        while ((callback = (WeakCallback) weakCallbackQueue.poll()) != null) {
            processWeakCallback(callback);
        }
    }

    private void processWeakCallback(WeakCallback callback) {
        weakCallbacks.remove(callback);
        if (callback.callback != 0) {
            if (callback.type == -1) {
                DeleterCallback deleter = (DeleterCallback) callback;
                NativeAccess.deleterCallback(deleter.callback, deleter.data, deleter.length, deleter.deleterData);
            } else {
                NativeAccess.weakCallback(callback.callback, callback.data, callback.type);
            }
        }
    }

    public void makeWeak(Object object, long reference, long data, long callbackPointer, int type) {
        if (object == null) {
            System.err.println("null object given to makeWeak!");
            return;
        }

        updateWeakCallback(object, reference, data, callbackPointer, type);
        pollWeakCallbackQueue(false);
    }

    public long clearWeak(Object object, long reference) {
        if (object == null) {
            // Clear weak called on a reference that has been GCed already.
            // Misuse of ClearWeak() probably. The name of the method is confusing.
            // Hence, some of the calls seem to be attempts to reset the reference
            // instead of attempts to clear the weak status (i.e. restore the strong one).
            return 0;
        }

        WeakCallback callback = updateWeakCallback(object, reference, 0, 0, 0);
        return callback.data;
    }

    @TruffleBoundary
    public <T> T lookupInstrument(String instrumentId, Class<T> instrumentClass) {
        TruffleLanguage.Env env = envForInstruments;
        InstrumentInfo info = env.getInstruments().get(instrumentId);
        return (info == null) ? null : env.lookup(info, instrumentClass);
    }

    private boolean createChildContext;
    private Set<JSRealm> childContextSet = Collections.newSetFromMap(new WeakHashMap<JSRealm, Boolean>());

    public Object contextNew(Object templateObj) {
        JSRealm realm;
        JSContext context;
        if (createChildContext) {
            realm = mainJSRealm.createChildRealm();
            childContextSet.add(realm);
            context = realm.getContext();
            assert realm.getAgent() == agent;
        } else {
            realm = mainJSRealm;
            context = mainJSContext;
            context.setEmbedderData(new ContextData(context));
            createChildContext = true;
        }
        RealmData realmData = new RealmData();
        realm.setEmbedderData(realmData);
        DynamicObject global = realm.getGlobalObject();
        // Node.js does not have global arguments property
        JSObject.delete(global, JSRealm.ARGUMENTS_NAME);
        if (exposeGC) {
            contextExposeGC(realm);
        }
        if (templateObj != null) {
            ObjectTemplate template = (ObjectTemplate) templateObj;
            if (template.hasPropertyHandler()) {
                global = propertyHandlerInstantiate(context, realm, template, global, true);
                realm.setGlobalObject(global);
            } else {
                DynamicObject prototype = JSOrdinary.create(context, realm);
                objectTemplateInstantiate(realm, template, prototype);
                JSObject.setPrototype(global, prototype);
            }
        }
        realmData.setSecurityToken(global);
        return realm;
    }

    public static RealmData getRealmEmbedderData(Object realm) {
        return (RealmData) ((JSRealm) realm).getEmbedderData();
    }

    public static ContextData getContextEmbedderData(JSContext context) {
        return (ContextData) context.getEmbedderData();
    }

    private void contextExposeGC(JSRealm realm) {
        DynamicObject global = realm.getGlobalObject();
        JavaScriptRootNode rootNode = new JavaScriptRootNode() {
            @Override
            public Object execute(VirtualFrame vf) {
                isolatePerformGC();
                return Undefined.instance;
            }
        };
        JSFunctionData functionData = JSFunctionData.createCallOnly(realm.getContext(), Truffle.getRuntime().createCallTarget(rootNode), 0, "gc");
        DynamicObject function = JSFunction.create(realm, functionData);
        JSObject.set(global, "gc", function);
    }

    @TruffleBoundary
    private void isolatePerformGC() {
        NativeAccess.notifyGCCallbacks(true);
        pollWeakCallbackQueue(true);
        for (int i = 0; i < 3; i++) {
            System.gc();
            pollWeakCallbackQueue(true);
        }
        NativeAccess.notifyGCCallbacks(false);
    }

    public void contextSetSecurityToken(Object context, Object securityToken) {
        RealmData contextData = getRealmEmbedderData(context);
        contextData.setSecurityToken(securityToken);
    }

    public Object contextGetSecurityToken(Object context) {
        RealmData contextData = getRealmEmbedderData(context);
        Object securityToken = contextData.getSecurityToken();
        return (securityToken == null) ? Undefined.instance : securityToken;
    }

    public Object contextGetExtrasBindingObject(Object context) {
        RealmData contextData = getRealmEmbedderData(context);
        DynamicObject extras = contextData.getExtrasBindingObject();
        if (extras == null) {
            extras = initializeExtrasBindingObject((JSRealm) context);
            contextData.setExtrasBindingObject(extras);
        }
        return extras;
    }

    private DynamicObject initializeExtrasBindingObject(JSRealm realm) {
        DynamicObject extras = JSOrdinary.create(realm.getContext(), realm);

        // isTraceCategoryEnabled
        JavaScriptRootNode isEnabledRootNode = new JavaScriptRootNode() {
            @Override
            public Object execute(VirtualFrame vf) {
                return false; // tracing is not supported
            }
        };
        JSFunctionData isEnabledFunctionData = JSFunctionData.createCallOnly(realm.getContext(), Truffle.getRuntime().createCallTarget(isEnabledRootNode), 0, "isTraceCategoryEnabled");
        DynamicObject isEnabledFunction = JSFunction.create(realm, isEnabledFunctionData);
        JSObject.set(extras, "isTraceCategoryEnabled", isEnabledFunction);

        // trace
        JavaScriptRootNode traceRootNode = new JavaScriptRootNode() {
            @Override
            public Object execute(VirtualFrame vf) {
                return Undefined.instance; // tracing is not supported
            }
        };
        JSFunctionData traceFunctionData = JSFunctionData.createCallOnly(realm.getContext(), Truffle.getRuntime().createCallTarget(traceRootNode), 0, "trace");
        DynamicObject traceFunction = JSFunction.create(realm, traceFunctionData);
        JSObject.set(extras, "trace", traceFunction);

        return extras;
    }

    public void contextSetPointerInEmbedderData(Object context, int index, long pointer) {
        contextSetEmbedderData(context, index, pointer);
    }

    public long contextGetPointerInEmbedderData(Object context, int index) {
        Long pointer = (Long) contextGetEmbedderData(context, index);
        return (pointer == null) ? 0 : pointer;
    }

    public void contextSetEmbedderData(Object realm, int index, Object value) {
        RealmData data = getRealmEmbedderData(realm);
        data.setEmbedderData(index, value);
    }

    public Object contextGetEmbedderData(Object realm, int index) {
        RealmData data = getRealmEmbedderData(realm);
        return data.getEmbedderData(index);
    }

    public void isolateRunMicrotasks() {
        pollWeakCallbackQueue(false);
        try {
            try {
                agent.processAllPromises(true);
            } catch (AbstractTruffleException atex) {
                InteropLibrary interop = InteropLibrary.getUncached(atex);
                if (interop.isException(atex)) {
                    ExceptionType type = interop.getExceptionType(atex);
                    if (type == ExceptionType.INTERRUPT || type == ExceptionType.EXIT) {
                        throw atex;
                    }
                    mainJSContext.notifyPromiseRejectionTracker(JSPromise.create(mainJSContext, getCurrentRealm()), JSPromise.REJECTION_TRACKER_OPERATION_REJECT, atex);
                } else {
                    throw atex;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static Object getInternalFieldKey(int index) {
        if (index < INTERNAL_FIELD_KEYS_ARRAY.length) {
            return INTERNAL_FIELD_KEYS_ARRAY[index];
        } else {
            return INTERNAL_FIELD_KEYS_MAP.computeIfAbsent(index, GraalJSAccess::createInternalFieldKey);
        }
    }

    private static HiddenKey createInternalFieldKey(int index) {
        return new HiddenKey("InternalField" + index);
    }

    private static HiddenKey[] createInternalFieldKeysArray(int nPreallocatedKeys) {
        HiddenKey[] keyArray = new HiddenKey[nPreallocatedKeys];
        for (int i = 0; i < keyArray.length; i++) {
            HiddenKey key = createInternalFieldKey(i);
            keyArray[i] = key;
            INTERNAL_FIELD_KEYS_MAP.put(0, key);
        }
        return keyArray;
    }

    public int objectInternalFieldCount(Object target) {
        return internalFieldCount((DynamicObject) target);
    }

    public static int internalFieldCount(DynamicObject target) {
        if (target instanceof JSOrdinaryObject.InternalFieldLayout) {
            return ((JSOrdinaryObject.InternalFieldLayout) target).getInternalFieldCount();
        }
        Object ret = JSObjectUtil.getHiddenProperty(target, INTERNAL_FIELD_COUNT_KEY);
        if (ret instanceof Integer) {
            return (int) ret;
        } else {
            return 0;
        }
    }

    public long objectSlowGetAlignedPointerFromInternalField(Object target, int index) {
        if (target instanceof JSOrdinaryObject.InternalFieldLayout) {
            return ((JSOrdinaryObject.InternalFieldLayout) target).getInternalFieldPointer(index);
        } else {
            Object key = getInternalFieldKey(index);
            return getAlignedPointerFromInternalField((DynamicObject) target, key);
        }
    }

    private static long getAlignedPointerFromInternalField(DynamicObject target, Object key) {
        try {
            return DynamicObjectLibrary.getUncached().getLongOrDefault(target, key, 0L);
        } catch (UnexpectedResultException e) {
            return 0L;
        }
    }

    public void objectSetAlignedPointerInInternalField(Object target, int index, long value) {
        if (target instanceof JSOrdinaryObject.InternalFieldLayout) {
            ((JSOrdinaryObject.InternalFieldLayout) target).setInternalFieldPointer(index, value);
        } else {
            Object key = getInternalFieldKey(index);
            DynamicObjectLibrary.getUncached().putLong((DynamicObject) target, key, value);
        }
    }

    public void objectSetInternalField(Object object, int index, Object value) {
        if (object instanceof JSOrdinaryObject.InternalFieldLayout) {
            ((JSOrdinaryObject.InternalFieldLayout) object).setInternalFieldObject(index, value);
        } else {
            Object key = getInternalFieldKey(index);
            JSObjectUtil.putHiddenProperty((DynamicObject) object, key, value);
        }
    }

    public Object objectSlowGetInternalField(Object object, int index) {
        Object value;
        if (object instanceof JSOrdinaryObject.InternalFieldLayout) {
            value = ((JSOrdinaryObject.InternalFieldLayout) object).getInternalFieldObject(index);
        } else {
            Object key = getInternalFieldKey(index);
            value = JSObjectUtil.getHiddenProperty((DynamicObject) object, key);
        }
        if (value == null) {
            if (JSPromise.isJSPromise(object)) {
                value = 0;
            } else {
                value = Undefined.instance;
            }
        }
        return processReturnValue(value);
    }

    public Object objectPreviewEntries(Object object) {
        DynamicObject dynamicObject = (DynamicObject) object;
        JSContext context = JSObject.getJSContext(dynamicObject);
        JSHashMap.Cursor cursor = (JSHashMap.Cursor) JSObjectUtil.getHiddenProperty(dynamicObject, JSRuntime.ITERATOR_NEXT_INDEX);
        if (cursor != null) {
            Object kindObject = JSObjectUtil.getHiddenProperty(dynamicObject, JSMap.MAP_ITERATION_KIND_ID);
            boolean isSet = (kindObject == null);
            if (isSet) {
                kindObject = JSObjectUtil.getHiddenProperty(dynamicObject, JSSet.SET_ITERATION_KIND_ID);
            }
            int kind = ((Number) kindObject).intValue();
            cursor = cursor.copy();
            List<Object> entries = new ArrayList<>();
            while (cursor.advance()) {
                Object key = cursor.getKey();
                Object value = isSet ? key : cursor.getValue();
                if (kind == JSRuntime.ITERATION_KIND_KEY) {
                    entries.add(key);
                } else if (kind == JSRuntime.ITERATION_KIND_VALUE) {
                    entries.add(value);
                } else {
                    entries.add(key);
                    entries.add(value);
                    assert kind == JSRuntime.ITERATION_KIND_KEY_PLUS_VALUE;
                }
            }
            resetSharedBuffer();
            sharedBuffer.putInt(kind == JSRuntime.ITERATION_KIND_KEY_PLUS_VALUE ? 1 : 0);
            return JSArray.createConstantObjectArray(context, getCurrentRealm(), entries.toArray());
        }
        if (JSWeakMap.isJSWeakMap(object) || JSWeakSet.isJSWeakSet(object)) {
            // Implementation of these collections does not allow to preview entries
            resetSharedBuffer();
            sharedBuffer.putInt(0);
            return JSArray.createConstantEmptyArray(context, getCurrentRealm());
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public void isolateInternalErrorCheck(Object exception) {
        boolean internalError = !(exception instanceof com.oracle.truffle.api.TruffleException) && !(exception instanceof StackOverflowError) && !(exception instanceof OutOfMemoryError) &&
                        !(exception instanceof ControlFlowException) && !(exception instanceof GraalJSKillException);
        if (internalError) {
            ((Throwable) exception).printStackTrace();
            exit(1);
        }
    }

    public void isolateThrowStackOverflowError() {
        throw Errors.createRangeErrorStackOverflow();
    }

    public void isolateGetHeapStatistics() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long max = runtime.maxMemory();
        resetSharedBuffer();
        sharedBuffer.putLong(total);
        sharedBuffer.putLong(max);
        sharedBuffer.putLong(total - free);
    }

    private boolean terminateExecution;

    public synchronized void isolateCancelTerminateExecution() {
        terminateExecution = false;
        if (Thread.currentThread() == agent.getThread()) {
            Thread.interrupted(); // Clear the interrupted flag
        }
    }

    public synchronized void isolateTerminateExecution() {
        if (terminateExecution) {
            return; // termination in progress already
        }
        terminateExecution = true;
        Thread thread = agent.getThread();
        if (thread != null) {
            thread.interrupt();
        }
        Debugger debugger = lookupInstrument("debugger", Debugger.class);
        if (debugger == null) {
            System.err.println("Debugger is not available!");
            return;
        }
        debugger.startSession(new SuspendedCallback() {
            @Override
            public void onSuspend(SuspendedEvent se) {
                se.getSession().close();
                synchronized (GraalJSAccess.this) {
                    if (!terminateExecution) {
                        return; // termination has been cancelled
                    }
                }
                throw new GraalJSKillException();
            }
        }).suspendNextExecution();
    }

    private static final class GraalJSKillException extends ThreadDeath {
        private static final long serialVersionUID = 3930431622452607906L;
    }

    public Object isolateGetIntPlaceholder() {
        return INT_PLACEHOLDER;
    }

    public Object isolateGetSafeIntPlaceholder() {
        return SAFE_INT_PLACEHOLDER;
    }

    public Object isolateGetDoublePlaceholder() {
        return DOUBLE_PLACEHOLDER;
    }

    public void isolateDispose(boolean exit, int status) {
        if (exit) {
            exit(status);
        }
    }

    public void isolateEnablePromiseHook(boolean enable) {
        PromiseHook hook = enable ? new PromiseHook() {
            @Override
            public void promiseChanged(int changeType, DynamicObject promise, DynamicObject parentPromise) {
                NativeAccess.notifyPromiseHook(changeType, promise, parentPromise);
            }
        } : null;
        mainJSContext.setPromiseHook(hook);
    }

    public void isolateEnablePromiseRejectCallback(boolean enable) {
        PromiseRejectionTracker tracker = enable ? new PromiseRejectionTracker() {
            @Override
            public void promiseRejected(DynamicObject promise, Object value) {
                NativeAccess.notifyPromiseRejectionTracker(
                                promise,
                                0, // v8::PromiseRejectEvent::kPromiseRejectWithNoHandler
                                value);
            }

            @Override
            public void promiseRejectionHandled(DynamicObject promise) {
                NativeAccess.notifyPromiseRejectionTracker(
                                promise,
                                1, // v8::PromiseRejectEvent::kPromiseHandlerAddedAfterReject
                                Undefined.instance);
            }

            @Override
            public void promiseRejectedAfterResolved(DynamicObject promise, Object value) {
                NativeAccess.notifyPromiseRejectionTracker(
                                promise,
                                2, // v8::PromiseRejectEvent::kPromiseRejectAfterResolved
                                value);
            }

            @Override
            public void promiseResolvedAfterResolved(DynamicObject promise, Object value) {
                NativeAccess.notifyPromiseRejectionTracker(
                                promise,
                                3, // v8::PromiseRejectEvent::kPromiseResolveAfterResolved
                                value);
            }
        } : null;
        mainJSContext.setPromiseRejectionTracker(tracker);
    }

    public void isolateEnableImportMetaInitializer(boolean enable) {
        ImportMetaInitializer initializer = enable ? new ImportMetaInitializer() {
            @Override
            public void initializeImportMeta(DynamicObject importMeta, JSModuleRecord module) {
                NativeAccess.notifyImportMetaInitializer(importMeta, module);
            }
        } : null;
        mainJSContext.setImportMetaInitializer(initializer);
    }

    public void isolateEnableImportModuleDynamically(boolean enable) {
        ImportModuleDynamicallyCallback callback = enable ? new ImportModuleDynamicallyCallback() {
            @Override
            public DynamicObject importModuleDynamically(JSRealm realm, ScriptOrModule referrer, ModuleRequest moduleRequest) {
                return (DynamicObject) NativeAccess.executeImportModuleDynamicallyCallback(realm, referrer, moduleRequest.getSpecifier());
            }
        } : null;
        mainJSContext.setImportModuleDynamicallyCallback(callback);
    }

    public void isolateEnablePrepareStackTraceCallback(boolean enable) {
        PrepareStackTraceCallback callback = enable ? new PrepareStackTraceCallback() {
            @Override
            public Object prepareStackTrace(JSRealm realm, DynamicObject error, DynamicObject structuredStackTrace) {
                return NativeAccess.executePrepareStackTraceCallback(realm, error, structuredStackTrace);
            }
        } : null;
        mainJSContext.setPrepareStackTraceCallback(callback);
    }

    private void exit(int status) {
        try {
            evaluator.close();
        } finally {
            System.exit(status);
        }
    }

    public void isolateEnterPolyglotEngine(long callback, long isolate, long param1, long param2, long args, long execArgs) {
        org.graalvm.polyglot.Source source = org.graalvm.polyglot.Source.newBuilder(JavaScriptLanguage.ID, "(function(r) { r.run(); })", "polyglotEngineWrapper").internal(true).buildLiteral();
        org.graalvm.polyglot.Value wrapper = evaluator.eval(source);
        wrapper.execute(new RunnableInvoker(new Runnable() {
            @Override
            public void run() {
                NativeAccess.polyglotEngineEntered(callback, isolate, param1, param2, args, execArgs);
            }
        }));
    }

    private static final ThreadLocal<Deque<Pair<Long, Object>>> isolateStack = new ThreadLocal<>();

    public void isolateEnter(long isolate) {
        Deque<Pair<Long, Object>> list = isolateStack.get();
        if (list == null) {
            list = new LinkedList<>();
            isolateStack.set(list);
        }
        Object previous = mainJSRealm.getTruffleContext().enter(null);
        if (list.isEmpty()) {
            agent.setThread(Thread.currentThread());
        }
        list.push(new Pair<>(isolate, previous));
    }

    public long isolateExit(long isolate) {
        Deque<Pair<Long, Object>> list = isolateStack.get();
        Pair<Long, Object> pair = list.pop();
        assert pair.getFirst() == isolate;
        mainJSRealm.getTruffleContext().leave(null, pair.getSecond());
        if (list.isEmpty()) {
            agent.setThread(null);
            return 0;
        } else {
            return list.peekLast().getFirst();
        }
    }

    public void isolateEnqueueMicrotask(Object microtask) {
        agent.enqueuePromiseJob((DynamicObject) microtask);
    }

    public void isolateSchedulePauseOnNextStatement() {
        Breakpoint breakpoint = Breakpoint.newBuilder((URI) null).oneShot().build();
        Debugger debugger = lookupInstrument("debugger", Debugger.class);
        debugger.install(breakpoint);
    }

    public void isolateMeasureMemory(Object resolver, boolean detailed) {
        Runtime runtime = Runtime.getRuntime();
        double total = runtime.totalMemory();
        double used = total - runtime.freeMemory();

        JSRealm realm = getCurrentRealm();
        DynamicObject result = JSOrdinary.create(mainJSContext, realm);
        JSObject.set(result, "total", createMemoryInfoObject(used, total, realm));

        if (detailed) {
            JSObject.set(result, "current", createMemoryInfoObject(used, total, realm));

            int contexts = childContextSet.size();
            Object[] array = new Object[contexts];
            for (int i = 0; i < contexts; i++) {
                array[i] = createMemoryInfoObject(0, total, realm);
            }

            JSObject.set(result, "other", JSArray.createConstantObjectArray(mainJSContext, realm, array));
        }

        promiseResolverResolve(resolver, result);
    }

    private Object createMemoryInfoObject(double used, double total, JSRealm realm) {
        Object range = JSArray.createConstantDoubleArray(mainJSContext, realm, new double[]{used, total});
        DynamicObject result = JSOrdinary.create(mainJSContext, realm);
        JSObject.set(result, "jsMemoryEstimate", used);
        JSObject.set(result, "jsMemoryRange", range);
        return result;
    }

    public Object correctReturnValue(Object value) {
        if (value == INT_PLACEHOLDER) {
            resetSharedBuffer();
            return getSharedBuffer().getInt();
        } else if (value == SAFE_INT_PLACEHOLDER) {
            resetSharedBuffer();
            return SafeInteger.valueOf(getSharedBuffer().getLong());
        } else if (value == DOUBLE_PLACEHOLDER) {
            resetSharedBuffer();
            return getSharedBuffer().getDouble();
        } else {
            return value;
        }
    }

    public void stringExternalResourceCallback(Object object, long data, long callbackPointer) {
        WeakCallback weakCallback = new WeakCallback(object, data, callbackPointer, 1, weakCallbackQueue);
        weakCallbacks.add(weakCallback);
        pollWeakCallbackQueue(false);
    }

    public Object proxyGetTarget(Object proxy) {
        return JSProxy.getTarget((DynamicObject) proxy);
    }

    public Object proxyGetHandler(Object proxy) {
        return JSProxy.getHandler((DynamicObject) proxy);
    }

    public boolean proxyIsFunction(Object proxy) {
        return JSRuntime.isCallableProxy((DynamicObject) proxy);
    }

    public Object booleanObjectNew(Object context, boolean value) {
        JSRealm jsRealm = (JSRealm) context;
        return JSBoolean.create(jsRealm.getContext(), jsRealm, value);
    }

    public boolean booleanObjectValueOf(Object object) {
        return JSBoolean.valueOf((DynamicObject) object);
    }

    public Object stringObjectNew(Object context, Object value) {
        JSRealm jsRealm = (JSRealm) context;
        return JSString.create(jsRealm.getContext(), jsRealm, (String) value);
    }

    public String stringObjectValueOf(Object object) {
        return JSString.getString((DynamicObject) object);
    }

    public Object numberObjectNew(Object context, double value) {
        JSRealm jsRealm = (JSRealm) context;
        return JSNumber.create(jsRealm.getContext(), jsRealm, value);
    }

    private static String regexpFlagsToString(int flags) {
        StringBuilder builder = new StringBuilder();
        if ((flags & 1 /* kGlobal */) != 0) {
            builder.append('g');
        } else if ((flags & 2 /* kIgnoreCase */) != 0) {
            builder.append('i');
        } else if ((flags & 4 /* kMultiline */) != 0) {
            builder.append('m');
        } else if ((flags & 8 /* kSticky */) != 0) {
            builder.append('y');
        } else if ((flags & 16 /* kUnicode */) != 0) {
            builder.append('u');
        } else if ((flags & 32 /* kDotAll */) != 0) {
            builder.append('s');
        }
        return builder.toString();
    }

    public Object regexpNew(Object context, Object pattern, int flags) {
        JSRealm jsRealm = (JSRealm) context;
        JSContext jsContext = jsRealm.getContext();
        return regexpCreate(jsContext, jsRealm, (String) pattern, flags);
    }

    public static Object regexpCreate(JSContext context, JSRealm realm, String pattern, int v8Flags) {
        Object compiledRegexp = RegexCompilerInterface.compile(pattern, regexpFlagsToString(v8Flags), context);
        return JSRegExp.create(context, realm, compiledRegexp);
    }

    @TruffleBoundary
    public String regexpGetSource(Object regexp) {
        return regexpPattern((DynamicObject) regexp);
    }

    public static String regexpPattern(DynamicObject regexp) {
        assert JSRegExp.isJSRegExp(regexp);
        Object compiledRegex = JSRegExp.getCompiledRegex(regexp);
        return TRegexUtil.InteropReadStringMemberNode.getUncached().execute(compiledRegex, TRegexUtil.Props.CompiledRegex.PATTERN);
    }

    @TruffleBoundary
    public int regexpGetFlags(Object regexp) {
        return regexpV8Flags((DynamicObject) regexp);
    }

    public static int regexpV8Flags(DynamicObject regexp) {
        Object compiledRegex = JSRegExp.getCompiledRegex(regexp);
        Object flagsObj = TRegexUtil.InteropReadMemberNode.getUncached().execute(compiledRegex, TRegexUtil.Props.CompiledRegex.FLAGS);

        int v8Flags = 0; // v8::RegExp::Flags::kNone
        if (TRegexUtil.InteropReadBooleanMemberNode.getUncached().execute(flagsObj, TRegexUtil.Props.Flags.GLOBAL)) {
            v8Flags |= 1; // v8::RegExp::Flags::kGlobal
        }
        if (TRegexUtil.InteropReadBooleanMemberNode.getUncached().execute(flagsObj, TRegexUtil.Props.Flags.IGNORE_CASE)) {
            v8Flags |= 2; // v8::RegExp::Flags::kIgnoreCase
        }
        if (TRegexUtil.InteropReadBooleanMemberNode.getUncached().execute(flagsObj, TRegexUtil.Props.Flags.MULTILINE)) {
            v8Flags |= 4; // v8::RegExp::Flags::kMultiline
        }
        if (TRegexUtil.InteropReadBooleanMemberNode.getUncached().execute(flagsObj, TRegexUtil.Props.Flags.STICKY)) {
            v8Flags |= 8; // v8::RegExp::Flags::kSticky
        }
        if (TRegexUtil.InteropReadBooleanMemberNode.getUncached().execute(flagsObj, TRegexUtil.Props.Flags.UNICODE)) {
            v8Flags |= 16; // v8::RegExp::Flags::kUnicode
        }
        if (TRegexUtil.InteropReadBooleanMemberNode.getUncached().execute(flagsObj, TRegexUtil.Props.Flags.DOT_ALL)) {
            v8Flags |= 32; // v8::RegExp::Flags::kDotAll
        }
        return v8Flags;
    }

    public Object[] findDynamicObjectFields(Object context) {
        if (!JSConfig.SubstrateVM) {
            Object arrayBuffer = arrayBufferNew(context, 4);
            Object typedArray = uint8ArrayNew(arrayBuffer, 2, 1);

            return new Object[]{
                            findDeclaredField(arrayBuffer.getClass(), "byteBuffer"),
                            findDeclaredField(typedArray.getClass(), "arrayBuffer"),
            };
        }
        return null;
    }

    private static Field findDeclaredField(Class<?> bottom, String fieldName) {
        for (Class<?> cls = bottom; cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            try {
                return cls.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                continue;
            } catch (SecurityException e) {
                break;
            }
        }
        return null;
    }

    public Object jsonParse(Object context, Object string) {
        JSRealm realm = (JSRealm) context;
        TruffleJSONParser parser = new TruffleJSONParser(realm.getContext());
        return parser.parse((String) string, realm);
    }

    public String jsonStringify(Object context, Object object, String gap) {
        DynamicObject stringify = ((JSRealm) context).lookupFunction(JSONBuiltins.BUILTINS, "stringify");
        return (String) JSFunction.call(stringify, Undefined.instance, new Object[]{
                        object,
                        Undefined.instance, // replacer
                        (gap == null) ? Undefined.instance : gap
        });
    }

    public Object promiseResult(Object promise) {
        return JSObjectUtil.getHiddenProperty((DynamicObject) promise, JSPromise.PROMISE_RESULT);
    }

    public int promiseState(Object promise) {
        return JSPromise.getPromiseState((DynamicObject) promise);
    }

    public Object promiseResolverNew(Object context) {
        DynamicObject resolverFactory = getResolverFactory(context);
        return JSFunction.call(JSArguments.create(Undefined.instance, resolverFactory, RESOLVER_RESOLVE, RESOLVER_REJECT));
    }

    private DynamicObject getResolverFactory(Object realm) {
        RealmData data = getRealmEmbedderData(realm);
        DynamicObject resolverFactory = data.getResolverFactory();
        if (resolverFactory == null) {
            resolverFactory = createResolverFactory((JSRealm) realm);
            data.setResolverFactory(resolverFactory);
        }
        return resolverFactory;
    }

    private DynamicObject createResolverFactory(JSRealm realm) {
        JSContext context = realm.getContext();
        JSParser parser = (JSParser) context.getEvaluator();
        String code = "(function(resolveKey, rejectKey) {\n" +
                        "    var resolve, reject;\n" +
                        "    var promise = new Promise(function() {\n" +
                        "        resolve = arguments[0];\n" +
                        "        reject = arguments[1];\n" +
                        "    });\n" +
                        "    Object.defineProperty(promise, resolveKey, { value : resolve });\n" +
                        "    Object.defineProperty(promise, rejectKey, { value : reject });\n" +
                        "    return promise;\n" +
                        "})";
        ScriptNode scriptNode = parser.parseScript(context, code);
        return (DynamicObject) scriptNode.run(realm);
    }

    public boolean promiseResolverResolve(Object resolver, Object value) {
        Object resolve = JSObject.get((DynamicObject) resolver, RESOLVER_RESOLVE);
        JSFunction.call(JSArguments.create(Undefined.instance, resolve, value));
        return true;
    }

    public boolean promiseResolverReject(Object resolver, Object value) {
        Object reject = JSObject.get((DynamicObject) resolver, RESOLVER_REJECT);
        JSFunction.call(JSArguments.create(Undefined.instance, reject, value));
        return true;
    }

    private ESModuleLoader getModuleLoader() {
        if (moduleLoader == null) {
            moduleLoader = new ESModuleLoader();
        }
        return moduleLoader;
    }

    public Object moduleCompile(Object context, Object sourceCode, Object name, Object hostDefinedOptions) {
        JSRealm realm = (JSRealm) context;
        String moduleName = (String) name;
        Source.LiteralBuilder builder = Source.newBuilder(JavaScriptLanguage.ID, (String) sourceCode, moduleName);
        try {
            builder = builder.uri(new URI(moduleName));
        } catch (URISyntaxException usex) {
        }
        builder.mimeType(JavaScriptLanguage.MODULE_MIME_TYPE);
        Source source = builder.build();
        hostDefinedOptionsMap.put(source, hostDefinedOptions);
        TruffleContext truffleContext = realm.getEnv().getContext();
        Object prev = truffleContext.enter(null);
        JSModuleData parsedModule;
        try {
            parsedModule = realm.getContext().getEvaluator().envParseModule(realm, source);
        } finally {
            truffleContext.leave(null, prev);
        }
        JSModuleRecord moduleRecord = new JSModuleRecord(parsedModule, getModuleLoader());
        return moduleRecord;
    }

    public void moduleInstantiate(Object context, Object module, long resolveCallback) {
        JSRealm jsRealm = (JSRealm) context;
        JSContext jsContext = jsRealm.getContext();
        ESModuleLoader loader = getModuleLoader();
        loader.setResolver(resolveCallback);
        try {
            jsContext.getEvaluator().moduleInstantiation(jsRealm, (JSModuleRecord) module);
        } finally {
            loader.setResolver(0);
        }
    }

    public Object moduleEvaluate(Object context, Object module) {
        JSRealm jsRealm = (JSRealm) context;
        JSContext jsContext = jsRealm.getContext();
        JSModuleRecord moduleRecord = (JSModuleRecord) module;

        if (!moduleRecord.isEvaluated()) {
            jsContext.getEvaluator().moduleEvaluation(jsRealm, moduleRecord);
        }

        if (!moduleRecord.isTopLevelAsync()) {
            Throwable evaluationError = moduleRecord.getEvaluationError();
            if (evaluationError != null) {
                throw JSRuntime.rethrow(evaluationError);
            }
        }
        PromiseCapabilityRecord promiseCapability = moduleRecord.getTopLevelCapability();
        if (promiseCapability == null) {
            return moduleRecord.getExecutionResult();
        } else {
            return promiseCapability.getPromise();
        }
    }

    public int moduleGetStatus(Object module) {
        JSModuleRecord record = (JSModuleRecord) module;
        switch (record.getStatus()) {
            case Unlinked:
                return 0; // v8::Module::Status::kUninstantiated
            case Linking:
                return 1; // v8::Module::Status::kInstantiating
            case Linked:
                return 2; // v8::Module::Status::kInstantiated
            case Evaluating:
                return 3; // v8::Module::Status::Evaluating
            case Evaluated:
            default:
                assert record.getStatus() == JSModuleRecord.Status.Evaluated;
                if (record.getEvaluationError() == null) {
                    return 4; // v8::Module::Status::kEvaluated
                } else {
                    return 5; // v8::Module::Status::kErrored
                }
        }
    }

    public Object moduleGetException(Object module) {
        JSModuleRecord record = (JSModuleRecord) module;
        Throwable evaluationError = record.getEvaluationError();
        if (evaluationError instanceof GraalJSException) {
            return ((GraalJSException) evaluationError).getErrorObjectEager(getCurrentRealm());
        }
        return evaluationError;
    }

    public int moduleGetRequestsLength(Object module) {
        JSModuleRecord record = (JSModuleRecord) module;
        return record.getModule().getRequestedModules().size();
    }

    public String moduleGetRequest(Object module, int index) {
        JSModuleRecord record = (JSModuleRecord) module;
        return record.getModule().getRequestedModules().get(index).getSpecifier();
    }

    public Object moduleGetNamespace(Object module) {
        JSModuleRecord record = (JSModuleRecord) module;
        GraalJSEvaluator graalEvaluator = (GraalJSEvaluator) record.getContext().getEvaluator();
        return graalEvaluator.getModuleNamespace(record);
    }

    public int moduleGetIdentityHash(Object module) {
        return System.identityHashCode(module);
    }

    public Object moduleCreateSyntheticModule(String moduleName, Object[] exportNames, final long evaluationStepsCallback) {
        FrameDescriptor frameDescriptor = new FrameDescriptor(Undefined.instance);
        List<Module.ExportEntry> localExportEntries = new ArrayList<>();
        for (Object exportName : exportNames) {
            frameDescriptor.addFrameSlot(exportName);
            localExportEntries.add(Module.ExportEntry.exportSpecifier((String) exportName));
        }
        Module moduleNode = new Module(Collections.emptyList(), Collections.emptyList(), localExportEntries, Collections.emptyList(), Collections.emptyList(), null, null);
        Source source = Source.newBuilder(JavaScriptLanguage.ID, "<unavailable>", moduleName).build();
        JavaScriptRootNode rootNode = new JavaScriptRootNode(mainJSContext.getLanguage(), source.createUnavailableSection(), frameDescriptor) {
            @Override
            public Object execute(VirtualFrame frame) {
                JSModuleRecord module = (JSModuleRecord) JSArguments.getUserArgument(frame.getArguments(), 0);
                if (module.getEnvironment() == null) {
                    assert module.getStatus() == Status.Linking;
                    module.setEnvironment(frame.materialize());
                    return Undefined.instance;
                } else {
                    assert module.getStatus() == Status.Evaluating;
                    return invokeEvaluationStepsCallback(getRealm(), module);
                }
            }

            @TruffleBoundary
            private Object invokeEvaluationStepsCallback(JSRealm realm, JSModuleRecord module) {
                return NativeAccess.syntheticModuleEvaluationSteps(evaluationStepsCallback, realm, module);
            }
        };
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        JSFunctionData functionData = JSFunctionData.createCallOnly(mainJSContext, callTarget, 0, moduleName);
        final JSModuleData parsedModule = new JSModuleData(moduleNode, source, functionData, frameDescriptor);
        final JSModuleRecord moduleRecord = new JSModuleRecord(parsedModule, getModuleLoader());
        return moduleRecord;
    }

    public void moduleSetSyntheticModuleExport(Object module, String exportName, Object exportValue) {
        JSModuleRecord moduleRecord = (JSModuleRecord) module;
        FrameDescriptor frameDescriptor = moduleRecord.getFrameDescriptor();
        FrameSlot frameSlot = frameDescriptor.findFrameSlot(exportName);
        if (frameSlot == null) {
            throw Errors.createReferenceError("Export '" + exportName + "' is not defined in module");
        }
        MaterializedFrame frame = moduleRecord.getEnvironment();
        frame.setObject(frameSlot, exportValue);
    }

    private static final ByteBuffer DUMMY_UNBOUND_MODULE_PARSE_RESULT = ByteBuffer.allocate(0);

    public Object moduleGetUnboundModuleScript(Object module) {
        JSModuleRecord moduleRecord = (JSModuleRecord) module;
        return new UnboundScript(moduleRecord.getSource(), DUMMY_UNBOUND_MODULE_PARSE_RESULT);
    }

    public String scriptOrModuleGetResourceName(Object scriptOrModule) {
        ScriptOrModule record = (ScriptOrModule) scriptOrModule;
        return record.getSource().getName();
    }

    public Object scriptOrModuleGetHostDefinedOptions(Object scriptOrModule) {
        ScriptOrModule record = (ScriptOrModule) scriptOrModule;
        Object hostDefinedOptions = hostDefinedOptionsMap.get(record.getSource());
        return (hostDefinedOptions == null) ? new Object[0] : hostDefinedOptions;
    }

    public Object valueSerializerNew(long delegatePointer) {
        TruffleLanguage.Env env = getCurrentRealm().getEnv();
        return new Serializer(env, this, delegatePointer);
    }

    public int valueSerializerSize(Object serializer) {
        return ((Serializer) serializer).size();
    }

    public void valueSerializerRelease(Object serializer, Object targetBuffer) {
        ((Serializer) serializer).release((ByteBuffer) targetBuffer);
    }

    public void valueSerializerWriteHeader(Object serializer) {
        ((Serializer) serializer).writeHeader();
    }

    public void valueSerializerWriteValue(Object serializer, Object value) {
        ((Serializer) serializer).writeValue(value);
    }

    public void valueSerializerWriteUint32(Object serializer, int value) {
        ((Serializer) serializer).writeVarInt(Integer.toUnsignedLong(value));
    }

    public void valueSerializerWriteUint64(Object serializer, long value) {
        ((Serializer) serializer).writeVarInt(value);
    }

    public void valueSerializerWriteDouble(Object serializer, double value) {
        ((Serializer) serializer).writeDouble(value);
    }

    public void valueSerializerWriteRawBytes(Object serializer, Object bytes) {
        ((Serializer) serializer).writeBytes((ByteBuffer) bytes);
    }

    public void valueSerializerSetTreatArrayBufferViewsAsHostObjects(Object serializer, boolean treatArrayBufferViewsAsHostObjects) {
        ((Serializer) serializer).setTreatArrayBufferViewsAsHostObjects(treatArrayBufferViewsAsHostObjects);
    }

    public void valueSerializerTransferArrayBuffer(Object serializer, int id, Object arrayBuffer) {
        ((Serializer) serializer).transferArrayBuffer(id, arrayBuffer);
    }

    public Object valueDeserializerNew(long delegate, Object buffer) {
        return new Deserializer(delegate, (ByteBuffer) buffer);
    }

    public void valueDeserializerReadHeader(Object deserializer) {
        ((Deserializer) deserializer).readHeader();
    }

    public Object valueDeserializerReadValue(Object context, Object deserializer) {
        return ((Deserializer) deserializer).readValue((JSRealm) context);
    }

    public int valueDeserializerReadUint32(Object deserializer) {
        return ((Deserializer) deserializer).readVarInt();
    }

    public long valueDeserializerReadUint64(Object deserializer) {
        return ((Deserializer) deserializer).readVarLong();
    }

    public double valueDeserializerReadDouble(Object deserializer) {
        return ((Deserializer) deserializer).readDouble();
    }

    public int valueDeserializerReadRawBytes(Object deserializer, int length) {
        return ((Deserializer) deserializer).readBytes(length);
    }

    public void valueDeserializerTransferArrayBuffer(Object deserializer, int id, Object arrayBuffer) {
        ((Deserializer) deserializer).transferArrayBuffer(id, (DynamicObject) arrayBuffer);
    }

    public int valueDeserializerGetWireFormatVersion(Object deserializer) {
        return ((Deserializer) deserializer).getWireFormatVersion();
    }

    public Object mapNew(Object context) {
        JSRealm jsRealm = (JSRealm) context;
        JSContext jsContext = jsRealm.getContext();
        return JSMap.create(jsContext, jsRealm);
    }

    public void mapSet(Object set, Object key, Object value) {
        DynamicObject object = (DynamicObject) set;
        JSMap.getInternalMap(object).put(JSSet.normalize(key), value);
    }

    public Object setNew(Object context) {
        JSRealm jsRealm = (JSRealm) context;
        JSContext jsContext = jsRealm.getContext();
        return JSSet.create(jsContext, jsRealm);
    }

    public void setAdd(Object set, Object key) {
        DynamicObject object = (DynamicObject) set;
        JSSet.getInternalSet(object).put(JSSet.normalize(key), new Object());
    }

    public long bigIntInt64Value(Object value) {
        BigInteger bigInt = ((BigInt) value).bigIntegerValue();
        resetSharedBuffer();
        sharedBuffer.putInt(bigInt.bitLength() <= 63 ? 1 : 0); // lossless
        return bigInt.longValue();
    }

    public long bigIntUint64Value(Object value) {
        BigInteger bigInt = ((BigInt) value).bigIntegerValue();
        resetSharedBuffer();
        sharedBuffer.putInt((bigInt.signum() != -1 && bigInt.bitLength() <= 64) ? 1 : 0); // lossless
        return bigInt.longValue();
    }

    public Object bigIntNew(long value) {
        return BigInt.valueOf(value);
    }

    public Object bigIntNewFromUnsigned(long value) {
        BigInteger bigInt = BigInteger.valueOf(value & 0x7FFFFFFFFFFFFFFFL);
        if (value < 0) {
            bigInt = bigInt.setBit(63);
        }
        return new BigInt(bigInt);
    }

    public Object bigIntNewFromWords() { // all arguments are in sharedBuffer
        resetSharedBuffer();
        int sign = sharedBuffer.getInt();
        int count = sharedBuffer.getInt();
        BigInteger result = BigInteger.ZERO;
        for (int wordIdx = 0; wordIdx < count; wordIdx++) {
            long word = sharedBuffer.getLong();
            for (int bit = 0; bit < 64; bit++) {
                if ((word & (1L << bit)) != 0) {
                    result = result.setBit(bit + 64 * wordIdx);
                }
            }
        }
        if (sign != 0) {
            result = result.negate();
        }
        return new BigInt(result);
    }

    public int bigIntWordCount(Object value) {
        BigInteger bigInt = ((BigInt) value).bigIntegerValue();
        return (bigInt.bitLength() + ((bigInt.signum() == -1) ? 1 : 0) + 63) / 64;
    }

    public void bigIntToWordsArray(Object value) {
        BigInteger bigInt = ((BigInt) value).bigIntegerValue();
        resetSharedBuffer();
        int count = bigIntWordCount(value);
        sharedBuffer.putInt(count);
        sharedBuffer.putInt(bigInt.signum() == -1 ? 1 : 0);
        if (bigInt.signum() == -1) {
            bigInt = bigInt.negate();
        }
        for (int wordIdx = 0; wordIdx < count; wordIdx++) {
            long word = 0;
            for (int bit = 63; bit >= 0; bit--) {
                word <<= 1;
                if (bigInt.testBit(bit + 64 * wordIdx)) {
                    word++;
                }
            }
            sharedBuffer.putLong(word);
        }
    }

    public void backingStoreRegisterCallback(Object backingStore, long data, int byteLength, long deleterData, long callback) {
        weakCallbacks.add(new DeleterCallback(backingStore, data, byteLength, deleterData, callback, weakCallbackQueue));
    }

    private static class WeakCallback extends WeakReference<Object> {

        long data;
        long callback;
        int type;

        WeakCallback(Object object, long data, long callback, int type, ReferenceQueue<Object> queue) {
            super(object, queue);
            this.data = data;
            this.callback = callback;
            this.type = type;
        }

    }

    // v8:BackingStore::DeleterCallback
    private static class DeleterCallback extends WeakCallback {
        int length;
        long deleterData;

        DeleterCallback(Object backingStore, long data, int length, long deleterData, long callback, ReferenceQueue<Object> queue) {
            super(backingStore, data, callback, -1, queue);
            this.length = length;
            this.deleterData = deleterData;
        }
    }

    static class PropertyHandlerPrototypeNode extends JavaScriptRootNode {
        private final boolean global;
        @Child private GetPrototypeNode getPrototypeNode;

        PropertyHandlerPrototypeNode(boolean global) {
            this.global = global;
            if (!global) {
                this.getPrototypeNode = GetPrototypeNode.create();
            }
        }

        @Override
        public Object execute(VirtualFrame vf) {
            Object target = vf.getArguments()[2];
            if (global) {
                return target;
            } else {
                return getPrototypeNode.executeJSObject(target);
            }
        }

    }

    static class ESModuleLoader implements JSModuleLoader {
        private final Map<ScriptOrModule, Map<String, JSModuleRecord>> cache = new HashMap<>();
        private long resolver;

        void setResolver(long resolver) {
            this.resolver = resolver;
        }

        @Override
        public JSModuleRecord resolveImportedModule(ScriptOrModule referrer, ModuleRequest moduleRequest) {
            Map<String, JSModuleRecord> referrerCache = cache.get(referrer);
            String specifier = moduleRequest.getSpecifier();
            if (referrerCache == null) {
                referrerCache = new HashMap<>();
                cache.put(referrer, referrerCache);
            } else {
                JSModuleRecord cached = referrerCache.get(specifier);
                if (cached != null) {
                    return cached;
                }
            }
            if (resolver == 0) {
                System.err.println("Cannot resolve module outside module instantiation!");
                System.exit(1);
            }
            JSRealm realm = JSRealm.get(null);
            JSModuleRecord result = (JSModuleRecord) NativeAccess.executeResolveCallback(resolver, realm, specifier, referrer);
            referrerCache.put(specifier, result);
            return result;
        }

        @Override
        public JSModuleRecord loadModule(Source moduleSource, JSModuleData moduleData) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Used to establish a communication channels between node workers when they attempt to exchange
     * Java host objects.
     */
    private JavaMessagePortData currentMessagePortData = null;

    public void unsetCurrentMessagePortData() {
        currentMessagePortData.encodingEnd();
        currentMessagePortData = null;
    }

    public void setCurrentMessagePortData(JSExternalObject nativeMessagePortData) {
        assert nativeMessagePortData != null;
        assert currentMessagePortData == null;
        currentMessagePortData = SharedMemMessagingManager.getJavaMessagePortDataFor(nativeMessagePortData);
        assert currentMessagePortData != null;
        currentMessagePortData.encodingBegin();
    }

    public JavaMessagePortData getCurrentMessagePortData() {
        return currentMessagePortData;
    }

    /**
     * {@code ScriptOrModule} of a function produced by
     * {@code ScriptCompiler::CompileFunctionInContext()}. It supports native weak references.
     */
    static class NodeScriptOrModule extends ScriptOrModule {
        static final HiddenKey SCRIPT_OR_MODULE = new HiddenKey("scriptOrModule");

        private Map<Long, WeakCallback> weakCallbackMap;

        NodeScriptOrModule(JSContext context, Source source) {
            super(context, source);
        }

        Map<Long, WeakCallback> getWeakCallbackMap() {
            if (weakCallbackMap == null) {
                weakCallbackMap = new HashMap<>();
            }
            return weakCallbackMap;
        }

    }

}
