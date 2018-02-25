/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.RegexObject;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.util.NumberConversion;

@ImportStatic(com.oracle.truffle.api.interop.Message.class)
public abstract class NewExecuteRegexDispatchNode extends Node {

    public abstract TruffleObject execute(TruffleObject receiver, RegexObject regexObject, Object input, Object fromIndex);

    @Specialization(guards = "receiver == cachedReceiver", limit = "4")
    public TruffleObject doCached(TruffleObject receiver, RegexObject regexObject, Object input, Object fromIndex,
                    @Cached("create()") ExpectStringOrTruffleObjectNode expectStringOrTruffleObjectNode,
                    @Cached("create()") ExpectNumberNode expectNumberNode,
                    @SuppressWarnings("unused") @Cached("receiver") TruffleObject cachedReceiver,
                    @Cached("createExecute(2).createNode()") Node executeNode) {
        final Object unboxedInput = expectStringOrTruffleObjectNode.execute(input);
        final Number fromIndexNumber = expectNumberNode.execute(fromIndex);
        if (fromIndexNumber instanceof Long && ((Long) fromIndexNumber) > Integer.MAX_VALUE) {
            return RegexResult.NO_MATCH;
        }
        try {
            return (TruffleObject) ForeignAccess.sendExecute(executeNode, receiver, regexObject, unboxedInput, NumberConversion.intValue(fromIndexNumber));
        } catch (InteropException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Specialization(replaces = "doCached")
    public TruffleObject doUnCached(TruffleObject receiver, RegexObject regexObject, Object input, Object fromIndex,
                    @Cached("create()") ExpectStringOrTruffleObjectNode expectStringOrTruffleObjectNode,
                    @Cached("create()") ExpectNumberNode expectNumberNode,
                    @Cached("createExecute(2).createNode()") Node executeNode) {
        final Object unboxedInput = expectStringOrTruffleObjectNode.execute(input);
        final Number fromIndexNumber = expectNumberNode.execute(fromIndex);
        if (fromIndexNumber instanceof Long && ((Long) fromIndexNumber) > Integer.MAX_VALUE) {
            return RegexResult.NO_MATCH;
        }
        try {
            return (TruffleObject) ForeignAccess.sendExecute(executeNode, receiver, regexObject, unboxedInput, NumberConversion.intValue(fromIndexNumber));
        } catch (InteropException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static NewExecuteRegexDispatchNode create() {
        return NewExecuteRegexDispatchNodeGen.create();
    }
}