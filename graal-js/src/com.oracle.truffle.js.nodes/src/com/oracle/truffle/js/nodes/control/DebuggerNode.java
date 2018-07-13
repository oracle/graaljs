/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.nodes.control;

import java.text.MessageFormat;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
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

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == DebuggerTags.AlwaysHalt.class) {
            return true;
        }
        return super.hasTag(tag);
    }
}
