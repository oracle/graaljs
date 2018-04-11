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
package com.oracle.truffle.regex.nashorn.regexp.joni;

// @formatter:off

import com.oracle.truffle.regex.nashorn.regexp.joni.ast.AnchorNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.BackRefNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.CClassNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.ConsAltNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.EncloseNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.Node;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.QuantifierNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.StringNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.EncloseType;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.NodeType;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.ErrorMessages;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.InternalException;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.SyntaxException;

public abstract class Compiler implements ErrorMessages {
    protected final Analyser analyser;
    protected final Regex regex;

    protected Compiler(final Analyser analyser) {
        this.analyser = analyser;
        this.regex = analyser.regex;
    }

    public final void compile() {
        prepare();
        compileTree(analyser.root);
        finish();
    }

    protected abstract void prepare();
    protected abstract void finish();

    protected abstract void compileAltNode(ConsAltNode node);

    protected void compileStringRawNode(final StringNode sn) {
        if (sn.length() <= 0) {
            return;
        }
        addCompileString(sn.chars, sn.p, sn.length(), false);
    }

    protected void compileStringNode(final StringNode node) {
        final StringNode sn = node;
        if (sn.length() <= 0) {
            return;
        }

        final boolean ambig = sn.isAmbig();

        int p, prev;
        p = prev = sn.p;
        final int end = sn.end;
        final char[] chars = sn.chars;
        p++;
        int slen = 1;

        while (p < end) {
            slen++;
            p++;
        }
        addCompileString(chars, prev, slen, ambig);
    }

    protected abstract void addCompileString(char[] chars, int p, int strLength, boolean ignoreCase);

    protected abstract void compileCClassNode(CClassNode node);
    protected abstract void compileAnyCharNode();
    protected abstract void compileBackrefNode(BackRefNode node);
    protected abstract void compileNonCECQuantifierNode(QuantifierNode node);
    protected abstract void compileOptionNode(EncloseNode node);
    protected abstract void compileEncloseNode(EncloseNode node);
    protected abstract void compileAnchorNode(AnchorNode node);

    protected void compileTree(final Node node) {
        switch (node.getType()) {
        case NodeType.LIST:
            ConsAltNode lin = (ConsAltNode)node;
            do {
                compileTree(lin.car);
            } while ((lin = lin.cdr) != null);
            break;

        case NodeType.ALT:
            compileAltNode((ConsAltNode)node);
            break;

        case NodeType.STR:
            final StringNode sn = (StringNode)node;
            if (sn.isRaw()) {
                compileStringRawNode(sn);
            } else {
                compileStringNode(sn);
            }
            break;

        case NodeType.CCLASS:
            compileCClassNode((CClassNode)node);
            break;

        case NodeType.CANY:
            compileAnyCharNode();
            break;

        case NodeType.BREF:
            compileBackrefNode((BackRefNode)node);
            break;

        case NodeType.QTFR:
            compileNonCECQuantifierNode((QuantifierNode)node);
            break;

        case NodeType.ENCLOSE:
            final EncloseNode enode = (EncloseNode)node;
            if (enode.isOption()) {
                compileOptionNode(enode);
            } else {
                compileEncloseNode(enode);
            }
            break;

        case NodeType.ANCHOR:
            compileAnchorNode((AnchorNode)node);
            break;

        default:
            // undefined node type
            newInternalException(ERR_PARSER_BUG);
        } // switch
    }

    protected final void compileTreeNTimes(final Node node, final int n) {
        for (int i=0; i<n; i++) {
            compileTree(node);
        }
    }

    protected void newSyntaxException(final String message) {
        throw new SyntaxException(message);
    }

    protected void newInternalException(final String message) {
        throw new InternalException(message);
    }

    protected Range findEnclosedCaptureGroups(final Node node) {
        switch(node.getType()) {
            case NodeType.LIST:
            case NodeType.ALT:
                ConsAltNode lin = (ConsAltNode)node;
                Range ret = findEnclosedCaptureGroups(lin.car);
                while ((lin = lin.cdr) != null) {
                    ret = ret.union(findEnclosedCaptureGroups(lin.car));
                }
                return ret;
            case NodeType.QTFR:
                QuantifierNode quantifierNode = (QuantifierNode)node;
                return findEnclosedCaptureGroups(quantifierNode.target);
            case NodeType.ENCLOSE:
                EncloseNode encloseNode = (EncloseNode)node;
                Range inner = findEnclosedCaptureGroups(encloseNode.target);
                if (encloseNode.type == EncloseType.MEMORY) {
                    return new Range(encloseNode.regNum).union(inner);
                } else {
                    return inner;
                }
            case NodeType.STR:
            case NodeType.CCLASS:
            case NodeType.CANY:
            case NodeType.BREF:
            case NodeType.ANCHOR:
                return Range.EMPTY;
            default:
                // undefined node type
                throw new InternalException(ERR_PARSER_BUG);
        }
    }

    /**
     * Range represents a range of integers. The representation is an implementation
     * of the variant type {@code Empty | NonEmpty of (from:int) * (to:int)}.
     * The lower bound {@link #from} is inclusive, the upper bound {@link #to} is exclusive.
     */
    protected static class Range {
        public boolean empty;
        public int from;
        public int to;

        public static final Range EMPTY = new Range();

        public Range() {
            this.empty = true;
        }

        public Range(int singleton) {
            this.empty = false;
            this.from = singleton;
            this.to = singleton + 1;
        }

        public Range(int from, int to) {
            this.empty = false;
            this.from = from;
            this.to = to;
        }

        public Range union(Range other) {
            if (this.empty) {
                return other;
            } else if (other.empty) {
                return this;
            } else {
                return new Range(Math.min(this.from, other.from), Math.max(this.to, other.to));
            }
        }
    }
}
