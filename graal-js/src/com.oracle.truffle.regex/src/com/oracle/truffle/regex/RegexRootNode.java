/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

@GenerateWrapper
public abstract class RegexRootNode extends RootNode implements InstrumentableNode {

    private static final FrameDescriptor SHARED_EMPTY_FRAMEDESCRIPTOR = new FrameDescriptor();

    private final RegexSource source;

    protected RegexRootNode(RegexLanguage language, FrameDescriptor frameDescriptor, RegexSource source) {
        super(language, frameDescriptor);
        this.source = source;
    }

    public RegexRootNode(RegexLanguage language, RegexSource source) {
        this(language, SHARED_EMPTY_FRAMEDESCRIPTOR, source);
    }

    protected RegexRootNode() {
        // necessary for @Instrumentable wrapper generation
        this(null, null);
    }

    public RegexSource getSource() {
        return source;
    }

    @Override
    public SourceSection getSourceSection() {
        return source.getSourceSection();
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.RootTag.class;
    }

    @Override
    public boolean isInstrumentable() {
        return super.isInstrumentable();
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new RegexRootNodeWrapper(this, probe);
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public final String toString() {
        return "regex " + getEngineLabel() + ": " + source;
    }

    protected String getEngineLabel() {
        return "no_engine_label";
    }
}
