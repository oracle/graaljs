/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexObject;

@NodeInfo(language = "REGEX", description = "REGEX RootNode responsible for providing access to a RegexCompiledRegex")
public class RegexGetRegexObjectRootNode extends RootNode {

    private final RegexObject regex;

    public RegexGetRegexObjectRootNode(RegexLanguage language, RegexObject regex) {
        super(language, null);
        this.regex = regex;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return regex;
    }
}
