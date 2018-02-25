/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.RegexObject;
import com.oracle.truffle.regex.runtime.RegexObjectMessageResolutionFactory.ReadCacheNodeGen;
import com.oracle.truffle.regex.runtime.nodes.NewExecuteRegexDispatchNode;

@MessageResolution(receiverType = RegexObject.class)
public class RegexObjectMessageResolution {

    abstract static class RegexObjectPropertyNode extends Node {

        abstract Object execute(RegexObject receiver);
    }

    static class GetPatternNode extends RegexObjectPropertyNode {

        @Override
        Object execute(RegexObject receiver) {
            return receiver.getSource().getPattern();
        }
    }

    static class GetFlagsNode extends RegexObjectPropertyNode {

        @Override
        Object execute(RegexObject receiver) {
            return receiver.getSource().getFlags();
        }
    }

    static class GetExecMethodNode extends RegexObjectPropertyNode {

        @Override
        Object execute(RegexObject receiver) {
            return receiver.getExecMethod();
        }
    }

    abstract static class ReadCacheNode extends Node {

        abstract Object execute(RegexObject receiver, String symbol);

        @Specialization(guards = "symbol == cachedSymbol", limit = "3")
        Object readIdentity(RegexObject receiver, @SuppressWarnings("unused") String symbol,
                        @Cached("symbol") @SuppressWarnings("unused") String cachedSymbol,
                        @Cached("getResultProperty(symbol)") RegexObjectPropertyNode propertyNode) {
            return propertyNode.execute(receiver);
        }

        @Specialization(guards = "symbol.equals(cachedSymbol)", limit = "3", replaces = "readIdentity")
        Object readEquals(RegexObject receiver, @SuppressWarnings("unused") String symbol,
                        @Cached("symbol") @SuppressWarnings("unused") String cachedSymbol,
                        @Cached("getResultProperty(symbol)") RegexObjectPropertyNode propertyNode) {
            return propertyNode.execute(receiver);
        }

        static RegexObjectPropertyNode getResultProperty(String symbol) {
            switch (symbol) {
                case "exec":
                    return new GetExecMethodNode();
                case "pattern":
                    return new GetPatternNode();
                case "flags":
                    return new GetFlagsNode();
                default:
                    throw UnknownIdentifierException.raise(symbol);
            }
        }
    }

    @Resolve(message = "READ")
    abstract static class RegexObjectReadNode extends Node {

        @Child ReadCacheNode cache = ReadCacheNodeGen.create();

        public Object access(RegexObject receiver, String symbol) {
            return cache.execute(receiver, symbol);
        }
    }

    @Resolve(message = "INVOKE")
    abstract static class RegexObjectInvokeNode extends Node {

        @Child private NewExecuteRegexDispatchNode doExecute = NewExecuteRegexDispatchNode.create();

        public Object access(RegexObject receiver, String name, Object[] args) {
            if (!name.equals("exec")) {
                throw UnknownIdentifierException.raise(name);
            }
            if (args.length != 2) {
                throw ArityException.raise(2, args.length);
            }
            return doExecute.execute(receiver.getCompiledRegexObject(), receiver, args[0], args[1]);
        }
    }
}
