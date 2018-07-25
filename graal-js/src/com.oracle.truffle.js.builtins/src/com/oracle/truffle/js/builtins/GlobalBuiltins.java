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
package com.oracle.truffle.js.builtins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Map;
import java.util.StringTokenizer;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.GlobalNashornExtensionParseToJSONNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.GlobalScriptingEXECNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalDecodeURINodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalEncodeURINodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalExitNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalIndirectEvalNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalIsFiniteNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalIsNaNNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalLoadNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalLoadWithNewGlobalNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalParseFloatNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalParseIntNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalPrintNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalReadBufferNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalReadFullyNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalReadLineNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalUnEscapeNodeGen;
import com.oracle.truffle.js.builtins.helper.FloatParser;
import com.oracle.truffle.js.builtins.helper.StringEscape;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeEvaluator;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.RealmNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSTrimWhitespaceNode;
import com.oracle.truffle.js.nodes.function.EvalNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSLoadNode;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.ExitException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.oracle.truffle.js.runtime.builtins.JSURLDecoder;
import com.oracle.truffle.js.runtime.builtins.JSURLEncoder;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

/**
 * Contains builtins for the global object.
 */
public class GlobalBuiltins extends JSBuiltinsContainer.SwitchEnum<GlobalBuiltins.Global> {

    protected GlobalBuiltins() {
        super(null, Global.class);
    }

    public enum Global implements BuiltinEnum<Global> {
        isNaN(1),
        isFinite(1),
        parseFloat(1),
        parseInt(2),
        encodeURI(1),
        encodeURIComponent(1),
        decodeURI(1),
        decodeURIComponent(1),
        eval(1),

        // Annex B
        escape(1),
        unescape(1),

        // non-standard extensions
        print(1),
        printErr(1),
        load(1),
        loadWithNewGlobal(-1),
        exit(1),
        quit(1),
        readline(1),
        readLine(1),
        read(1),
        readFully(1),
        readbuffer(1);

        private final int length;

