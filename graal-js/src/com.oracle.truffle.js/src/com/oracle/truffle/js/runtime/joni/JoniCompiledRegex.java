/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.joni;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.runtime.joni.interop.ToLongNode;
import com.oracle.truffle.js.runtime.joni.interop.ToStringNode;
import com.oracle.truffle.js.runtime.joni.interop.TruffleNull;
import com.oracle.truffle.js.runtime.joni.interop.TruffleReadOnlyKeysArray;
import com.oracle.truffle.js.runtime.joni.result.JoniNoMatchResult;
import com.oracle.truffle.regex.nashorn.regexp.joni.Regex;

@ExportLibrary(InteropLibrary.class)
public class JoniCompiledRegex extends AbstractConstantKeysObject {

    private static final TruffleReadOnlyKeysArray KEYS = new TruffleReadOnlyKeysArray("exec", "pattern", "flags", "groupCount", "groups");

    private final String pattern;
    private final JoniRegexFlags flags;
    private final Regex joniRegex;
    private final CallTarget regexCallTarget;

    public JoniCompiledRegex(String pattern, JoniRegexFlags flags, Regex joniRegex, CallTarget regexCallTarget) {
        this.pattern = pattern;
        this.flags = flags;
        this.joniRegex = joniRegex;
        this.regexCallTarget = regexCallTarget;
    }

    public Regex getJoniRegex() {
        return joniRegex;
    }

    public CallTarget getRegexCallTarget() {
        return regexCallTarget;
    }

    public JoniCompiledRegexExecMethod getExecMethod() {
        // this allocation should get virtualized and optimized away by graal
        return new JoniCompiledRegexExecMethod(this);
    }

    @Override
    public TruffleReadOnlyKeysArray getKeys() {
        return KEYS;
    }

    @Override
    public Object readMemberImpl(String symbol) throws UnknownIdentifierException {
        switch (symbol) {
            case "exec":
                return getExecMethod();
            case "pattern":
                return pattern;
            case "flags":
                return flags;
            case "groupCount":
                return joniRegex.numberOfCaptures() + 1;
            case "groups":
                return TruffleNull.getInstance();
            default:
                CompilerDirectives.transferToInterpreter();
                throw UnknownIdentifierException.create(symbol);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isMemberInvocable(String member) {
        return "exec".equals(member);
    }

    @ExportMessage
    Object invokeMember(String member, Object[] args,
                    @Cached("create()") ToStringNode toStringNode,
                    @Cached ToLongNode toLongNode,
                    @Cached JoniCompiledRegexDispatchNode executeNode) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
        if (!"exec".equals(member)) {
            CompilerDirectives.transferToInterpreter();
            throw UnsupportedMessageException.create();
        }
        if (args.length != 2) {
            CompilerDirectives.transferToInterpreter();
            throw ArityException.create(2, args.length);
        }
        String input = toStringNode.execute(args[0]);
        long fromIndex = toLongNode.execute(args[1]);
        if (fromIndex > Integer.MAX_VALUE) {
            return JoniNoMatchResult.getInstance();
        }
        return executeNode.execute(this, input, (int) fromIndex);
    }
}
