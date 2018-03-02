/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.source.SourceSection;

@GenerateWrapper
public abstract class RegexBodyNode extends ExecutableNode implements InstrumentableNode {

    private final RegexSource source;
    private final RegexLanguage language;

    protected RegexBodyNode(RegexLanguage language, RegexSource source) {
        super(language);
        this.source = source;
        this.language = language;
    }

    protected RegexBodyNode(RegexBodyNode copy) {
        this(copy.language, copy.source);
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
        return true;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new RegexBodyNodeWrapper(this, this, probe);
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
