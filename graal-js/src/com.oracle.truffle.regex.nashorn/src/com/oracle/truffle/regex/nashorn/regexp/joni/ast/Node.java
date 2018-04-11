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

import java.util.Set;

import com.oracle.truffle.regex.nashorn.regexp.joni.Config;
import com.oracle.truffle.regex.nashorn.regexp.joni.WarnCallback;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.NodeType;

public abstract class Node implements NodeType {
    public Node parent;

    public abstract int getType();

    public final int getType2Bit() {
        return 1 << getType();
    }

    @SuppressWarnings("unused")
    protected void setChild(final Node tgt) {
        //empty, default definition
    }
    protected Node getChild() {
        return null; // default definition
        }

    public void swap(final Node with) {
        Node tmp;

        //if (getChild() != null) getChild().parent = with;
        //if (with.getChild() != null) with.getChild().parent = this;

        //tmp = getChild();
        //setChild(with.getChild());
        //with.setChild(tmp);

        if (parent != null) {
            parent.setChild(with);
        }

        if (with.parent != null) {
            with.parent.setChild(this);
        }

        tmp = parent;
        parent = with.parent;
        with.parent = tmp;
    }

    // overridden by ConsAltNode and CallNode
    public void verifyTree(final Set<Node> set, final WarnCallback warnings) {
        if (!set.contains(this) && getChild() != null) {
            set.add(this);
            if (getChild().parent != this) {
                warnings.warn("broken link to child: " + this.getAddressName() + " -> " + getChild().getAddressName());
            }
            getChild().verifyTree(set, warnings);
        }
    }

    public abstract String getName();
    protected abstract String toString(int level);

    public String getAddressName() {
        return getName() + ":0x" + Integer.toHexString(System.identityHashCode(this));
    }

    @Override
    public final String toString() {
        final StringBuilder s = new StringBuilder();
        s.append("<" + getAddressName() + " (" + (parent == null ? "NULL" : parent.getAddressName())  + ")>");
        return s + toString(0);
    }

    protected static String pad(final Object value, final int level) {
        if (value == null) {
            return "NULL";
        }

        final StringBuilder pad = new StringBuilder("  ");
        for (int i=0; i<level; i++) {
            pad.append(pad);
        }

        return value.toString().replace("\n",  "\n" + pad);
    }

    public final boolean isInvalidQuantifier() {
        if (!Config.VANILLA) {
            return false;
        }

        ConsAltNode node;

        switch(getType()) {

        case ANCHOR:
            return true;

        case ENCLOSE:
            /* allow enclosed elements */
            /* return is_invalid_quantifier_target(NENCLOSE(node)->target); */
            break;

        case LIST:
            node = (ConsAltNode)this;
            do {
                if (!node.car.isInvalidQuantifier()) {
                    return false;
                }
            } while ((node = node.cdr) != null);
            return false;

        case ALT:
            node = (ConsAltNode)this;
            do {
                if (node.car.isInvalidQuantifier()) {
                    return true;
                }
            } while ((node = node.cdr) != null);
            break;

        default:
            break;
        }

        return false;
    }

    public final boolean isAllowedInLookBehind() {
        return (getType2Bit() & ALLOWED_IN_LB) != 0;
    }

    public final boolean isSimple() {
        return (getType2Bit() & SIMPLE) != 0;
    }
}
