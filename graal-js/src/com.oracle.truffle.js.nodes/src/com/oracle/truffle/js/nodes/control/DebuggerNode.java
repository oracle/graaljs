/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import java.text.MessageFormat;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;

/**
 * 12.15 The debugger statement. This is basically just a breakpoint.
 */
@NodeInfo(shortName = "debugger")
public class DebuggerNode extends StatementNode {

    private static long time;
    private static boolean timingEnabled;

    DebuggerNode() {
        addAlwaysHaltTag();
    }

    public static DebuggerNode create() {
        return new DebuggerNode();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        doTiming();
        return EMPTY;
    }

    public static void timingStart() {
        time = System.nanoTime();
        timingEnabled = true;
    }

    public static void timingStop() {
        timingEnabled = false;
    }

    @TruffleBoundary
    private static void doTiming() {
        if (timingEnabled) {
            long now = System.nanoTime();
            if (time != 0L) {
                System.out.println(MessageFormat.format("run time: {0,number,#,###.##} ms", (now - time) / 1000d / 1000d));
            }
            time = now;
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create();
    }
}
