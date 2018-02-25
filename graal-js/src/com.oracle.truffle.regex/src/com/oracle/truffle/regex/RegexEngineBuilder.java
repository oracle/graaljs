/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.TRegexCompiler;

public class RegexEngineBuilder implements RegexLanguageObject {

    private final RegexLanguage language;

    public RegexEngineBuilder(RegexLanguage language) {
        this.language = language;
    }
    
    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexCompiler;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexEngineBuilderMessageResolutionForeign.ACCESS;
    }

    @MessageResolution(receiverType = RegexEngineBuilder.class)
    static class RegexEngineBuilderMessageResolution {

        @Resolve(message = "EXECUTE")
        abstract static class RegexEngineBuilderExecuteNode extends Node {

            private Node isExecutableNode = Message.IS_EXECUTABLE.createNode();

            public Object access(RegexEngineBuilder receiver, Object[] args) {
                if (args.length > 2) {
                    throw ArityException.raise(2, args.length);
                }
                RegexOptions options = RegexOptions.DEFAULT;
                if (args.length >= 1) {
                    if (!(args[0] instanceof String)) {
                        throw UnsupportedTypeException.raise(args);
                    }
                    options = RegexOptions.parse((String)args[0]);
                }
                TruffleObject fallbackEngine = null;
                if (args.length >= 2) {
                    if (!(args[1] instanceof TruffleObject && ForeignAccess.sendIsExecutable(isExecutableNode, (TruffleObject)args[1]))) {
                        throw UnsupportedTypeException.raise(args);
                    }
                    fallbackEngine = (TruffleObject)args[1];
                }
                if (fallbackEngine != null) {
                    return new RegexEngine(new CachingRegexCompiler(new RegexCompilerWithFallback(new TRegexCompiler(receiver.language, options), fallbackEngine)));
                } else {
                    return new RegexEngine(new CachingRegexCompiler(new TRegexCompiler(receiver.language, options)));
                }
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        abstract static class RegexEngineBuilderIsExecutableNode extends Node {

            @SuppressWarnings("unused")
            public boolean access(RegexEngineBuilder receiver) {
                return true;
            }
        }
    }
}
