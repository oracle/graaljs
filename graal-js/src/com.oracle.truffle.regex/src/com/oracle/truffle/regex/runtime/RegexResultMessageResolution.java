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
package com.oracle.truffle.regex.runtime;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.runtime.RegexResultMessageResolutionFactory.ReadCacheNodeGen;

@MessageResolution(receiverType = RegexResult.class)
public class RegexResultMessageResolution {

    abstract static class ResultPropertyNode extends Node {

        abstract Object execute(RegexResult receiver);
    }

    static class GetInputNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResult receiver) {
            return receiver.getInput();
        }
    }

    static class IsMatchNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResult receiver) {
            return receiver != RegexResult.NO_MATCH;
        }
    }

    static class GetGroupCountNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResult receiver) {
            return receiver.getGroupCount();
        }
    }

    static class GetStartArrayNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResult receiver) {
            return receiver.getStartArrayObject();
        }
    }

    static class GetEndArrayNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResult receiver) {
            return receiver.getEndArrayObject();
        }
    }

    static class GetCompiledRegexNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResult receiver) {
            return receiver.getCompiledRegex();
        }
    }

    abstract static class ReadCacheNode extends Node {

        abstract Object execute(RegexResult receiver, String symbol);

        @Specialization(guards = "symbol == cachedSymbol", limit = "6")
        public Object readIdentity(RegexResult receiver, @SuppressWarnings("unused") String symbol,
                        @Cached("symbol") @SuppressWarnings("unused") String cachedSymbol,
                        @Cached("getResultProperty(symbol)") ResultPropertyNode propertyNode) {
            return propertyNode.execute(receiver);
        }

        @Specialization(guards = "symbol.equals(cachedSymbol)", limit = "6", replaces = "readIdentity")
        public Object readEquals(RegexResult receiver, @SuppressWarnings("unused") String symbol,
                        @Cached("symbol") @SuppressWarnings("unused") String cachedSymbol,
                        @Cached("getResultProperty(symbol)") ResultPropertyNode propertyNode) {
            return propertyNode.execute(receiver);
        }

        static ResultPropertyNode getResultProperty(String symbol) {
            switch (symbol) {
                case "input":
                    return new GetInputNode();
                case "isMatch":
                    return new IsMatchNode();
                case "groupCount":
                    return new GetGroupCountNode();
                case "start":
                    return new GetStartArrayNode();
                case "end":
                    return new GetEndArrayNode();
                case "regex":
                    return new GetCompiledRegexNode();
                default:
                    throw UnknownIdentifierException.raise(symbol);
            }
        }
    }

    @Resolve(message = "READ")
    abstract static class RegexResultReadNode extends Node {

        @Child ReadCacheNode cache = ReadCacheNodeGen.create();

        public Object access(RegexResult receiver, String symbol) {
            return cache.execute(receiver, symbol);
        }
    }
}
