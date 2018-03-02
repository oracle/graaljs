/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

public final class RegexRootNode extends RootNode {

    @Child private RegexBodyNode body;

    public RegexRootNode(RegexLanguage language, FrameDescriptor frameDescriptor, RegexBodyNode body) {
        super(language, frameDescriptor);
        this.body = body;
    }

    private static final FrameDescriptor SHARED_EMPTY_FRAMEDESCRIPTOR = new FrameDescriptor();

    public RegexRootNode(RegexLanguage language, RegexBodyNode body) {
        super(language, SHARED_EMPTY_FRAMEDESCRIPTOR);
        this.body = body;
    }

    public RegexSource getSource() {
        return body.getSource();
    }

    @Override
    public SourceSection getSourceSection() {
        return body.getSourceSection();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return body.execute(frame);
    }

    @Override
    public String toString() {
        return body.toString();
    }
}
