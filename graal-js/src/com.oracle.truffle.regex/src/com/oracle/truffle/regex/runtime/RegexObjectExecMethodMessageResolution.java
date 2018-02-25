/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = RegexObjectExecMethod.class)
class RegexObjectExecMethodMessageResolution {

    @Resolve(message = "EXECUTE")
    abstract static class ExecuteRegexExecNode extends Node {

        @Child Node executeNode = Message.createExecute(3).createNode();

        public Object access(RegexObjectExecMethod receiver, Object[] args) {
            if (args.length != 2) {
                throw ArityException.raise(2, args.length);
            }
            try {
                return ForeignAccess.sendExecute(executeNode, receiver.getRegexObject().getCompiledRegexObject(), receiver.getRegexObject(), args[0], args[1]);
            } catch (InteropException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Resolve(message = "IS_EXECUTABLE")
    abstract static class IsExecutableRegexExecNode extends Node {

        @SuppressWarnings("unused")
        public boolean access(RegexObjectExecMethod receiver) {
            return true;
        }
    }
}
