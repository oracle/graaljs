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
import com.oracle.truffle.regex.RegexCompiledRegex;
import com.oracle.truffle.regex.runtime.RegexCompiledRegexMessageResolutionFactory.ReadCacheNodeGen;
import com.oracle.truffle.regex.runtime.nodes.ExecuteRegexDispatchNode;

@MessageResolution(receiverType = RegexCompiledRegex.class)
public class RegexCompiledRegexMessageResolution {

    abstract static class CompiledRegexPropertyNode extends Node {

        abstract Object execute(RegexCompiledRegex receiver);
    }

    static class GetPatternNode extends CompiledRegexPropertyNode {

        @Override
        Object execute(RegexCompiledRegex receiver) {
            return receiver.getSource().getPattern();
        }
    }

    static class GetFlagsNode extends CompiledRegexPropertyNode {

        @Override
        Object execute(RegexCompiledRegex receiver) {
            return receiver.getSource().getFlags();
        }
    }

    static class GetExecMethodNode extends CompiledRegexPropertyNode {

        @Override
        Object execute(RegexCompiledRegex receiver) {
            return receiver.getExecMethod();
        }
    }

    abstract static class ReadCacheNode extends Node {

        abstract Object execute(RegexCompiledRegex receiver, String symbol);

        @Specialization(guards = "symbol == cachedSymbol", limit = "3")
        Object readIdentity(RegexCompiledRegex receiver, @SuppressWarnings("unused") String symbol,
                        @Cached("symbol") @SuppressWarnings("unused") String cachedSymbol,
                        @Cached("getResultProperty(symbol)") CompiledRegexPropertyNode propertyNode) {
            return propertyNode.execute(receiver);
        }

        @Specialization(guards = "symbol.equals(cachedSymbol)", limit = "3", replaces = "readIdentity")
        Object readEquals(RegexCompiledRegex receiver, @SuppressWarnings("unused") String symbol,
                        @Cached("symbol") @SuppressWarnings("unused") String cachedSymbol,
                        @Cached("getResultProperty(symbol)") CompiledRegexPropertyNode propertyNode) {
            return propertyNode.execute(receiver);
        }

        static CompiledRegexPropertyNode getResultProperty(String symbol) {
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
    abstract static class CompiledRegexReadNode extends Node {

        @Child ReadCacheNode cache = ReadCacheNodeGen.create();

        public Object access(RegexCompiledRegex receiver, String symbol) {
            return cache.execute(receiver, symbol);
        }
    }

    @Resolve(message = "INVOKE")
    abstract static class CompiledRegexInvokeNode extends Node {

        @Child private ExecuteRegexDispatchNode doExecute = ExecuteRegexDispatchNode.create();

        public Object access(RegexCompiledRegex receiver, String name, Object[] args) {
            if (!name.equals("exec")) {
                throw UnknownIdentifierException.raise(name);
            }
            if (args.length != 2) {
                throw ArityException.raise(2, args.length);
            }
            return doExecute.execute(receiver, args[0], args[1]);
        }
    }
}
