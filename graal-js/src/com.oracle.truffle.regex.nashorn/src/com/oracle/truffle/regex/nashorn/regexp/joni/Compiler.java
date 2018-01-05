/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
}
