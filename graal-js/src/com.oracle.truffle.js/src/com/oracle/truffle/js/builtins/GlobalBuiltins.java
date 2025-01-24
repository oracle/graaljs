/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.StringTokenizer;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.io.TruffleProcessBuilder;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.GlobalNashornExtensionParseToJSONNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.GlobalScriptingEXECNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalDecodeURINodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalEncodeURINodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalExitNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalImportScriptEngineGlobalBindingsNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalIndirectEvalNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalIsFiniteNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalIsNaNNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalLoadNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalLoadWithNewGlobalNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalParseFloatNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalParseIntNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalPostMessageNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalPrintNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalReadBufferNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalReadFullyNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalReadLineNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalUnEscapeNodeGen;
import com.oracle.truffle.js.builtins.helper.FloatParserNode;
import com.oracle.truffle.js.builtins.helper.StringEscape;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSTrimWhitespaceNode;
import com.oracle.truffle.js.nodes.function.EvalNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSLoadNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSConsoleUtil;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.SuppressFBWarnings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.WorkerAgent;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSURLDecoder;
import com.oracle.truffle.js.runtime.builtins.JSURLEncoder;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.StringBuilderProfile;

/**
 * Contains builtins for the global object.
 */
public class GlobalBuiltins extends JSBuiltinsContainer.SwitchEnum<GlobalBuiltins.Global> {
    public static final JSBuiltinsContainer GLOBAL_FUNCTIONS = new GlobalBuiltins();
    public static final JSBuiltinsContainer GLOBAL_SHELL = new GlobalShellBuiltins();
    public static final JSBuiltinsContainer GLOBAL_NASHORN_EXTENSIONS = new GlobalNashornScriptingBuiltins();
    public static final JSBuiltinsContainer GLOBAL_PRINT = new GlobalPrintBuiltins();
    public static final JSBuiltinsContainer GLOBAL_LOAD = new GlobalLoadBuiltins();
    public static final JSBuiltinsContainer GLOBAL_WORKER = new GlobalWorkerBuiltins();

