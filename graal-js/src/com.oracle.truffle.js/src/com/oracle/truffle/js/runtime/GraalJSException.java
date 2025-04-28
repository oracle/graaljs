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
package com.oracle.truffle.js.runtime;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleStackTrace;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.utilities.TriState;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseAllNode.PromiseAllMarkerRootNode;
import com.oracle.truffle.js.nodes.promise.PromiseReactionJobNode.PromiseReactionJobRootNode;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

@SuppressWarnings("serial")
@ImportStatic({JSConfig.class})
@ExportLibrary(InteropLibrary.class)
public abstract class GraalJSException extends AbstractTruffleException {

    private static final JSStackTraceElement[] EMPTY_STACK_TRACE = new JSStackTraceElement[0];

    private JSStackTraceElement[] jsStackTrace;
    private Object location;
    private int stackTraceLimit;

    protected GraalJSException(String message, Throwable cause, Node node, int stackTraceLimit) {
        super(message, cause, stackTraceLimit, node);
        this.location = node;
        this.stackTraceLimit = stackTraceLimit;
        this.jsStackTrace = stackTraceLimit == 0 ? EMPTY_STACK_TRACE : null;
    }

    protected GraalJSException(String message, Node node, int stackTraceLimit) {
        super(message, null, stackTraceLimit, node);
        this.location = node;
        this.stackTraceLimit = stackTraceLimit;
        this.jsStackTrace = stackTraceLimit == 0 ? EMPTY_STACK_TRACE : null;
    }

    protected GraalJSException(String message, Throwable cause, SourceSection location, int stackTraceLimit) {
        super(message, cause, stackTraceLimit, null);
        this.location = location;
        this.stackTraceLimit = stackTraceLimit;
        this.jsStackTrace = stackTraceLimit == 0 ? EMPTY_STACK_TRACE : null;
    }

    protected static <T extends GraalJSException> T fillInStackTrace(T exception, boolean capture, JSDynamicObject skipFramesUpTo, boolean customSkip) {
        exception.fillInStackTrace(capture, skipFramesUpTo, customSkip);
        return exception;
    }

    protected static <T extends GraalJSException> T fillInStackTrace(T exception, boolean capture) {
        exception.fillInStackTrace(capture, Undefined.instance, false);
        return exception;
    }

    protected final GraalJSException fillInStackTrace(boolean capture, JSDynamicObject skipFramesUpTo, boolean customSkip) {
        // We can only skip frames when capturing eagerly.
        assert capture || skipFramesUpTo == Undefined.instance;
        assert jsStackTrace == (stackTraceLimit == 0 ? EMPTY_STACK_TRACE : null);
        if (capture || JSConfig.EagerStackTrace) {
            if (stackTraceLimit > 0) {
                this.jsStackTrace = getJSStackTrace(skipFramesUpTo, customSkip);
            }
        }
        return this;
    }

    @ExportMessage
    public boolean hasSourceLocation() {
        if (location instanceof SourceSection) {
            return true;
        }
        Node locationNode = getLocation();
        SourceSection sourceSection = locationNode != null ? locationNode.getEncapsulatingSourceSection() : null;
        return sourceSection != null;
    }

    @ExportMessage(name = "getSourceLocation")
    public SourceSection getSourceLocationInterop() throws UnsupportedMessageException {
        if (location instanceof SourceSection) {
            return (SourceSection) location;
        }
        Node locationNode = getLocation();
        SourceSection sourceSection = locationNode != null ? locationNode.getEncapsulatingSourceSection() : null;
        if (sourceSection == null) {
            throw UnsupportedMessageException.create();
        }
        return sourceSection;
    }

    /** Could still be null due to lazy initialization. */
    public abstract Object getErrorObjectLazy();

    /**
     * Eager access to the ErrorObject. Use only if you must get a non-null error object.
     */
    public abstract Object getErrorObject();

    public JSStackTraceElement[] getJSStackTrace() {
        if (jsStackTrace != null) {
            return jsStackTrace;
        }
        jsStackTrace = materializeJSStackTrace();
        return jsStackTrace;
    }

