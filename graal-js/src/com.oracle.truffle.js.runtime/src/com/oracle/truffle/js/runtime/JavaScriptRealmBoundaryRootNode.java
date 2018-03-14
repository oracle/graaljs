/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

/**
 * A RootNode for all cases where the body could throw and we would need to set the Realm to the
 * exception.
 *
 */
public abstract class JavaScriptRealmBoundaryRootNode extends JavaScriptRootNode {

    @CompilationFinal private boolean seenException;
    @CompilationFinal private boolean seenNullRealm;

    protected JavaScriptRealmBoundaryRootNode(AbstractJavaScriptLanguage lang, SourceSection sourceSection, FrameDescriptor frameDescriptor) {
        super(lang, sourceSection, frameDescriptor);
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        try {
            return executeAndSetRealm(frame);
        } catch (JSException ex) {
            if (!seenException) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                seenException = true;
            }
            if (ex.getRealm() == null) {
                if (!seenNullRealm) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    seenNullRealm = true;
                }
                ex.setRealm(getRealm());
            }
            throw ex;
        } catch (StackOverflowError ex) {
            CompilerDirectives.transferToInterpreter();
            throw Errors.createRangeErrorStackOverflow(ex).setRealm(getRealm());
        }
    }

    protected abstract Object executeAndSetRealm(VirtualFrame frame);

    protected abstract JSRealm getRealm();
}