    protected GlobalBuiltins() {
        super(Global.class);
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
        unescape(1);

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
        }
        return null;
    }

    /**
     * Built-ins for js shell (for compatibility with e.g. d8).
     */
    public static final class GlobalShellBuiltins extends JSBuiltinsContainer.SwitchEnum<GlobalShellBuiltins.GlobalShell> {
        protected GlobalShellBuiltins() {
            super(GlobalShell.class);
        }

        public enum GlobalShell implements BuiltinEnum<GlobalShell> {
            quit(1),
            readline(1),
            read(1),
            readbuffer(1);

            private final int length;

            GlobalShell(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, GlobalShell builtinEnum) {
            switch (builtinEnum) {
                case quit:
                    return JSGlobalExitNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                case readline:
                    return JSGlobalReadLineNodeGen.create(context, builtin, false, new JavaScriptNode[]{JSConstantNode.createUndefined()});
                case read:
                    return JSGlobalReadFullyNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                case readbuffer:
                    return JSGlobalReadBufferNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            }
            return null;
        }
    }

    /**
     * Built-ins for print.
     */
    public static final class GlobalPrintBuiltins extends JSBuiltinsContainer.SwitchEnum<GlobalPrintBuiltins.GlobalPrint> {
        protected GlobalPrintBuiltins() {
            super(GlobalPrint.class);
        }

        public enum GlobalPrint implements BuiltinEnum<GlobalPrint> {
            print(1),
            printErr(1);

            private final int length;

            GlobalPrint(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, GlobalPrint builtinEnum) {
            boolean noNewline = context.getLanguageOptions().printNoNewline();
            switch (builtinEnum) {
                case print:
                    return JSGlobalPrintNodeGen.create(context, builtin, false, noNewline, args().varArgs().createArgumentNodes(context));
                case printErr:
                    return JSGlobalPrintNodeGen.create(context, builtin, true, noNewline, args().varArgs().createArgumentNodes(context));
            }
            return null;
        }
    }

    /**
     * Built-ins for load.
     */
    public static final class GlobalLoadBuiltins extends JSBuiltinsContainer.SwitchEnum<GlobalLoadBuiltins.GlobalLoad> {
        protected GlobalLoadBuiltins() {
            super(GlobalLoad.class);
        }

        public enum GlobalLoad implements BuiltinEnum<GlobalLoad> {
            load(1),
            loadWithNewGlobal(1);

            private final int length;

            GlobalLoad(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, GlobalLoad builtinEnum) {
            switch (builtinEnum) {
                case load:
                    return JSGlobalLoadNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context));
                case loadWithNewGlobal:
                    return JSGlobalLoadWithNewGlobalNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context));
            }
            return null;
        }
    }

    public static final class GlobalNashornScriptingBuiltins extends JSBuiltinsContainer.SwitchEnum<GlobalNashornScriptingBuiltins.GlobalNashornScripting> {
        protected GlobalNashornScriptingBuiltins() {
            super(GlobalNashornScripting.class);
        }

        /**
         * Manually added in initGlobalNashornExtensions or initGlobalScriptingExtensions.
         *
         * @see JSRealm
         */
        public enum GlobalNashornScripting implements BuiltinEnum<GlobalNashornScripting> {
            exit(1),
            quit(1),
            readLine(1),
            readFully(1),
            exec(1), // $EXEC
            parseToJSON(3),
            importScriptEngineGlobalBindings(1);

            private final int length;

            GlobalNashornScripting(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, GlobalNashornScripting builtinEnum) {
            switch (builtinEnum) {
                case exit:
                case quit:
                    return JSGlobalExitNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                case readLine:
                    return JSGlobalReadLineNodeGen.create(context, builtin, true, args().fixedArgs(1).createArgumentNodes(context));
                case readFully:
                    return JSGlobalReadFullyNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                case parseToJSON:
                    return GlobalNashornExtensionParseToJSONNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
                case exec:
                    return GlobalScriptingEXECNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
                case importScriptEngineGlobalBindings:
                    return JSGlobalImportScriptEngineGlobalBindingsNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context));
            }
            return null;
        }
    }

    /**
     * Built-ins available in a worker only.
     */
    public static final class GlobalWorkerBuiltins extends JSBuiltinsContainer.SwitchEnum<GlobalWorkerBuiltins.GlobalWorker> {
        protected GlobalWorkerBuiltins() {
            super(GlobalWorker.class);
        }

        public enum GlobalWorker implements BuiltinEnum<GlobalWorker> {
            postMessage(1);

            private final int length;

            GlobalWorker(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, GlobalWorker builtinEnum) {
            switch (builtinEnum) {
                case postMessage:
                    return JSGlobalPostMessageNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
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
        protected TruffleString parseToJSON(Object code0, Object name0, Object location0) {
            String code = JSRuntime.toJavaString(code0);
            String name = name0 == Undefined.instance ? "<unknown>" : JSRuntime.toJavaString(name0);
            boolean location = JSRuntime.toBoolean(location0);
            return Strings.fromJavaString(getContext().getEvaluator().parseToJSON(getContext(), code, name, location));
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
            String cmdStr = JSRuntime.toJavaString(cmd);
            String inputStr = input != Undefined.instance ? JSRuntime.toJavaString(input) : null;
            return execIntl(cmdStr, inputStr);
        }

        @TruffleBoundary
        private Object execIntl(String cmd, String input) {
            JSRealm realm = getRealm();
            TruffleLanguage.Env env = realm.getEnv();
            JSDynamicObject globalObj = realm.getGlobalObject();
            StringTokenizer tok = new StringTokenizer(cmd);
            String[] cmds = new String[tok.countTokens()];
            for (int i = 0; tok.hasMoreTokens(); i++) {
                cmds[i] = tok.nextToken();
            }

            int exitCode = 0;
            String outStr = "";
            String errStr = "";
            Process process = null;
            try {
                TruffleProcessBuilder builder = env.newProcessBuilder(cmds);

                Object envObj = JSObject.get(globalObj, Strings.DOLLAR_ENV);
                if (JSGuards.isJSObject(envObj)) {
                    JSDynamicObject dynEnvObj = (JSDynamicObject) envObj;
                    Object pwd = JSObject.get(dynEnvObj, Strings.CAPS_PWD);
                    if (pwd != Undefined.instance) {
                        builder.directory(env.getPublicTruffleFile(JSRuntime.toJavaString(pwd)));
                    }

                    builder.clearEnvironment(true);
                    for (TruffleString key : JSObject.enumerableOwnNames(dynEnvObj)) {
                        builder.environment(Strings.toJavaString(key), JSRuntime.toJavaString(JSObject.get(dynEnvObj, key)));
                    }
                }

                ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
                ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

                builder.redirectOutput(builder.createRedirectToStream(outBuffer));
                builder.redirectError(builder.createRedirectToStream(errBuffer));

                process = builder.start();

                try (OutputStreamWriter outputStream = new OutputStreamWriter(process.getOutputStream(), realm.getCharset())) {
                    if (input != null) {
                        outputStream.write(input, 0, input.length());
                    }
                } catch (IOException ex) {
                }

                exitCode = process.waitFor();

                outStr = outBuffer.toString();
                errStr = errBuffer.toString();
            } catch (InterruptedException e) {
                if (process.isAlive()) {
                    process.destroy();
                }
                if (exitCode == 0) {
                    exitCode = process.exitValue();
                }
            } catch (IOException | SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
                throw Errors.createError(e.getMessage());
            }

            TruffleString outStrTS = Strings.fromJavaString(outStr);
            JSObject.set(globalObj, Strings.$_OUT, outStrTS);
            JSObject.set(globalObj, Strings.$_ERR, Strings.fromJavaString(errStr));
            JSObject.set(globalObj, Strings.$_EXIT, exitCode);

            return outStrTS;
        }
    }

    private abstract static class JSGlobalOperation extends JSBuiltinNode {

        JSGlobalOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToStringNode toString1Node;

        protected final TruffleString toString1(Object target) {
            if (toString1Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString1Node = insert(JSToStringNode.create());
            }
            return toString1Node.executeString(target);
        }
    }

    public abstract static class JSFileLoadingOperation extends JSGlobalOperation {

        protected JSFileLoadingOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        protected Source sourceFromPath(String path, JSRealm realm) {
            Source source = null;
            try {
                TruffleFile file = resolveRelativeFilePath(path, realm.getEnv());
                if (file.isRegularFile()) {
                    source = sourceFromTruffleFile(file);
                }
            } catch (SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
                throw Errors.createErrorFromException(e);
            }
            if (source == null) {
                throw cannotLoadScript(path);
            }
            return source;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static JSException cannotLoadScript(Object script) {
            return Errors.createTypeError("Cannot load script: " + JSRuntime.safeToString(script));
        }

        @TruffleBoundary
        protected static Source sourceFromTruffleFile(TruffleFile file) {
            try {
                return Source.newBuilder(JavaScriptLanguage.ID, file).build();
            } catch (IOException | SecurityException e) {
                throw Errors.createErrorFromException(e);
            }
        }

    }

    /**
     * @throws SecurityException
     */
    public static TruffleFile resolveRelativeFilePath(String path, TruffleLanguage.Env env) {
        CompilerAsserts.neverPartOfCompilation();
        TruffleFile file = env.getPublicTruffleFile(path);
        if (!file.isAbsolute() && !file.exists()) {
            TruffleFile f = tryResolveCallerRelativeFilePath(path, env);
            if (f != null) {
                return f;
            }
        }
        return file;
    }

    private static TruffleFile tryResolveCallerRelativeFilePath(String path, TruffleLanguage.Env env) {
        CompilerAsserts.neverPartOfCompilation();
        Source callerSource = JSFunction.getCallerSource();
        if (callerSource != null) {
            String callerPath = callerSource.getPath();
            if (callerPath != null) {
                TruffleFile callerFile = env.getPublicTruffleFile(callerPath);
                if (callerFile.isAbsolute()) {
                    TruffleFile file = callerFile.resolveSibling(path).normalize();
                    if (file.isRegularFile()) {
                        return file;
                    }
                }
            }
        }
        return null;
    }

    public abstract static class JSLoadOperation extends JSFileLoadingOperation {
        public JSLoadOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSLoadNode loadNode;

        protected final Object runImpl(JSRealm realm, Source source) {
            if (loadNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                loadNode = insert(JSLoadNode.create());
            }
            return loadNode.executeLoad(source, realm);
        }

        protected final ScriptNode loadStringImpl(TruffleString name, TruffleString script) {
            CompilerAsserts.neverPartOfCompilation();
            long startTime = getContext().getLanguageOptions().profileTime() ? System.nanoTime() : 0L;
            try {
                return getContext().getEvaluator().evalCompile(getContext(), Strings.toJavaString(script), Strings.toJavaString(name));
            } finally {
                if (getContext().getLanguageOptions().profileTime()) {
                    getContext().getTimeProfiler().printElapsed(startTime, "parsing " + name);
                }
            }
        }

        @TruffleBoundary
        protected final Source sourceFromURL(URL url, JSRealm realm) {
            assert getContext().isOptionNashornCompatibilityMode() || realm.getContextOptions().isLoadFromURL();
            try {
                return Source.newBuilder(JavaScriptLanguage.ID, url).name(url.getFile()).build();
            } catch (IOException | SecurityException e) {
                throw Errors.createErrorFromException(e);
            }
        }

        @TruffleBoundary
        protected static Source sourceFromFileName(String fileName, JSRealm realm) {
            try {
                return Source.newBuilder(JavaScriptLanguage.ID, realm.getEnv().getPublicTruffleFile(fileName)).name(fileName).build();
            } catch (IOException | SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
                throw Errors.createErrorFromException(e);
            }
        }

        @Override
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected Source sourceFromPath(String path, JSRealm realm) {
            Source source = null;
            JSContext ctx = getContext();
            if (path.indexOf(':') >= 2) {
                if (ctx.isOptionNashornCompatibilityMode() || realm.getContextOptions().isLoadFromURL() || realm.getContextOptions().isLoadFromClasspath()) {
                    source = sourceFromURI(path, realm);
                    if (source != null) {
                        return source;
                    }
                }
            } else {
                try {
                    TruffleFile file = resolveRelativeFilePath(path, realm.getEnv());
                    if (file.isRegularFile()) {
                        source = sourceFromTruffleFile(file);
                    }
                } catch (SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
                    throw Errors.createErrorFromException(e);
                }
            }

            if (source == null) {
                throw cannotLoadScript(path);
            }
            return source;
        }

        public static final String LOAD_CLASSPATH = "classpath:";
        public static final String LOAD_FX = "fx:";
        public static final String LOAD_NASHORN = "nashorn:";

        public static final String RESOURCES_PATH = "resources/";
        public static final String FX_RESOURCES_PATH = "resources/fx/";
        public static final String NASHORN_BASE_PATH = "jdk/nashorn/internal/runtime/";
        public static final String NASHORN_PARSER_JS = "nashorn:parser.js";
        public static final String NASHORN_MOZILLA_COMPAT_JS = "nashorn:mozilla_compat.js";

        private Source sourceFromURI(String resource, JSRealm realm) {
            CompilerAsserts.neverPartOfCompilation();
            assert resource.indexOf(':') != -1;
            if (JSConfig.SubstrateVM) {
                return null;
            }
            if ((getContext().isOptionNashornCompatibilityMode() &&
                            (resource.startsWith(LOAD_NASHORN) || resource.startsWith(LOAD_CLASSPATH) || resource.startsWith(LOAD_FX))) ||
                            (realm.getContextOptions().isLoadFromClasspath() && resource.startsWith(LOAD_CLASSPATH))) {
                return sourceFromResourceURL(resource, realm);
            }
            if (getContext().isOptionNashornCompatibilityMode() || realm.getContextOptions().isLoadFromURL()) {
                if (resource.startsWith("file:")) {
                    try {
                        TruffleLanguage.Env env = realm.getEnv();
                        TruffleFile truffleFile;
                        try {
                            URI uri = new URI(resource);
                            assert "file".equals(uri.getScheme());
                            truffleFile = env.getPublicTruffleFile(uri);
                        } catch (URISyntaxException e) {
                            // Not a valid URI, try parsing it as a path.
                            boolean windowsPath = env.getFileNameSeparator().equals("\\");
                            String path = windowsPath ? resource.replace('\\', '/') : resource;
                            // Skip to start of path ("file:///path" --> "/path")
                            int start = "file:".length();
                            if (path.startsWith("///", start)) {
                                start += 2;
                            }
                            // "/c:/path" --> "c:/path"
                            if (windowsPath && path.length() > start + 2 && path.charAt(start) == '/' && path.charAt(start + 2) == ':') {
                                start += 1;
                            }
                            path = path.substring(start);
                            truffleFile = env.getPublicTruffleFile(path);
                        }
                        return sourceFromTruffleFile(truffleFile);
                    } catch (SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
                        throw Errors.createErrorFromException(e);
                    }
                } else {
                    try {
                        URI uri = new URI(resource);
                        return sourceFromURL(uri.toURL(), realm);
                    } catch (MalformedURLException | URISyntaxException e) {
                        throw Errors.createErrorFromException(e);
                    }
                }
            }
            return null;
        }

        private Source sourceFromResourceURL(String resource, JSRealm realm) {
            CompilerAsserts.neverPartOfCompilation();
            assert getContext().isOptionNashornCompatibilityMode() || realm.getContextOptions().isLoadFromClasspath();
            InputStream stream = null;
            if (resource.startsWith(LOAD_NASHORN)) {
                if (NASHORN_PARSER_JS.equals(resource) || NASHORN_MOZILLA_COMPAT_JS.equals(resource)) {
                    stream = JSContext.class.getResourceAsStream(RESOURCES_PATH + resource.substring(LOAD_NASHORN.length()));
                }
            } else if (!JSConfig.SubstrateVM) {
                if (resource.startsWith(LOAD_CLASSPATH)) {
                    stream = ClassLoader.getSystemResourceAsStream(resource.substring(LOAD_CLASSPATH.length()));
                } else if (resource.startsWith(LOAD_FX)) {
                    stream = ClassLoader.getSystemResourceAsStream(NASHORN_BASE_PATH + FX_RESOURCES_PATH + resource.substring(LOAD_FX.length()));
                }
            }
            if (stream != null) {
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    return Source.newBuilder(JavaScriptLanguage.ID, reader, resource).build();
                } catch (IOException | SecurityException e) {
                }
            }
            return null;
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
        protected static boolean isNaNInt(@SuppressWarnings("unused") int value) {
            return false;
        }

        @Specialization
        protected static boolean isNaNDouble(double value) {
            return Double.isNaN(value);
        }

        @Specialization(guards = "!isUndefined(value)")
        protected static boolean isNaNGeneric(Object value,
                        @Cached JSToDoubleNode toDoubleNode) {
            return isNaNDouble(toDoubleNode.executeDouble(value));
        }

        @Specialization(guards = "isUndefined(value)")
        protected static boolean isNaNUndefined(@SuppressWarnings("unused") Object value) {
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
        protected static boolean isFiniteInt(@SuppressWarnings("unused") int value) {
            return true;
        }

        @Specialization
        protected static boolean isFiniteDouble(double value) {
            return !Double.isInfinite(value) && !Double.isNaN(value);
        }

        @Specialization(guards = "!isUndefined(value)")
        protected static boolean isFiniteGeneric(Object value,
                        @Cached JSToDoubleNode toDoubleNode) {
            return isFiniteDouble(toDoubleNode.executeDouble(value));
        }

        @Specialization(guards = "isUndefined(value)")
        protected static boolean isFiniteUndefined(@SuppressWarnings("unused") Object value) {
            return false;
        }
    }

    /**
     * Implementation of ECMAScript 5.1 15.1.2.3 parseFloat() method.
     */
    public abstract static class JSGlobalParseFloatNode extends JSGlobalOperation {
        @Child protected JSTrimWhitespaceNode trimWhitespaceNode;
        @Child protected TruffleString.RegionEqualByteIndexNode regionEqualsNode;
        @Child protected FloatParserNode floatParserNode;

        private static final int INFINITY_LENGTH = "Infinity".length();

        public JSGlobalParseFloatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int parseFloatInt(int value) {
            return value;
        }

        @Specialization
        protected long parseFloatLong(long value) {
            return value;
        }

        @Specialization
        protected double parseFloatDouble(double value,
                        @Cached InlinedConditionProfile negativeZero) {
            if (negativeZero.profile(this, JSRuntime.isNegativeZero(value))) {
                return 0;
            }
            return value;
        }

        @Specialization
        protected double parseFloatBoolean(@SuppressWarnings("unused") boolean value) {
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
        protected double parseFloat(TruffleString value) {
            return parseFloatIntl(value);
        }

        @Specialization(guards = {"!isJSNull(value)", "!isUndefined(value)", "!isString(value)"})
        protected double parseFloat(TruffleObject value) {
            return parseFloatIntl(toString1(value));
        }

        private double parseFloatIntl(TruffleString inputString) {
            TruffleString trimmedString = trimWhitespace(inputString);
            return parseFloatIntl2(trimmedString);
        }

        private double parseFloatIntl2(TruffleString trimmedString) {
            int trimmedLength = Strings.length(trimmedString);
            if (trimmedLength >= INFINITY_LENGTH) {
                if (regionEqualsNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    regionEqualsNode = insert(TruffleString.RegionEqualByteIndexNode.create());
                }
                if (Strings.startsWith(regionEqualsNode, trimmedString, Strings.INFINITY) || Strings.startsWith(regionEqualsNode, trimmedString, Strings.POSITIVE_INFINITY)) {
                    return Double.POSITIVE_INFINITY;
                } else if (Strings.startsWith(regionEqualsNode, trimmedString, Strings.NEGATIVE_INFINITY)) {
                    return Double.NEGATIVE_INFINITY;
                }
            }
            if (floatParserNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                floatParserNode = insert(FloatParserNode.create());
            }
            return floatParserNode.parse(trimmedString);
        }

        protected TruffleString trimWhitespace(TruffleString s) {
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

        @Specialization(guards = "isUndefined(radix0)")
        protected int parseIntNoRadix(int value, @SuppressWarnings("unused") Object radix0) {
            return value;
        }

        @Specialization(guards = "!isUndefined(radix0)")
        protected Object parseIntInt(int value, Object radix0,
                        @Cached @Shared JSToInt32Node toInt32,
                        @Cached @Shared InlinedBranchProfile needsRadixConversion,
                        @Cached @Shared InlinedBranchProfile needsNaN) {
            int radix = toInt32.executeInt(radix0);
            if (radix == 10 || radix == 0) {
                return value;
            }
            if (radix < 2 || radix > 36) {
                needsNaN.enter(this);
                return Double.NaN;
            }
            needsRadixConversion.enter(this);
            return convertToRadix(value, radix);
        }

        @Specialization(guards = {"hasRegularToStringInInt32Range(value)", "isUndefined(radix0)"})
        protected int parseIntDoubleToInt(double value, @SuppressWarnings("unused") Object radix0) {
            return (int) value;
        }

        @Specialization(guards = {"hasRegularToString(value)", "isUndefined(radix0)"})
        protected double parseIntDoubleNoRadix(double value, @SuppressWarnings("unused") Object radix0) {
            return JSRuntime.truncateDouble(value);
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

        @Specialization(guards = "hasRegularToString(value)")
        protected double parseIntDouble(double value, Object radix0,
                        @Cached @Shared JSToInt32Node toInt32,
                        @Cached @Shared InlinedBranchProfile needsRadixConversion,
                        @Cached @Shared InlinedBranchProfile needsNaN) {
            int radix = toInt32.executeInt(radix0);
            if (radix == 0) {
                radix = 10;
            } else if (radix < 2 || radix > 36) {
                needsNaN.enter(this);
                return Double.NaN;
            }
            double truncated = JSRuntime.truncateDouble(value);
            if (radix == 10) {
                return truncated;
            } else {
                needsRadixConversion.enter(this);
                return convertToRadix(truncated, radix);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"radix == 10", "stringLength(string) < 15"})
        protected Object parseIntStringInt10(TruffleString string, int radix,
                        @Cached @Shared TruffleString.ReadCharUTF16Node readRawNode,
                        @Cached @Shared InlinedBranchProfile needsRadix16,
                        @Cached @Shared InlinedBranchProfile needsDontFitLong) {
            assert isShortStringInt10(string, radix);

            int pos = 0;
            int lastIdx = Strings.length(string);
            boolean negate = false;

            if (lastIdx == 0) { // empty string
                return Double.NaN;
            }

            char firstChar = Strings.charAt(readRawNode, string, pos);
            if (!JSRuntime.isAsciiDigit(firstChar)) {
                if (JSRuntime.isWhiteSpaceOrLineTerminator(firstChar)) {
                    pos = JSRuntime.firstNonWhitespaceIndex(string, readRawNode);
                    if (Strings.length(string) <= pos) {
                        return Double.NaN;
                    }
                    firstChar = Strings.charAt(readRawNode, string, pos);
                }
                if (firstChar == '-') {
                    pos++;
                    negate = true;
                } else if (firstChar == '+') {
                    pos++;
                }
                if (pos >= lastIdx) {
                    return Double.NaN;
                }
            }

            int firstPos = pos;
            long value = 0;
            while (pos < lastIdx) {
                char c = Strings.charAt(readRawNode, string, pos);
                int cval = JSRuntime.valueInRadix10(c);
                if (cval < 0) {
                    if (pos != firstPos) {
                        break;
                    } else {
                        return Double.NaN;
                    }
                }
                value *= 10;
                value += cval;
                pos++;
            }

            if (value == 0 && negate) {
                return -0.0; // long case below cannot handle negative zero
            }

            assert value >= 0;
            long signedValue = negate ? -value : value;

            if (value <= Integer.MAX_VALUE) {
                return (int) signedValue;
            } else {
                return (double) signedValue;
            }
        }

        protected static boolean isShortStringInt10(Object input, Object radix) {
            return input instanceof TruffleString inputStr && Strings.length(inputStr) < 15 && radix instanceof Integer radixInt && radixInt == 10;
        }

        @Specialization(guards = "!isShortStringInt10(input, radix0)")
        protected static Object parseIntGeneric(Object input, Object radix0,
                        @Bind Node node,
                        @Cached JSToStringNode toStringNode,
                        @Cached @Shared JSToInt32Node toInt32,
                        @Cached @Shared InlinedBranchProfile needsNaN,
                        @Cached @Shared InlinedBranchProfile needsRadix16,
                        @Cached @Shared InlinedBranchProfile needsDontFitLong,
                        @Cached @Shared TruffleString.ReadCharUTF16Node readRawNode,
                        @Cached TruffleString.SubstringByteIndexNode substringNode) {
            TruffleString inputStr = toStringNode.executeString(input);

            int firstIdx = JSRuntime.firstNonWhitespaceIndex(inputStr, readRawNode);
            int lastIdx = JSRuntime.lastNonWhitespaceIndex(inputStr, readRawNode) + 1;

            int radix = toInt32.executeInt(radix0);
            if (lastIdx <= firstIdx) {
                needsNaN.enter(node);
                return Double.NaN;
            }

            char firstChar = Strings.charAt(readRawNode, inputStr, firstIdx);
            boolean negate = false;
            if (firstChar == '-') {
                negate = true;
                firstIdx++;
            } else if (firstChar == '+') {
                firstIdx++;
            }

            if (radix == 16 || radix == 0) {
                needsRadix16.enter(node);
                if (hasHexStart(readRawNode, inputStr, firstIdx, lastIdx)) {
                    firstIdx += 2;
                    radix = 16; // could be 0
                } else if (radix == 0) {
                    radix = 10;
                }
            } else if (radix < 2 || radix > 36) {
                needsNaN.enter(node);
                return Double.NaN;
            }

            int lastValidIdx = validStringLastIdx(readRawNode, inputStr, radix, firstIdx, lastIdx);
            int len = lastValidIdx - firstIdx;
            if (len <= 0) {
                needsNaN.enter(node);
                return Double.NaN;
            }
            if ((radix <= 10 && len >= 18) || (10 < radix && radix <= 16 && len >= 15) || (radix > 16 && len >= 12)) {
                needsDontFitLong.enter(node);
                if (radix == 10) {
                    // parseRawDontFitLong() can produce an incorrect result
                    // due to subtle rounding errors (for radix 10) but the spec.
                    // requires exact processing for this radix
                    return parseDouble(Strings.lazySubstring(substringNode, inputStr, firstIdx, len), negate);
                } else {
                    return JSRuntime.parseRawDontFitLong(inputStr, radix, firstIdx, lastValidIdx, negate);
                }
            }
            return JSRuntime.parseRawFitsLong(inputStr, radix, firstIdx, lastValidIdx, negate);
        }

        @TruffleBoundary
        private static double parseDouble(TruffleString s, boolean negate) {
            double value = Double.parseDouble(Strings.toJavaString(s));
            return negate ? -value : value;
        }

        private static Object convertToRadix(int inputValue, int radix) {
            assert radix >= 2 && radix <= 36;
            boolean negative = inputValue < 0;
            long value = inputValue;
            if (negative) {
                value = -value;
            }
            long result = 0;
            long radixVal = 1;
            while (value != 0) {
                long digit = value % 10;
                value /= 10;
                if (digit >= radix) {
                    if (value == 0) { // first digit is invalid
                        return Double.NaN;
                    } else {
                        // ignore the digits seen so far and try again
                        result = 0;
                        radixVal = 1;
                        continue;
                    }
                }
                result += digit * radixVal;
                radixVal *= radix;
            }
            if (negative) {
                result = -result;
            }
            return JSRuntime.longToIntOrDouble(result);
        }

        @SuppressFBWarnings(value = "FL_FLOATS_AS_LOOP_COUNTERS", justification = "intentional use of floating-point variable as loop counter")
        private static double convertToRadix(double inputValue, int radix) {
            assert (radix >= 2 && radix <= 36);
            boolean negative = inputValue < 0;
            double value = negative ? -inputValue : inputValue;
            double result = 0;
            double radixVal = 1;
            while (value != 0) {
                double digit = (value % 10);
                value -= digit;
                value /= 10;
                if (digit >= radix) {
                    if (value == 0) { // first digit is invalid
                        return Double.NaN;
                    } else {
                        // ignore the digits seen so far and try again
                        result = 0;
                        radixVal = 1;
                        continue;
                    }
                }
                result += digit * radixVal;
                radixVal *= radix;
            }
            return negative ? -result : result;
        }

        // searches for '0x12345', assumes NO sign!
        private static boolean hasHexStart(TruffleString.ReadCharUTF16Node readRawNode, TruffleString inputString, int firstPos, int lastPos) {
            int length = lastPos - firstPos;
            if (length >= 2 && Strings.charAt(readRawNode, inputString, firstPos) == '0') {
                char c1 = Strings.charAt(readRawNode, inputString, firstPos + 1);
                return (c1 == 'x' || c1 == 'X');
            }
            return false;
        }

        private static int validStringLastIdx(TruffleString.ReadCharUTF16Node readRawNode, TruffleString input, int radix, int firstIdx, int lastIdx) {
            int pos = firstIdx;
            while (pos < lastIdx) {
                char c = Strings.charAt(readRawNode, input, pos);
                if (JSRuntime.valueInRadix(c, radix) == -1) {
                    break;
                }
                pos++;
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
        protected TruffleString encodeURI(Object value) {
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
        protected Object decodeURI(Object value) {
            return decoder.decode(toString1(value));
        }
    }

    /**
     * This node is used only for indirect calls to eval. Direct calls are handled by
     * {@link EvalNode}.
     */
    public abstract static class JSGlobalIndirectEvalNode extends JSBuiltinNode {
        @Child private IndirectCallNode callNode = IndirectCallNode.create();

        public JSGlobalIndirectEvalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object indirectEvalString(TruffleString source,
                        @Cached @Shared TruffleString.ToJavaStringNode toJavaString) {
            JSRealm realm = getRealm();
            return parseIndirectEval(realm, Strings.toJavaString(toJavaString, source)).runEval(callNode, realm);
        }

        @InliningCutoff
        @Specialization(guards = "isForeignObject(source)", limit = "3")
        protected Object indirectEvalForeignObject(Object source,
                        @CachedLibrary("source") InteropLibrary interop,
                        @Cached TruffleString.SwitchEncodingNode switchEncoding,
                        @Cached @Shared TruffleString.ToJavaStringNode toJavaString) {
            if (interop.isString(source)) {
                return indirectEvalString(Strings.interopAsTruffleString(source, interop, switchEncoding), toJavaString);
            } else {
                return source;
            }
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private ScriptNode parseIndirectEval(JSRealm realm, String sourceCode) {
            assert isCallerSensitive();
            Node caller = EvalNode.findCallNode(realm);
            String sourceName = EvalNode.formatEvalOrigin(caller, getContext(), Evaluator.EVAL_SOURCE_NAME);
            ScriptOrModule activeScriptOrModule = EvalNode.findActiveScriptOrModule(caller);
            Source source = Source.newBuilder(JavaScriptLanguage.ID, sourceCode, sourceName).cached(false).build();
            return getContext().getEvaluator().parseEval(getContext(), this, source, activeScriptOrModule);
        }

        @Specialization
        protected int indirectEvalInt(int source) {
            return source;
        }

        @Specialization
        protected SafeInteger indirectEvalSafeInteger(SafeInteger source) {
            return source;
        }

        @Specialization
        protected long indirectEvalLong(long source) {
            return source;
        }

        @Specialization
        protected double indirectEvalDouble(double source) {
            return source;
        }

        @Specialization
        protected boolean indirectEvalBoolean(boolean source) {
            return source;
        }

        @Specialization
        protected Symbol indirectEvalSymbol(Symbol source) {
            return source;
        }

        @Specialization
        protected BigInt indirectEvalBigInt(BigInt source) {
            return source;
        }

        @Specialization
        public JSDynamicObject indirectEvalJSType(JSDynamicObject object) {
            return object;
        }

        @Override
        public boolean isCallerSensitive() {
            return true;
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
        protected TruffleString escape(Object value) {
            TruffleString s = toString1(value);
            return unescape ? StringEscape.unescape(s) : StringEscape.escape(s);
        }
    }

    /**
     * Non-standard print()/printErr() method to write to the console.
     */
    public abstract static class JSGlobalPrintNode extends JSGlobalOperation {

        private final boolean useErr;
        private final boolean noNewLine;

        public JSGlobalPrintNode(JSContext context, JSBuiltin builtin, boolean useErr, boolean noNewline) {
            super(context, builtin);
            this.useErr = useErr;
            this.noNewLine = noNewline;
        }

        public abstract Object executeObjectArray(Object[] args);

        @Specialization
        protected Object print(Object[] arguments,
                        @Cached InlinedConditionProfile argumentsCount,
                        @Cached(parameters = "getContext().getStringLengthLimit()") StringBuilderProfile builderProfile,
                        @Cached TruffleStringBuilder.AppendCodePointNode appendCodePointNode,
                        @Cached TruffleStringBuilder.AppendStringNode appendStringNode,
                        @Cached TruffleStringBuilder.ToStringNode toStringNode) {
            // without a StringBuilder, synchronization fails testnashorn JDK-8041998.js
            JSRealm realm = getRealm();
            TruffleStringBuilderUTF16 sb = builderProfile.newStringBuilder();
            JSConsoleUtil consoleUtil = realm.getConsoleUtil();
            if (consoleUtil.getConsoleIndentation() > 0) {
                builderProfile.repeat(appendCodePointNode, sb, ' ', consoleUtil.getConsoleIndentation() * 2);
            }
            if (argumentsCount.profile(this, arguments.length == 1)) {
                builderProfile.append(appendStringNode, sb, toString1(arguments[0]));
            } else {
                for (int i = 0; i < arguments.length; i++) {
                    if (i != 0) {
                        builderProfile.append(appendCodePointNode, sb, ' ');
                    }
                    builderProfile.append(appendStringNode, sb, toString1(arguments[i]));
                }
            }
            if (!noNewLine) {
                builderProfile.append(appendStringNode, sb, Strings.LINE_SEPARATOR);
            }
            TruffleString string = StringBuilderProfile.toString(toStringNode, sb);
            return printString(string, realm);
        }

        @TruffleBoundary
        private Object printString(TruffleString string, JSRealm realm) {
            PrintWriter writer = useErr ? realm.getErrorWriter() : realm.getOutputWriter();
            writer.print(string);
            writer.flush();
            return Undefined.instance;
        }
    }

    @ImportStatic({JSInteropUtil.class, JSConfig.class})
    public abstract static class JSGlobalLoadNode extends JSLoadOperation {

        public JSGlobalLoadNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object loadString(TruffleString path, Object[] args) {
            JSRealm realm = getRealm();
            return loadFromPath(path, realm, args);
        }

        protected Object loadFromPath(TruffleString path, JSRealm realm, @SuppressWarnings("unused") Object[] args) {
            Source source = sourceFromPath(Strings.toJavaString(path), realm);
            return runImpl(realm, source);
        }

        @Specialization(guards = "isForeignObject(scriptObj)")
        protected Object loadTruffleObject(Object scriptObj, Object[] args,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            JSRealm realm = getRealm();
            TruffleLanguage.Env env = realm.getEnv();
            if (env.isHostObject(scriptObj)) {
                if (getContext().isOptionNashornCompatibilityMode() && env.asHostObject(scriptObj) instanceof URL) {
                    return loadURL(realm, (URL) env.asHostObject(scriptObj));
                } else if (interop.isMemberInvocable(scriptObj, "getPath")) {
                    // argument is most likely a java.io.File
                    return loadFile(realm, fileGetPath(scriptObj, interop));
                }
            }
            if (interop.isNull(scriptObj)) {
                throw cannotLoadScript(scriptObj);
            }
            TruffleString stringPath = toString1(scriptObj);
            return loadFromPath(stringPath, realm, args);
        }

        private String fileGetPath(Object scriptObj, InteropLibrary interop) {
            try {
                return interop.asString(interop.invokeMember(scriptObj, "getPath"));
            } catch (UnsupportedMessageException | ArityException | UnknownIdentifierException | UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(scriptObj, e, "getPath", this);
            }
        }

        @Specialization
        protected Object loadScriptObj(JSObject scriptObj, Object[] args) {
            if (JSObject.hasProperty(scriptObj, Strings.EVAL_OBJ_FILE_NAME) && JSObject.hasProperty(scriptObj, Strings.EVAL_OBJ_SOURCE)) {
                Object scriptNameObj = JSObject.get(scriptObj, Strings.EVAL_OBJ_FILE_NAME);
                Object sourceObj = JSObject.get(scriptObj, Strings.EVAL_OBJ_SOURCE);
                return evalObjectLiteral(scriptNameObj, sourceObj, args);
            } else {
                throw cannotLoadScript(scriptObj);
            }
        }

        private Object evalObjectLiteral(Object scriptName, Object scriptSource, Object[] args) {
            JSRealm realm = getRealm();
            return evalImpl(realm, toString1(scriptName), toString1(scriptSource), args);
        }

        @Specialization(guards = {"!isString(fileName)", "!isForeignObject(fileName)", "!isJSObject(fileName)"})
        protected Object loadConvertToString(Object fileName, Object[] args) {
            return loadString(toString1(fileName), args);
        }

        protected Object loadFile(JSRealm realm, String filePath) {
            return runImpl(realm, sourceFromFileName(filePath, realm));
        }

        protected Object loadURL(JSRealm realm, URL url) {
            assert getContext().isOptionNashornCompatibilityMode();
            return runImpl(realm, sourceFromURL(url, realm));
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        protected Object evalImpl(JSRealm realm, TruffleString fileName, TruffleString source, @SuppressWarnings("unused") Object[] args) {
            return loadStringImpl(fileName, source).run(realm);
        }
    }

    /**
     * Implementation of non-standard method loadWithNewGlobal() as defined by Nashorn.
     *
     */
    public abstract static class JSGlobalLoadWithNewGlobalNode extends JSGlobalLoadNode {

        public JSGlobalLoadWithNewGlobalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Override
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected Object evalImpl(JSRealm realm, TruffleString fileName, TruffleString source, Object[] args) {
            JSRealm childRealm = realm.createChildRealm();
            JSRealm mainRealm = JSRealm.getMain(this);
            JSRealm prevRealm = mainRealm.enterRealm(this, childRealm);
            try {
                JSDynamicObject argumentsArray = JSArray.createConstant(getContext(), childRealm, args);
                assert JSObject.getPrototype(argumentsArray) == childRealm.getArrayPrototype();
                JSRuntime.createDataProperty(childRealm.getGlobalObject(), JSFunction.ARGUMENTS, argumentsArray);
                return loadStringImpl(fileName, source).run(childRealm);
            } finally {
                mainRealm.leaveRealm(this, prevRealm);
            }
        }

        @Override
        @TruffleBoundary
        protected Object loadFromPath(TruffleString path, JSRealm realm, Object[] args) {
            JSRealm childRealm = realm.createChildRealm();
            JSRealm mainRealm = JSRealm.getMain(this);
            JSRealm prevRealm = mainRealm.enterRealm(this, childRealm);
            try {
                JSDynamicObject argumentsArray = JSArray.createConstant(getContext(), childRealm, args);
                assert JSObject.getPrototype(argumentsArray) == childRealm.getArrayPrototype();
                JSRuntime.createDataProperty(childRealm.getGlobalObject(), JSFunction.ARGUMENTS, argumentsArray);
                Source source = sourceFromPath(Strings.toJavaString(path), childRealm);
                return runImpl(childRealm, source);
            } finally {
                mainRealm.leaveRealm(this, prevRealm);
            }
        }
    }

    /**
     * Non-standard global exit function to provide compatibility with Nashorn (exit() and quit())
     * and V8 (only quit()) shells. Available as quit() if the {@code js.shell} option is enabled.
     */
    public abstract static class JSGlobalExitNode extends JSBuiltinNode {

        public JSGlobalExitNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isUndefined(arg)")
        protected Object exit(@SuppressWarnings("unused") Object arg) {
            return exit(0);
        }

        @Specialization
        protected Object exit(int exitCode) {
            getRealm().getEnv().getContext().closeExited(this, exitCode);
            return Undefined.instance;
        }

        @Specialization
        protected Object exit(Object arg,
                        @Cached JSToNumberNode toNumberNode) {
            int exitCode = (int) JSRuntime.toInteger(toNumberNode.executeNumber(arg));
            return exit(exitCode);
        }
    }

    /**
     * Non-standard readline() for V8 compatibility, and readLine(prompt) for Nashorn compatibility
     * (only available in nashorn-compat mode with scripting enabled).
     *
     * The prompt argument is only accepted and printed by Nashorn's variant.
     */
    public abstract static class JSGlobalReadLineNode extends JSGlobalOperation {

        // null for Nashorn, undefined for V8
        private final boolean returnNullWhenEmpty;

        public JSGlobalReadLineNode(JSContext context, JSBuiltin builtin, boolean returnNullWhenEmpty) {
            super(context, builtin);
            this.returnNullWhenEmpty = returnNullWhenEmpty;
        }

        @Specialization
        protected Object readLine(Object prompt) {
            TruffleString promptString = null;
            if (prompt != Undefined.instance) {
                promptString = toString1(prompt);
            }
            JSRealm realm = getRealm();
            return doReadLine(promptString, realm);
        }

        @TruffleBoundary
        private Object doReadLine(TruffleString promptString, JSRealm realm) {
            if (promptString != null) {
                realm.getOutputWriter().print(Strings.toJavaString(promptString));
            }
            try {
                final BufferedReader inReader = new BufferedReader(new InputStreamReader(realm.getEnv().in(), realm.getCharset()));
                String result = inReader.readLine();
                return result == null ? (returnNullWhenEmpty ? Null.instance : Undefined.instance) : Strings.fromJavaString(result);
            } catch (Exception ex) {
                throw Errors.createError(ex.getMessage());
            }
        }

    }

    static TruffleFile getFileFromArgument(Object arg, TruffleLanguage.Env env) {
        CompilerAsserts.neverPartOfCompilation();
        try {
            String path;
            if (arg instanceof TruffleString str) {
                path = Strings.toJavaString(str);
            } else {
                // if arg is a java.io.File, invokes toString() (equivalent to getPath()).
                path = JSRuntime.toJavaString(arg);
            }

            TruffleFile file = resolveRelativeFilePath(path, env);
            if (!file.isRegularFile()) {
                throw Errors.createNotAFileError(path);
            }
            return file;
        } catch (SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
            throw Errors.createErrorFromException(e);
        }
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
        protected TruffleString read(Object fileParam) {
            TruffleFile file = getFileFromArgument(fileParam, getRealm().getEnv());

            try {
                return readImpl(file.newBufferedReader());
            } catch (Exception ex) {
                throw Errors.createErrorFromException(ex);
            }
        }

        private static TruffleString readImpl(BufferedReader reader) throws IOException {
            var sb = Strings.builderCreate();
            final char[] arr = new char[BUFFER_SIZE];
            try {
                int numChars;
                while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
                    Strings.builderAppend(sb, new String(arr, 0, numChars));
                }
            } finally {
                reader.close();
            }
            return Strings.builderToString(sb);
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
        protected final JSDynamicObject readbuffer(Object fileParam) {
            JSRealm realm = getRealm();
            TruffleFile file = getFileFromArgument(fileParam, realm.getEnv());

            try {
                final byte[] bytes = file.readAllBytes();

                final JSDynamicObject arrayBuffer;
                if (getContext().isOptionDirectByteBuffer()) {
                    ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
                    buffer.put(bytes);
                    buffer.rewind();
                    arrayBuffer = JSArrayBuffer.createDirectArrayBuffer(getContext(), realm, buffer);
                } else {
                    arrayBuffer = JSArrayBuffer.createArrayBuffer(getContext(), realm, bytes);
                }
                return arrayBuffer;
            } catch (Exception ex) {
                throw Errors.createErrorFromException(ex);
            }
        }
    }

    /**
     * Non-standard import helper function for support of global scope bindings in
     * GraalJSScriptEngine.
     */
    abstract static class JSGlobalImportScriptEngineGlobalBindingsNode extends JSBuiltinNode {

        JSGlobalImportScriptEngineGlobalBindingsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final Object importGlobalContext(Object globalContextBindings) {
            doImport(globalContextBindings);
            return Undefined.instance;
        }

        @TruffleBoundary
        private void doImport(Object globalContextBindings) {
            JSRealm realm = getRealm();
            JSDynamicObject globalObject = realm.getGlobalObject();

            InteropLibrary bindingsInterop = InteropLibrary.getUncached(globalContextBindings);
            try {
                Object members = bindingsInterop.getMembers(globalContextBindings);
                InteropLibrary membersInterop = InteropLibrary.getUncached(members);
                long size = membersInterop.getArraySize(members);
                for (long i = 0; i < size; i++) {
                    Object hashKey = membersInterop.readArrayElement(members, i);
                    InteropLibrary keyInterop = InteropLibrary.getUncached(hashKey);
                    if (keyInterop.isString(hashKey)) {
                        TruffleString stringKey = Strings.interopAsTruffleString(hashKey, keyInterop);
                        Object value = DynamicObjectLibrary.getUncached().getOrDefault(globalObject, stringKey, Undefined.instance);
                        if ((value == Undefined.instance || value instanceof ScriptEngineGlobalScopeBindingsPropertyProxy &&
                                        ((ScriptEngineGlobalScopeBindingsPropertyProxy) value).get(globalObject) == Undefined.instance) &&
                                        !JSObject.getPrototype(globalObject).getShape().hasProperty(stringKey)) {
                            JSObjectUtil.defineProxyProperty(globalObject, stringKey, new ScriptEngineGlobalScopeBindingsPropertyProxy(stringKey, globalContextBindings, bindingsInterop),
                                            JSAttributes.getDefault());
                        }
                    }
                }
            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                throw Errors.createTypeErrorInteropException(globalContextBindings, e, "importScriptEngineGlobalBindings", this);
            }
        }

        private static final class ScriptEngineGlobalScopeBindingsPropertyProxy extends PropertyProxy {

            private final TruffleString key;
            private final Object globalContextBindings;
            private final InteropLibrary bindingsInterop;

            ScriptEngineGlobalScopeBindingsPropertyProxy(TruffleString key, Object globalContextBindings, InteropLibrary bindingsInterop) {
                this.key = key;
                this.globalContextBindings = globalContextBindings;
                this.bindingsInterop = bindingsInterop;
            }

            @Override
            @TruffleBoundary
            public Object get(JSDynamicObject store) {
                return JSInteropUtil.readMemberOrDefault(globalContextBindings, key, Undefined.instance, bindingsInterop, ImportValueNode.getUncached());
            }

            @TruffleBoundary
            @Override
            public boolean set(JSDynamicObject store, Object value) {
                JSObjectUtil.defineDataProperty(store, key, value, JSAttributes.getDefault());
                return true;
            }
        }
    }

    public abstract static class JSGlobalPostMessageNode extends JSBuiltinNode {

        public JSGlobalPostMessageNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object postMessage(Object message) {
            ((WorkerAgent) getRealm().getAgent()).postOutMessage(message);
            return Undefined.instance;
        }

    }

}
