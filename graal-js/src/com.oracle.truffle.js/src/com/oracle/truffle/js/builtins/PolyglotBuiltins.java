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

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotEvalFileNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotEvalNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotExportNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotImportNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;

public final class PolyglotBuiltins extends JSBuiltinsContainer.SwitchEnum<PolyglotBuiltins.Polyglot> {
    public static final JSBuiltinsContainer BUILTINS = new PolyglotBuiltins();

    protected PolyglotBuiltins() {
        super(JSRealm.POLYGLOT_CLASS_NAME, Polyglot.class);
    }

    public enum Polyglot implements BuiltinEnum<Polyglot> {
        // external
        export(2),
        import_(1),
        eval(2),
        evalFile(2);

        private final int length;

        Polyglot(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isOptional() {
            // under special "polyglot-evalfile" flag
            return this == evalFile;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Polyglot builtinEnum) {
        switch (builtinEnum) {
            case export:
                return PolyglotExportNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case import_:
                return PolyglotImportNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case eval:
                return PolyglotEvalNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case evalFile:
                return PolyglotEvalFileNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotExportNode extends JSBuiltinNode {
        @Child private ExportValueNode exportValue;

        PolyglotExportNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            exportValue = ExportValueNode.create();
        }

        @Specialization
        protected Object doString(TruffleString identifier, Object value,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            Object polyglotBindings;
            try {
                polyglotBindings = getRealm().getEnv().getPolyglotBindings();
            } catch (SecurityException e) {
                throw Errors.createErrorFromException(e);
            }
            JSInteropUtil.writeMember(polyglotBindings, identifier, value, interop, exportValue, true, this);
            return value;
        }

        @InliningCutoff
        @Specialization(guards = {"!isString(identifier)"})
        protected Object doMaybeUnbox(TruffleObject identifier, Object value,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached TruffleString.SwitchEncodingNode switchEncoding) {
            if (interop.isString(identifier)) {
                TruffleString unboxed = Strings.interopAsTruffleString(identifier, interop, switchEncoding);
                return doString(unboxed, value, interop);
            }
            return doInvalid(identifier, value);
        }

        @Specialization(guards = "!isString(identifier)")
        @TruffleBoundary
        protected Object doInvalid(Object identifier, @SuppressWarnings("unused") Object value) {
            throw Errors.createTypeErrorInvalidIdentifier(identifier);
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotImportNode extends JSBuiltinNode {
        PolyglotImportNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doString(TruffleString identifier,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Shared @Cached ImportValueNode importValueNode,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaStringNode) {
            Object polyglotBindings;
            try {
                polyglotBindings = getRealm().getEnv().getPolyglotBindings();
            } catch (SecurityException e) {
                throw Errors.createErrorFromException(e);
            }
            try {
                return importValueNode.executeWithTarget(interop.readMember(polyglotBindings, Strings.toJavaString(toJavaStringNode, identifier)));
            } catch (UnknownIdentifierException e) {
                return Undefined.instance;
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(polyglotBindings, e, "readMember", identifier, this);
            }
        }

        @InliningCutoff
        @Specialization(guards = {"!isString(identifier)"})
        protected Object doMaybeUnbox(TruffleObject identifier,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Shared @Cached ImportValueNode importValueNode,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached TruffleString.SwitchEncodingNode switchEncoding) {
            if (interop.isString(identifier)) {
                TruffleString unboxed = Strings.interopAsTruffleString(identifier, interop, switchEncoding);
                return doString(unboxed, interop, importValueNode, toJavaStringNode);
            }
            return doInvalid(identifier);
        }

        @Specialization(guards = {"!isString(identifier)", "!isTruffleObject(identifier)"})
        @TruffleBoundary
        protected Object doInvalid(Object identifier) {
            throw Errors.createTypeErrorInvalidIdentifier(identifier);
        }
    }

    @ImportStatic(Strings.class)
    abstract static class PolyglotEvalBaseNode extends JSBuiltinNode {

        PolyglotEvalBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @NeverDefault
        protected Pair<String, String> getLanguageIdAndMimeType(TruffleString.ToJavaStringNode toJavaStringNode, TruffleString languageIdOrMimeTypeTS) {
            String languageIdOrMimeType = Strings.toJavaString(toJavaStringNode, languageIdOrMimeTypeTS);
            String languageId = languageIdOrMimeType;
            String mimeType = null;
            if (languageIdOrMimeType.indexOf('/') >= 0) {
                String language = Source.findLanguage(languageIdOrMimeType);
                if (language != null) {
                    languageId = language;
                    mimeType = languageIdOrMimeType;
                }
            }
            return new Pair<>(languageId, mimeType);
        }
    }

    abstract static class PolyglotEvalNode extends PolyglotEvalBaseNode {

        PolyglotEvalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"equals(strEq, language, cachedLanguage)"}, limit = "1")
        @TruffleBoundary
        protected Object evalCachedLanguage(TruffleString language, TruffleString source,
                        @Cached("language") TruffleString cachedLanguage,
                        @Cached TruffleString.EqualNode strEq,
                        @Cached @Shared TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached("getLanguageIdAndMimeType(toJavaStringNode, language)") Pair<String, String> languagePair,
                        @Cached @Shared IndirectCallNode callNode,
                        @Cached @Shared ImportValueNode importValueNode) {
            return importValueNode.executeWithTarget(callNode.call(evalStringIntl(source, languagePair.getFirst(), languagePair.getSecond())));
        }

        @Specialization(replaces = "evalCachedLanguage")
        @TruffleBoundary
        protected Object evalString(TruffleString language, TruffleString source,
                        @Cached @Shared TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached @Shared IndirectCallNode callNode,
                        @Cached @Shared ImportValueNode importValueNode) {
            Pair<String, String> pair = getLanguageIdAndMimeType(toJavaStringNode, language);
            return importValueNode.executeWithTarget(callNode.call(evalStringIntl(source, pair.getFirst(), pair.getSecond())));
        }

        private CallTarget evalStringIntl(TruffleString sourceText, String languageId, String mimeType) {
            CompilerAsserts.neverPartOfCompilation();
            getContext().checkEvalAllowed();
            Source source = Source.newBuilder(languageId, Strings.toJavaString(sourceText), Evaluator.EVAL_SOURCE_NAME).mimeType(mimeType).build();

            TruffleLanguage.Env env = getRealm().getEnv();
            try {
                return env.parsePublic(source);
            } catch (IllegalStateException | IllegalArgumentException ex) {
                throw Errors.createErrorFromException(ex);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isString(languageId) || !isString(source)")
        protected Object eval(Object languageId, Object source) {
            throw Errors.createTypeError("Expected arguments: (String languageId, String sourceCode)");
        }
    }

    abstract static class PolyglotEvalFileNode extends PolyglotEvalBaseNode {

        PolyglotEvalFileNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"equals(strEq, language, cachedLanguage)"}, limit = "1")
        @TruffleBoundary
        protected Object evalFileCachedLanguage(TruffleString language, TruffleString file,
                        @Cached("language") TruffleString cachedLanguage,
                        @Cached TruffleString.EqualNode strEq,
                        @Cached @Shared TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached("getLanguageIdAndMimeType(toJavaStringNode, language)") Pair<String, String> languagePair,
                        @Cached @Shared IndirectCallNode callNode,
                        @Cached @Shared ImportValueNode importValueNode) {
            return importValueNode.executeWithTarget(callNode.call(evalFileIntl(file, languagePair.getFirst(), languagePair.getSecond())));
        }

        @Specialization(replaces = "evalFileCachedLanguage")
        @TruffleBoundary
        protected Object evalFileString(TruffleString language, TruffleString file,
                        @Cached @Shared TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached @Shared IndirectCallNode callNode,
                        @Cached @Shared ImportValueNode importValueNode) {
            Pair<String, String> pair = getLanguageIdAndMimeType(toJavaStringNode, language);
            return importValueNode.executeWithTarget(callNode.call(evalFileIntl(file, pair.getFirst(), pair.getSecond())));
        }

        private CallTarget evalFileIntl(TruffleString fileName, String languageId, String mimeType) {
            CompilerAsserts.neverPartOfCompilation();
            TruffleLanguage.Env env = getRealm().getEnv();
            Source source;
            try {
                source = Source.newBuilder(languageId, env.getPublicTruffleFile(Strings.toJavaString(fileName))).mimeType(mimeType).build();
            } catch (IOException | SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
                String reason;
                if (e instanceof AccessDeniedException) {
                    reason = "access denied";
                } else if (e instanceof NoSuchFileException) {
                    reason = "no such file";
                } else {
                    reason = e.getMessage();
                }
                throw Errors.createError("Cannot evaluate file " + fileName + ": " + reason, e);
            }

            try {
                return env.parsePublic(source);
            } catch (IllegalStateException | IllegalArgumentException ex) {
                throw Errors.createErrorFromException(ex);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isString(languageId) || !isString(fileName)")
        protected Object eval(Object languageId, Object fileName) {
            throw Errors.createTypeError("Expected arguments: (String languageId, String fileName)");
        }
    }
}