        Global(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isAnnexB() {
            return EnumSet.of(escape, unescape).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Global builtinEnum) {
        switch (builtinEnum) {
            case isNaN:
                return JSGlobalIsNaNNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isFinite:
                return JSGlobalIsFiniteNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case parseFloat:
                return JSGlobalParseFloatNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case parseInt:
                return JSGlobalParseIntNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case encodeURI:
                return JSGlobalEncodeURINodeGen.create(context, builtin, true, args().fixedArgs(1).createArgumentNodes(context));
            case encodeURIComponent:
                return JSGlobalEncodeURINodeGen.create(context, builtin, false, args().fixedArgs(1).createArgumentNodes(context));
            case decodeURI:
                return JSGlobalDecodeURINodeGen.create(context, builtin, true, args().fixedArgs(1).createArgumentNodes(context));
            case decodeURIComponent:
                return JSGlobalDecodeURINodeGen.create(context, builtin, false, args().fixedArgs(1).createArgumentNodes(context));
            case eval:
                return JSGlobalIndirectEvalNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case escape:
                return JSGlobalUnEscapeNodeGen.create(context, builtin, false, args().fixedArgs(1).createArgumentNodes(context));
            case unescape:
                return JSGlobalUnEscapeNodeGen.create(context, builtin, true, args().fixedArgs(1).createArgumentNodes(context));
            case print:
                return JSGlobalPrintNodeGen.create(context, builtin, false, args().varArgs().createArgumentNodes(context));
            case printErr:
                return JSGlobalPrintNodeGen.create(context, builtin, true, args().varArgs().createArgumentNodes(context));
            case load:
                return JSGlobalLoadNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case loadWithNewGlobal:
                return JSGlobalLoadWithNewGlobalNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context));
            case exit:
            case quit:
                return JSGlobalExitNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case readline:
                return JSGlobalReadLineNodeGen.create(context, builtin, new JavaScriptNode[]{JSConstantNode.createUndefined()});
            case readLine:
                return JSGlobalReadLineNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case readFully:
            case read:
                return JSGlobalReadFullyNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case readbuffer:
                return JSGlobalReadBufferNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public static final class GlobalNashornExtensionsBuiltins extends JSBuiltinsContainer.SwitchEnum<GlobalNashornExtensionsBuiltins.GlobalNashornExtensions> {
        protected GlobalNashornExtensionsBuiltins() {
            super(JSGlobalObject.CLASS_NAME_NASHORN_EXTENSIONS, GlobalNashornExtensions.class);
        }

        // attention: those are manually added in JSRealm.initGlobalNashornExtensions or
        // JSRealm.initGlobalScriptingExtensions.
        public enum GlobalNashornExtensions implements BuiltinEnum<GlobalNashornExtensions> {
            exec(1), // $EXEC
            parseToJSON(3);

            private final int length;

            GlobalNashornExtensions(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, GlobalNashornExtensions builtinEnum) {
            switch (builtinEnum) {
                case parseToJSON:
                    return GlobalNashornExtensionParseToJSONNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
                case exec:
                    return GlobalScriptingEXECNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            }
            return null;
        }
    }

    /**
     * For load("nashorn:parser.js") compatibility.
     */
    public abstract static class GlobalNashornExtensionParseToJSONNode extends JSBuiltinNode {
        public GlobalNashornExtensionParseToJSONNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected String parseToJSON(Object code0, Object name0, Object location0) {
            String code = JSRuntime.toString(code0);
            String name = name0 == Undefined.instance ? "<unknown>" : JSRuntime.toString(name0);
            boolean location = JSRuntime.toBoolean(location0);
            return getContext().getEvaluator().parseToJSON(getContext(), code, name, location);
        }
    }

    /**
     * Implements $EXEC() in Nashorn scripting mode.
     */
    public abstract static class GlobalScriptingEXECNode extends JSBuiltinNode {
        public GlobalScriptingEXECNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object exec(Object cmd, Object input) {
            String cmdStr = JSRuntime.toString(cmd);
            String inputStr = input != Undefined.instance ? JSRuntime.toString(input) : null;
            return execIntl(cmdStr, inputStr);
        }

        @TruffleBoundary
        private Object execIntl(String cmd, String input) {
            try {
                DynamicObject globalObj = getContext().getRealm().getGlobalObject();

                StringTokenizer tok = new StringTokenizer(cmd);
                String[] cmds = new String[tok.countTokens()];
                for (int i = 0; tok.hasMoreTokens(); i++) {
                    cmds[i] = tok.nextToken();
                }

                ProcessBuilder builder = new ProcessBuilder(cmds);

                Object env = JSObject.get(globalObj, "$ENV");
                if (env instanceof DynamicObject) {
                    DynamicObject dynEnvObj = (DynamicObject) env;
                    Object pwd = JSObject.get(dynEnvObj, "PWD");
                    if (pwd != Undefined.instance) {
                        builder.directory(new File(JSRuntime.toString(pwd)));
                    }

                    Map<String, String> environment = builder.environment();
                    environment.clear();
                    for (String key : JSObject.enumerableOwnNames(dynEnvObj)) {
                        environment.put(key, JSRuntime.toString(JSObject.get(dynEnvObj, key)));
                    }
                }

                Process process = builder.start();
                IOException[] exception = new IOException[2];
                StringBuilder outBuffer = new StringBuilder();
                StringBuilder errBuffer = new StringBuilder();

                Thread outThread = captureThread(exception, 0, outBuffer, process.getInputStream(), "$EXEC output");
                Thread errThread = captureThread(exception, 1, errBuffer, process.getErrorStream(), "$EXEC error");

                outThread.start();
                errThread.start();

                try (OutputStreamWriter outputStream = new OutputStreamWriter(process.getOutputStream())) {
                    if (input != null) {
                        outputStream.write(input, 0, input.length());
                    }
                } catch (IOException ex) {
                }

                int exitCode = process.waitFor();
                outThread.join();
                errThread.join();

                String outStr = outBuffer.toString();

                JSObject.set(globalObj, "$OUT", outStr);
                JSObject.set(globalObj, "$ERR", errBuffer.toString());
                JSObject.set(globalObj, "$EXIT", exitCode);

                for (int i = 0; i < exception.length; i++) {
                    if (exception[i] != null) {
                        throw Errors.createTypeError(exception[i].getMessage());
                    }
                }
                return outStr;
            } catch (IOException e) {
                throw Errors.createTypeError(e.getMessage());
            } catch (InterruptedException e) {
                throw Errors.createTypeError(e.getMessage());
            }
        }

        private static Thread captureThread(IOException[] exception, int exceptionIdx, StringBuilder outBuffer, InputStream stream, String name) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    char[] buffer = new char[1024];
                    try (InputStreamReader inputStream = new InputStreamReader(stream)) {
                        for (int length; (length = inputStream.read(buffer, 0, buffer.length)) != -1;) {
                            outBuffer.append(buffer, 0, length);
                        }
                    } catch (IOException ex) {
                        exception[exceptionIdx] = ex;
                    }
                }
            }, name);
            return thread;
        }
    }

    private abstract static class JSGlobalOperation extends JSBuiltinNode {

        JSGlobalOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToStringNode toString1Node;

        protected final String toString1(Object target) {
            if (toString1Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString1Node = insert(JSToStringNode.create());
            }
            return toString1Node.executeString(target);
        }
    }

    static File resolveRelativeFilePath(String path) {
        CompilerAsserts.neverPartOfCompilation();
        File file = new File(path);
        if (!file.isAbsolute() && !file.exists()) {
            File f = tryResolveCallerRelativeFilePath(path);
            if (f != null) {
                return f;
            }
        }
        return file;
    }

    private static File tryResolveCallerRelativeFilePath(String path) {
        CompilerAsserts.neverPartOfCompilation();
        CallTarget caller = Truffle.getRuntime().getCallerFrame().getCallTarget();
        if (caller instanceof RootCallTarget) {
            SourceSection callerSourceSection = ((RootCallTarget) caller).getRootNode().getSourceSection();
            if (callerSourceSection != null && callerSourceSection.isAvailable()) {
                String callerPath = callerSourceSection.getSource().getPath();
                if (callerPath != null) {
                    File callerFile = new File(callerPath);
                    if (callerFile.isAbsolute()) {
                        File file = callerFile.toPath().resolveSibling(path).normalize().toFile();
                        if (file.isFile()) {
                            return file;
                        }
                    }
                }
            }
        }
        return null;
    }

    public abstract static class JSLoadOperation extends JSGlobalOperation {
        public JSLoadOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSLoadNode loadNode;

        protected final Object runImpl(JSRealm realm, Source source) {
            if (loadNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                loadNode = insert(JSLoadNode.create(realm.getContext()));
            }
            return loadNode.executeLoad(source, realm);
        }

        protected static ScriptNode loadStringImpl(JSContext ctxt, String name, String script) {
            CompilerAsserts.neverPartOfCompilation();
            long startTime = JSTruffleOptions.ProfileTime ? System.nanoTime() : 0L;
            try {
                return ((NodeEvaluator) ctxt.getEvaluator()).evalCompile(ctxt, script, name);
            } finally {
                if (JSTruffleOptions.ProfileTime) {
                    ctxt.getTimeProfiler().printElapsed(startTime, "parsing " + name);
                }
            }
        }

        @TruffleBoundary
        protected final Source sourceFromURL(URL url) {
            try {
                return Source.newBuilder(url).name(url.getFile()).language(AbstractJavaScriptLanguage.ID).build();
            } catch (IOException e) {
                throw JSException.create(JSErrorType.EvalError, e.getMessage(), e, this);
            }
        }

        @TruffleBoundary
        protected final Source sourceFromFileName(String fileName) {
            try {
                return AbstractJavaScriptLanguage.sourceFromFileName(fileName);
            } catch (IOException e) {
                throw JSException.create(JSErrorType.EvalError, e.getMessage(), e, this);
            }
        }

        @TruffleBoundary
        protected static final String fileGetPath(File file) {
            return file.getPath();
        }
    }

    /**
     * Implementation of ECMAScript 5.1 15.1.2.4 isNaN() method.
     *
     */
    public abstract static class JSGlobalIsNaNNode extends JSBuiltinNode {

        public JSGlobalIsNaNNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static boolean isNaN(@SuppressWarnings("unused") int value) {
            return false;
        }

        @Specialization
        protected static boolean isNaN(double value) {
            return Double.isNaN(value);
        }

        @Specialization(guards = "!isUndefined(value)")
        protected static boolean isNaN(Object value,
                        @Cached("create()") JSToDoubleNode toDoubleNode) {
            return isNaN(toDoubleNode.executeDouble(value));
        }

        @Specialization(guards = "isUndefined(value)")
        protected static boolean isNaN(@SuppressWarnings("unused") Object value) {
            return true;
        }
    }

    /**
     * Implementation of ECMAScript 5.1 15.1.2.5 isFinite() method.
     *
     */
    public abstract static class JSGlobalIsFiniteNode extends JSBuiltinNode {

        public JSGlobalIsFiniteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static boolean isFinite(@SuppressWarnings("unused") int value) {
            return true;
        }

        @Specialization
        protected static boolean isFinite(double value) {
            return !Double.isInfinite(value) && !Double.isNaN(value);
        }

        @Specialization(guards = "!isUndefined(value)")
        protected static boolean isFinite(Object value,
                        @Cached("create()") JSToDoubleNode toDoubleNode) {
            return isFinite(toDoubleNode.executeDouble(value));
        }

        @Specialization(guards = "isUndefined(value)")
        protected static boolean isFinite(@SuppressWarnings("unused") Object value) {
            return false;
        }
    }

    /**
     * Implementation of ECMAScript 5.1 15.1.2.3 parseFloat() method.
     */
    public abstract static class JSGlobalParseFloatNode extends JSGlobalOperation {
        private final BranchProfile exponentBranch = BranchProfile.create();
        @Child protected JSTrimWhitespaceNode trimWhitespaceNode;

        public JSGlobalParseFloatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int parseFloat(int value) {
            return value;
        }

        @Specialization
        protected double parseFloat(double value, @Cached("createBinaryProfile()") ConditionProfile negativeZero) {
            if (negativeZero.profile(JSRuntime.isNegativeZero(value))) {
                return 0;
            }
            return value;
        }

        @Specialization
        protected double parseFloat(@SuppressWarnings("unused") boolean value) {
            return Double.NaN;
        }

        @Specialization(guards = "isUndefined(value)")
        protected double parseFloatUndefined(@SuppressWarnings("unused") Object value) {
            return Double.NaN;
        }

        @Specialization(guards = "isJSNull(value)")
        protected double parseFloatNull(@SuppressWarnings("unused") Object value) {
            return Double.NaN;
        }

        @Specialization
        protected double parseFloat(String value) {
            return parseFloatIntl(value);
        }

        @Specialization(guards = {"!isJSNull(value)", "!isUndefined(value)"})
        protected double parseFloat(TruffleObject value) {
            return parseFloatIntl(toString1(value));
        }

        private double parseFloatIntl(String inputString) {
            String trimmedString = trimWhitespace(inputString);
            return parseFloatIntl2(trimmedString);
        }

        @TruffleBoundary
        private double parseFloatIntl2(String trimmedString) {
            if (trimmedString.startsWith(JSRuntime.INFINITY_STRING) || trimmedString.startsWith(JSRuntime.POSITIVE_INFINITY_STRING)) {
                return Double.POSITIVE_INFINITY;
            } else if (trimmedString.startsWith(JSRuntime.NEGATIVE_INFINITY_STRING)) {
                return Double.NEGATIVE_INFINITY;
            }
            try {
                FloatParser parser = new FloatParser(trimmedString, exponentBranch);
                return parser.getResult();
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }

        protected String trimWhitespace(String s) {
            if (trimWhitespaceNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                trimWhitespaceNode = insert(JSTrimWhitespaceNode.create());
            }
            return trimWhitespaceNode.executeString(s);
        }
    }

    /**
     * Implementation of ECMAScript 5.1 15.1.2.2 parseInt() method.
     *
     */
    public abstract static class JSGlobalParseIntNode extends JSBuiltinNode {

        public JSGlobalParseIntNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToInt32Node toInt32Node;
        private final BranchProfile needsNaN = BranchProfile.create();

        protected int toInt32(Object target) {
            if (toInt32Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toInt32Node = insert(JSToInt32Node.create());
            }
            return toInt32Node.executeInt(target);
        }

        @Specialization(guards = "isUndefined(radix0)")
        protected int parseIntNoRadix(int thing, @SuppressWarnings("unused") Object radix0) {
            return thing;
        }

        @Specialization(guards = "!isUndefined(radix0)")
        protected Object parseInt(int thing, Object radix0,
                        @Cached("create()") BranchProfile needsRadixConversion) {
            int radix = toInt32(radix0);
            if (radix == 10 || radix == 0) {
                return thing;
            }
            if (radix < 2 || radix > 36) {
                needsNaN.enter();
                return Double.NaN;
            }
            needsRadixConversion.enter();
            return convertToRadix(thing, radix);
        }

        @Specialization(guards = {"hasRegularToStringInInt32Range(thing)", "isUndefined(radix0)"})
        protected int parseIntDoubleToInt(double thing, @SuppressWarnings("unused") Object radix0) {
            return (int) thing;
        }

        @Specialization(guards = {"hasRegularToString(thing)", "isUndefined(radix0)"})
        protected double parseIntNoRadix(double thing, @SuppressWarnings("unused") Object radix0) {
            return JSRuntime.truncateDouble2(thing);
        }

        // double specializations should not be used for numbers
        // that use a scientific notation when stringified
        // (parseInt(1e21) === parseInt('1e21') === 1)
        protected static boolean hasRegularToString(double value) {
            return (-1e21 < value && value <= -1e-6) || (1e-6 <= value && value < 1e21);
        }

        protected static boolean hasRegularToStringInInt32Range(double value) {
            return (Integer.MIN_VALUE - 1.0 < value && value <= -1) || (value == 0) || (1e-6 <= value && value < Integer.MAX_VALUE + 1.0);
        }

        @Specialization(guards = "hasRegularToString(thing)")
        protected double parseInt(double thing, Object radix0,
                        @Cached("create()") BranchProfile needsRadixConversion) {
            int radix = toInt32(radix0);
            if (radix == 0) {
                radix = 10;
            } else if (radix < 2 || radix > 36) {
                needsNaN.enter();
                return Double.NaN;
            }
            double truncated = JSRuntime.truncateDouble2(thing);
            if (radix == 10) {
                return truncated;
            } else {
                needsRadixConversion.enter();
                return convertToRadix(truncated, radix);
            }
        }

        @Specialization
        protected Object parseIntGeneric(Object thing, Object radix0,
                        @Cached("create()") JSToStringNode toStringNode,
                        @Cached("create()") JSTrimWhitespaceNode trimWhitespaceNode,
                        @Cached("create()") BranchProfile needsRadix16,
                        @Cached("create()") BranchProfile needsDontFitLong,
                        @Cached("create()") BranchProfile needsTrimming) {
            String inputString = trimWhitespaceNode.executeString(toStringNode.executeString(thing));
            int radix = toInt32(radix0);
            if (inputString.length() <= 0) {
                needsNaN.enter();
                return Double.NaN;
            }

            if (radix == 16 || radix == 0) {
                needsRadix16.enter();
                if (hasHexStart(inputString)) {
                    needsTrimming.enter();
                    if (inputString.charAt(0) == '0') {
                        inputString = Boundaries.substring(inputString, 2);
                    } else {
                        String sign = Boundaries.substring(inputString, 0, 1);
                        String number = Boundaries.substring(inputString, 3);
                        inputString = JSRuntime.stringConcat(sign, number);
                    }
                    radix = 16; // could be 0
                } else if (radix == 0) {
                    radix = 10;
                }
            } else if (radix < 2 || radix > 36) {
                needsNaN.enter();
                return Double.NaN;
            }

            String valueString = trimInvalidChars(inputString, radix);
            int len = valueString.length();
            if (len <= 0) {
                needsNaN.enter();
                return Double.NaN;
            }
            if ((radix <= 10 && len >= 18) || (10 < radix && radix <= 16 && len >= 15) || (radix > 16 && len >= 12)) {
                needsDontFitLong.enter();
                if (radix == 10) {
                    // parseRawDontFitLong() can produce an incorrect result
                    // due to subtle rounding errors (for radix 10) but the spec.
                    // requires exact processing for this radix
                    return parseDouble(valueString);
                } else {
                    return JSRuntime.parseRawDontFitLong(valueString, radix);
                }
            }
            try {
                return JSRuntime.parseRawFitsLong(valueString, radix);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }

        @TruffleBoundary
        private static double parseDouble(String s) {
            return Double.parseDouble(s);
        }

        private static Object convertToRadix(int thing, int radix) {
            assert radix >= 2 && radix <= 36;
            boolean negative = thing < 0;
            long value = thing;
            if (negative) {
                value = -value;
            }
            long result = 0;
            long radixVal = 1;
            while (value != 0) {
                long digit = value % 10;
                if (digit >= radix) {
                    return Double.NaN;
                }
                result += digit * radixVal;
                value /= 10;
                radixVal *= radix;
            }
            if (negative) {
                result = -result;
            }
            return JSRuntime.longToIntOrDouble(result);
        }

        private static double convertToRadix(double thing, int radix) {
            assert (radix >= 2 && radix <= 36);
            boolean negative = thing < 0;
            double value = negative ? -thing : thing;
            double result = 0;
            double radixVal = 1;
            while (value != 0) {
                double digit = (value % 10);
                if (digit >= radix) {
                    return Double.NaN;
                }
                result += digit * radixVal;
                value -= digit;
                value /= 10;
                radixVal *= radix;
            }
            return negative ? -result : result;
        }

        private static boolean hasHexStart(String inputString) {
            int length = inputString.length();
            if (length >= 2 && inputString.charAt(0) == '0') {
                char c1 = inputString.charAt(1);
                return (c1 == 'x' || c1 == 'X');
            } else if (length >= 3 && inputString.charAt(1) == '0') {
                char c0 = inputString.charAt(0);
                if (c0 == '-' || c0 == '+') {
                    char c2 = inputString.charAt(2);
                    return (c2 == 'x' || c2 == 'X');
                }
            }
            return false;
        }

        @TruffleBoundary
        private static String trimInvalidChars(String thing, int radix) {
            return thing.substring(0, validStringLength(thing, radix));
        }

        private static int validStringLength(String thing, int radix) {
            boolean hasSign = false;
            int pos = 0;
            if (!thing.isEmpty()) {
                char c = thing.charAt(0);
                if (c == '+' || c == '-') {
                    hasSign = true;
                    pos++;
                }
            }
            while (pos < thing.length()) {
                char c = thing.charAt(pos);
                if (JSRuntime.valueInRadix(c, radix) == -1) {
                    break;
                }
                pos++;
            }
            if (pos == 1 && hasSign) {
                pos = 0; // sign only
            }
            return pos;
        }
    }

    /**
     * Implementation of ECMAScript 5.1 15.1.3.3 encodeURI() and of ECMAScript 5.1 15.1.3.4
     * encodeURIComponent().
     *
     */
    public abstract static class JSGlobalEncodeURINode extends JSGlobalOperation {

        private final JSURLEncoder encoder;

        public JSGlobalEncodeURINode(JSContext context, JSBuiltin builtin, boolean isSpecial) {
            super(context, builtin);
            this.encoder = new JSURLEncoder(isSpecial);
        }

        @Specialization
        protected String encodeURI(Object value) {
            return encoder.encode(toString1(value));
        }
    }

    /**
     * Implementation of ECMAScript 5.1 15.1.3.1 decodeURI() and of ECMAScript 5.1 15.1.3.2
     * decodeURIComponent().
     *
     */
    public abstract static class JSGlobalDecodeURINode extends JSGlobalOperation {

        private final JSURLDecoder decoder;

        public JSGlobalDecodeURINode(JSContext context, JSBuiltin builtin, boolean isSpecial) {
            super(context, builtin);
            this.decoder = new JSURLDecoder(isSpecial);
        }

        @Specialization
        protected String decodeURI(Object value) {
            return decoder.decode(toString1(value));
        }
    }

    /**
     * This node is used only for indirect calls to eval. Direct calls are handled by
     * {@link EvalNode}.
     */
    public abstract static class JSGlobalIndirectEvalNode extends JSBuiltinNode {
        @Child private RealmNode realmNode;

        public JSGlobalIndirectEvalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.realmNode = RealmNode.create(context);
        }

        @Specialization(guards = "isString(source)")
        protected Object indirectEvalString(VirtualFrame frame, Object source) {
            JSRealm realm = realmNode.execute(frame);
            return indirectEvalImpl(realm, source);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private Object indirectEvalImpl(JSRealm realm, Object source) {
            String sourceText = source.toString();
            String sourceName = null;
            if (getContext().isOptionV8CompatibilityMode()) {
                sourceName = EvalNode.findAndFormatEvalOrigin(null);
            }
            if (sourceName == null) {
                sourceName = Evaluator.EVAL_SOURCE_NAME;
            }
            return getContext().getEvaluator().evaluate(realm, this, Source.newBuilder(sourceText).name(sourceName).language(AbstractJavaScriptLanguage.ID).build());
        }

        @Specialization(guards = "!isString(source)")
        protected Object indirectEval(Object source) {
            return source;
        }
    }

    /**
     * Implementation of ECMAScript 5.1 B.2.1 escape() method and of ECMAScript 5.1 B.2.2 unescape()
     * method.
     *
     */
    public abstract static class JSGlobalUnEscapeNode extends JSGlobalOperation {
        private final boolean unescape;

        public JSGlobalUnEscapeNode(JSContext context, JSBuiltin builtin, boolean unescape) {
            super(context, builtin);
            this.unescape = unescape;
        }

        @Specialization
        protected String escape(Object value) {
            String s = toString1(value);
            return unescape ? StringEscape.unescape(s) : StringEscape.escape(s);
        }
    }

    /**
     * Non-standard print()/printErr() method to write to the console.
     */
    public abstract static class JSGlobalPrintNode extends JSGlobalOperation {

        private final ConditionProfile argumentsCount = ConditionProfile.createBinaryProfile();
        private final boolean useErr;

        public JSGlobalPrintNode(JSContext context, JSBuiltin builtin, boolean useErr) {
            super(context, builtin);
            this.useErr = useErr;
        }

        @Specialization
        protected Object print(Object[] arguments) {
            // without a StringBuilder, synchronization fails testnashorn JDK-8041998.js
            StringBuilder builder = new StringBuilder();
            if (argumentsCount.profile(arguments.length == 1)) {
                Boundaries.builderAppend(builder, toString1(arguments[0]));
            } else {
                for (int i = 0; i < arguments.length; i++) {
                    if (i != 0) {
                        Boundaries.builderAppend(builder, ' ');
                    }
                    Boundaries.builderAppend(builder, toString1(arguments[i]));
                }
            }
            return printIntl(builder);
        }

        @TruffleBoundary
        private Object printIntl(StringBuilder builder) {
            builder.append(JSRuntime.LINE_SEPARATOR);
            JSRealm realm = getContext().getRealm();
            PrintWriter writer = useErr ? realm.getErrorWriter() : realm.getOutputWriter();
            writer.print(builder.toString());
            writer.flush();
            return Undefined.instance;
        }
    }

    /**
     * Inspired by jdk.nashorn.internal.runtime.Context.load(...).
     */
    @ImportStatic(value = JSInteropUtil.class)
    public abstract static class JSGlobalLoadNode extends JSLoadOperation {
        // nashorn load pseudo URL prefixes
        private static final String LOAD_CLASSPATH = "classpath:";
        private static final String LOAD_FX = "fx:";
        private static final String LOAD_NASHORN = "nashorn:";
        // nashorn default paths
        private static final String RESOURCES_PATH = "resources/";
        private static final String FX_RESOURCES_PATH = "resources/fx/";
        private static final String NASHORN_BASE_PATH = "jdk/nashorn/internal/runtime/";
        private static final String NASHORN_PARSER_JS = "nashorn:parser.js";
        private static final String NASHORN_MOZILLA_COMPAT_JS = "nashorn:mozilla_compat.js";

        private static final String EVAL_OBJ_FILE_NAME = "name";
        private static final String EVAL_OBJ_SOURCE = "script";

        @Child private RealmNode realmNode;

        public JSGlobalLoadNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.realmNode = RealmNode.create(context);
        }

        @Specialization
        protected Object loadString(VirtualFrame frame, String path) {
            Source source = sourceFromPath(path);
            return runImpl(realmNode.execute(frame), source);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private Source sourceFromPath(String path) {
            Source source = null;
            if (path.indexOf(':') != -1) {
                source = sourceFromURI(path);
                if (source != null) {
                    return source;
                }
            }

            File file = resolveRelativeFilePath(path);
            if (file.isFile()) {
                source = sourceFromFileName(file.getPath());
            }

            if (source == null) {
                throw cannotLoadScript(path);
            }
            return source;
        }

        @Specialization
        protected Object loadURL(VirtualFrame frame, URL url) {
            return runImpl(realmNode.execute(frame), sourceFromURL(url));
        }

        @Specialization
        protected Object loadFile(VirtualFrame frame, File file) {
            return runImpl(realmNode.execute(frame), sourceFromFileName(fileGetPath(file)));
        }

        @Specialization(guards = "isForeignObject(scriptObj)")
        protected Object loadTruffleObject(VirtualFrame frame, TruffleObject scriptObj,
                        @Cached("createUnbox()") Node unboxNode) {
            TruffleLanguage.Env env = realmNode.execute(frame).getEnv();
            if (env.isHostObject(scriptObj)) {
                Object hostObject = env.asHostObject(scriptObj);
                if (hostObject instanceof File) {
                    return loadFile(frame, (File) hostObject);
                } else if (hostObject instanceof URL) {
                    return loadURL(frame, (URL) hostObject);
                }
            }
            Object unboxed = JSInteropNodeUtil.unbox(scriptObj, unboxNode);
            String stringPath = toString1(unboxed);
            return loadString(frame, stringPath);
        }

        @Specialization(guards = "isJSObject(scriptObj)")
        protected Object loadScriptObj(VirtualFrame frame, DynamicObject scriptObj,
                        @Cached("create()") JSToStringNode sourceToStringNode) {
            JSRealm realm = realmNode.execute(frame);
            if (JSObject.hasProperty(scriptObj, EVAL_OBJ_FILE_NAME) && JSObject.hasProperty(scriptObj, EVAL_OBJ_SOURCE)) {
                Object fileNameObj = JSObject.get(scriptObj, EVAL_OBJ_FILE_NAME);
                Object sourceObj = JSObject.get(scriptObj, EVAL_OBJ_SOURCE);
                return evalImpl(realm, toString1(fileNameObj), sourceToStringNode.executeString(sourceObj));
            } else {
                throw cannotLoadScript(scriptObj);
            }
        }

        @Specialization
        protected Object loadMap(VirtualFrame frame, Map<?, ?> map,
                        @Cached("create()") JSToStringNode sourceToStringNode) {
            JSRealm realm = realmNode.execute(frame);
            if (Boundaries.mapContainsKey(map, EVAL_OBJ_FILE_NAME) && Boundaries.mapContainsKey(map, EVAL_OBJ_SOURCE)) {
                Object fileNameObj = Boundaries.mapGet(map, EVAL_OBJ_FILE_NAME);
                Object sourceObj = Boundaries.mapGet(map, EVAL_OBJ_SOURCE);
                return evalImpl(realm, toString1(fileNameObj), sourceToStringNode.executeString(sourceObj));
            } else {
                throw cannotLoadScript(map);
            }
        }

        @Specialization(guards = "isFallback(fileName)")
        protected Object loadConvertToString(VirtualFrame frame, Object fileName) {
            return loadString(frame, toString1(fileName));
        }

        protected static boolean isFallback(Object value) {
            return !(JSGuards.isString(value) || value instanceof URL || value instanceof File || value instanceof Map || JSGuards.isForeignObject(value) || JSGuards.isJSObject(value));
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private Object evalImpl(JSRealm realm, String fileName, String source) {
            return loadStringImpl(getContext(), fileName, source).run(realm);
        }

        private Source sourceFromURI(String resource) {
            if (resource.startsWith(LOAD_CLASSPATH) || resource.startsWith(LOAD_NASHORN) || resource.startsWith(LOAD_FX)) {
                if (JSTruffleOptions.SubstrateVM) {
                    return null;
                } else {
                    return sourceFromResourceURL(resource);
                }
            }

            try {
                URL url = new URL(resource);
                return sourceFromURL(url);
            } catch (MalformedURLException e) {
            }
            return null;
        }

        private Source sourceFromResourceURL(String resource) {
            assert !JSTruffleOptions.SubstrateVM;
            InputStream stream = null;
            if (resource.startsWith(LOAD_CLASSPATH)) {
                stream = ClassLoader.getSystemResourceAsStream(resource.substring(LOAD_CLASSPATH.length()));
            } else if (getContext().isOptionNashornCompatibilityMode() && resource.startsWith(LOAD_NASHORN) && (resource.equals(NASHORN_PARSER_JS) || resource.equals(NASHORN_MOZILLA_COMPAT_JS))) {
                stream = JSContext.class.getResourceAsStream(RESOURCES_PATH + resource.substring(LOAD_NASHORN.length()));
            } else if (resource.startsWith(LOAD_FX)) {
                stream = ClassLoader.getSystemResourceAsStream(NASHORN_BASE_PATH + FX_RESOURCES_PATH + resource.substring(LOAD_FX.length()));
            }
            if (stream != null) {
                try {
                    return Source.newBuilder(new InputStreamReader(stream, StandardCharsets.UTF_8)).name(resource).language(AbstractJavaScriptLanguage.ID).build();
                } catch (IOException e) {
                }
            }
            return null;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static JSException cannotLoadScript(Object script) {
            return Errors.createTypeError("Cannot load script: " + JSRuntime.safeToString(script));
        }
    }

    /**
     * Implementation of non-standard method loadWithNewGlobal() as defined by Nashorn.
     *
     */
    public abstract static class JSGlobalLoadWithNewGlobalNode extends JSLoadOperation {

        public JSGlobalLoadWithNewGlobalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object loadURL(URL from, @SuppressWarnings("unused") Object[] args) {
            return runImpl(createRealm(), sourceFromURL(from));
        }

        @Specialization(guards = "!isJSObject(from)")
        protected Object load(Object from, @SuppressWarnings("unused") Object[] args) {
            String name = toString1(from);
            URL url = toURL(name);
            if (url == null) {
                return fail(name);
            }
            return runImpl(createRealm(), sourceFromURL(url));
        }

        @TruffleBoundary
        private static Object fail(String name) {
            throw Errors.createTypeError("Cannot load script from " + name);
        }

        @TruffleBoundary
        private static URL toURL(String urlStr) {
            try {
                return new URL(urlStr);
            } catch (MalformedURLException e) {
                return null;
            }
        }

        @Specialization
        protected Object load(DynamicObject from, Object[] args,
                        @Cached("create()") JSToStringNode toString2Node) {
            String name = toString1(JSObject.get(from, "name"));
            String script = toString2Node.executeString(JSObject.get(from, "script"));
            return loadIntl(name, script, args);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private Object loadIntl(String name, String script, Object[] args) {
            JSRealm childRealm = createRealm();
            ScriptNode scriptNode = loadStringImpl(childRealm.getContext(), name, script);
            DynamicObject globalObject = childRealm.getGlobalObject();
            if (args.length > 0) {
                DynamicObject argObj = JSArgumentsObject.createStrict(getContext(), childRealm, args);
                JSRuntime.createDataProperty(globalObject, JSFunction.ARGUMENTS, argObj);
            }
            return scriptNode.run(JSArguments.create(globalObject, JSFunction.create(childRealm, scriptNode.getFunctionData()), args));
        }

        @TruffleBoundary
        private JSRealm createRealm() {
            return getContext().getRealm().createChildRealm();
        }
    }

    /**
     * Non-standard global exit() and quit() functions to provide compatibility with Nashorn (both)
     * and V8 (only quit).
     */
    public abstract static class JSGlobalExitNode extends JSBuiltinNode {

        public JSGlobalExitNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object exit(Object[] arg,
                        @Cached("create()") JSToNumberNode toNumberNode) {
            int exitCode = arg.length == 0 ? 0 : (int) JSRuntime.toInteger(toNumberNode.executeNumber(arg[0]));
            throw new ExitException(exitCode, this);
        }
    }

    /**
     * Non-standard readline() and readLine() to provide compatibility with V8 and Nashorn,
     * respectively.
     */
    public abstract static class JSGlobalReadLineNode extends JSGlobalOperation {

        public JSGlobalReadLineNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String readLine(Object prompt) {
            String promptString = null;
            if (prompt != Undefined.instance) {
                promptString = toString1(prompt);
            }
            return doReadLine(promptString);
        }

        @TruffleBoundary
        private static String doReadLine(String promptString) {
            if (promptString != null) {
                System.out.println(promptString);
            }
            try {
                final BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                return inReader.readLine();
            } catch (Exception ex) {
                throw Errors.createError(ex.getMessage());
            }
        }
    }

    static File getFileFromArgument(Object arg, TruffleLanguage.Env env) {
        CompilerAsserts.neverPartOfCompilation();
        String path;
        File file = null;
        if (JSRuntime.isString(arg)) {
            path = arg.toString();
        } else if (!JSTruffleOptions.SubstrateVM && env.isHostObject(arg) && env.asHostObject(arg) instanceof File) {
            file = (File) env.asHostObject(arg);
            path = file.getPath();
        } else if (JSTruffleOptions.NashornJavaInterop && arg instanceof File) {
            file = (File) arg;
            path = file.getPath();
        } else {
            path = JSRuntime.toString(arg);
        }

        if (file == null) {
            file = resolveRelativeFilePath(JSRuntime.toString(path));
        }

        if (!file.isFile()) {
            throw Errors.createTypeError("Not a file: " + path);
        }
        return file;
    }

    /**
     * Non-standard read() and readFully() to provide compatibility with V8 and Nashorn,
     * respectively.
     */
    public abstract static class JSGlobalReadFullyNode extends JSBuiltinNode {
        private static final int BUFFER_SIZE = 2048;

        public JSGlobalReadFullyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected String read(Object fileParam) {
            File file = getFileFromArgument(fileParam, getContext().getRealm().getEnv());

            try {
                return readImpl(file);
            } catch (Exception ex) {
                throw JSException.create(JSErrorType.Error, ex.getMessage(), ex, this);
            }
        }

        private static String readImpl(File file) throws IOException {
            final StringBuilder sb = new StringBuilder();
            final char[] arr = new char[BUFFER_SIZE];
            try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
                int numChars;
                while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
                    sb.append(arr, 0, numChars);
                }
            }
            return sb.toString();
        }
    }

    /**
     * Non-standard readbuffer() to provide compatibility with V8.
     */
    public abstract static class JSGlobalReadBufferNode extends JSBuiltinNode {

        public JSGlobalReadBufferNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final DynamicObject readbuffer(Object fileParam) {
            File file = getFileFromArgument(fileParam, getContext().getRealm().getEnv());

            try {
                final byte[] bytes = Files.readAllBytes(file.toPath());
                final DynamicObject arrayBuffer;
                if (getContext().isOptionDirectByteBuffer()) {
                    ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
                    buffer.put(bytes).rewind();
                    arrayBuffer = JSArrayBuffer.createDirectArrayBuffer(getContext(), buffer);
                } else {
                    arrayBuffer = JSArrayBuffer.createArrayBuffer(getContext(), bytes);
                }
                return arrayBuffer;
            } catch (Exception ex) {
                throw JSException.create(JSErrorType.Error, ex.getMessage(), ex, this);
            }
        }
    }
}
