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
package com.oracle.truffle.regex.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.result.LazyCaptureGroupsResult;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.result.SingleIndexArrayResult;
import com.oracle.truffle.regex.result.SingleResult;
import com.oracle.truffle.regex.result.SingleResultLazyStart;
import com.oracle.truffle.regex.result.StartsEndsIndexArrayResult;
import com.oracle.truffle.regex.result.TraceFinderResult;
import com.oracle.truffle.regex.runtime.RegexResultStartArrayObjectMessageResolutionFactory.RegexResultGetStartNodeGen;
import com.oracle.truffle.regex.runtime.nodes.CalcResultNode;
import com.oracle.truffle.regex.runtime.nodes.LazyCaptureGroupGetResultNode;
import com.oracle.truffle.regex.runtime.nodes.TraceFinderGetResultNode;

@MessageResolution(receiverType = RegexResultStartArrayObject.class)
public class RegexResultStartArrayObjectMessageResolution {

    abstract static class RegexResultGetStartNode extends Node {

        abstract int execute(RegexResult receiver, int groupNumber);

        @Specialization(guards = {"isNoMatch(receiver)"})
        int doNoMatch(@SuppressWarnings("unused") RegexResult receiver, int groupNumber) {
            return outOfBoundsException(groupNumber, 0);
        }

        static boolean isNoMatch(RegexResult receiver) {
            return receiver == RegexResult.NO_MATCH;
        }

        @Specialization(guards = {"groupNumber == 0"})
        int doSingleResult(SingleResult receiver, @SuppressWarnings("unused") int groupNumber) {
            return receiver.getStart();
        }

        @Specialization(guards = {"groupNumber != 0"})
        int doSingleResultOutOfBounds(@SuppressWarnings("unused") SingleResult receiver, int groupNumber) {
            return outOfBoundsException(groupNumber, 1);
        }

        @Specialization(guards = {"groupNumber == 0", "isMinusOne(receiver.getStart())"})
        int doSingleResultLazyStartCalc(SingleResultLazyStart receiver, @SuppressWarnings("unused") int groupNumber,
                        @Cached("create()") CalcResultNode calcResult) {
            receiver.setStart((int) calcResult.execute(receiver.getFindStartCallTarget(),
                            new Object[]{receiver.getInput(), receiver.getEnd() - 1, receiver.getFromIndex()}) + 1);
            return receiver.getStart();
        }

        @Specialization(guards = {"groupNumber == 0", "!isMinusOne(receiver.getStart())"})
        int doSingleResultLazyStart(SingleResultLazyStart receiver, @SuppressWarnings("unused") int groupNumber) {
            return receiver.getStart();
        }

        static boolean isMinusOne(int i) {
            return i == -1;
        }

        @Specialization(guards = {"groupNumber != 0"})
        int doSingleResultLazyStartOutOfBounds(@SuppressWarnings("unused") SingleResultLazyStart receiver, int groupNumber) {
            return outOfBoundsException(groupNumber, 1);
        }

        @Specialization
        int doStartsEndsIndexArray(StartsEndsIndexArrayResult receiver, int groupNumber) {
            return receiver.getStarts()[groupNumber];
        }

        @Specialization
        int doSingleIndexArray(SingleIndexArrayResult receiver, int groupNumber) {
            return fromSingleArray(receiver.getIndices(), groupNumber);
        }

        @Specialization
        int doTraceFinder(TraceFinderResult receiver, int groupNumber,
                        @Cached("create()") TraceFinderGetResultNode getResultNode) {
            return fromSingleArray(getResultNode.execute(receiver), groupNumber);
        }

        @Specialization
        int doLazyCaptureGroups(LazyCaptureGroupsResult receiver, int groupNumber,
                        @Cached("create()") LazyCaptureGroupGetResultNode getResultNode) {
            return fromSingleArray(getResultNode.execute(receiver), groupNumber) - 1;
        }

        private static int fromSingleArray(int[] array, int groupNumber) {
            return array[groupNumber * 2];
        }

        public static RegexResultGetStartNode create() {
            return RegexResultGetStartNodeGen.create();
        }
    }

    private static int outOfBoundsException(int groupNumber, int size) {
        CompilerDirectives.transferToInterpreter();
        throw new IndexOutOfBoundsException(String.format("index: %d, size: %d", groupNumber, size));
    }

    @Resolve(message = "READ")
    abstract static class RegexResultStartReadNode extends Node {

        @Child RegexResultGetStartNode getStartNode = RegexResultGetStartNode.create();

        public Object access(RegexResultStartArrayObject receiver, int index) {
            return getStartNode.execute(receiver.getResult(), index);
        }
    }

    @Resolve(message = "HAS_SIZE")
    abstract static class RegexResultStartHasSizeNode extends Node {

        @SuppressWarnings("unused")
        public boolean access(RegexResultStartArrayObject receiver) {
            return true;
        }
    }

    @Resolve(message = "GET_SIZE")
    abstract static class RegexResultStartGetSizeNode extends Node {

        public int access(RegexResultStartArrayObject receiver) {
            return receiver.getResult().getGroupCount();
        }
    }
}