    @TruffleBoundary
    private JSStackTraceElement[] materializeJSStackTrace() {
        return getJSStackTrace(Undefined.instance, false);
    }

    @TruffleBoundary
    private JSStackTraceElement[] getJSStackTrace(JSDynamicObject skipUpTo, boolean customSkip) {
        assert stackTraceLimit > 0;
        JSContext context = JavaScriptLanguage.getCurrentLanguage().getJSContext();
        boolean nashornMode = context.isOptionNashornCompatibilityMode();
        // Nashorn does not support skipping of frames
        JSDynamicObject skipFramesUpTo = nashornMode ? Undefined.instance : skipUpTo;
        boolean skippingFrames = JSFunction.isJSFunction(skipFramesUpTo);
        if (skippingFrames && customSkip) {
            FunctionRootNode.setOmitFromStackTrace(JSFunction.getFunctionData((JSFunctionObject) skipFramesUpTo));
        }
        List<TruffleStackTraceElement> stackTrace = TruffleStackTrace.getStackTrace(this);
        if (skippingFrames && customSkip) {
            FunctionRootNode.setOmitFromStackTrace(null);
        }
        if (stackTrace == null) {
            return EMPTY_STACK_TRACE;
        }
        FrameVisitorImpl visitor = new FrameVisitorImpl(getLocation(), stackTraceLimit, skipFramesUpTo, nashornMode);
        boolean asyncStackTraces = context.isOptionAsyncStackTraces();
        List<List<TruffleStackTraceElement>> asyncStacks = null;
        for (TruffleStackTraceElement element : stackTrace) {
            if (!visitor.visitFrame(element)) {
                asyncStacks = null;
                break;
            }
            if (asyncStackTraces) {
                List<TruffleStackTraceElement> asyncStack = getAsynchronousStackTrace(element);
                if (asyncStack != null && !asyncStack.isEmpty()) {
                    if (asyncStacks == null) {
                        asyncStacks = new ArrayList<>();
                    }
                    asyncStacks.add(asyncStack);
                }
            }
        }
        if (asyncStacks != null && !asyncStacks.isEmpty()) {
            out: for (List<TruffleStackTraceElement> asyncStack : asyncStacks) {
                visitor.async = true;
                for (TruffleStackTraceElement element : asyncStack) {
                    if (!visitor.visitFrame(element)) {
                        break out;
                    }
                }
            }
        }
        return visitor.getStackTrace().toArray(EMPTY_STACK_TRACE);
    }

    private static List<TruffleStackTraceElement> getAsynchronousStackTrace(TruffleStackTraceElement element) {
        if (element.getFrame() == null) {
            // getAsynchronousStackTrace requires a frame.
            return null;
        }
        RootNode rootNode = element.getTarget().getRootNode();
        if (rootNode.getLanguageInfo() == null) {
            // getAsynchronousStackTrace requires the RootNode to have language info.
            return null;
        }
        if (rootNode instanceof JavaScriptRootNode) {
            if (rootNode instanceof PromiseReactionJobRootNode) {
                return JavaScriptRootNode.findAsynchronousFrames((JavaScriptRootNode) rootNode, element.getFrame());
            } else {
                // We do not want to include any of the extra stack trace elements available when
                // getAsynchronousStackDepth() > 0.
                return null;
            }
        }
        return TruffleStackTrace.getAsynchronousStackTrace(element.getTarget(), element.getFrame());
    }

    public void setJSStackTrace(JSStackTraceElement[] jsStackTrace) {
        this.jsStackTrace = jsStackTrace;
    }

    @TruffleBoundary
    public static JSStackTraceElement[] getJSStackTrace(Node originatingNode) {
        int stackTraceLimit = JavaScriptLanguage.get(originatingNode).getJSContext().getLanguageOptions().stackTraceLimit();
        return getJSStackTrace(originatingNode, stackTraceLimit);
    }

    @TruffleBoundary
    public static JSStackTraceElement[] getJSStackTrace(Node originatingNode, int stackTraceLimit) {
        return UserScriptException.createCapture("", originatingNode, stackTraceLimit).getJSStackTrace();
    }

