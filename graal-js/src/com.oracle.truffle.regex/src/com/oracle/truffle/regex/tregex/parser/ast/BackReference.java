/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast;

import com.oracle.truffle.regex.tregex.util.DebugUtil;

/**
 * A reference to the contents of a previously matched capturing group.
 * <p>
 * Corresponds to the goal symbol <em>DecimalEscape</em> in the ECMAScript RegExp syntax.
 * <p>
 * Currently not implemented in TRegex and so any use of this node type causes TRegex to bail out.
 */
public class BackReference extends Term {

    private final int groupNr;

    BackReference(int groupNr) {
        this.groupNr = groupNr;
    }

    private BackReference(BackReference copy) {
        super(copy);
        groupNr = copy.groupNr;
    }

    @Override
    public BackReference copy(RegexAST ast) {
        return ast.register(new BackReference(this));
    }

    @Override
    public String toString() {
        return "\\" + groupNr;
    }

    public int getGroupNr() {
        return groupNr;
    }

    @Override
    public DebugUtil.Table toTable() {
        return toTable("BackReference").append(new DebugUtil.Value("groupNr", groupNr));
    }
}
