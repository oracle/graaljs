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
package com.oracle.truffle.js.builtins;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugArrayTypeNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugAssertIntNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugClassNameNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugClassNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugCompileFunctionNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugContinueInInterpreterNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugCreateLazyStringNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugCreateSafeIntegerNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugDumpCountersNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugDumpFunctionTreeNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugHeapDumpNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugIsHolesArrayNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugJSStackNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugLoadModuleNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugNeverPartOfCompilationNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugPrintObjectNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugPrintSourceAttributionNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugShapeNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugStringCompareNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugSystemPropertiesNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugSystemPropertyNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugToJavaStringNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugTypedArrayDetachBufferNodeGen;
import com.oracle.truffle.js.builtins.helper.GCNodeGen;
import com.oracle.truffle.js.builtins.helper.HeapDump;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSGlobal;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@code Debug} object.
 */
public final class DebugBuiltins extends JSBuiltinsContainer.SwitchEnum<DebugBuiltins.Debug> {

    public static final JSBuiltinsContainer BUILTINS = new DebugBuiltins();

    protected DebugBuiltins() {
        super(JSRealm.DEBUG_CLASS_NAME, Debug.class);
    }

    public enum Debug implements BuiltinEnum<Debug> {
        class_(1),
        getClass(1),
        className(1),
        shape(1),
        dumpCounters(0),
        dumpFunctionTree(1),
        compileFunction(2),
        printObject(1),
        toJavaString(1),
        srcattr(1),
        arraytype(1),
        assertInt(2),
        continueInInterpreter(0),
        stringCompare(2),
        isHolesArray(1),
        jsStack(0),
        loadModule(2),
        createSafeInteger(1),
        createLazyString(2),
        typedArrayDetachBuffer(1),
        systemGC(0),
        systemProperty(1),
        systemProperties(0),
        neverPartOfCompilation(0),
        dumpHeap(2);

        private final int length;

        Debug(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Debug builtinEnum) {
        switch (builtinEnum) {
            case class_:
                return DebugClassNodeGen.create(context, builtin, true, args().fixedArgs(1).createArgumentNodes(context));
            case getClass:
                return DebugClassNodeGen.create(context, builtin, false, args().fixedArgs(1).createArgumentNodes(context));
            case className:
                return DebugClassNameNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case shape:
                return DebugShapeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case dumpCounters:
                return DebugDumpCountersNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case dumpFunctionTree:
                return DebugDumpFunctionTreeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case compileFunction:
                return DebugCompileFunctionNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case printObject:
                return DebugPrintObjectNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case toJavaString:
                return DebugToJavaStringNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case srcattr:
                return DebugPrintSourceAttributionNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case arraytype:
                return DebugArrayTypeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case assertInt:
                return DebugAssertIntNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case continueInInterpreter:
                return DebugContinueInInterpreterNodeGen.create(context, builtin, false, args().createArgumentNodes(context));
            case stringCompare:
                return DebugStringCompareNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case isHolesArray:
                return DebugIsHolesArrayNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case jsStack:
                return DebugJSStackNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case loadModule:
                return DebugLoadModuleNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));

