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
package com.oracle.truffle.js.runtime;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class GraalJSException extends RuntimeException implements TruffleException {
    private static final long serialVersionUID = -6624166672101791072L;
    private static final JSStackTraceElement[] EMPTY_STACK_TRACE = new JSStackTraceElement[0];
    private JSStackTraceElement[] jsStackTrace;
    private Object location;
    private int stackTraceLimit;

    private static final String DYNAMIC_FUNCTION_NAME = "anonymous";

    protected GraalJSException(String message, Throwable cause, Node node, int stackTraceLimit) {
        super(message, cause);
        this.location = node;
        this.stackTraceLimit = stackTraceLimit;
        this.jsStackTrace = stackTraceLimit == 0 ? EMPTY_STACK_TRACE : null;
    }

    protected GraalJSException(String message, Node node, int stackTraceLimit) {
        super(message);
        this.location = node;
        this.stackTraceLimit = stackTraceLimit;
        this.jsStackTrace = stackTraceLimit == 0 ? EMPTY_STACK_TRACE : null;
    }

    protected GraalJSException(String message, SourceSection location, int stackTraceLimit) {
        super(message);
        this.location = location;
        this.stackTraceLimit = stackTraceLimit;
        this.jsStackTrace = stackTraceLimit == 0 ? EMPTY_STACK_TRACE : null;
    }

    protected static <T extends GraalJSException> T fillInStackTrace(T exception, DynamicObject skipFramesUpTo, boolean capture) {
        exception.fillInStackTrace(skipFramesUpTo, capture);
        return exception;
    }

    protected final GraalJSException fillInStackTrace(DynamicObject skipFramesUpTo, boolean capture) {
        // We can only skip frames when capturing eagerly.
        assert capture || skipFramesUpTo == Undefined.instance;
        assert jsStackTrace == (stackTraceLimit == 0 ? EMPTY_STACK_TRACE : null);
        if (capture || JSTruffleOptions.EagerStackTrace) {
            if (stackTraceLimit > 0) {
                this.jsStackTrace = getJSStackTrace(skipFramesUpTo);
            }
        }
        return this;
    }

    @Override
    public Node getLocation() {
        return location instanceof Node ? (Node) location : null;
    }

    @Override
    public SourceSection getSourceLocation() {
        if (location instanceof SourceSection) {
            return (SourceSection) location;
        }
        return TruffleException.super.getSourceLocation();
    }

    @Override
    public Object getExceptionObject() {
        Object error = getErrorObject();
        return (error == null) ? null : JSRuntime.exportValue(error);
    }

    @Override
    public int getStackTraceElementLimit() {
        if (stackTraceLimit <= 0) {
            return 0;
        }
        // since we might skip stack frames, we do not know in advance how many we have to visit.
        return -1;
    }

    /** Could still be null due to lazy initialization. */
    public abstract Object getErrorObject();

    /**
     * Eager access to the ErrorObject. Use only if you must get a non-null error object. Could
     * result in an error object from the wrong realm, thus non spec-compliant.
     */
    public abstract Object getErrorObjectEager(JSContext context);

    /**
     * Omit creating stack trace for JavaScript exceptions.
     */
    @SuppressWarnings("sync-override")
    @Override
    public final Throwable fillInStackTrace() {
        CompilerAsserts.neverPartOfCompilation("GraalJSException.fillInStackTrace");
        if (JSTruffleOptions.FillExceptionStack) {
            return super.fillInStackTrace();
        } else {
            return null;
        }
    }

    public JSStackTraceElement[] getJSStackTrace() {
        if (jsStackTrace != null) {
            return jsStackTrace;
        }
        return jsStackTrace = materializeJSStackTrace();
    }

    @TruffleBoundary
    private JSStackTraceElement[] materializeJSStackTrace() {
        return getJSStackTrace(Undefined.instance);
    }

    @TruffleBoundary
    private JSStackTraceElement[] getJSStackTrace(DynamicObject skipUpTo) {
        assert stackTraceLimit > 0;
        // Nashorn does not support skipping of frames
        DynamicObject skipFramesUpTo = JSTruffleOptions.NashornCompatibilityMode ? Undefined.instance : skipUpTo;
        List<TruffleStackTraceElement> stackTrace = TruffleStackTraceElement.getStackTrace(this);
        if (stackTrace == null) {
            return EMPTY_STACK_TRACE;
        }
        FrameVisitorImpl visitor = new FrameVisitorImpl(getLocation(), stackTraceLimit, skipFramesUpTo);
        for (TruffleStackTraceElement element : stackTrace) {
            if (!visitor.visitFrame(element)) {
                break;
            }
        }
        return visitor.getStackTrace().toArray(EMPTY_STACK_TRACE);
    }

    public void setJSStackTrace(JSStackTraceElement[] jsStackTrace) {
        this.jsStackTrace = jsStackTrace;
    }

    @TruffleBoundary
    public static JSStackTraceElement[] getJSStackTrace(Node originatingNode) {
        return UserScriptException.createCapture("", originatingNode, JSTruffleOptions.StackTraceLimit, Undefined.instance).getJSStackTrace();
    }

    private static final class FrameVisitorImpl {
        private static final int STACK_FRAME_SKIP = 0;
        private static final int STACK_FRAME_JS = 1;
        private static final int STACK_FRAME_FOREIGN = 2;

        private final List<JSStackTraceElement> stackTrace = new ArrayList<>();
        private final Node originatingNode;
        private final int stackTraceLimit;
        private final DynamicObject skipFramesUpTo;

        private boolean inStrictMode;
        private boolean skippingFrames;
        private boolean first = true;

        FrameVisitorImpl(Node originatingNode, int stackTraceLimit, DynamicObject skipFramesUpTo) {
            this.originatingNode = originatingNode;
            this.stackTraceLimit = stackTraceLimit;
            this.skipFramesUpTo = skipFramesUpTo;
            this.skippingFrames = (skipFramesUpTo != Undefined.instance);
        }

        private static int stackFrameType(Node callNode) {
            if (callNode == null) {
                return STACK_FRAME_SKIP;
            }
            SourceSection sourceSection = callNode.getEncapsulatingSourceSection();
            if (sourceSection == null) {
                return STACK_FRAME_SKIP;
            }
            if (JSFunction.isBuiltinSourceSection(sourceSection)) {
                return JSTruffleOptions.NashornCompatibilityMode ? STACK_FRAME_SKIP : STACK_FRAME_JS;
            }
            if (sourceSection.getSource().isInternal() || !sourceSection.isAvailable()) {
                return STACK_FRAME_SKIP;
            }
            if (JSRuntime.isJSRootNode(callNode.getRootNode())) {
                return STACK_FRAME_JS;
            } else {
                return STACK_FRAME_FOREIGN;
            }
        }

        public boolean visitFrame(TruffleStackTraceElement element) {
            Node callNode = element.getLocation();
            if (first) {
                first = false;
                callNode = originatingNode;
            }
            if (callNode == null) {
                CallTarget callTarget = element.getTarget();
                if (callTarget instanceof RootCallTarget) {
                    callNode = ((RootCallTarget) callTarget).getRootNode();
                }
            }
            switch (stackFrameType(callNode)) {
                case STACK_FRAME_JS: {
                    RootNode rootNode = callNode.getRootNode();
                    assert JSRuntime.isJSRootNode(rootNode);
                    final Object[] arguments;
                    if (JSRuntime.isJSFunctionRootNode(rootNode)) {
                        arguments = element.getFrame().getArguments();
                    } else if (((JavaScriptRootNode) rootNode).isResumption()) {
                        // first argument is the context frame
                        Frame frame = (Frame) element.getFrame().getArguments()[0];
                        arguments = frame.getArguments();
                    } else {
                        break;
                    }
                    Object thisObj = JSArguments.getThisObject(arguments);
                    Object functionObj = JSArguments.getFunctionObject(arguments);
                    if (JSFunction.isJSFunction(functionObj)) {
                        JSFunctionData functionData = JSFunction.getFunctionData((DynamicObject) functionObj);
                        if (functionData.isStrict()) {
                            inStrictMode = true;
                        } else if (functionData.isBuiltin() && JSFunction.isStrictBuiltin((DynamicObject) functionObj)) {
                            inStrictMode = true;
                        }
                        if (skippingFrames && functionObj == skipFramesUpTo) {
                            skippingFrames = false;
                            return true; // skip this frame as well
                        }
                        JSRealm realm = JSFunction.getRealm((DynamicObject) functionObj);
                        if (functionObj == realm.getApplyFunctionObject() || functionObj == realm.getCallFunctionObject()) {
                            return true; // skip Function.apply and Function.call
                        }
                        if (!skippingFrames) {
                            if (functionData.isAsync() && !functionData.isGenerator() && JSRuntime.isJSFunctionRootNode(rootNode)) {
                                // async function calls produce two frames, skip one
                                return true;
                            }
                            stackTrace.add(processJSFrame(rootNode, callNode, thisObj, (DynamicObject) functionObj, inStrictMode));
                        }
                    }
                    break;
                }
                case STACK_FRAME_FOREIGN:
                    if (!skippingFrames) {
                        JSStackTraceElement elem = processForeignFrame(callNode, inStrictMode);
                        if (elem != null) {
                            stackTrace.add(elem);
                        }
                    }
                    break;
            }
            return stackTrace.size() < stackTraceLimit;
        }

        public List<JSStackTraceElement> getStackTrace() {
            return stackTrace;
        }
    }

    private static JSStackTraceElement processJSFrame(RootNode rootNode, Node node, Object thisObj, DynamicObject functionObj, boolean inStrictMode) {
        Node callNode = node;
        while (callNode.getSourceSection() == null) {
            callNode = callNode.getParent();
        }
        SourceSection callNodeSourceSection = callNode.getSourceSection();
        Source source = callNodeSourceSection.getSource();

        String fileName = getFileName(source);
        String functionName;
        if (JSFunction.isBuiltin(functionObj)) {
            functionName = JSFunction.getName(functionObj);
        } else {
            functionName = rootNode.getName();
        }
        boolean eval = false;
        if (functionName == null || isInternalFunctionName(functionName)) {
            functionName = "";
        } else if (isEvalSource(source, functionName)) {
            functionName = "eval";
            eval = true;
        }
        SourceSection targetSourceSection = null;
        if (!JSTruffleOptions.NashornCompatibilityMode) { // for V8
            if (callNode instanceof JavaScriptFunctionCallNode) {
                Node target = ((JavaScriptFunctionCallNode) callNode).getTarget();
                targetSourceSection = target == null ? null : target.getSourceSection();
            }
        }
        boolean global = isGlobalObject(thisObj, JSFunction.getRealm(functionObj));

        return new JSStackTraceElement(fileName, functionName, callNodeSourceSection, thisObj, functionObj, targetSourceSection, inStrictMode, eval, global);
    }

    private static boolean isEvalSource(Source source, String functionName) {
        return (functionName.equals(DYNAMIC_FUNCTION_NAME) && source.getName().equals(Evaluator.FUNCTION_SOURCE_NAME)) ||
                        (functionName.equals(JSFunction.PROGRAM_FUNCTION_NAME) &&
                                        (source.getName().equals(Evaluator.EVAL_SOURCE_NAME) || source.getName().startsWith(Evaluator.EVAL_AT_SOURCE_NAME_PREFIX)));
    }

    private static boolean isInternalFunctionName(String functionName) {
        return functionName.length() >= 1 && functionName.charAt(0) == ':';
    }

    private static boolean isGlobalObject(Object object, JSRealm realm) {
        return JSObject.isJSObject(object) && (realm != null) && (realm.getGlobalObject() == object);
    }

    private static JSStackTraceElement processForeignFrame(Node node, boolean strict) {
        RootNode rootNode = node.getRootNode();
        SourceSection sourceSection = rootNode.getSourceSection();
        if (sourceSection == null) {
            // can happen around FastR root nodes, see GR-6604
            return null;
        }
        String fileName = getFileName(sourceSection.getSource());
        String functionName = rootNode.getName();
        Object thisObj = null;
        Object functionObj = null;

        return new JSStackTraceElement(fileName, functionName, sourceSection, thisObj, functionObj, null, strict, false, false);
    }

    private static String getPrimitiveConstructorName(Object thisObj) {
        assert JSRuntime.isJSPrimitive(thisObj);
        if (thisObj instanceof Boolean) {
            return "Boolean";
        } else if (JSRuntime.isNumber(thisObj)) {
            return "Number";
        } else if (JSRuntime.isString(thisObj)) {
            return "String";
        } else if (thisObj instanceof Symbol) {
            return "Symbol";
        }
        return null;
    }

    private static int correctColumnNumber(int columnNumber, SourceSection callNodeSourceSection, SourceSection targetSourceSection) {
        int correctNumber = columnNumber;
        String code = callNodeSourceSection.getCharacters().toString();

        // skip code for the target
        if (targetSourceSection != null) {
            String targetCode = targetSourceSection.getCharacters().toString();
            int index = code.indexOf(targetCode);
            if (index != -1) {
                index += targetCode.length();
                correctNumber += index;
                code = code.substring(index);
            }
        }

        // column number corresponds to the function invocation (left
        // parenthesis) unless it is preceded by an identifier (column
        // number is the beginning of the identified then)
        int index = code.indexOf('(');
        if (index != -1) {
            index--;
            int i = index;
            while (i >= 0 && Character.isWhitespace(code.charAt(i))) {
                i--;
            }
            if (i >= 0 && Character.isJavaIdentifierPart(code.charAt(i))) {
                do {
                    i--;
                } while (i >= 0 && Character.isJavaIdentifierPart(code.charAt(i)));
                index = i;
            }
            correctNumber += index + 1;
        }
        return correctNumber;
    }

    private static String getFileName(Source source) {
        return source != null ? source.getName() : "<unknown>";
    }

    public void printJSStackTrace() {
        System.err.println(getMessage());
        for (JSStackTraceElement jsste : jsStackTrace) {
            System.err.println(jsste);
        }
    }

    @TruffleBoundary
    public static void printJSStackTrace(Node originatingNode) {
        JSStackTraceElement[] jsstes = getJSStackTrace(originatingNode);
        for (JSStackTraceElement jsste : jsstes) {
            System.err.println(jsste);
        }
    }

    @TruffleBoundary
    private static StackTraceElement[] toStackTraceElements(JSStackTraceElement[] jsStack) {
        StackTraceElement[] ste = new StackTraceElement[jsStack.length];
        for (int i = 0; i < jsStack.length; i++) {
            JSStackTraceElement jsStackElement = jsStack[i];
            ste[i] = new StackTraceElement(JSError.correctMethodName(jsStackElement.getFunctionName()), "", jsStackElement.getFileName(), jsStackElement.getLineNumber());
        }
        return ste;
    }

    public static final class JSStackTraceElement {
        private final String fileName;
        private final String functionName;
        private final SourceSection sourceSection;
        private final Object thisObj;
        private final Object functionObj;
        private final SourceSection targetSourceSection;
        private final boolean strict;
        private final boolean eval;
        private final boolean global;

        private JSStackTraceElement(String fileName, String functionName, SourceSection sourceSection, Object thisObj, Object functionObj, SourceSection targetSourceSection, boolean strict,
                        boolean eval, boolean global) {
            CompilerAsserts.neverPartOfCompilation();
            this.fileName = fileName;
            this.functionName = functionName;
            this.sourceSection = sourceSection;
            this.thisObj = thisObj;
            this.functionObj = functionObj;
            this.targetSourceSection = targetSourceSection;
            this.strict = strict;
            this.eval = eval;
            this.global = global;
        }

        // This method is called from nashorn tests via java interop
        @TruffleBoundary
        public String getFileName() {
            if (fileName.startsWith(Evaluator.EVAL_AT_SOURCE_NAME_PREFIX)) {
                return Evaluator.EVAL_SOURCE_NAME;
            }
            return fileName;
        }

        // This method is called from nashorn tests via java interop
        public String getClassName() {
            return getTypeName(false);
        }

        public String getTypeName() {
            return getTypeName(true);
        }

        @TruffleBoundary
        public String getTypeName(boolean checkGlobal) {
            if (JSTruffleOptions.NashornCompatibilityMode) {
                return "<" + fileName + ">";
            } else {
                if (checkGlobal && global) {
                    return "global";
                }
                Object thisObject = getThis();
                if (!JSRuntime.isNullOrUndefined(thisObject) && !global) {
                    if (JSObject.isDynamicObject(thisObject)) {
                        return JSRuntime.getConstructorName((DynamicObject) thisObject);
                    } else if (JSRuntime.isJSPrimitive(thisObject)) {
                        return getPrimitiveConstructorName(thisObject);
                    }
                }
                return null;
            }
        }

        public String getFunctionName() {
            if (JSFunction.isJSFunction(functionObj)) {
                String dynamicName = findFunctionName((DynamicObject) functionObj);
                // The default name of dynamic functions is "anonymous" as per the spec.
                // Yet, in V8 stack traces it is "eval" unless overwritten.
                if (dynamicName != null && !dynamicName.isEmpty() &&
                                (!isEval() || !dynamicName.equals(DYNAMIC_FUNCTION_NAME) || !JSObject.getJSContext((DynamicObject) functionObj).isOptionV8CompatibilityMode())) {
                    return dynamicName;
                }
            }
            return functionName;
        }

        private static String findFunctionName(DynamicObject functionObj) {
            assert JSFunction.isJSFunction(functionObj);
            PropertyDescriptor desc = JSObject.getOwnProperty(functionObj, JSFunction.NAME);
            if (desc != null) {
                if (desc.isDataDescriptor()) {
                    Object name = desc.getValue();
                    if (JSRuntime.isString(name)) {
                        return JSRuntime.javaToString(name);
                    }
                }
            }
            return null;
        }

        // This method is called from nashorn tests via java interop
        @TruffleBoundary
        public String getMethodName() {
            if (JSTruffleOptions.NashornCompatibilityMode) {
                return JSError.correctMethodName(functionName);
            }
            if (JSRuntime.isNullOrUndefined(thisObj) || !JSObject.isJSObject(thisObj)) {
                return null;
            }
            if (!JSFunction.isJSFunction(functionObj)) {
                return null;
            }

            DynamicObject receiver = (DynamicObject) thisObj;
            DynamicObject function = (DynamicObject) functionObj;
            if (functionName != null && !functionName.isEmpty()) {
                String name = findMethodPropertyNameByFunctionName(receiver, functionName, function);
                if (name != null) {
                    return name;
                }
            }
            return findMethodPropertyName(receiver, function);
        }

        private static String findMethodPropertyNameByFunctionName(DynamicObject receiver, String functionName, DynamicObject functionObj) {
            String propertyName = functionName;
            boolean accessor = false;
            if (propertyName.startsWith("get ") || propertyName.startsWith("set ")) {
                propertyName = propertyName.substring(4);
                accessor = true;
            }
            if (propertyName.isEmpty()) {
                return null;
            }
            for (DynamicObject current = receiver; current != Null.instance && !JSProxy.isProxy(current); current = JSObject.getPrototype(current)) {
                PropertyDescriptor desc = JSObject.getOwnProperty(current, propertyName);
                if (desc != null) {
                    if (desc.isAccessorDescriptor() == accessor && (desc.getValue() == functionObj || desc.getGet() == functionObj || desc.getSet() == functionObj)) {
                        return propertyName;
                    }
                    break;
                }
            }
            return null;
        }

        private static String findMethodPropertyName(DynamicObject receiver, DynamicObject functionObj) {
            String name = null;
            for (DynamicObject current = receiver; current != Null.instance && !JSProxy.isProxy(current); current = JSObject.getPrototype(current)) {
                for (String key : JSObject.enumerableOwnNames(current)) {
                    PropertyDescriptor desc = JSObject.getOwnProperty(current, key);
                    if (desc.getValue() == functionObj || desc.getGet() == functionObj || desc.getSet() == functionObj) {
                        if (name == null) {
                            name = key;
                        } else {
                            return null; // method name is ambiguous
                        }
                    }
                }
            }
            return name;
        }

        // This method is called from nashorn tests via java interop
        @TruffleBoundary
        public int getLineNumber() {
            return sourceSection != null ? sourceSection.getStartLine() : -1;
        }

        @TruffleBoundary
        public String getLine() {
            int lineNumber = getLineNumber();
            if (sourceSection == null || sourceSection.getSource() == null || lineNumber <= 0) {
                return "<unknown>";
            }
            return sourceSection.getSource().getCharacters(lineNumber).toString();
        }

        @TruffleBoundary
        public int getColumnNumber() {
            if (sourceSection == null) {
                return -1;
            }
            int columnNumber = sourceSection.getStartColumn();
            if (!JSTruffleOptions.NashornCompatibilityMode && targetSourceSection != null) {
                // for V8
                columnNumber = correctColumnNumber(columnNumber, sourceSection, targetSourceSection);
            }
            return columnNumber;
        }

        public int getPosition() {
            return sourceSection != null ? sourceSection.getCharIndex() : -1;
        }

        public Object getThis() {
            return thisObj;
        }

        @TruffleBoundary
        public Object getThisOrGlobal() {
            if (thisObj == Undefined.instance && JSFunction.isJSFunction(functionObj) && !JSFunction.isStrict((DynamicObject) functionObj)) {
                return JSFunction.getRealm((DynamicObject) functionObj).getGlobalObject();
            }
            return thisObj;
        }

        public Object getFunction() {
            return functionObj;
        }

        public boolean isStrict() {
            return strict;
        }

        @TruffleBoundary
        public boolean isConstructor() {
            if (!JSRuntime.isNullOrUndefined(thisObj) && JSObject.isJSObject(thisObj)) {
                Object constructor = JSRuntime.getDataProperty((DynamicObject) thisObj, JSObject.CONSTRUCTOR);
                return constructor != null && constructor == functionObj;
            }
            return false;
        }

        public boolean isEval() {
            return eval;
        }

        @TruffleBoundary
        public String getEvalOrigin() {
            if (fileName.startsWith("<")) {
                return null;
            }
            return fileName;
        }

        @Override
        @TruffleBoundary
        public String toString() {
            StringBuilder builder = new StringBuilder();
            String className = getClassName();
            String methodName = JSError.correctMethodName(getFunctionName());
            if (methodName == null || methodName.isEmpty()) {
                String name = getMethodName();
                if (name == null) {
                    methodName = JSError.ANONYMOUS_FUNCTION_NAME_STACK_TRACE;
                } else {
                    methodName = name;
                }
            }
            boolean includeMethodName = className != null || !JSError.ANONYMOUS_FUNCTION_NAME_STACK_TRACE.equals(methodName);
            if (includeMethodName) {
                if (className != null) {
                    if (className.equals(methodName)) {
                        if (isConstructor()) {
                            builder.append("new ");
                        }
                    } else {
                        builder.append(className).append('.');
                    }
                }
                builder.append(methodName);
                builder.append(" (");
            }
            if (JSFunction.isBuiltinSourceSection(sourceSection)) {
                builder.append("native");
            } else {
                String evalOrigin = getEvalOrigin();
                String sourceName = evalOrigin != null ? evalOrigin : getFileName();
                builder.append(sourceName);
                builder.append(":");
                builder.append(getLineNumber());
                builder.append(":");
                builder.append(getColumnNumber());
            }
            if (includeMethodName) {
                builder.append(")");
            }
            return builder.toString();
        }
    }
}
