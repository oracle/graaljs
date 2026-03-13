/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.util;

import java.util.ArrayDeque;

import com.oracle.truffle.js.runtime.objects.Undefined;

public final class DisposeCapability {
    public static final Object NO_ERROR = new Object();
    private final ArrayDeque<DisposableResource> disposableResourceStack;

    public DisposeCapability() {
        this.disposableResourceStack = new ArrayDeque<>();
    }

    public void pushResource(DisposableResource resource) {
        disposableResourceStack.push(resource);
    }

    public DisposableResource popResource() {
        return (isEmpty() ? null : disposableResourceStack.pop());
    }

    public boolean isEmpty() {
        return disposableResourceStack.isEmpty();
    }

    public static DisposableResource forResource(Object resourceValue, boolean asyncDispose, Object disposeMethod) {
        return new DisposableResource(resourceValue, asyncDispose, disposeMethod, null);
    }

    public static DisposableResource forCallback(Object disposeMethod, Object argument, boolean asyncDispose) {
        return new DisposableResource(Undefined.instance, asyncDispose, disposeMethod, argument);
    }

    public static final class DisposableResource {
        private final Object resourceValue;
        private final boolean asyncDispose;
        private final Object disposeMethod;
        private final Object disposeArgument;

        private DisposableResource(Object resourceValue, boolean asyncDispose, Object disposeMethod, Object disposeArgument) {
            this.resourceValue = resourceValue;
            this.asyncDispose = asyncDispose;
            this.disposeMethod = disposeMethod;
            this.disposeArgument = disposeArgument;
        }

        public Object getResourceValue() {
            return resourceValue;
        }

        public boolean isAsyncDispose() {
            return asyncDispose;
        }

        public Object getDisposeMethod() {
            return disposeMethod;
        }

        public Object getDisposeArgument() {
            return disposeArgument;
        }

        public boolean hasDisposeArgument() {
            return disposeArgument != null;
        }
    }
}