            case systemGC:
                return GCNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case systemProperty:
                return DebugSystemPropertyNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case systemProperties:
                return DebugSystemPropertiesNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case neverPartOfCompilation:
                return DebugNeverPartOfCompilationNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case typedArrayDetachBuffer:
                return DebugTypedArrayDetachBufferNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));

            case createSafeInteger:
                return DebugCreateSafeIntegerNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case createLazyString:
                return DebugCreateLazyStringNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));

            case dumpHeap:
                return DebugHeapDumpNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class DebugContinueInInterpreter extends JSBuiltinNode {
        private final boolean invalidate;

        public DebugContinueInInterpreter(JSContext context, JSBuiltin builtin, boolean invalidate) {
            super(context, builtin);
            this.invalidate = invalidate;
        }

        @Specialization
        protected Object continueInInterpreter() {
            if (invalidate) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
            } else {
                CompilerDirectives.transferToInterpreter();
            }
            return Undefined.instance;
        }
    }

    public abstract static class DebugClassNode extends JSBuiltinNode {
        private final boolean getName;

        public DebugClassNode(JSContext context, JSBuiltin builtin, boolean getName) {
            super(context, builtin);
            this.getName = getName;
        }

        @Specialization
        protected Object clazz(Object obj) {
            return obj == null ? Null.instance : getName ? obj.getClass().getName() : wrap(obj.getClass());
        }

        private Object wrap(Class<? extends Object> class1) {
            return getContext().getRealm().getEnv().asGuestValue(class1);
        }
    }

    public abstract static class DebugClassNameNode extends JSBuiltinNode {
        public DebugClassNameNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected static Object clazz(Object obj) {
            if (obj instanceof Symbol) {
                return Null.instance;
            } else if (JSDynamicObject.isJSDynamicObject(obj)) {
                DynamicObject jsObj = (DynamicObject) obj;
                if (JSObjectUtil.hasHiddenProperty(jsObj, JSRuntime.ITERATED_OBJECT_ID)) {
                    DynamicObject iteratedObj = (DynamicObject) JSObjectUtil.getHiddenProperty(jsObj, JSRuntime.ITERATED_OBJECT_ID);
                    return JSObject.getClassName(iteratedObj) + " Iterator";
                } else if (JSObjectUtil.hasHiddenProperty(jsObj, JSFunction.GENERATOR_STATE_ID)) {
                    return "Generator";
                } else if (JSObjectUtil.hasHiddenProperty(jsObj, JSFunction.ASYNC_GENERATOR_STATE_ID)) {
                    return "Async Generator";
                } else if (JSProxy.isJSProxy(jsObj)) {
                    return clazz(JSProxy.getTarget(jsObj));
                }
                return JSObject.getClassName(jsObj);
            } else {
                return "not_an_object";
            }
        }
    }

    public abstract static class DebugShapeNode extends JSBuiltinNode {
        public DebugShapeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected static Object shape(Object obj) {
            if (obj instanceof DynamicObject) {
                return ((DynamicObject) obj).getShape().toString();
            }
            return Undefined.instance;
        }
    }

    public abstract static class DebugDumpCountersNode extends JSBuiltinNode {
        public DebugDumpCountersNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected static Object dumpCounters() {
            com.oracle.truffle.object.DebugCounter.dumpCounters();
            com.oracle.truffle.js.runtime.util.DebugCounter.dumpCounters();
            return Undefined.instance;
        }
    }

    public abstract static class DebugDumpFunctionTreeNode extends JSBuiltinNode {
        public DebugDumpFunctionTreeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization(guards = "isJSFunction(functionObj)")
        protected Object dumpFunctionTree(DynamicObject functionObj) {
            CallTarget target = JSFunction.getCallTarget(functionObj);
            if (target instanceof RootCallTarget) {
                NodeUtil.printTree(getContext().getRealm().getOutputWriter(), ((RootCallTarget) target).getRootNode());
                return Undefined.instance;
            } else {
                throw Errors.shouldNotReachHere();
            }
        }
    }

    public abstract static class DebugCompileFunctionNode extends JSBuiltinNode {
        private static final MethodHandle COMPILE_HANDLE;

        public DebugCompileFunctionNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected static boolean compileFunction(Object fnObj) {
            if (!JSFunction.isJSFunction(fnObj)) {
                throw Errors.createTypeErrorNotAFunction(fnObj);
            }

            CallTarget callTarget = JSFunction.getCallTarget((DynamicObject) fnObj);

            if (COMPILE_HANDLE != null) {
                try {
                    COMPILE_HANDLE.invokeExact(callTarget);
                    return true;
                } catch (Throwable e) {
                }
            }

            return false;
        }

        static {
            MethodHandle compileHandle = null;
            if (Truffle.getRuntime().getName().contains("Graal")) {
                try {
                    Class<? extends CallTarget> optimizedCallTargetClass = Class.forName("com.oracle.graal.truffle.OptimizedCallTarget").asSubclass(CallTarget.class);
                    compileHandle = MethodHandles.lookup().findVirtual(optimizedCallTargetClass, "compile", MethodType.methodType(void.class)).asType(
                                    MethodType.methodType(void.class, CallTarget.class));
                } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | SecurityException e) {
                }
            }
            COMPILE_HANDLE = compileHandle;
        }
    }

    public abstract static class DebugPrintObjectNode extends JSBuiltinNode {
        public DebugPrintObjectNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object printObject(DynamicObject object, Object level0) {
            int level = level0 == Undefined.instance ? 1 : JSRuntime.toInt32(level0);
            getContext().getRealm().getOutputWriter().println(debugPrint(object, 0, level));
            return Undefined.instance;
        }

        @TruffleBoundary
        protected String debugPrint(DynamicObject object, int level, int levelStop) {
            List<String> properties = JSObject.enumerableOwnNames(object);
            StringBuilder sb = new StringBuilder(properties.size() * 10);
            sb.append("{\n");
            for (String key : properties) {
                indent(sb, level + 1);
                PropertyDescriptor desc = JSObject.getOwnProperty(object, key);

                // must not invoke accessor functions here
                sb.append(key);
                if (desc.isDataDescriptor()) {
                    Object value = JSObject.get(object, key);
                    if (JSDynamicObject.isJSDynamicObject(value)) {
                        if ((JSGuards.isJSOrdinaryObject(value) || JSGlobal.isJSGlobalObject(value)) && !key.equals(JSObject.CONSTRUCTOR)) {
                            if (level < levelStop && !key.equals(JSObject.CONSTRUCTOR)) {
                                value = debugPrint((DynamicObject) value, level + 1, levelStop);
                            } else {
                                value = "{...}";
                            }
                        } else {
                            value = JSObject.getJSClass((DynamicObject) value);
                        }
                    }
                    sb.append(": ");
                    sb.append(value);
                }
                if (!key.equals(properties.get(properties.size() - 1))) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            indent(sb, level);
            sb.append('}');
            return sb.toString();
        }

        private static StringBuilder indent(StringBuilder sb, int level) {
            for (int i = 0; i < level; i++) {
                sb.append(' ');
            }
            return sb;
        }
    }

    public abstract static class DebugToJavaStringNode extends JSBuiltinNode {
        public DebugToJavaStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected static Object toJavaString(Object thing) {
            return String.valueOf(thing);
        }
    }

    public abstract static class DebugPrintSourceAttribution extends JSBuiltinNode {
        public DebugPrintSourceAttribution(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization(guards = "isJSFunction(function)")
        protected Object printSourceAttribution(DynamicObject function) {
            CallTarget callTarget = JSFunction.getCallTarget(function);
            if (callTarget instanceof RootCallTarget) {
                return NodeUtil.printSourceAttributionTree(((RootCallTarget) callTarget).getRootNode());
            }
            return Undefined.instance;
        }

        @TruffleBoundary
        @Specialization
        protected Object printSourceAttribution(String code) {
            ScriptNode scriptNode = getContext().getEvaluator().evalCompile(getContext(), code, "<eval>");
            return NodeUtil.printSourceAttributionTree(scriptNode.getRootNode());
        }
    }

    public abstract static class DebugArrayTypeNode extends JSBuiltinNode {
        public DebugArrayTypeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object arraytype(Object array) {
            if (!(JSDynamicObject.isJSDynamicObject(array)) || !(JSObject.hasArray(array))) {
                return "NOT_AN_ARRAY";
            }
            return JSObject.getArray((DynamicObject) array).getClass().getSimpleName();
        }
    }

    public abstract static class DebugAssertIntNode extends JSBuiltinNode {
        public DebugAssertIntNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object assertInt(Object value, Object message) {
            if (!(value instanceof Integer)) {
                throw Errors.createTypeError("assert: expected integer here, got " + value.getClass().getSimpleName() + ", message: " + JSRuntime.toString(message));
            }
            return Undefined.instance;
        }
    }

    public abstract static class DebugHeapDumpNode extends JSBuiltinNode {
        public DebugHeapDumpNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected String heapDump(Object fileName0, Object live0) {
            String fileName = fileName0 == Undefined.instance ? HeapDump.defaultDumpName() : JSRuntime.toString(fileName0);
            boolean live = live0 == Undefined.instance ? true : JSRuntime.toBoolean(live0);
            try {
                HeapDump.dump(fileName, live);
            } catch (IOException e) {
                throw JSException.create(JSErrorType.Error, e.getMessage(), e, this);
            } catch (IllegalArgumentException e) {
                throw JSException.create(JSErrorType.Error, getBuiltin().getFullName() + " unsupported", e, this);
            }

            return fileName;
        }
    }

    /**
     * Used by testV8!
     */
    public abstract static class DebugStringCompareNode extends JSBuiltinNode {

        public DebugStringCompareNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int stringCompare(Object a, Object b) {
            String str1 = JSRuntime.toString(a);
            String str2 = JSRuntime.toString(b);
            int result = str1.compareTo(str2);
            if (result == 0) {
                return 0;
            } else if (result < 0) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    /**
     * Exposes the "holes" property of arrays. Used e.g. by V8HasFastHoleyElements.
     */
    public abstract static class DebugIsHolesArrayNode extends JSBuiltinNode {
        @Child private JSToObjectNode toObjectNode;
        private final ValueProfile arrayType;
        private final ConditionProfile isArray;

        public DebugIsHolesArrayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.toObjectNode = JSToObjectNode.createToObject(getContext());
            this.arrayType = ValueProfile.createClassProfile();
            this.isArray = ConditionProfile.createBinaryProfile();
        }

        public abstract boolean executeBoolean(Object object);

        @Specialization
        protected boolean isHolesArray(Object arr) {
            Object obj = toObjectNode.execute(arr);
            if (isArray.profile(JSArray.isJSArray(obj))) {
                DynamicObject dynObj = (DynamicObject) obj;
                return arrayType.profile(JSObject.getArray(dynObj)).hasHoles(dynObj);
            } else {
                return false;
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return DebugIsHolesArrayNodeGen.create(getContext(), getBuiltin(), cloneUninitialized(getArguments(), materializedTags));
        }
    }

    /**
     * Prints the current JS stack.
     */
    public abstract static class DebugJSStackNode extends JSBuiltinNode {

        public DebugJSStackNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object printJSStack() {
            JSException.printJSStackTrace(getParent());
            return Undefined.instance;
        }
    }

    public abstract static class DebugLoadModuleNode extends JSBuiltinNode {

        public DebugLoadModuleNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected String loadModule(Object nameObj, Object modulesSourceMapObj) {
            String name = JSRuntime.toString(nameObj);
            DynamicObject modulesSourceMap = (DynamicObject) modulesSourceMapObj;
            JSContext context = getContext();
            Evaluator evaluator = context.getEvaluator();
            JSModuleLoader moduleLoader = new JSModuleLoader() {
                private final Map<String, JSModuleRecord> moduleMap = new HashMap<>();

                private Source resolveModuleSource(@SuppressWarnings("unused") ScriptOrModule referencingModule, String specifier) {
                    Object moduleEntry = JSObject.get(modulesSourceMap, specifier);
                    if (moduleEntry == Undefined.instance) {
                        throw Errors.createSyntaxError(String.format("Could not find imported module %s", specifier));
                    }
                    String code = JSRuntime.toString(moduleEntry);
                    return Source.newBuilder(JavaScriptLanguage.ID, code, name).build();
                }

                @Override
                public JSModuleRecord resolveImportedModule(ScriptOrModule referencingModule, String specifier) {
                    return moduleMap.computeIfAbsent(specifier, (key) -> evaluator.parseModule(context, resolveModuleSource(referencingModule, key), this));
                }

                @Override
                public JSModuleRecord loadModule(Source moduleSource) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public JSModuleRecord resolveImportedModuleBlock(Source source, DynamicObject specifier) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public JSModuleRecord resolveImportedModuleBlock(JSModuleRecord moduleBlock, DynamicObject specifier) {
                    throw new UnsupportedOperationException();
                }
            };
            JSModuleRecord module = moduleLoader.resolveImportedModule(null, name);
            JSRealm realm = context.getRealm();
            evaluator.moduleInstantiation(realm, module);
            evaluator.moduleEvaluation(realm, module);
            return String.valueOf(module);
        }
    }

    public abstract static class DebugSystemProperties extends JSBuiltinNode {

        public DebugSystemProperties(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object systemProperties() {
            DynamicObject result = JSOrdinary.create(getContext());
            for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key instanceof String && value instanceof String) {
                    JSObject.set(result, key, value);
                }
            }
            return result;
        }
    }

    public abstract static class DebugSystemProperty extends JSBuiltinNode {

        public DebugSystemProperty(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected static Object systemProperty(Object name) {
            String key = JSRuntime.toString(name);
            String value = System.getProperty(key);
            return (value == null) ? Undefined.instance : value;
        }
    }

    public abstract static class DebugTypedArrayDetachBufferNode extends JSBuiltinNode {
        public DebugTypedArrayDetachBufferNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected static Object detachBuffer(Object obj) {
            if (!(JSArrayBuffer.isJSHeapArrayBuffer(obj) || JSArrayBuffer.isJSDirectArrayBuffer(obj) || JSArrayBuffer.isJSInteropArrayBuffer(obj))) {
                throw Errors.createTypeError("ArrayBuffer expected");
            }
            JSArrayBuffer.detachArrayBuffer((DynamicObject) obj);
            return Undefined.instance;
        }
    }

    public abstract static class DebugCreateSafeInteger extends JSBuiltinNode {

        public DebugCreateSafeInteger(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static SafeInteger createSafeInteger(int a) {
            return SafeInteger.valueOf(a);
        }

        @Specialization
        protected static SafeInteger createSafeInteger(SafeInteger a) {
            return a;
        }

        @Specialization
        protected static SafeInteger createSafeInteger(Object a) {
            long integer = JSRuntime.toInteger(a);
            integer = Math.max(integer, JSRuntime.MIN_SAFE_INTEGER_LONG);
            integer = Math.min(integer, JSRuntime.MAX_SAFE_INTEGER_LONG);
            return SafeInteger.valueOf(integer);
        }
    }

    public abstract static class DebugCreateLazyString extends JSBuiltinNode {

        public DebugCreateLazyString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected CharSequence createLazyString(String left, String right) {
            CharSequence result = JSLazyString.create(left, right);
            if (!(result instanceof JSLazyString)) {
                throw Errors.createError(resultIsNotLazyStringMessage(left, right));
            }
            return result;
        }

        @TruffleBoundary
        private static String resultIsNotLazyStringMessage(String left, String right) {
            return "Concatenation of '" + left + "' and '" + right + "' does not produce JSLazyString.";
        }

        @Specialization
        protected CharSequence createLazyString(Object left, Object right) {
            return createLazyString(JSRuntime.toString(left), JSRuntime.toString(right));
        }

    }

    public abstract static class DebugNeverPartOfCompilationNode extends JSBuiltinNode implements JSBuiltinNode.Inlineable, JSBuiltinNode.Inlined {

        public DebugNeverPartOfCompilationNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static Object neverPartOfCompilation() {
            if (!CompilerDirectives.inCompilationRoot()) {
                fail();
            }
            return Undefined.instance;
        }

        protected static void fail() {
            CompilerDirectives.bailout("Debug.neverPartOfCompilation()");
        }

        @Override
        public Object callInlined(Object[] arguments) {
            fail();
            return Undefined.instance;
        }

        @Override
        public Inlined createInlined() {
            return DebugNeverPartOfCompilationNodeGen.create(getContext(), getBuiltin(), getArguments());
        }
    }
}
