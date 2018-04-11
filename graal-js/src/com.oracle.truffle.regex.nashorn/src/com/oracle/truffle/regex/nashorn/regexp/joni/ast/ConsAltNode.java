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

import com.oracle.truffle.regex.nashorn.regexp.joni.WarnCallback;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.ErrorMessages;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.InternalException;

public final class ConsAltNode extends Node {
    public Node car;
    public ConsAltNode cdr;
    private int type;           // List or Alt

    private ConsAltNode(final Node car, final ConsAltNode cdr, final int type) {
        this.car = car;
        if (car != null) {
            car.parent = this;
        }
        this.cdr = cdr;
        if (cdr != null) {
            cdr.parent = this;
        }

        this.type = type;
    }

    public static ConsAltNode newAltNode(final Node left, final ConsAltNode right) {
        return new ConsAltNode(left, right, ALT);
    }

    public static ConsAltNode newListNode(final Node left, final ConsAltNode right) {
        return new ConsAltNode(left, right, LIST);
    }

    public static ConsAltNode listAdd(final ConsAltNode listp, final Node x) {
        final ConsAltNode n = newListNode(x, null);
        ConsAltNode list = listp;

        if (list != null) {
            while (list.cdr != null) {
                list = list.cdr;
            }
            list.setCdr(n);
        }
        return n;
    }

    public void toListNode() {
        type = LIST;
    }

    public void toAltNode() {
        type = ALT;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    protected void setChild(final Node newChild) {
        car = newChild;
    }

    @Override
    protected Node getChild() {
        return car;
    }

    @Override
    public void swap(final Node with) {
        if (cdr != null) {
            cdr.parent = with;
            if (with instanceof ConsAltNode) {
                final ConsAltNode withCan = (ConsAltNode)with;
                withCan.cdr.parent = this;
                final ConsAltNode tmp = cdr;
                cdr = withCan.cdr;
                withCan.cdr = tmp;
            }
        }
        super.swap(with);
    }

    @Override
    public void verifyTree(final Set<Node> set, final WarnCallback warnings) {
        if (!set.contains(this)) {
            set.add(this);
            if (car != null) {
                if (car.parent != this) {
                    warnings.warn("broken list car: " + this.getAddressName() + " -> " +  car.getAddressName());
                }
                car.verifyTree(set,warnings);
            }
            if (cdr != null) {
                if (cdr.parent != this) {
                    warnings.warn("broken list cdr: " + this.getAddressName() + " -> " +  cdr.getAddressName());
                }
                cdr.verifyTree(set,warnings);
            }
        }
    }

    public Node setCar(final Node ca) {
        car = ca;
        ca.parent = this;
        return car;
    }

    public ConsAltNode setCdr(final ConsAltNode cd) {
        cdr = cd;
        cd.parent = this;
        return cdr;
    }

    @Override
    public String getName() {
        switch (type) {
        case ALT:
            return "Alt";
        case LIST:
            return "List";
        default:
            throw new InternalException(ErrorMessages.ERR_PARSER_BUG);
        }
    }

    @Override
    public String toString(final int level) {
        final StringBuilder value = new StringBuilder();
        value.append("\n  car: " + pad(car, level + 1));
        value.append("\n  cdr: " + (cdr == null ? "NULL" : cdr.toString()));

        return value.toString();
    }

}
