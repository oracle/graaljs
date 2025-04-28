/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.interop;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

@RunWith(Parameterized.class)
public abstract class ParameterizedInteropLibraryTest {
    public enum TestRun {
        CACHED,
        UNCACHED,
        DISPATCHED_CACHED,
        DISPATCHED_UNCACHED;

        public boolean isDispatched() {
            return this == DISPATCHED_CACHED || this == DISPATCHED_UNCACHED;
        }

        public boolean isCached() {
            return this == CACHED || this == TestRun.DISPATCHED_CACHED;
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<TestRun> data() {
        return Arrays.asList(TestRun.CACHED, TestRun.UNCACHED, TestRun.DISPATCHED_CACHED, TestRun.DISPATCHED_UNCACHED);
    }

    @Parameterized.Parameter(0) public TestRun run;

    protected final InteropLibrary createInteropLibrary(Object receiver) {
        return createLibrary(InteropLibrary.class, receiver);
    }

    protected final <T extends Library> T createLibrary(Class<T> library, Object receiver) {
        LibraryFactory<T> lib = LibraryFactory.resolve(library);
        return switch (run) {
            case CACHED -> adoptNode(lib.create(receiver));
            case UNCACHED -> lib.getUncached(receiver);
            case DISPATCHED_CACHED -> adoptNode(lib.createDispatched(2));
            case DISPATCHED_UNCACHED -> lib.getUncached();
        };
    }

    protected static <T extends Node> T adoptNode(T node) {
        return RootNode.createConstantNode("dummy").insert(node);
    }
}
