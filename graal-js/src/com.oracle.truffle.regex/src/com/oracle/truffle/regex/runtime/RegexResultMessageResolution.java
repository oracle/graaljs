/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
