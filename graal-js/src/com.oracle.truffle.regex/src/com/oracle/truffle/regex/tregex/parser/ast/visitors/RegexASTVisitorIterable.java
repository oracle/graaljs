/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast.visitors;

import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;

public interface RegexASTVisitorIterable {

    boolean visitorHasNext();

    RegexASTNode visitorGetNext(boolean reverse);

    void resetVisitorIterator();
}
