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
import com.oracle.truffle.regex.runtime.RegexResultObjectMessageResolutionFactory.ReadCacheNodeGen;

@MessageResolution(receiverType = RegexResultObject.class)
class RegexResultObjectMessageResolution {

    abstract static class ResultPropertyNode extends Node {

        abstract Object execute(RegexResultObject receiver);
    }

    static class GetInputNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResultObject receiver) {
            return receiver.getResult().getInput();
        }
    }

    static class IsMatchNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResultObject receiver) {
            return receiver.getResult() != RegexResult.NO_MATCH;
        }
    }

    static class GetGroupCountNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResultObject receiver) {
            return receiver.getResult().getGroupCount();
        }
    }

    static class GetStartArrayNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResultObject receiver) {
            return new RegexResultStartArrayObject(receiver.getResult());
        }
    }

    static class GetEndArrayNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResultObject receiver) {
            return new RegexResultEndArrayObject(receiver.getResult());
        }
    }

    static class GetCompiledRegexNode extends ResultPropertyNode {

        @Override
        Object execute(RegexResultObject receiver) {
            return new RegexCompiledRegex(receiver.getResult().getCompiledRegex());
        }
    }

    abstract static class ReadCacheNode extends Node {

        abstract Object execute(RegexResultObject receiver, String symbol);

        @Specialization(guards = "symbol == cachedSymbol", limit = "6")
        public Object readIdentity(RegexResultObject receiver, @SuppressWarnings("unused") String symbol,
                        @Cached("symbol") @SuppressWarnings("unused") String cachedSymbol,
                        @Cached("getResultProperty(symbol)") ResultPropertyNode propertyNode) {
            return propertyNode.execute(receiver);
        }

        @Specialization(guards = "symbol.equals(cachedSymbol)", limit = "6", replaces = "readIdentity")
        public Object readEquals(RegexResultObject receiver, @SuppressWarnings("unused") String symbol,
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

        public Object access(RegexResultObject receiver, String symbol) {
            return cache.execute(receiver, symbol);
        }
    }
}
