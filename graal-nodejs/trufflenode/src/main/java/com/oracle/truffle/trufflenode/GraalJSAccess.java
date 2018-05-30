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
package com.oracle.truffle.trufflenode;

import static com.oracle.truffle.trufflenode.ValueType.ARRAY_BUFFER_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.ARRAY_BUFFER_VIEW_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.BOOLEAN_VALUE_FALSE;
import static com.oracle.truffle.trufflenode.ValueType.BOOLEAN_VALUE_TRUE;
import static com.oracle.truffle.trufflenode.ValueType.DATA_VIEW_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DATE_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.EXTERNAL_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.FLOAT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.FLOAT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.FUNCTION_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INT8ARRAY_OBJECT;
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
import static com.oracle.truffle.trufflenode.ValueType.UINT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.UINT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.UINT8ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.UINT8CLAMPEDARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.UNDEFINED_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.UNKNOWN_TYPE;
import static com.oracle.truffle.trufflenode.buffer.NIOBufferObject.NIO_BUFFER_MODULE_NAME;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.graalvm.polyglot.Context;

import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.Module;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.InstrumentInfo;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.SuspendedCallback;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.function.ConstructorRootNode;
import com.oracle.truffle.js.parser.GraalJSEvaluator;
import com.oracle.truffle.js.parser.GraalJSParserHelper;
import com.oracle.truffle.js.parser.GraalJSParserOptions;
import com.oracle.truffle.js.parser.JSParser;
import com.oracle.truffle.js.parser.JavaScriptLanguage;
import com.oracle.truffle.js.parser.JavaScriptTranslator;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.ExitException;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.PromiseHook;
import com.oracle.truffle.js.runtime.PromiseRejectionTracker;
import com.oracle.truffle.js.runtime.RegexCompilerInterface;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSON;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.PropertyReference;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.trufflenode.buffer.NIOBufferObject;
import com.oracle.truffle.trufflenode.info.Accessor;
import com.oracle.truffle.trufflenode.info.FunctionTemplate;
import com.oracle.truffle.trufflenode.info.ObjectTemplate;
import com.oracle.truffle.trufflenode.info.PropertyHandler;
import com.oracle.truffle.trufflenode.info.Script;
import com.oracle.truffle.trufflenode.info.UnboundScript;
import com.oracle.truffle.trufflenode.info.Value;
import com.oracle.truffle.trufflenode.interop.GraalJSJavaInteropMainWorker;
import com.oracle.truffle.trufflenode.node.ExecuteNativeFunctionNode;
import com.oracle.truffle.trufflenode.node.ExecuteNativePropertyHandlerNode;
import com.oracle.truffle.trufflenode.node.debug.IteratorPreviewNode;
import com.oracle.truffle.trufflenode.node.debug.MakeMirrorNode;
import com.oracle.truffle.trufflenode.node.debug.PromiseStatusNode;
import com.oracle.truffle.trufflenode.node.debug.PromiseValueNode;
import com.oracle.truffle.trufflenode.node.debug.SetBreakPointNode;

/**
 *
 * @author Jan Stola
 */
@SuppressWarnings("static-method")
public final class GraalJSAccess {

    private static final boolean VERBOSE = Boolean.getBoolean("truffle.node.js.verbose");
    private static final boolean USE_NIO_BUFFER = !"false".equals(System.getProperty("node.buffer.nio"));
    private static final boolean USE_SNAPSHOTS = !"false".equalsIgnoreCase(System.getProperty("truffle.node.js.snapshots"));

    private static final HiddenKey PRIVATE_VALUES_KEY = new HiddenKey("PrivateValues");
    private static final HiddenKey FUNCTION_TEMPLATE_DATA_KEY = new HiddenKey("FunctionTemplateData");
    private static final HiddenKey INTERNAL_FIELD_COUNT_KEY = new HiddenKey("InternalFieldCount");
    private static final HiddenKey INTERNAL_FIELD_ZERO_KEY = new HiddenKey("InternalField0");

    private static final Symbol RESOLVER_RESOLVE = Symbol.create("Resolve");
    private static final Symbol RESOLVER_REJECT = Symbol.create("Reject");

    public static final HiddenKey HOLDER_KEY = new HiddenKey("Holder");

    // Placeholders returned by a native function when a primitive value
    // written into a shared buffer should be returned instead
    private static final Object INT_PLACEHOLDER = new Object();
    private static final Object LARGE_INT_PLACEHOLDER = new Object();
    private static final Object DOUBLE_PLACEHOLDER = new Object();

    private final Context evaluator;
    private final JSContext mainJSContext;
    private JSRealm debugRealm;
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

    private final List<Reference<FunctionTemplate>> functionTemplates = new ArrayList<>();

    private final boolean exposeGC;

    // static accessors to JSRegExp properties (usually via nodes)
    private static final TRegexUtil.TRegexCompiledRegexAccessor STATIC_COMPILED_REGEX_ACCESSOR = TRegexUtil.TRegexCompiledRegexAccessor.create();
    private static final TRegexUtil.TRegexFlagsAccessor STATIC_FLAGS_ACCESSOR = TRegexUtil.TRegexFlagsAccessor.create();

    private GraalJSAccess(String[] args, long loopAddress) throws Exception {
        try {
            Options options = Options.parseArguments(prepareArguments(args));
            Context.Builder contextBuilder = options.getContextBuilder();

            contextBuilder.option(JSContextOptions.DIRECT_BYTE_BUFFER_NAME, "true");
            contextBuilder.option(JSContextOptions.V8_COMPATIBILITY_MODE_NAME, "true");
            contextBuilder.option(JSContextOptions.INTL_402_NAME, "true");
            contextBuilder.option(GraalJSParserOptions.SYNTAX_EXTENSIONS_NAME, "false");

            exposeGC = options.isGCExposed();
            evaluator = contextBuilder.build();
        } catch (IllegalArgumentException iaex) {
            System.err.printf("ERROR: %s", iaex.getMessage());
            System.exit(1);
            throw iaex; // avoids compiler complaints that final fields are not initialized
        }

        JSRealm mainJSRealm = JavaScriptLanguage.getJSRealm(evaluator);
        mainJSContext = mainJSRealm.getContext();
        assert mainJSContext != null : "JSContext initialized";
        GraalJSJavaInteropMainWorker worker = new GraalJSJavaInteropMainWorker(this, loopAddress);
        mainJSContext.initializeJavaInteropWorkers(worker, worker);
        deallocator = new Deallocator();
        envForInstruments = mainJSRealm.getEnv();

        ensureErrorClassesInitialized();
    }

