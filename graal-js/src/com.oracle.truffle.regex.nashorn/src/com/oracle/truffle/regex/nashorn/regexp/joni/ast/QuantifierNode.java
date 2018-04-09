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

import static com.oracle.truffle.regex.nashorn.regexp.joni.ast.QuantifierNode.ReduceType.A;
import static com.oracle.truffle.regex.nashorn.regexp.joni.ast.QuantifierNode.ReduceType.AQ;
import static com.oracle.truffle.regex.nashorn.regexp.joni.ast.QuantifierNode.ReduceType.ASIS;
import static com.oracle.truffle.regex.nashorn.regexp.joni.ast.QuantifierNode.ReduceType.DEL;
import static com.oracle.truffle.regex.nashorn.regexp.joni.ast.QuantifierNode.ReduceType.PQ_Q;
import static com.oracle.truffle.regex.nashorn.regexp.joni.ast.QuantifierNode.ReduceType.P_QQ;
import static com.oracle.truffle.regex.nashorn.regexp.joni.ast.QuantifierNode.ReduceType.QQ;

import com.oracle.truffle.regex.nashorn.regexp.joni.Config;
import com.oracle.truffle.regex.nashorn.regexp.joni.ScanEnvironment;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.TargetInfo;

public final class QuantifierNode extends StateNode {

    public Node target;
    public int lower;
    public int upper;
    public boolean greedy;

    public int targetEmptyInfo;

    public Node headExact;
    public Node nextHeadExact;
    public boolean isRefered;           /* include called node. don't eliminate even if {0} */

    enum ReduceType {
        ASIS,       /* as is */
        DEL,        /* delete parent */
        A,          /* to '*'    */
        AQ,         /* to '*?'   */
        QQ,         /* to '??'   */
        P_QQ,       /* to '+)??' */
        PQ_Q,       /* to '+?)?' */
    }

    private final static ReduceType[][] REDUCE_TABLE = {
            {DEL,     A,      A,      QQ,     AQ,     ASIS}, /* '?'  */
            {DEL,     DEL,    DEL,    P_QQ,   P_QQ,   DEL},  /* '*'  */
            {A,       A,      DEL,    ASIS,   P_QQ,   DEL},  /* '+'  */
            {DEL,     AQ,     AQ,     DEL,    AQ,     AQ},   /* '??' */
            {DEL,     DEL,    DEL,    DEL,    DEL,    DEL},  /* '*?' */
            {ASIS,    PQ_Q,   DEL,    AQ,     AQ,     DEL}   /* '+?' */
    };

    private final static String PopularQStr[] = new String[] {
            "?", "*", "+", "??", "*?", "+?"
    };

    private final static String ReduceQStr[]= new String[] {
            "", "", "*", "*?", "??", "+ and ??", "+? and ?"
    };


    public QuantifierNode(final int lower, final int upper, final boolean byNumber) {
        this.lower = lower;
        this.upper = upper;
        greedy = true;
        targetEmptyInfo = TargetInfo.ISNOT_EMPTY;

        if (byNumber) {
            setByNumber();
        }
    }

