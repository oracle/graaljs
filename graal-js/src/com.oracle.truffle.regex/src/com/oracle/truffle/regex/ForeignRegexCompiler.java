/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

/**
 * {@link ForeignRegexCompiler} wraps a {@link TruffleObject} that is compatible with
 * {@link RegexCompiler} and lets us use it as if it were an actual {@link RegexCompiler}.
 * 
 * @author Jirka Marsik <jiri.marsik@oracle.com>
 */
public class ForeignRegexCompiler extends RegexCompiler {

    private final TruffleObject foreignCompiler;

    private final Node executeNode = Message.createExecute(2).createNode();

    public ForeignRegexCompiler(TruffleObject foreignCompiler) {
        this.foreignCompiler = foreignCompiler;
    }

    /**
     * Wraps the supplied {@link TruffleObject} in a {@link ForeignRegexCompiler}, unless it already
     * is a {@link RegexCompiler}. Use this when accepting {@link RegexCompiler}s over Truffle
     * interop.
     */
    public static RegexCompiler importRegexCompiler(TruffleObject regexCompiler) {
        return regexCompiler instanceof RegexCompiler ? (RegexCompiler) regexCompiler : new ForeignRegexCompiler(regexCompiler);
    }

    @Override
    public TruffleObject compile(RegexSource source) throws RegexSyntaxException {
        try {
            return (TruffleObject) ForeignAccess.sendExecute(executeNode, foreignCompiler, source.getPattern(), source.getFlags().toString());
        } catch (InteropException ex) {
            throw ex.raise();
        }
    }
}
