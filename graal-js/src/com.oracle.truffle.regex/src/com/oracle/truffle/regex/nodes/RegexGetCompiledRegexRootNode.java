/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.runtime.RegexCompiledRegex;

@NodeInfo(language = "REGEX", description = "REGEX RootNode responsible for providing access to a RegexCompiledRegex")
public class RegexGetCompiledRegexRootNode extends RootNode {

    private final RegexCompiledRegex regex;

    public RegexGetCompiledRegexRootNode(RegexLanguage language, FrameDescriptor frameDescriptor, RegexCompiledRegex regex) {
        super(language, frameDescriptor);
        this.regex = regex;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return regex;
    }
}