    private static void ensureErrorClassesInitialized() {
        if (JSTruffleOptions.SubstrateVM) {
            return;
        }
        // Ensure initialization of error-related classes (to avoid NoClassDefFoundError
        // during conversion of StackOverflowError to RangeError)
        TruffleStackTraceElement.getStackTrace(Errors.createRangeError(""));
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

    public static Object create(String[] args, long loopAddress) throws Exception {
        return new GraalJSAccess(args, loopAddress);
    }

    public Object undefinedInstance() {
        return Undefined.instance;
    }

    public Object nullInstance() {
        return Null.instance;
    }

    public void resetSharedBuffer() {
        sharedBuffer.clear();
    }

    public ByteBuffer getSharedBuffer() {
        return sharedBuffer;
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
        } else if (JSObject.isJSObject(value)) {
            return valueTypeJSObject((DynamicObject) value, useSharedBuffer);
        } else if (JSRuntime.isForeignObject(value)) {
            return valueTypeForeignObject((TruffleObject) value, useSharedBuffer);
        } else if (JSRuntime.isString(value)) { // JSLazyString
            return LAZY_STRING_VALUE;
        } else if (value instanceof Symbol) {
            return SYMBOL_VALUE;
        }
        if (JSTruffleOptions.NashornJavaInterop) {
            return ORDINARY_OBJECT;
        }
        if (value instanceof Throwable) {
            valueTypeError(value);
        }
        return UNKNOWN_TYPE;
    }

    private int valueTypeForeignObject(TruffleObject value, boolean useSharedBuffer) {
        if (ForeignAccess.sendIsBoxed(Message.IS_BOXED.createNode(), value)) {
            Object unboxedValue = JSInteropNodeUtil.unbox(value);
            return valueType(unboxedValue, useSharedBuffer);
        } else if (ForeignAccess.sendIsExecutable(Message.IS_EXECUTABLE.createNode(), value)) {
            return FUNCTION_OBJECT;
        } else {
            return ORDINARY_OBJECT;
        }
    }

    private int valueTypeJSObject(DynamicObject obj, boolean useSharedBuffer) {
        if (JSExternalObject.isJSExternalObject(obj)) {
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
            return ARRAY_BUFFER_OBJECT;
        } else if (JSDataView.isJSDataView(obj)) {
            return DATA_VIEW_OBJECT;
        } else if (JSMap.isJSMap(obj)) {
            return MAP_OBJECT;
        } else if (JSSet.isJSSet(obj)) {
            return SET_OBJECT;
        } else if (JSPromise.isJSPromise(obj)) {
            return PROMISE_OBJECT;
        } else if (JSProxy.isProxy(obj)) {
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
            return UINT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint8ClampedArray) {
            return UINT8CLAMPEDARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt8Array) {
            return INT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint16Array) {
            return UINT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt16Array) {
            return INT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint32Array) {
            return UINT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt32Array) {
            return INT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectFloat32Array) {
            return FLOAT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectFloat64Array) {
            return FLOAT64ARRAY_OBJECT;
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
        return JSExternalObject.getPointer((DynamicObject) obj);
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
            Object iteratedObj = dynamicObject.get(JSRuntime.ITERATED_OBJECT_ID);
            return JSSet.isJSSet(iteratedObj);
        }
        return false;
    }

    public boolean valueIsMapIterator(Object object) {
        if (JSRuntime.isObject(object)) {
            DynamicObject dynamicObject = (DynamicObject) object;
            Object iteratedObj = dynamicObject.get(JSRuntime.ITERATED_OBJECT_ID);
            return JSMap.isJSMap(iteratedObj);
        }
        return false;
    }