    @Override
    public int getType() {
        return QTFR;
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

    public StringNode convertToString(final int flag) {
        final StringNode sn = new StringNode();
        sn.flag = flag;
        sn.swap(this);
        return sn;
    }

    @Override
    public String getName() {
        return "Quantifier";
    }

    @Override
    public String toString(final int level) {
        final StringBuilder value = new StringBuilder(super.toString(level));
        value.append("\n  target: " + pad(target, level + 1));
        value.append("\n  lower: " + lower);
        value.append("\n  upper: " + upper);
        value.append("\n  greedy: " + greedy);
        value.append("\n  targetEmptyInfo: " + targetEmptyInfo);
        value.append("\n  headExact: " + pad(headExact, level + 1));
        value.append("\n  nextHeadExact: " + pad(nextHeadExact, level + 1));
        value.append("\n  isRefered: " + isRefered);

        return value.toString();
    }

    public boolean isAnyCharStar() {
        return greedy && isRepeatInfinite(upper) && target.getType() == CANY;
    }

    /* ?:0, *:1, +:2, ??:3, *?:4, +?:5 */
    protected int popularNum() {
        if (greedy) {
            if (lower == 0) {
                if (upper == 1) {
                    return 0;
                } else if (isRepeatInfinite(upper)) {
                    return 1;
                }
            } else if (lower == 1) {
                if (isRepeatInfinite(upper)) {
                    return 2;
                }
            }
        } else {
            if (lower == 0) {
                if (upper == 1) {
                    return 3;
                } else if (isRepeatInfinite(upper)) {
                    return 4;
                }
            } else if (lower == 1) {
                if (isRepeatInfinite(upper)) {
                    return 5;
                }
            }
        }
        return -1;
    }

    protected void set(final QuantifierNode other) {
        setTarget(other.target);
        other.target = null;
        lower = other.lower;
        upper = other.upper;
        greedy = other.greedy;
        targetEmptyInfo = other.targetEmptyInfo;

        //setHeadExact(other.headExact);
        //setNextHeadExact(other.nextHeadExact);
        headExact = other.headExact;
        nextHeadExact = other.nextHeadExact;
        isRefered = other.isRefered;
    }

    public void reduceNestedQuantifier(final QuantifierNode other) {
        final int pnum = popularNum();
        final int cnum = other.popularNum();

        if (pnum < 0 || cnum < 0) {
            return;
        }

        switch(REDUCE_TABLE[cnum][pnum]) {
        case DEL:
            // no need to set the parent here...
            // swap ?
            set(other); // *pnode = *cnode; ???
            break;

        case A:
            setTarget(other.target);
            lower = 0;
            upper = REPEAT_INFINITE;
            greedy = true;
            break;

        case AQ:
            setTarget(other.target);
            lower = 0;
            upper = REPEAT_INFINITE;
            greedy = false;
            break;

        case QQ:
            setTarget(other.target);
            lower = 0;
            upper = 1;
            greedy = false;
            break;

        case P_QQ:
            setTarget(other);
            lower = 0;
            upper = 1;
            greedy = false;
            other.lower = 1;
            other.upper = REPEAT_INFINITE;
            other.greedy = true;
            return;

        case PQ_Q:
            setTarget(other);
            lower = 0;
            upper = 1;
            greedy = true;
            other.lower = 1;
            other.upper = REPEAT_INFINITE;
            other.greedy = false;
            return;

        case ASIS:
            setTarget(other);
            return;

        default:
            break;
        }
        // ??? remove the parent from target ???
        other.target = null; // remove target from reduced quantifier
    }

    @SuppressWarnings("fallthrough")
    public int setQuantifier(final Node tgt, final boolean group, final ScanEnvironment env, final char[] chars, final int p, final int end) {
        if (lower == 1 && upper == 1) {
            return 1;
        }

        switch(tgt.getType()) {

        case STR:
            if (!group) {
                final StringNode sn = (StringNode)tgt;
                if (sn.canBeSplit()) {
                    final StringNode n = sn.splitLastChar();
                    if (n != null) {
                        setTarget(n);
                        return 2;
                    }
                }
            }
            break;

        case QTFR:
            /* check redundant double repeat. */
            /* verbose warn (?:.?)? etc... but not warn (.?)? etc... */
            final QuantifierNode qnt = (QuantifierNode)tgt;
            final int nestQNum = popularNum();
            final int targetQNum = qnt.popularNum();

            if (Config.USE_WARNING_REDUNDANT_NESTED_REPEAT_OPERATOR) {
                if (!isByNumber() && !qnt.isByNumber() && env.syntax.warnReduntantNestedRepeat()) {
                    switch(REDUCE_TABLE[targetQNum][nestQNum]) {
                    case ASIS:
                        break;

                    case DEL:
                        env.reg.getWarnings().warn(new String(chars, p, end) +
                                " redundant nested repeat operator");
                        break;

                    default:
                        env.reg.getWarnings().warn(new String(chars, p, end) +
                                " nested repeat operator " + PopularQStr[targetQNum] +
                                " and " + PopularQStr[nestQNum] + " was replaced with '" +
                                ReduceQStr[REDUCE_TABLE[targetQNum][nestQNum].ordinal()] + "'");
                    }
                }
            } // USE_WARNING_REDUNDANT_NESTED_REPEAT_OPERATOR

            if (targetQNum >= 0) {
                if (nestQNum >= 0) {
                    reduceNestedQuantifier(qnt);
                    return 0;
                } else if (targetQNum == 1 || targetQNum == 2) { /* * or + */
                    /* (?:a*){n,m}, (?:a+){n,m} => (?:a*){n,n}, (?:a+){n,n} */
                    if (!isRepeatInfinite(upper) && upper > 1 && greedy) {
                        upper = lower == 0 ? 1 : lower;
                    }
                }
            }

        default:
            break;
        }

        setTarget(tgt);
        return 0;
    }

    public static final int REPEAT_INFINITE         = -1;
    public static boolean isRepeatInfinite(final int n) {
        return n == REPEAT_INFINITE;
    }

}