    private static final class FrameVisitorImpl {
        private static final int STACK_FRAME_SKIP = 0;
        private static final int STACK_FRAME_JS = 1;
        private static final int STACK_FRAME_FOREIGN = 2;

        private final List<JSStackTraceElement> stackTrace = new ArrayList<>();
        private final Node originatingNode;
        private final int stackTraceLimit;
        private final JSDynamicObject skipFramesUpTo;
        private final boolean inNashornMode;

        private boolean inStrictMode;
        private boolean skippingFrames;
        private boolean first = true;
        boolean async;

        FrameVisitorImpl(Node originatingNode, int stackTraceLimit, JSDynamicObject skipFramesUpTo, boolean nashornMode) {
            this.originatingNode = originatingNode;
            this.stackTraceLimit = stackTraceLimit;
            this.skipFramesUpTo = skipFramesUpTo;
            this.skippingFrames = (skipFramesUpTo != Undefined.instance);
            this.inNashornMode = nashornMode;
        }

        private int stackFrameType(Node callNode) {
            if (callNode == null) {
                return STACK_FRAME_SKIP;
            }
            SourceSection sourceSection = callNode.getEncapsulatingSourceSection();
            if (sourceSection == null) {
                return STACK_FRAME_SKIP;
            }
            if (JSFunction.isBuiltinSourceSection(sourceSection)) {
                return inNashornMode ? STACK_FRAME_SKIP : STACK_FRAME_JS;
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

        private static RootNode rootNode(TruffleStackTraceElement element) {
            CallTarget callTarget = element.getTarget();
            return (callTarget instanceof RootCallTarget) ? ((RootCallTarget) callTarget).getRootNode() : null;
        }

        public boolean visitFrame(TruffleStackTraceElement element) {
            Node callNode = element.getLocation();
            if (first) {
                first = false;
                if (JSRuntime.isJSRootNode(rootNode(element))) {
                    callNode = originatingNode;
                }
            }
            if (callNode == null) {
                callNode = rootNode(element);
            }

            // this check for code style analyzers
            if (callNode != null) {
                switch (stackFrameType(callNode)) {
                    case STACK_FRAME_JS: {
                        RootNode rootNode = callNode.getRootNode();
                        assert JSRuntime.isJSRootNode(rootNode);
                        final Object[] arguments;
                        int promiseIndex = -1;
                        if (element.getFrame() == null) {
                            break;
                        } else if (JSRuntime.isJSFunctionRootNode(rootNode)) {
                            arguments = element.getFrame().getArguments();
                        } else if (((JavaScriptRootNode) rootNode).isResumption()) {
                            arguments = element.getFrame().getArguments();
                        } else if (rootNode instanceof PromiseAllMarkerRootNode) {
                            arguments = element.getFrame().getArguments();
                            if (JSArguments.getUserArgumentCount(arguments) > 0) {
                                Object promiseIndexArg = JSArguments.getUserArgument(arguments, 0);
                                if (promiseIndexArg instanceof Integer) {
                                    promiseIndex = (int) promiseIndexArg;
                                }
                            }
                        } else {
                            break;
                        }
                        Object thisObj = JSArguments.getThisObject(arguments);
                        Object functionObj = JSArguments.getFunctionObject(arguments);
                        if (JSFunction.isJSFunction(functionObj)) {
                            JSFunctionObject function = (JSFunctionObject) functionObj;
                            JSFunctionData functionData = JSFunction.getFunctionData(function);
                            if (functionData.isBuiltin()) {
                                if (JSFunction.isStrictBuiltin(function, JSRealm.get(null))) {
                                    inStrictMode = true;
                                }
                            } else if (functionData.isStrict()) {
                                inStrictMode = true;
                            }
                            if (skippingFrames && function == skipFramesUpTo) {
                                skippingFrames = false;
                                return true; // skip this frame as well
                            }
                            JSRealm realm = JSFunction.getRealm(function);
                            if (JSFunction.isBuiltinThatShouldNotAppearInStackTrace(realm, function)) {
                                return true;
                            }
                            if (!skippingFrames) {
                                if (functionData.isAsync() && !functionData.isGenerator() && JSRuntime.isJSFunctionRootNode(rootNode)) {
                                    // async function calls produce two frames, skip one
                                    return true;
                                }
                                stackTrace.add(processJSFrame(rootNode, callNode, thisObj, function, inStrictMode, inNashornMode, async, promiseIndex));
                            }
                        }
                        break;
                    }
                    case STACK_FRAME_FOREIGN:
                        if (!skippingFrames) {
                            JSStackTraceElement elem = processForeignFrame(callNode, inStrictMode, inNashornMode, async);
                            if (elem != null) {
                                stackTrace.add(elem);
                            }
                        }
                        break;
                }
            }
            return stackTrace.size() < stackTraceLimit;
        }

        public List<JSStackTraceElement> getStackTrace() {
            return stackTrace;
        }

    }

    private static JSStackTraceElement processJSFrame(RootNode rootNode, Node node, Object thisObj, JSFunctionObject functionObj, boolean inStrictMode, boolean inNashornMode, boolean async,
                    int promiseIndex) {
        Node callNode = node;
        while (callNode.getSourceSection() == null) {
            callNode = callNode.getParent();
        }
        SourceSection callNodeSourceSection = callNode.getSourceSection();
        Source source = callNodeSourceSection.getSource();

        TruffleString fileName = getFileName(source);
        TruffleString functionName;
        if (JSFunction.isBuiltin(functionObj)) {
            functionName = JSFunction.getName(functionObj);
        } else if (rootNode instanceof FunctionRootNode) {
            functionName = ((FunctionRootNode) rootNode).getNameTString();
        } else {
            functionName = Strings.fromJavaString(rootNode.getName());
        }
        boolean eval = false;
        if (isEvalSource(source)) {
            functionName = Strings.EVAL;
            eval = true;
        } else if (functionName == null || isInternalFunctionName(functionName)) {
            functionName = Strings.EMPTY_STRING;
        }
        SourceSection targetSourceSection = null;
        if (!inNashornMode) { // for V8
            if (callNode instanceof JavaScriptFunctionCallNode) {
                Node target = ((JavaScriptFunctionCallNode) callNode).getTarget();
                targetSourceSection = target == null ? null : target.getSourceSection();
            }
        }
        boolean global = (JSRuntime.isNullOrUndefined(thisObj) && !JSFunction.isStrict(functionObj)) || isGlobalObject(thisObj, JSFunction.getRealm(functionObj));
        boolean hasPath = source.getPath() != null;
        return new JSStackTraceElement(fileName, functionName, callNodeSourceSection, thisObj, functionObj, targetSourceSection,
                        inStrictMode, eval, global, inNashornMode, async, hasPath, promiseIndex);
    }

    private static boolean isEvalSource(Source source) {
        return source.getName().startsWith(Evaluator.EVAL_AT_SOURCE_NAME_PREFIX);
    }

    private static boolean isInternalFunctionName(TruffleString functionName) {
        return Strings.length(functionName) >= 1 && Strings.charAt(functionName, 0) == ':';
    }

    private static boolean isGlobalObject(Object object, JSRealm realm) {
        return JSDynamicObject.isJSDynamicObject(object) && (realm != null) && (realm.getGlobalObject() == object);
    }

    private static JSStackTraceElement processForeignFrame(Node node, boolean strict, boolean inNashornMode, boolean async) {
        RootNode rootNode = node.getRootNode();
        SourceSection sourceSection = rootNode.getSourceSection();
        if (sourceSection == null) {
            // can happen around FastR root nodes, see GR-6604
            return null;
        }
        Source source = sourceSection.getSource();
        TruffleString fileName = getFileName(source);
        TruffleString functionName = Strings.fromJavaString(rootNode.getName());
        Object thisObj = null;
        Object functionObj = null;
        boolean hasPath = source.getPath() != null;

        return new JSStackTraceElement(fileName, functionName, sourceSection, thisObj, functionObj, null, strict, false, false, inNashornMode, async, hasPath, -1);
    }

    private static int sourceSectionOffset(SourceSection callNodeSourceSection, SourceSection targetSourceSection) {
        int offset = 0;
        String code = callNodeSourceSection.getCharacters().toString();

        // skip code for the target
        if (targetSourceSection != null) {
            String targetCode = targetSourceSection.getCharacters().toString();
            int index = code.indexOf(targetCode);
            if (index != -1) {
                index += targetCode.length();
                offset += index;
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
            offset += index + 1;
        }
        return offset;
    }

    private static TruffleString getFileName(Source source) {
        String fileName = source.getPath();
        if (fileName == null) {
            fileName = source.getName();
        }
        return Strings.fromJavaString(fileName);
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

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean hasLanguage() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final Class<? extends TruffleLanguage<?>> getLanguage() {
        return JavaScriptLanguage.class;
    }

    @ExportMessage
    public final Object toDisplayString(boolean allowSideEffects) {
        return JSRuntime.toDisplayString(this, allowSideEffects);
    }

    /**
     * Builds the display message from the error name (required) and message (optional).
     */
    protected static String concatErrorNameAndMessage(String name, String message) {
        assert name != null;
        return (message == null || message.isEmpty()) ? name : name + ": " + message;
    }

    protected static String getErrorNameSafe(JSObject errorObj, String name) {
        // Try error.name first, error.constructor.name second.
        Object nameValue = JSRuntime.getDataProperty(errorObj, JSError.NAME);
        if (nameValue instanceof TruffleString nameStr && !nameStr.isEmpty() && !nameStr.equals(Strings.UC_ERROR)) {
            return Strings.toJavaString(nameStr);
        }
        return Strings.toJavaString(JSRuntime.getConstructorName(errorObj, Strings.fromJavaString(name)));
    }

    protected static String getErrorMessageSafe(JSObject errorObj, String message) {
        Object messageValue = JSRuntime.getDataProperty(errorObj, JSError.MESSAGE);
        if (messageValue instanceof TruffleString messageStr && !messageStr.isEmpty()) {
            return Strings.toJavaString(messageStr);
        }
        return message;
    }

    @ImportStatic({JSConfig.class, JSGuards.class})
    @ExportMessage
    public static final class IsIdenticalOrUndefined {
        @Specialization
        public static TriState doException(GraalJSException receiver, GraalJSException other,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary thisLib,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary otherLib) {
            if (receiver == other) {
                return TriState.TRUE;
            }
            Object thisObj = receiver.getErrorObjectLazy();
            if (thisObj == null) {
                // Cannot be identical since this is a lazily allocated Error and receiver != other.
                return TriState.FALSE;
            }
            Object otherObj = other.getErrorObjectLazy();
            if (otherObj == null) {
                return TriState.FALSE;
            }
            // If the values are not JS objects, we need to delegate to InteropLibrary.
            if (thisLib.hasIdentity(thisObj) && otherLib.hasIdentity(other)) {
                return TriState.valueOf(thisLib.isIdentical(thisObj, other, otherLib));
            } else {
                return TriState.UNDEFINED;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"thisObj == null || isJSDynamicObject(thisObj)"})
        public static TriState doJSObject(GraalJSException receiver, JSDynamicObject other,
                        @Bind("receiver.getErrorObjectLazy()") Object thisObj) {
            // If thisObj is null, the Error object is lazily allocated and cannot be identical.
            return TriState.valueOf(thisObj == other);
        }

        @Specialization(guards = {"!isGraalJSException(other)"}, replaces = {"doJSObject"})
        public static TriState doOther(GraalJSException receiver, Object other,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary thisLib,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary otherLib) {
            Object thisObj = receiver.getErrorObjectLazy();
            if (thisObj == null) {
                // The error object cannot be identical since this is a lazily allocated Error.
                // Note: `other` could still be an identity-preserving wrapper of the receiver,
                // in which case we must not return FALSE but UNDEFINED.
                return (other instanceof JSDynamicObject) ? TriState.FALSE : TriState.UNDEFINED;
            }
            // If the values are not JS objects, we need to delegate to InteropLibrary.
            if (thisLib.hasIdentity(thisObj) && otherLib.hasIdentity(other)) {
                return TriState.valueOf(thisLib.isIdentical(thisObj, other, otherLib));
            } else {
                return TriState.UNDEFINED;
            }
        }

        static boolean isGraalJSException(Object value) {
            return value instanceof GraalJSException;
        }
    }

    @ExportMessage
    @TruffleBoundary
    public final int identityHashCode(
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary thisLib) throws UnsupportedMessageException {
        return thisLib.identityHashCode(getErrorObject());
    }

    public static final class JSStackTraceElement {
        private final TruffleString fileName;
        private final TruffleString functionName;
        private final SourceSection sourceSection;
        private final Object thisObj;
        private final Object functionObj;
        private final SourceSection targetSourceSection;
        private final boolean strict;
        private final boolean eval;
        private final boolean global;
        private final boolean inNashornMode;
        private final boolean async;
        private final boolean hasPath;
        private final int promiseIndex;

        private JSStackTraceElement(TruffleString fileName, TruffleString functionName, SourceSection sourceSection, Object thisObj, Object functionObj, SourceSection targetSourceSection,
                        boolean strict, boolean eval, boolean global, boolean inNashornMode, boolean async, boolean hasPath, int promiseIndex) {
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
            this.inNashornMode = inNashornMode;
            this.async = async;
            this.hasPath = hasPath;
            this.promiseIndex = promiseIndex;
        }

        @TruffleBoundary
        public TruffleString getFileName() {
            if (eval) {
                return Evaluator.TS_EVAL_SOURCE_NAME;
            }
            return fileName;
        }

        public TruffleString getClassName() {
            return getTypeName(false);
        }

        public TruffleString getTypeName() {
            return getTypeName(true);
        }

        @TruffleBoundary
        public TruffleString getTypeName(boolean checkGlobal) {
            if (inNashornMode) {
                return Strings.concatAll(Strings.ANGLE_BRACKET_OPEN, fileName, Strings.ANGLE_BRACKET_CLOSE);
            } else {
                if (checkGlobal && global) {
                    return Strings.GLOBAL;
                }
                Object thisObject = getThis();
                if (thisObject == JSFunction.CONSTRUCT) {
                    return getFunctionName();
                } else if (!JSRuntime.isNullOrUndefined(thisObject) && !global) {
                    if (thisObject instanceof JSObject receiver) {
                        return JSRuntime.getConstructorName(receiver);
                    } else if (JSRuntime.isJSPrimitive(thisObject)) {
                        return JSRuntime.getPrimitiveConstructorName(thisObject);
                    }
                }
                return null;
            }
        }

        @TruffleBoundary
        public TruffleString getFunctionName() {
            if (JSFunction.isJSFunction(functionObj)) {
                TruffleString dynamicName = findFunctionName((JSDynamicObject) functionObj);
                // The default name of dynamic functions is "anonymous" as per the spec.
                // Yet, in V8 stack traces it is "eval" unless overwritten.
                if (dynamicName != null && !Strings.isEmpty(dynamicName) &&
                                (!isEval() || !Strings.equals(Strings.DYNAMIC_FUNCTION_NAME, dynamicName) || !JSObject.getJSContext((JSDynamicObject) functionObj).isOptionV8CompatibilityMode())) {
                    return dynamicName;
                }
            }
            return functionName;
        }

        private static TruffleString findFunctionName(JSDynamicObject functionObj) {
            assert JSFunction.isJSFunction(functionObj);
            PropertyDescriptor desc = JSObject.getOwnProperty(functionObj, JSFunction.NAME);
            if (desc != null) {
                if (desc.isDataDescriptor()) {
                    Object name = desc.getValue();
                    if (name instanceof TruffleString nameStr) {
                        return nameStr;
                    }
                }
            }
            return null;
        }

        @TruffleBoundary
        public String getMethodName() {
            return Strings.toJavaString(getMethodName(JavaScriptLanguage.getCurrentLanguage().getJSContext()));
        }

        @TruffleBoundary
        public TruffleString getMethodName(JSContext context) {
            if (context.isOptionNashornCompatibilityMode()) {
                return JSError.correctMethodName(functionName, context);
            }
            if (JSRuntime.isNullOrUndefined(thisObj) || !JSDynamicObject.isJSDynamicObject(thisObj)) {
                return null;
            }
            if (!JSFunction.isJSFunction(functionObj)) {
                return null;
            }

            JSDynamicObject receiver = (JSDynamicObject) thisObj;
            JSFunctionObject function = (JSFunctionObject) functionObj;
            if (functionName != null && !Strings.isEmpty(functionName)) {
                TruffleString name = findMethodPropertyNameByFunctionName(receiver, functionName, function);
                if (name != null) {
                    return name;
                }
            }
            return findMethodPropertyName(receiver, function);
        }

        private static TruffleString findMethodPropertyNameByFunctionName(JSDynamicObject receiver, TruffleString functionName, JSFunctionObject functionObj) {
            TruffleString propertyName = functionName;
            boolean accessor = false;
            if (Strings.startsWith(propertyName, Strings.GET_SPC) || Strings.startsWith(propertyName, Strings.SET_SPC)) {
                propertyName = Strings.lazySubstring(propertyName, 4);
                accessor = true;
            }
            if (propertyName.isEmpty()) {
                return null;
            }
            for (JSDynamicObject current = receiver; current != Null.instance && !JSProxy.isJSProxy(current); current = JSObject.getPrototype(current)) {
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

        private static TruffleString findMethodPropertyName(JSDynamicObject receiver, JSDynamicObject functionObj) {
            TruffleString name = null;
            for (JSDynamicObject current = receiver; current != Null.instance && !JSProxy.isJSProxy(current); current = JSObject.getPrototype(current)) {
                for (TruffleString key : JSObject.enumerableOwnNames(current)) {
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

        @TruffleBoundary
        public int getLineNumber() {
            if (sourceSection == null) {
                return -1;
            }
            int lineNumber = sourceSection.getStartLine();
            if (!inNashornMode && targetSourceSection != null) {
                // for V8
                int offset = sourceSectionOffset(sourceSection, targetSourceSection);
                CharSequence chars = sourceSection.getCharacters();
                for (int pos = 0; pos < offset; pos++) {
                    if (chars.charAt(pos) == '\n') {
                        lineNumber++;
                    }
                }
            }
            return lineNumber;
        }

        @TruffleBoundary
        public TruffleString getLine() {
            int lineNumber = getLineNumber();
            if (sourceSection == null || sourceSection.getSource() == null || lineNumber <= 0) {
                return Strings.UNKNOWN_FILENAME;
            }
            return Strings.fromJavaString(sourceSection.getSource().getCharacters(lineNumber).toString());
        }

        @TruffleBoundary
        public int getColumnNumber() {
            if (sourceSection == null) {
                return -1;
            }
            int columnNumber = sourceSection.getStartColumn();
            if (!inNashornMode && targetSourceSection != null) {
                // for V8
                int offset = sourceSectionOffset(sourceSection, targetSourceSection);
                CharSequence chars = sourceSection.getCharacters();
                for (int pos = 0; pos < offset; pos++) {
                    if (chars.charAt(pos) == '\n') {
                        columnNumber = 1;
                    } else {
                        columnNumber++;
                    }
                }
            }
            return columnNumber;
        }

        public int getPosition() {
            return sourceSection != null ? sourceSection.getCharIndex() : -1;
        }

        public int getCharLength() {
            return sourceSection != null ? sourceSection.getCharLength() : 0;
        }

        public Object getThis() {
            return thisObj;
        }

        @TruffleBoundary
        public Object getThisOrGlobal() {
            if (global) {
                if (JSRuntime.isNullOrUndefined(thisObj)) {
                    return JSFunction.getRealm((JSFunctionObject) functionObj).getGlobalObject();
                } else {
                    assert thisObj == JSFunction.getRealm((JSFunctionObject) functionObj).getGlobalObject();
                    return thisObj;
                }
            }
            return (thisObj == JSFunction.CONSTRUCT) ? Undefined.instance : thisObj;
        }

        public Object getFunction() {
            return functionObj;
        }

        public boolean isStrict() {
            return strict;
        }

        @TruffleBoundary
        public boolean isConstructor() {
            if (thisObj == JSFunction.CONSTRUCT) {
                return true;
            } else if (!JSRuntime.isNullOrUndefined(thisObj) && JSDynamicObject.isJSDynamicObject(thisObj)) {
                Object constructor = JSRuntime.getDataProperty((JSDynamicObject) thisObj, JSObject.CONSTRUCTOR);
                return constructor != null && constructor == functionObj;
            }
            return false;
        }

        public boolean isEval() {
            return eval;
        }

        @TruffleBoundary
        public TruffleString getEvalOrigin() {
            if (eval && !Strings.startsWith(fileName, Strings.ANGLE_BRACKET_OPEN)) {
                return fileName;
            }
            return null;
        }

        public int getPromiseIndex() {
            return promiseIndex;
        }

        public boolean isPromiseAll() {
            return promiseIndex >= 0;
        }

        public boolean isAsync() {
            return async;
        }

        public boolean hasPath() {
            return hasPath;
        }

        @TruffleBoundary
        @Override
        public String toString() {
            JSContext context = JavaScriptLanguage.getCurrentJSRealm().getContext();
            return Strings.toJavaString(toString(context));
        }

        @TruffleBoundary
        public TruffleString toString(JSContext context) {
            var sb = Strings.builderCreate();
            if (isPromiseAll()) {
                Strings.builderAppend(sb, Strings.ASYNC_PROMISE_ALL_BEGIN);
                Strings.builderAppend(sb, promiseIndex);
                Strings.builderAppend(sb, Strings.PAREN_CLOSE);
                return Strings.builderToString(sb);
            }

            TruffleString className = getClassName();
            TruffleString methodName = JSError.correctMethodName(getFunctionName(), context);
            if (methodName == null || Strings.isEmpty(methodName)) {
                TruffleString name = getMethodName(context);
                if (name == null) {
                    methodName = JSError.getAnonymousFunctionNameStackTrace(context);
                } else {
                    methodName = name;
                }
            }
            boolean includeMethodName = className != null || !JSError.getAnonymousFunctionNameStackTrace(context).equals(methodName);
            if (includeMethodName) {
                if (async) {
                    Strings.builderAppend(sb, Strings.ASYNC_SPC);
                }
                if (className != null) {
                    if (className.equals(methodName)) {
                        if (isConstructor()) {
                            Strings.builderAppend(sb, Strings.NEW_SPACE);
                        }
                    } else {
                        Strings.builderAppend(sb, className);
                        Strings.builderAppend(sb, Strings.DOT);
                    }
                }
                Strings.builderAppend(sb, methodName);
                Strings.builderAppend(sb, Strings.SPACE_PAREN_OPEN);
            }
            if (JSFunction.isBuiltinSourceSection(sourceSection)) {
                Strings.builderAppend(sb, Strings.NATIVE);
            } else {
                TruffleString evalOrigin = getEvalOrigin();
                TruffleString sourceName = evalOrigin != null ? evalOrigin : getFileNameForStackTrace(context);
                Strings.builderAppend(sb, sourceName);
                if (eval) {
                    Strings.builderAppend(sb, Strings.COMMA_ANONYMOUS_BRACKETS);
                }
                Strings.builderAppend(sb, Strings.COLON);
                Strings.builderAppend(sb, getLineNumber());
                Strings.builderAppend(sb, Strings.COLON);
                Strings.builderAppend(sb, getColumnNumber());
            }
            if (includeMethodName) {
                Strings.builderAppend(sb, Strings.PAREN_CLOSE);
            }
            return Strings.builderToString(sb);
        }

        public TruffleString getFileNameForStackTrace(JSContext context) {
            if (hasPath && context.isOptionNashornCompatibilityMode() && sourceSection != null) {
                return Strings.fromJavaString(sourceSection.getSource().getName());
            } else {
                return getFileName();
            }
        }
    }
}
