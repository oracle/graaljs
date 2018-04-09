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
