/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;

public abstract class RegexCompiler implements RegexLanguageObject {

    /**
     * Uses the compiler to try and compile the regular expression described in {@code source}.
     * 
     * @return the {@link RegexObject} or null if the engine could not compile the regular
     *         expression
     * @throws RegexSyntaxException if the engine discovers a syntax error in the regular expression
     * @throws UnsupportedRegexException if the regular expression is not supported by the engine
     */
    public abstract TruffleObject compile(RegexSource source) throws RegexSyntaxException;

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexCompiler;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexCompilerMessageResolutionForeign.ACCESS;
    }

    @MessageResolution(receiverType = RegexCompiler.class)
    static class RegexCompilerMessageResolution {

        @Resolve(message = "EXECUTE")
        abstract static class RegexCompilerExecuteNode extends Node {

            public Object access(RegexCompiler receiver, Object[] args) {
                if (!(args.length == 1 || args.length == 2)) {
                    throw ArityException.raise(2, args.length);
                }
                if (!(args[0] instanceof String)) {
                    throw UnsupportedTypeException.raise(args);
                }
                String pattern = (String)args[0];
                String flags = "";
                if (args.length == 2) {
                    if (!(args[1] instanceof String)) {
                        throw UnsupportedTypeException.raise(args);
                    }
                    flags = (String)args[1];
                }
                RegexSource regexSource = new RegexSource(null, pattern, RegexFlags.parseFlags(flags), RegexOptions.DEFAULT);
                return receiver.compile(regexSource);
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        abstract static class RegexCompilerIsExecutableNode extends Node {

            @SuppressWarnings("unused")
            public boolean access(RegexCompiler receiver) {
                return true;
            }
        }
    }
}
