/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.node.debug;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.debug.Breakpoint;
import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.trufflenode.GraalJSAccess;

public class SetBreakPointNode extends JavaScriptRootNode {
    public static final String NAME = "setBreakPoint";
    private final GraalJSAccess graalJSAccess;

    public SetBreakPointNode(GraalJSAccess graalJSAccess) {
        this.graalJSAccess = graalJSAccess;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        Object arg0 = JSArguments.getUserArgument(args, 0);
        if (JSFunction.isJSFunction(arg0)) {
            CallTarget callTarget = JSFunction.getFunctionData((DynamicObject) arg0).getCallTarget();
            if (callTarget instanceof RootCallTarget) {
                SourceSection sourceSection = ((RootCallTarget) callTarget).getRootNode().getSourceSection();
                Source source = sourceSection.getSource();
                Object arg1 = JSArguments.getUserArgument(args, 1);
                if (arg1 instanceof Number) {
                    int lineNo = ((Number) arg1).intValue() + 1;
                    addBreakPoint(source, lineNo);
                    return 0;
                }
            }
        }
        unsupported();
        return 0;
    }

    @CompilerDirectives.TruffleBoundary
    private void addBreakPoint(Source source, int lineNo) {
        Breakpoint breakpoint = Breakpoint.newBuilder(source).lineIs(lineNo).build();
        Debugger debugger = graalJSAccess.lookupInstrument("debugger", Debugger.class);
        debugger.install(breakpoint);
    }

    @CompilerDirectives.TruffleBoundary
    private static void unsupported() {
        System.err.println("Unsupported usage of Debug.setBreakpoint!");
    }

}
