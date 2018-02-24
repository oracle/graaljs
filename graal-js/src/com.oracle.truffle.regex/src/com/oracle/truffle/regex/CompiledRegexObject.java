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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.runtime.nodes.ExecuteRegexDispatchNode;

public class CompiledRegexObject implements RegexLanguageObject, HasCompiledRegex {

    public CompiledRegex compiledRegex;

    public CompiledRegexObject(CompiledRegex compiledRegex) {
        this.compiledRegex = compiledRegex;
    }

    @Override
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
                if (args.length != 2) {
                    throw ArityException.raise(2, args.length);
                }
                return doExecute.execute(receiver, args[0], args[1]);
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        abstract static class CompiledRegexObjectIsExecutableNode extends Node {

            @SuppressWarnings("unused")
            public boolean access(CompiledRegex receiver) {
                return true;
            }
        }
    }
}