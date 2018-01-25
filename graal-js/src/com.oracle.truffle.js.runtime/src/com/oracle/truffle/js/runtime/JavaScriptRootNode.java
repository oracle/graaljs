/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

public abstract class JavaScriptRootNode extends RootNode {
    private static final FrameDescriptor SHARED_EMPTY_FRAMEDESCRIPTOR = new FrameDescriptor();
    private final SourceSection sourceSection;

    protected JavaScriptRootNode() {
        this(null, null, null);
    }

    protected JavaScriptRootNode(AbstractJavaScriptLanguage lang, SourceSection sourceSection, FrameDescriptor frameDescriptor) {
        super(lang, substituteNullWithSharedEmptyFrameDescriptor(frameDescriptor));
        this.sourceSection = sourceSection;
    }

    private static FrameDescriptor substituteNullWithSharedEmptyFrameDescriptor(FrameDescriptor frameDescriptor) {
        return frameDescriptor == null ? SHARED_EMPTY_FRAMEDESCRIPTOR : frameDescriptor;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    @Override
    public boolean isInternal() {
        SourceSection sc = getSourceSection();
        if (sc != null) {
            return sc.getSource().isInternal();
        }
        return false;
    }

    public boolean isFunction() {
        return false;
    }

    @Override
    public boolean isCaptureFramesForTrace() {
        return isFunction();
    }
}