    public boolean valueIsSharedArrayBuffer(Object object) {
        return JSSharedArrayBuffer.isJSSharedArrayBuffer(object);
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

    public Object objectNew(Object context) {
        return JSUserObject.create((JSRealm) context);
    }

    public boolean objectSet(Object object, Object key, Object value) {
        DynamicObject dynamicObject = (DynamicObject) object;
        if (key instanceof HiddenKey) {
            dynamicObject.define(key, value);
        } else {
            JSObject.set((DynamicObject) object, JSRuntime.toPropertyKey(key), value);
        }
        return true;
    }

    public boolean objectSetIndex(Object object, int index, Object value) {
        JSObject.set((DynamicObject) object, index, value);
        return true;
    }

    public boolean objectForceSet(Object object, Object key, Object value, int attributes) {
        Object propertyKey = JSRuntime.toPropertyKey(key);
        JSObject.delete((DynamicObject) object, propertyKey);
        PropertyDescriptor descriptor = propertyDescriptor(attributes, value);
        return JSObject.defineOwnProperty((DynamicObject) object, propertyKey, descriptor);
    }

    public boolean objectSetPrivate(Object context, Object object, Object key, Object value) {
        if (JSRuntime.isObject(object)) {
            DynamicObject dynamicObject = (DynamicObject) object;
            Object privateValues = dynamicObject.get(PRIVATE_VALUES_KEY);
            if (privateValues == null) {
                privateValues = objectNew(context);
                dynamicObject.define(PRIVATE_VALUES_KEY, privateValues);
            }
            JSObject.set((DynamicObject) privateValues, key, value);
        }
        return true;
    }

    public Object objectGetPrivate(Object object, Object key) {
        if (!JSObject.isJSObject(object)) {
            return null;
        }
        DynamicObject dynamicObject = (DynamicObject) object;
        DynamicObject privateValues = (DynamicObject) dynamicObject.get(PRIVATE_VALUES_KEY);
        if (privateValues == null) {
            return null;
        } else if (JSObject.hasOwnProperty(privateValues, key)) {
            return JSObject.get(privateValues, key);
        } else {
            return null;
        }
    }

    public boolean objectDeletePrivate(Object object, Object key) {
        if (!JSObject.isJSObject(object)) {
            return true;
        }
        DynamicObject dynamicObject = (DynamicObject) object;
        Object privateValues = dynamicObject.get(PRIVATE_VALUES_KEY);
        if (privateValues == null) {
            return true;
        } else {
            return JSObject.delete((DynamicObject) privateValues, key);
        }
    }

    public Object objectGet(Object object, Object key) {
        TruffleObject truffleObject;
        if (object instanceof TruffleObject) {
            truffleObject = (TruffleObject) object;
        } else {
            truffleObject = JSRuntime.toObject(mainJSContext, object);
        }
        Object value;
        if (key instanceof HiddenKey) {
            Object hiddenValue = ((DynamicObject) truffleObject).get(key);
            if (hiddenValue == null) {
                if (JSPromise.isJSPromise(object)) {
                    value = 0;
                } else {
                    value = Undefined.instance;
                }
            } else {
                value = hiddenValue;
            }
        } else {
            value = JSObject.get(truffleObject, JSRuntime.toPropertyKey(key));
        }
        Object flatten = valueFlatten(value);
        resetSharedBuffer();
        sharedBuffer.position(4);
        sharedBuffer.putInt(0, valueType(flatten, true));
        return flatten;
    }

    public Object objectGetIndex(Object object, int index) {
        Object value = valueFlatten(JSObject.get((DynamicObject) object, index));
        resetSharedBuffer();
        sharedBuffer.position(4);
        sharedBuffer.putInt(0, valueType(value, true));
        return value;
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
        } else if (value instanceof PropertyReference) {
            return ((PropertyReference) value).toString();
        } else if (JSRuntime.isForeignObject(value)) {
            TruffleObject truffleObject = (TruffleObject) value;
            if (ForeignAccess.sendIsBoxed(Message.IS_BOXED.createNode(), truffleObject)) {
                Object unboxedValue = JSInteropNodeUtil.unbox(truffleObject);
                if (unboxedValue instanceof String) {
                    return unboxedValue;
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
        if (JSProxy.isProxy(obj)) {
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
        JSRealm realm = context.getRealm();
        DynamicObject getter = functionFromFunctionData(realm, accessorFunctions.getFirst(), dynamicObject);
        DynamicObject setter = functionFromFunctionData(realm, accessorFunctions.getSecond(), dynamicObject);
        JSObjectUtil.putAccessorProperty(context, dynamicObject, accessor.getName(), getter, setter, flags);
        return true;
    }

    public Object objectClone(Object object) {
        DynamicObject dynamicObject = (DynamicObject) object;
        return dynamicObject.copy(dynamicObject.getShape());
    }

    public boolean objectSetPrototype(Object object, Object prototype) {
        return JSObject.setPrototype((DynamicObject) object, (DynamicObject) prototype);
    }

    public Object objectGetPrototype(Object object) {
        return JSObject.getPrototype((DynamicObject) object);
    }

    public String objectGetConstructorName(Object object) {
        DynamicObject dynamicObject = (DynamicObject) object;
        DynamicObject constructor = (DynamicObject) JSObject.get(dynamicObject, JSObject.CONSTRUCTOR);
        return JSFunction.getName(constructor);
    }

    public Object objectGetOwnPropertyNames(Object object) {
        DynamicObject dynamicObject = (DynamicObject) object;
        List<String> names = JSObject.enumerableOwnNames(dynamicObject);
        Object[] namesArray = names.toArray();
        convertArrayIndicesToNumbers(namesArray);
        JSContext context = JSObject.getJSContext(dynamicObject);
        return JSArray.createConstantObjectArray(context, namesArray);
    }

    public Object objectGetPropertyNames(Object object) {
        List<Object> names = new ArrayList<>();
        DynamicObject dynamicObject = (DynamicObject) object;
        JSContext context = JSObject.getJSContext(dynamicObject);
        while (dynamicObject != Null.instance) {
            List<String> newNames = JSObject.enumerableOwnNames(dynamicObject);
            names.addAll(newNames);
            dynamicObject = JSObject.getPrototype(dynamicObject);
        }
        Object[] namesArray = names.toArray();
        convertArrayIndicesToNumbers(namesArray);
        return JSArray.createConstantObjectArray(context, namesArray);
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
        Object obj = object;
        if (JSProxy.isProxy(obj)) {
            obj = JSProxy.getTarget((DynamicObject) obj);
        }
        boolean has = objectHas(obj, key);
        return has ? objectGet(obj, key) : null;
    }

    public int objectGetRealNamedPropertyAttributes(Object object, Object key) {
        Object obj = object;
        if (JSProxy.isProxy(obj)) {
            obj = JSProxy.getTarget((DynamicObject) obj);
        }
        PropertyDescriptor desc = JSObject.getOwnProperty((DynamicObject) obj, key);
        int attributes;
        if (desc == null) {
            attributes = -1;
        } else {
            attributes = 0;
            if (!desc.getWritable()) {
                attributes |= 1; /* v8::PropertyAttribute::ReadOnly */
            }
            if (!desc.getEnumerable()) {
                attributes |= 2; /* v8::PropertyAttribute::DontEnum */
            }
            if (!desc.getConfigurable()) {
                attributes |= 4; /* v8::PropertyAttribute::DontDelete */
            }
        }
        return attributes;
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
        if (!JSProxy.isProxy(object)) {
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
        throw new IllegalArgumentException("Cannot get creation context for this object");
    }

    public Object arrayNew(Object context, int length) {
        return JSArray.createConstantEmptyArray(((JSRealm) context).getContext(), length);
    }

    public long arrayLength(Object object) {
        return JSArray.arrayGetLength((DynamicObject) object);
    }

    public Object arrayBufferNew(Object context, Object buffer, long pointer) {
        ByteBuffer byteBuffer = (ByteBuffer) buffer;
        if (pointer != 0) {
            deallocator.register(byteBuffer, pointer);
        }
        return JSArrayBuffer.createDirectArrayBuffer(((JSRealm) context).getContext(), byteBuffer);
    }

    public Object arrayBufferNew(Object context, int byteLength) {
        return JSArrayBuffer.createDirectArrayBuffer(((JSRealm) context).getContext(), byteLength);
    }

    public Object arrayBufferGetContents(Object arrayBuffer) {
        return JSArrayBuffer.getDirectByteBuffer((DynamicObject) arrayBuffer);
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

    public static int arrayBufferViewByteLength(JSContext context, DynamicObject arrayBufferView) {
        if (JSDataView.isJSDataView(arrayBufferView)) {
            return JSDataView.typedArrayGetLength(arrayBufferView);
        } else {
            return JSArrayBufferView.getByteLength(arrayBufferView, JSArrayBufferView.isJSArrayBufferView(arrayBufferView), context);
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
            return JSArrayBufferView.getByteOffset(arrayBufferView, JSArrayBufferView.isJSArrayBufferView(arrayBufferView), context);
        }
    }

    public int typedArrayLength(Object typedArray) {
        return JSArrayBufferView.typedArrayGetLength((DynamicObject) typedArray);
    }

    private Object typedArrayNew(Object arrayBuffer, int offset, int length, TypedArrayFactory factory) {
        TypedArray arrayType = factory.createArrayType(true, offset != 0);
        DynamicObject dynamicObject = (DynamicObject) arrayBuffer;
        JSContext context = JSObject.getJSContext(dynamicObject);
        return JSArrayBufferView.createArrayBufferView(context, dynamicObject, arrayType, offset, length);
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

    public Object dataViewNew(Object arrayBuffer, int offset, int length) {
        DynamicObject dynamicObject = (DynamicObject) arrayBuffer;
        JSContext context = JSObject.getJSContext(dynamicObject);
        return JSDataView.createDataView(context, dynamicObject, offset, length);
    }

    public Object externalNew(Object context, long pointer) {
        return JSExternalObject.create(((JSRealm) context).getContext(), pointer);
    }

    public Object integerNew(long value) {
        return (double) value;
    }

    public Object numberNew(double value) {
        return value;
    }

    public Object dateNew(Object context, double value) {
        return JSDate.create(((JSRealm) context).getContext(), value);
    }

    public double dateValueOf(Object date) {
        return JSDate.getTimeMillisField((DynamicObject) date);
    }

    public Object symbolNew(Object name) {
        return Symbol.create((String) name);
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
        Object flatten = valueFlatten(value);
        resetSharedBuffer();
        sharedBuffer.position(4);
        sharedBuffer.putInt(0, valueType(flatten, true));
        return flatten;
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
        Object stack = JSObject.get(error, JSError.STACK_NAME);
        if (stack == Undefined.instance) {
            stack = String.format("%s: %s\n    at %s (native)", errorType, message, errorType);
            JSObject.set(error, JSError.STACK_NAME, stack);
        }
        return error;
    }

    public Object exceptionCreateMessage(Object exceptionObject) {
        return exceptionObjectToException(exceptionObject);
    }

    private GraalJSException exceptionObjectToException(Object exceptionObject) {
        if (JSObject.isDynamicObject(exceptionObject)) {
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

    public Object functionTemplateNew(int id, long pointer, Object additionalData, Object signature) {
        FunctionTemplate template = new FunctionTemplate(id, pointer, additionalData, (FunctionTemplate) signature);
        template.getInstanceTemplate().setParentFunctionTemplate(template);
        while (functionTemplates.size() <= id) {
            functionTemplates.add(null);
        }
        functionTemplates.set(id, new WeakReference<>(template));
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
        instanceTemplate.addValue(new Value(Symbol.SYMBOL_TO_STRING_TAG, name, JSAttributes.configurableEnumerableWritable()));
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

        if (template.getFunctionObject() == null) { // PENDING should be per context
            CompilerDirectives.transferToInterpreterAndInvalidate();
            DynamicObject obj = functionTemplateCreateCallback(jsContext, jsRealm, template);
            objectTemplateInstantiate(jsRealm, template.getFunctionObjectTemplate(), obj);

            DynamicObject proto = JSUserObject.create(jsContext);
            objectTemplateInstantiate(jsRealm, template.getPrototypeTemplate(), proto);
            JSObjectUtil.putConstructorProperty(jsContext, proto, obj);
            JSObject.set(obj, JSObject.PROTOTYPE, proto);

            FunctionTemplate parentTemplate = template.getParent();
            if (parentTemplate != null) {
                DynamicObject parentFunction = (DynamicObject) functionTemplateGetFunction(realm, parentTemplate);
                DynamicObject parentProto = (DynamicObject) JSObject.get(parentFunction, JSObject.PROTOTYPE);
                JSObject.setPrototype(proto, parentProto);
            }
        }

        return template.getFunctionObject();
    }

    public Object functionTemplateGetFunction(int id) {
        FunctionTemplate template = functionTemplates.get(id).get();
        if (template == null) {
            // should not happen
            System.err.println("functionTemplateGetFunction() called for an unknown/GCed template!");
            return null;
        } else {
            return template.getFunctionObject();
        }
    }

    private DynamicObject functionTemplateCreateCallback(JSContext context, JSRealm realm, FunctionTemplate template) {
        CompilerAsserts.neverPartOfCompilation("do not create function template in compiled code");
        JSFunctionData functionData = JSFunctionData.create(context, 0, template.getClassName(), true, false, false, false);
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new ExecuteNativeFunctionNode.NativeFunctionRootNode(this, context, template, false, false));
        CallTarget newCallTarget = Truffle.getRuntime().createCallTarget(new ExecuteNativeFunctionNode.NativeFunctionRootNode(this, context, template, true, false));
        CallTarget newTargetCallTarget = Truffle.getRuntime().createCallTarget(new ExecuteNativeFunctionNode.NativeFunctionRootNode(this, context, template, true, true));
        CallTarget constructTarget = Truffle.getRuntime().createCallTarget(ConstructorRootNode.create(functionData, newCallTarget, false));
        CallTarget constructNewTarget = Truffle.getRuntime().createCallTarget(ConstructorRootNode.create(functionData, newTargetCallTarget, true));
        functionData.setCallTarget(callTarget);
        functionData.setConstructTarget(constructTarget);
        functionData.setConstructNewTarget(constructNewTarget);
        DynamicObject functionObject = JSFunction.create(realm, functionData);
        template.setFunctionObject(functionObject);

        // Additional data are held weakly from C => we have to ensure that
        // they are not GCed before the corresponding function is GCed
        functionObject.set(FUNCTION_TEMPLATE_DATA_KEY, template.getAdditionalData());

        return functionObject;
    }

    public boolean functionTemplateHasInstance(Object templateObj, Object instance) {
        FunctionTemplate functionTemplate = (FunctionTemplate) templateObj;
        DynamicObject functionObject = functionTemplate.getFunctionObject();
        if (functionObject == null) {
            return false;
        }
        if (instance instanceof DynamicObject) {
            Object constructor = ((DynamicObject) instance).get(FunctionTemplate.CONSTRUCTOR);
            if (constructor == null) {
                return false; // not created from FunctionTemplate
            }
            DynamicObject templatePrototype = (DynamicObject) JSObject.get(functionObject, JSObject.PROTOTYPE);
            return JSRuntime.isPrototypeOf((DynamicObject) instance, templatePrototype);
        }
        return false;
    }

    public Object objectTemplateNew() {
        return new ObjectTemplate();
    }

    @CompilerDirectives.TruffleBoundary
    public Object objectTemplateNewInstance(Object realm, Object templateObj) {
        JSRealm jsRealm = (JSRealm) realm;
        JSContext jsContext = jsRealm.getContext();
        ObjectTemplate template = (ObjectTemplate) templateObj;
        FunctionTemplate parentFunctionTemplate = template.getParentFunctionTemplate();
        if (parentFunctionTemplate != null) {
            DynamicObject function = (DynamicObject) functionTemplateGetFunction(realm, parentFunctionTemplate);
            return JSFunction.getConstructTarget(function).call(JSFunction.CONSTRUCT, function);
        }
        FunctionTemplate functionHandler = template.getFunctionHandler();
        DynamicObject instance;
        if (functionHandler == null) {
            instance = JSUserObject.create(jsRealm);
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
        DynamicObject handler = JSUserObject.create(realm);
        DynamicObject proxy = JSProxy.create(context, target, handler);

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

        DynamicObject enumerator = functionFromRootNode(context, realm, new ExecuteNativePropertyHandlerNode(
                        this,
                        context,
                        template,
                        proxy,
                        ExecuteNativePropertyHandlerNode.Mode.ENUMERATOR), proxy);
        JSObject.set(handler, JSProxy.ENUMERATE, enumerator);

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
                proxy.define(name, value.getValue());
            } // else set on target (in objectTemplateInstantiate) already
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
            if (name instanceof HiddenKey) {
                if (!template.hasPropertyHandler()) {
                    obj.define(name, processedValue);
                } // else set on the proxy/handler
            } else if (JSObject.hasOwnProperty(obj, name)) {
                JSObject.set(obj, name, processedValue);
            } else {
                JSObjectUtil.putDataProperty(obj, name, processedValue, attributes);
            }
        }
    }

    public void objectTemplateSetAccessor(Object templateObj, Object name, long getterPtr, long setterPtr, Object data, Object signature, int attributes) {
        ObjectTemplate template = (ObjectTemplate) templateObj;
        template.addAccessor(new Accessor(this, name, getterPtr, setterPtr, data, (FunctionTemplate) signature, propertyAttributes(attributes)));
    }

    public void objectTemplateSetNamedPropertyHandler(Object templateObj,
                    long getter, long setter, long query, long deleter, long enumerator, Object data) {
        ObjectTemplate template = (ObjectTemplate) templateObj;
        template.setNamedPropertyHandler(new PropertyHandler(
                        getter, setter, query, deleter, enumerator, data), true);
    }

    public void objectTemplateSetHandler(Object templateObj, long getter, long setter, long query, long deleter, long enumerator, Object data, boolean named, boolean stringKeysOnly) {
        ObjectTemplate template = (ObjectTemplate) templateObj;
        PropertyHandler handler = new PropertyHandler(getter, setter, query, deleter, enumerator, data);
        if (named) {
            template.setNamedPropertyHandler(handler, stringKeysOnly);
        } else {
            template.setIndexedPropertyHandler(handler);
        }
    }

    public void objectTemplateSetCallAsFunctionHandler(Object templateObj, int id, long functionPointer, Object additionalData) {
        ObjectTemplate template = (ObjectTemplate) templateObj;
        FunctionTemplate functionHandler = (FunctionTemplate) functionTemplateNew(id, functionPointer, additionalData, null);
        template.setFunctionHandler(functionHandler);
    }

    public Object scriptCompile(Object context, Object sourceCode, Object fileName) {
        UnboundScript unboundScript = (UnboundScript) unboundScriptCompile(sourceCode, fileName);
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
        Object prev = realm.getTruffleContext().enter();
        try {
            Object result;
            if (boundScript.isGraalInternal()) {
                result = scriptRunInternal(scriptNode, arguments);
            } else {
                result = scriptNode.run(arguments);
            }
            return result;
        } finally {
            realm.getTruffleContext().leave(prev);
        }
    }

    private Object scriptRunInternal(ScriptNode scriptNode, Object[] arguments) {
        // Internal scripts are allowed to access implementation classes
        JSContext context = scriptNode.getContext();
        JSRealm realm = context.getRealm();
        final DynamicObject moduleFunction = (DynamicObject) scriptNode.run(arguments);

        JavaScriptRootNode wrapperNode = new JavaScriptRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                DynamicObject thisObject = (DynamicObject) JSArguments.getThisObject(args);
                Object result = JSFunction.call(moduleFunction, thisObject, getInternalModuleUserArguments(args, scriptNode));
                return result;
            }
        };
        JSFunctionData functionData = JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(wrapperNode), 5, "");
        return JSFunction.create(realm, functionData);
    }

    private Object[] getInternalModuleUserArguments(Object[] args, ScriptNode node) {
        Object[] userArgs = JSArguments.extractUserArguments(args);
        String moduleName = node.getRootNode().getSourceSection().getSource().getName();
        if (USE_NIO_BUFFER && NIO_BUFFER_MODULE_NAME.equals(moduleName)) {
            // NIO-based buffer APIs in internal/graal/buffer.js are initialized by passing one
            // extra argument to the module loading function.
            Object[] extendedArgs = new Object[userArgs.length + 1];
            System.arraycopy(userArgs, 0, extendedArgs, 0, userArgs.length);
            extendedArgs[userArgs.length] = NIOBufferObject.createInitFunction(node);
            return extendedArgs;
        } else {
            return userArgs;
        }
    }

    public Object scriptGetUnboundScript(Object script) {
        return new UnboundScript((Script) script);
    }

    private static FunctionNode parseSource(Source source, JSContext context) {
        ContextData contextData = (ContextData) context.getEmbedderData();
        String content = source.getCharacters().toString();
        FunctionNode parseResult = contextData.getFunctionNodeCache().get(content);
        if (parseResult == null) {
            parseResult = GraalJSParserHelper.parseScript(source, (GraalJSParserOptions) context.getParserOptions());
            contextData.getFunctionNodeCache().put(content, parseResult);
        }
        return parseResult;
    }

    public Object unboundScriptCompile(Object sourceCode, Object fileName) {
        String sourceCodeStr = (String) sourceCode;
        String fileNameStr = (String) fileName;
        Source source = UnboundScript.createSource(internSourceCode(sourceCodeStr), fileNameStr);

        if (USE_SNAPSHOTS && fileNameStr != null && UnboundScript.isCoreModule(fileNameStr)) {
            // bootstrap_node.js is located in the internal folder,
            // but is loaded as bootstrap_node.js
            String modulePath = fileNameStr.equals("bootstrap_node.js") ? "internal/bootstrap_node.js" : fileNameStr;
            ByteBuffer snapshotBinary = NativeAccess.getCoreModuleBinarySnapshot(modulePath);
            if (snapshotBinary != null) {
                if (VERBOSE) {
                    System.out.printf("successfully read snapshot for %s (%d bytes, %d source chars)\n", fileNameStr, snapshotBinary.limit(), sourceCodeStr.length());
                }
                return new UnboundScript(source, snapshotBinary);
            } else {
                if (VERBOSE) {
                    System.out.printf("no snapshot for %s\n", fileNameStr);
                }
            }
        }

        // Needed to generate potential syntax errors, see node --check
        FunctionNode functionNode = parseSource(source, mainJSContext);

        return new UnboundScript(source, functionNode);
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
                GraalJSParserOptions options = ((GraalJSParserOptions) jsContext.getParserOptions());
                NodeFactory factory = NodeFactory.getInstance(jsContext);
                Object prev = jsRealm.getTruffleContext().enter();
                try {
                    scriptNode = JavaScriptTranslator.translateFunction(factory, jsContext, null, source, options.isStrict(), (FunctionNode) parseResult);
                } finally {
                    jsRealm.getTruffleContext().leave(prev);
                }
                if (!"repl".equals(source.getName())) {
                    contextData.getScriptNodeCache().put(source, scriptNode);
                }
            }
        } else {
            scriptNode = parseScriptNodeFromSnapshot(jsContext, source, (ByteBuffer) parseResult);
        }
        return new Script(scriptNode, parseResult, jsRealm, unboundScript.getId());
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

    private ScriptNode parseScriptNodeFromSnapshot(JSContext context, Source source, ByteBuffer snapshotBinary) {
        JSParser parser = (JSParser) context.getEvaluator();
        try {
            return parser.parseScriptNode(context, source, snapshotBinary);
        } catch (IllegalArgumentException e) {
            if (VERBOSE) {
                String moduleName = source.getName();
                System.out.printf("error when parsing binary snapshot for %s: %s\n", moduleName, e);
                System.out.printf("falling back to parsing %s at runtime\n", moduleName);
            }
            return parser.parseScriptNode(context, source);
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
            function.define(HOLDER_KEY, holder);
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
        JSContext jsContext = jsRealm.getContext();
        if (!(throwable instanceof GraalJSException)) {
            isolateInternalErrorCheck(throwable);
            throwable = JSException.create(JSErrorType.Error, throwable.getMessage(), throwable, null);
        }
        GraalJSException truffleException = (GraalJSException) throwable;
        Object exceptionObject = truffleException.getErrorObjectEager(jsContext);
        if (JSRuntime.isObject(exceptionObject) && JSError.getException((DynamicObject) exceptionObject) == null) {
            ((DynamicObject) exceptionObject).define(JSError.EXCEPTION_PROPERTY_NAME, truffleException);
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

    public boolean tryCatchHasTerminated(Object exception) {
        return (exception instanceof GraalJSKillException);
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

    private static final Pattern SYNTAX_ERROR_PATTERN = Pattern.compile("(.+):(\\d+):(\\d+) ([^\n]+)\n([^\n]+)(?:\n(?:.|\n)*)?");
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
    private WeakCallback updateWeakCallback(DynamicObject object, long reference, long data, long callbackPointer, int type, HiddenKey key) {
        Map<Long, WeakCallback> map = (Map<Long, WeakCallback>) object.get(key);
        if (map == null) {
            map = new HashMap<>();
            object.define(key, map);
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
            NativeAccess.weakCallback(callback.callback, callback.data, callback.type);
        }
    }

    public void makeWeak(Object object, long reference, long data, long callbackPointer, int type) {
        if (object == null) {
            System.err.println("null object given to makeWeak!");
            return;
        }

        DynamicObject target;
        HiddenKey key;
        if (object instanceof JSRealm) {
            target = ((JSRealm) object).getGlobalObject();
            key = HIDDEN_WEAK_CALLBACK_CONTEXT;
        } else {
            target = (DynamicObject) object;
            key = HIDDEN_WEAK_CALLBACK;
        }

        updateWeakCallback(target, reference, data, callbackPointer, type, key);
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

        DynamicObject target;
        HiddenKey key;
        if (object instanceof JSRealm) {
            target = ((JSRealm) object).getGlobalObject();
            key = HIDDEN_WEAK_CALLBACK_CONTEXT;
        } else {
            target = (DynamicObject) object;
            key = HIDDEN_WEAK_CALLBACK;
        }

        WeakCallback callback = updateWeakCallback(target, reference, 0, 0, 0, key);
        pollWeakCallbackQueue(false);
        return callback.data;
    }

    public <T> T lookupInstrument(String instrumentId, Class<T> instrumentClass) {
        TruffleLanguage.Env env = envForInstruments;
        InstrumentInfo info = env.getInstruments().get(instrumentId);
        return env.lookup(info, instrumentClass);
    }

    private boolean createChildContext;

    public Object contextNew(Object templateObj) {
        JSRealm realm;
        JSContext context;
        if (createChildContext) {
            realm = mainJSContext.getRealm().createChildRealm();
            context = realm.getContext();
        } else {
            realm = mainJSContext.getRealm();
            context = mainJSContext;
            context.setEmbedderData(new ContextData(context));
            createChildContext = true;
        }
        realm.setEmbedderData(new RealmData());
        DynamicObject global = realm.getGlobalObject();
        // Node.js does not have global arguments and load properties
        global.delete(JSRealm.ARGUMENTS_NAME);
        global.delete("load");
        if (exposeGC) {
            contextExposeGC(realm);
        }
        if (templateObj != null) {
            ObjectTemplate template = (ObjectTemplate) templateObj;
            if (template.hasPropertyHandler()) {
                global = propertyHandlerInstantiate(context, realm, template, global, true);
                realm.setGlobalObject(global);
            } else {
                DynamicObject prototype = JSUserObject.create(context);
                objectTemplateInstantiate(realm, template, prototype);
                JSObject.setPrototype(global, prototype);
            }
        }
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
        try {
            boolean seenJob;
            do {
                seenJob = false;
                if (mainJSContext.processAllPendingPromiseJobs()) {
                    // the queue processed at least one job. We continue processing.
                    seenJob = true;
                }
            } while (seenJob); // some job may trigger a job in a context processed already
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Object isolateCreateInternalFieldCountKey() {
        return INTERNAL_FIELD_COUNT_KEY;
    }

    public Object isolateCreateInternalFieldKey(int index) {
        return (index == 0) ? INTERNAL_FIELD_ZERO_KEY : new HiddenKey("InternalField" + index);
    }

    public int objectInternalFieldCount(Object target) {
        Object ret = ((DynamicObject) target).get(INTERNAL_FIELD_COUNT_KEY);
        if (ret instanceof Integer) {
            return (int) ret;
        } else if (ret instanceof Double) {
            return ((Double) ret).intValue();
        } else {
            return 0;
        }
    }

    public long objectSlowGetAlignedPointerFromInternalField(Object target) {
        Object pointer = ((DynamicObject) target).get(INTERNAL_FIELD_ZERO_KEY);
        return (pointer == null) ? 0 : ((Number) pointer).longValue();
    }

    public void objectSetAlignedPointerInInternalField(Object target, long value) {
        ((DynamicObject) target).define(INTERNAL_FIELD_ZERO_KEY, value);
    }

    public void isolateInternalErrorCheck(Object exception) {
        boolean internalError = !(exception instanceof GraalJSException) && !(exception instanceof StackOverflowError) && !(exception instanceof OutOfMemoryError) &&
                        !(exception instanceof ControlFlowException) && !(exception instanceof ExitException) && !(exception instanceof GraalJSKillException);
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
    }

    public synchronized void isolateTerminateExecution() {
        if (terminateExecution) {
            return; // termination in progress already
        }
        terminateExecution = true;
        Debugger debugger = lookupInstrument("debugger", Debugger.class);
        if (debugger == null) {
            System.err.println("Debugger is not available!");
            return;
        }
        debugger.startSession(new SuspendedCallback() {
            @Override
            public void onSuspend(SuspendedEvent se) {
                synchronized (GraalJSAccess.this) {
                    if (!terminateExecution) {
                        return; // termination has been cancelled
                    }
                }
                se.getSession().close();
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

    public Object isolateGetLargeIntPlaceholder() {
        return LARGE_INT_PLACEHOLDER;
    }

    public Object isolateGetDoublePlaceholder() {
        return DOUBLE_PLACEHOLDER;
    }

    public void isolateDispose(boolean exit, int status) {
        if (exit) {
            exit(status);
        }
    }

    private void initDebugObject(JSRealm realm) {
        JSContext context = realm.getContext();
        DynamicObject debug = JSUserObject.create(context);
        DynamicObject global = realm.getGlobalObject();

        CallTarget setBreakPointCallTarget = Truffle.getRuntime().createCallTarget(new SetBreakPointNode(this));
        JSFunctionData setBreakPointData = JSFunctionData.createCallOnly(context, setBreakPointCallTarget, 3, SetBreakPointNode.NAME);
        DynamicObject setBreakPoint = JSFunction.create(realm, setBreakPointData);
        JSObject.set(debug, SetBreakPointNode.NAME, setBreakPoint);

        CallTarget promiseStatusCallTarget = Truffle.getRuntime().createCallTarget(new PromiseStatusNode());
        JSFunctionData promiseStatusData = JSFunctionData.createCallOnly(context, promiseStatusCallTarget, 0, PromiseStatusNode.NAME);

        CallTarget promiseValueCallTarget = Truffle.getRuntime().createCallTarget(new PromiseValueNode(context));
        JSFunctionData promiseValueData = JSFunctionData.createCallOnly(context, promiseValueCallTarget, 0, PromiseValueNode.NAME);

        CallTarget iteratorPreviewCallTarget = Truffle.getRuntime().createCallTarget(new IteratorPreviewNode(context));
        JSFunctionData iteratorPreviewData = JSFunctionData.createCallOnly(context, iteratorPreviewCallTarget, 0, IteratorPreviewNode.NAME);

        CallTarget makeMirrorCallTarget = Truffle.getRuntime().createCallTarget(new MakeMirrorNode(context, promiseStatusData, promiseValueData, iteratorPreviewData));
        JSFunctionData makeMirrorData = JSFunctionData.createCallOnly(context, makeMirrorCallTarget, 2, MakeMirrorNode.NAME);
        DynamicObject makeMirror = JSFunction.create(realm, makeMirrorData);
        JSObject.set(debug, MakeMirrorNode.NAME, makeMirror);

        JSObject.set(global, "Debug", debug);
    }

    public Object isolateGetDebugContext() {
        if (debugRealm == null) {
            JSRealm newDebugRealm = mainJSContext.getRealm().createChildRealm();
            newDebugRealm.setEmbedderData(new RealmData());
            initDebugObject(newDebugRealm);
            debugRealm = newDebugRealm;
        }
        return debugRealm;
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
            public void promiseRejected(DynamicObject promise) {
                NativeAccess.notifyPromiseRejectionTracker(
                                promise,
                                0, // v8::PromiseRejectEvent::kPromiseRejectWithNoHandler
                                promiseResult(promise));
            }

            @Override
            public void promiseRejectionHandled(DynamicObject promise) {
                NativeAccess.notifyPromiseRejectionTracker(
                                promise,
                                1, // v8::PromiseRejectEvent::kPromiseHandlerAddedAfterReject
                                promiseResult(promise));
            }
        } : null;
        mainJSContext.setPromiseRejectionTracker(tracker);
    }

    private void exit(int status) {
        try {
            evaluator.close();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            System.exit(status);
        }
    }

    public void isolateEnterPolyglotEngine(long callback, long isolate, long param1, long param2, int argc, long argv, int execArgc, long execArgv) {
        org.graalvm.polyglot.Source source = org.graalvm.polyglot.Source.newBuilder(JavaScriptLanguage.ID, "(function(r) { r.run(); })", "polyglotEngineWrapper").internal(true).buildLiteral();
        org.graalvm.polyglot.Value wrapper = evaluator.eval(source);
        wrapper.execute(new RunnableInvoker(new Runnable() {
            @Override
            public void run() {
                NativeAccess.polyglotEngineEntered(callback, isolate, param1, param2, argc, argv, execArgc, execArgv);
            }
        }));
    }

    public Object correctReturnValue(Object value) {
        if (value == INT_PLACEHOLDER) {
            resetSharedBuffer();
            return getSharedBuffer().getInt();
        } else if (value == LARGE_INT_PLACEHOLDER) {
            resetSharedBuffer();
            return LargeInteger.valueOf(getSharedBuffer().getLong());
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
        return JSBoolean.create(((JSRealm) context).getContext(), value);
    }

    public boolean booleanObjectValueOf(Object object) {
        return JSBoolean.valueOf((DynamicObject) object);
    }

    public Object stringObjectNew(Object context, Object value) {
        return JSString.create(((JSRealm) context).getContext(), (String) value);
    }

    public String stringObjectValueOf(Object object) {
        return JSString.getString((DynamicObject) object);
    }

    public Object numberObjectNew(Object context, double value) {
        return JSNumber.create(((JSRealm) context).getContext(), value);
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
        JSContext jsContext = ((JSRealm) context).getContext();
        TruffleObject compiledRegexp = RegexCompilerInterface.compile((String) pattern, regexpFlagsToString(flags), jsContext);
        return JSRegExp.create(jsContext, compiledRegexp);
    }

    @TruffleBoundary
    public String regexpGetSource(Object regexp) {
        TruffleObject compiledRegex = JSRegExp.getCompiledRegex((DynamicObject) regexp);
        return STATIC_COMPILED_REGEX_ACCESSOR.pattern(compiledRegex);
    }

    @TruffleBoundary
    public int regexpGetFlags(Object regexp) {
        TruffleObject compiledRegex = JSRegExp.getCompiledRegex((DynamicObject) regexp);
        TruffleObject flagsObj = STATIC_COMPILED_REGEX_ACCESSOR.flags(compiledRegex);

        int v8Flags = 0; // v8::RegExp::Flags::kNone
        if (STATIC_FLAGS_ACCESSOR.global(flagsObj)) {
            v8Flags |= 1; // v8::RegExp::Flags::kGlobal
        }
        if (STATIC_FLAGS_ACCESSOR.ignoreCase(flagsObj)) {
            v8Flags |= 2; // v8::RegExp::Flags::kIgnoreCase
        }
        if (STATIC_FLAGS_ACCESSOR.multiline(flagsObj)) {
            v8Flags |= 4; // v8::RegExp::Flags::kMultiline
        }
        if (STATIC_FLAGS_ACCESSOR.sticky(flagsObj)) {
            v8Flags |= 8; // v8::RegExp::Flags::kSticky
        }
        if (STATIC_FLAGS_ACCESSOR.unicode(flagsObj)) {
            v8Flags |= 16; // v8::RegExp::Flags::kUnicode
        }
        return v8Flags;
    }

    public Object[] findDynamicObjectFields(Object context) {
        if (!JSTruffleOptions.SubstrateVM) {
            Object arrayBuffer = arrayBufferNew(context, 4);
            Object typedArray = uint8ArrayNew(arrayBuffer, 2, 1);

            String byteBuffer = findObjectFieldName(arrayBuffer, JSArrayBuffer.getDirectByteBuffer((DynamicObject) arrayBuffer));
            String buffer = findObjectFieldName(typedArray, arrayBuffer);

            return new Object[]{arrayBuffer.getClass(), byteBuffer, typedArray.getClass(), buffer};
        }
        return null;
    }

    private static String findObjectFieldName(Object object, Object search) {
        if (!JSTruffleOptions.SubstrateVM) {
            Field[] declaredFields = object.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.getType() != Object.class) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    if (field.get(object) == search) {
                        return field.getName();
                    }
                } catch (IllegalAccessException e) {
                    break;
                }
            }
        }
        return null;
    }

    public Object jsonParse(Object context, Object string) {
        return GraalJSParserHelper.parseJSON((String) string, ((JSRealm) context).getContext());
    }

    public String jsonStringify(Object context, Object object, String gap) {
        DynamicObject stringify = ((JSRealm) context).lookupFunction(JSON.CLASS_NAME, "stringify");
        return (String) JSFunction.call(stringify, Undefined.instance, new Object[]{
                        object,
                        Undefined.instance, // replacer
                        (gap == null) ? Undefined.instance : gap
        });
    }

    public Object promiseResult(Object promise) {
        return ((DynamicObject) promise).get(JSPromise.PROMISE_RESULT);
    }

    public int promiseState(Object promise) {
        Object state = ((DynamicObject) promise).get(JSPromise.PROMISE_STATE);
        return ((Number) state).intValue();
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
        ScriptNode scriptNode = parser.parseScriptNode(context, code);
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

    public Object moduleCompile(Object context, Object sourceCode, Object name) {
        JSContext jsContext = ((JSRealm) context).getContext();
        NodeFactory factory = NodeFactory.getInstance(jsContext);
        String moduleName = (String) name;
        URI uri = URI.create(moduleName);
        Source source = Source.newBuilder(moduleName).content((String) sourceCode).uri(uri).name(moduleName).language(AbstractJavaScriptLanguage.ID).build();
        return JavaScriptTranslator.translateModule(factory, jsContext, source, getModuleLoader());
    }

    public void moduleInstantiate(Object context, Object module, long resolveCallback) {
        ESModuleLoader loader = getModuleLoader();
        loader.setResolver(resolveCallback);
        JSContext jsContext = ((JSRealm) context).getContext();
        jsContext.getEvaluator().moduleDeclarationInstantiation((JSModuleRecord) module);
        loader.setResolver(0);
    }

    public Object moduleEvaluate(Object context, Object module) {
        JSRealm jsRealm = (JSRealm) context;
        JSContext jsContext = jsRealm.getContext();
        JSModuleRecord moduleRecord = (JSModuleRecord) module;
        Object result = jsContext.getEvaluator().moduleEvaluation(jsRealm, moduleRecord);
        return result;
    }

    public int moduleGetStatus(Object module) {
        JSModuleRecord record = (JSModuleRecord) module;
        if (record.isResolved()) {
            if (record.isEvaluated()) {
                return 4; // v8::Module::Status::kEvaluated
            } else {
                return 2; // v8::Module::Status::kInstantiated
            }
        } else {
            return 0; // v8::Module::Status::kUninstantiated
        }
    }

    public int moduleGetRequestsLength(Object module) {
        JSModuleRecord record = (JSModuleRecord) module;
        return ((Module) record.getModule()).getRequestedModules().size();
    }

    public String moduleGetRequest(Object module, int index) {
        JSModuleRecord record = (JSModuleRecord) module;
        return ((Module) record.getModule()).getRequestedModules().get(index);
    }

    public Object moduleGetNamespace(Object module) {
        JSModuleRecord record = (JSModuleRecord) module;
        GraalJSEvaluator graalEvaluator = (GraalJSEvaluator) record.getContext().getEvaluator();
        return graalEvaluator.getModuleNamespace(record);
    }

    public int moduleGetIdentityHash(Object module) {
        return System.identityHashCode(module);
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
        private final Map<JSModuleRecord, Map<String, JSModuleRecord>> cache = new HashMap<>();
        private long resolver;

        void setResolver(long resolver) {
            this.resolver = resolver;
        }

        @Override
        public JSModuleRecord resolveImportedModule(JSModuleRecord referrer, String specifier) {
            Map<String, JSModuleRecord> referrerCache = cache.get(referrer);
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
            JSModuleRecord result = (JSModuleRecord) NativeAccess.executeResolveCallback(resolver, referrer.getContext().getRealm(), specifier, referrer);
            referrerCache.put(specifier, result);
            return result;
        }

        @Override
        public JSModuleRecord loadModule(Source moduleSource) {
            throw new UnsupportedOperationException();
        }
    }

}
