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
import com.oracle.truffle.regex.runtime.RegexFlagsObjectMessageResolutionFactory.ReadCacheNodeGen;

@MessageResolution(receiverType = RegexFlagsObject.class)
class RegexFlagsObjectMessageResolution {

    abstract static class RegexFlagsObjectPropertyNode extends Node {

        abstract Object execute(RegexFlagsObject receiver);
    }

    static class RegexFlagsGetSourceNode extends RegexFlagsObjectPropertyNode {

        @Override
        Object execute(RegexFlagsObject receiver) {
            return receiver.getFlags().getSource();
        }
    }

    static class RegexFlagsGetIgnoreCaseNode extends RegexFlagsObjectPropertyNode {

        @Override
        Object execute(RegexFlagsObject receiver) {
            return receiver.getFlags().isIgnoreCase();
        }
    }

    static class RegexFlagsGetMultilineNode extends RegexFlagsObjectPropertyNode {

        @Override
        Object execute(RegexFlagsObject receiver) {
            return receiver.getFlags().isMultiline();
        }
    }

    static class RegexFlagsGetStickyNode extends RegexFlagsObjectPropertyNode {

        @Override
        Object execute(RegexFlagsObject receiver) {
            return receiver.getFlags().isSticky();
        }
    }

    static class RegexFlagsGetGlobalNode extends RegexFlagsObjectPropertyNode {

        @Override
        Object execute(RegexFlagsObject receiver) {
            return receiver.getFlags().isGlobal();
        }
    }

    static class RegexFlagsGetUnicodeNode extends RegexFlagsObjectPropertyNode {

        @Override
        Object execute(RegexFlagsObject receiver) {
            return receiver.getFlags().isUnicode();
        }
    }

    static class RegexFlagsGetDotAllNode extends RegexFlagsObjectPropertyNode {

        @Override
        Object execute(RegexFlagsObject receiver) {
            return receiver.getFlags().isDotAll();
        }
    }

    abstract static class ReadCacheNode extends Node {

        abstract Object execute(RegexFlagsObject receiver, String symbol);

        @Specialization(guards = "symbol == cachedSymbol", limit = "7")
        public Object readIdentity(RegexFlagsObject receiver, @SuppressWarnings("unused") String symbol,
                        @Cached("symbol") @SuppressWarnings("unused") String cachedSymbol,
                        @Cached("getResultProperty(symbol)") RegexFlagsObjectPropertyNode propertyNode) {
            return propertyNode.execute(receiver);
        }

        @Specialization(guards = "symbol.equals(cachedSymbol)", limit = "7", replaces = "readIdentity")
        public Object readEquals(RegexFlagsObject receiver, @SuppressWarnings("unused") String symbol,
                        @Cached("symbol") @SuppressWarnings("unused") String cachedSymbol,
                        @Cached("getResultProperty(symbol)") RegexFlagsObjectPropertyNode propertyNode) {
            return propertyNode.execute(receiver);
        }

        static RegexFlagsObjectPropertyNode getResultProperty(String symbol) {
            switch (symbol) {
                case "source":
                    return new RegexFlagsGetSourceNode();
                case "ignoreCase":
                    return new RegexFlagsGetIgnoreCaseNode();
                case "multiline":
                    return new RegexFlagsGetMultilineNode();
                case "sticky":
                    return new RegexFlagsGetStickyNode();
                case "global":
                    return new RegexFlagsGetGlobalNode();
                case "unicode":
                    return new RegexFlagsGetUnicodeNode();
                case "dotAll":
                    return new RegexFlagsGetDotAllNode();
                default:
                    throw UnknownIdentifierException.raise(symbol);
            }
        }
    }

    @Resolve(message = "READ")
    abstract static class RegexFlagsReadNode extends Node {

        @Child ReadCacheNode cache = ReadCacheNodeGen.create();

        public Object access(RegexFlagsObject receiver, String symbol) {
            return cache.execute(receiver, symbol);
        }
    }
}
