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
package com.oracle.truffle.regex.nashorn.regexp.joni.constants;

// @formatter:off

public interface NodeType {
    /* node type */
    final int  STR        = 0;
    final int  CCLASS     = 1;
    final int  CTYPE      = 2;
    final int  CANY       = 3;
    final int  BREF       = 4;
    final int  QTFR       = 5;
    final int  ENCLOSE    = 6;
    final int  ANCHOR     = 7;
    final int  LIST       = 8;
    final int  ALT        = 9;
    final int  CALL       = 10;

    final int BIT_STR        = 1 << STR;
    final int BIT_CCLASS     = 1 << CCLASS;
    final int BIT_CTYPE      = 1 << CTYPE;
    final int BIT_CANY       = 1 << CANY;
    final int BIT_BREF       = 1 << BREF;
    final int BIT_QTFR       = 1 << QTFR;
    final int BIT_ENCLOSE    = 1 << ENCLOSE;
    final int BIT_ANCHOR     = 1 << ANCHOR;
    final int BIT_LIST       = 1 << LIST;
    final int BIT_ALT        = 1 << ALT;
    final int BIT_CALL       = 1 << CALL;

    /* allowed node types in look-behind */
    final int ALLOWED_IN_LB = ( BIT_LIST |
                                BIT_ALT |
                                BIT_STR |
                                BIT_CCLASS |
                                BIT_CTYPE |
                                BIT_CANY |
                                BIT_ANCHOR |
                                BIT_ENCLOSE |
                                BIT_QTFR |
                                BIT_CALL );

    final int SIMPLE =        ( BIT_STR |
                                BIT_CCLASS |
                                BIT_CTYPE |
                                BIT_CANY |
                                BIT_BREF);

}
