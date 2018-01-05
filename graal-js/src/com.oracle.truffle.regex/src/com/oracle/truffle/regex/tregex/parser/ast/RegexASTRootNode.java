/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast;

import com.oracle.truffle.regex.tregex.parser.ast.visitors.InitIDVisitor;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

/**
 * Root node of every AST.
 */
public class RegexASTRootNode extends RegexASTSubtreeRootNode {

    RegexASTRootNode() {
        setId(InitIDVisitor.REGEX_AST_ROOT_PARENT_ID);
    }

    private RegexASTRootNode(RegexASTRootNode copy, RegexAST ast) {
        super(copy, ast);
    }

    @Override
    public RegexASTSubtreeRootNode copy(RegexAST ast) {
        return new RegexASTRootNode(this, ast);
    }

    @Override
    public String getPrefix() {
        return "ROOT";
    }

    @Override
    public String toString() {
        return getGroup().toString();
    }

    @Override
    public DebugUtil.Table toTable() {
        return getGroup().toTable();
    }
}
