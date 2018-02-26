/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.runtime.nodes.ExecuteRegexDispatchNode;

/**
 * {@link CompiledRegexObject}s represent the {@link CallTarget}s produced by regular expression
 * compilers as executable {@link TruffleObject}s. The execute method of these objects has the same
 * signature as the {@link CallTarget}s contained in {@link CompiledRegex}es. They accept the
 * following arguments:
 * <ol>
 * <li>{@link RegexObject} {@code regexObject}: the {@link RegexObject} to which this
 * {@link CompiledRegexObject} belongs</li>
 * <li>{@link Object} {@code input}: the character sequence to search in. This may either be a
 * {@link String} or a {@link TruffleObject} that responds to {@link Message#GET_SIZE} and returns
 * {@link Character}s on indexed {@link Message#READ} requests.</li>
 * <li>{@link Number} {@code fromIndex}: the position to start searching from. This argument will be
 * cast to {@code int}, since a {@link String} can not be longer than {@link Integer#MAX_VALUE}. If
 * {@code fromIndex} is greater than {@link Integer#MAX_VALUE}, this method will immediately return
 * NO_MATCH.</li>
 * </ol>
 * The return value is a {@link RegexResult} or a compatible {@link TruffleObject}.
 * <p>
 * A {@link CompiledRegexObject} can be obtained by executing a {@link RegexCompiler}. The purpose
 * of this class is to move from {@link RegexLanguage}-specific {@link CallTarget}s to executable
 * {@link TruffleObject}s that can be passed around via interop and can come from external RegExp
 * compilers (e.g. see {@link ForeignRegexCompiler}).
 */
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
