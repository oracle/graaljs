/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.regex.runtime.nodes.ExecuteRegexDispatchNode;

public class CompiledRegexObject implements RegexLanguageObject {

    private final CompiledRegex compiledRegex;

    public CompiledRegexObject(CompiledRegex compiledRegex) {
        this.compiledRegex = compiledRegex;
    }

    public CompiledRegex getCompiledRegex() {
        return compiledRegex;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof CompiledRegexObject;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return CompiledRegexObjectMessageResolutionForeign.ACCESS;
    }

    @MessageResolution(receiverType = CompiledRegexObject.class)
    static class CompiledRegexObjectMessageResolution {

        @Resolve(message = "EXECUTE")
        abstract static class CompiledRegexObjectExecuteNode extends Node {

            @Child private ExecuteRegexDispatchNode doExecute = ExecuteRegexDispatchNode.create();

            public Object access(CompiledRegexObject receiver, Object[] args) {
                if (args.length != 3) {
                    throw ArityException.raise(3, args.length);
                }
                if (!(args[0] instanceof RegexObject)) {
                    throw UnsupportedTypeException.raise(args);
                }
                return doExecute.execute(receiver.getCompiledRegex(), (RegexObject) args[0], args[1], args[2]);
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        abstract static class CompiledRegexObjectIsExecutableNode extends Node {

            @SuppressWarnings("unused")
            public boolean access(CompiledRegexObject receiver) {
                return true;
            }
        }
    }
}
