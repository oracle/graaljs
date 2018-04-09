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

import static com.oracle.truffle.regex.nashorn.regexp.joni.BitStatus.bsClear;

import com.oracle.truffle.regex.nashorn.regexp.joni.ast.Node;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.ErrorMessages;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.InternalException;

public final class ScanEnvironment {

    private static final int SCANENV_MEMNODES_SIZE = 8;

    int option;
    final int caseFoldFlag;
    final public Syntax syntax;
    int captureHistory;
    int btMemStart;
    int btMemEnd;
    int backrefedMem;

    final public Regex reg;

    public int numMem;

    public Node memNodes[];


    public ScanEnvironment(final Regex regex, final Syntax syntax) {
        this.reg = regex;
        option = regex.options;
        caseFoldFlag = regex.caseFoldFlag;
        this.syntax = syntax;
    }

    public void clear() {
        captureHistory = bsClear();
        btMemStart = bsClear();
        btMemEnd = bsClear();
        backrefedMem = bsClear();

        numMem = 0;
        memNodes = null;
    }

    public int addMemEntry() {
        if (numMem++ == 0) {
            memNodes = new Node[SCANENV_MEMNODES_SIZE];
        } else if (numMem >= memNodes.length) {
            final Node[]tmp = new Node[memNodes.length << 1];
            System.arraycopy(memNodes, 0, tmp, 0, memNodes.length);
            memNodes = tmp;
        }

        return numMem;
    }

    public void setMemNode(final int num, final Node node) {
        if (numMem >= num) {
            memNodes[num] = node;
        } else {
            throw new InternalException(ErrorMessages.ERR_PARSER_BUG);
        }
    }

    public int convertBackslashValue(final int c) {
        if (syntax.opEscControlChars()) {
            switch (c) {
            case 'n': return '\n';
            case 't': return '\t';
            case 'r': return '\r';
            case 'f': return '\f';
            case 'a': return '\007';
            case 'b': return '\010';
            case 'e': return '\033';
            case 'v':
                if (syntax.op2EscVVtab())
                 {
                    return 11; // ???
                }
                break;
            default:
                break;
            }
        }
        return c;
    }

    void ccEscWarn(final String s) {
        if (Config.USE_WARN) {
            if (syntax.warnCCOpNotEscaped() && syntax.backSlashEscapeInCC()) {
                reg.warnings.warn("character class has '" + s + "' without escape");
            }
        }
    }

}
