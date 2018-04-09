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
/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.regex.nashorn.regexp.joni.ast;

// @formatter:off

import com.oracle.truffle.regex.nashorn.regexp.joni.Option;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.EncloseType;

public final class EncloseNode extends StateNode implements EncloseType {

    public final int type;                // enclose type
    public int regNum;
    public int option;
    public Node target;             /* EncloseNode : ENCLOSE_MEMORY */
    public int callAddr;            // AbsAddrType
    public int minLength;           // OnigDistance
    public int maxLength;           // OnigDistance
    public int charLength;
    public int optCount;            // referenced count in optimize_node_left()

    // node_new_enclose / onig_node_new_enclose
    public EncloseNode(final int type) {
        this.type = type;
        callAddr = -1;
    }

    // node_new_enclose_memory
    public EncloseNode() {
        this(MEMORY);
    }

    // node_new_option
    @SuppressWarnings("unused")
    public EncloseNode(final int option, final int i) {
        this(OPTION);
        this.option = option;
    }

    @Override
    public int getType() {
        return ENCLOSE;
    }

    @Override
    protected void setChild(final Node newChild) {
        target = newChild;
    }

    @Override
    protected Node getChild() {
        return target;
    }

    public void setTarget(final Node tgt) {
        target = tgt;
        tgt.parent = this;
    }

    @Override
    public String getName() {
        return "Enclose";
    }

    @Override
    public String toString(final int level) {
        final StringBuilder value = new StringBuilder(super.toString(level));
        value.append("\n  type: " + typeToString());
        value.append("\n  regNum: " + regNum);
        value.append("\n  option: " + Option.toString(option));
        value.append("\n  target: " + pad(target, level + 1));
        value.append("\n  callAddr: " + callAddr);
        value.append("\n  minLength: " + minLength);
        value.append("\n  maxLength: " + maxLength);
        value.append("\n  charLength: " + charLength);
        value.append("\n  optCount: " + optCount);

        return value.toString();
    }

    public String typeToString() {
        final StringBuilder types = new StringBuilder();
        if (isStopBacktrack()) types.append("STOP_BACKTRACK ");
        if (isMemory()) types.append("MEMORY ");
        if (isOption()) types.append("OPTION ");

        return types.toString();
    }

    public boolean isMemory() {
        return (type & MEMORY) != 0;
    }

    public boolean isOption() {
        return (type & OPTION) != 0;
    }

    public boolean isStopBacktrack() {
        return (type & STOP_BACKTRACK) != 0;
    }

}
