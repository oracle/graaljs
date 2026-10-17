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
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;

@GenerateUncached
public abstract class ImportArgumentsNode extends JavaScriptBaseNode {
    static final int MAX_CACHED_ARGUMENT_COUNT = 8;

    public abstract Object[] execute(Object[] arguments);

    @NeverDefault
    public static ImportArgumentsNode create() {
        return ImportArgumentsNodeGen.create();
    }

    public static ImportArgumentsNode getUncached() {
        return ImportArgumentsNodeGen.getUncached();
    }

    @Specialization(guards = "arguments.length == 0")
    static Object[] doZero(Object[] arguments) {
        return arguments;
    }

    @ExplodeLoop
    @Specialization(guards = {"arguments.length <= MAX_CACHED_ARGUMENT_COUNT", "arguments.length == importNodes.length"}, limit = "1", replaces = "doZero")
    static Object[] doCached(Object[] arguments,
                    @Cached("createImportNodes(arguments.length)") ImportValueNode[] importNodes) {
        Object[] importedArguments = arguments;
        boolean copy = false;
        for (int i = 0; i < importNodes.length; i++) {
            Object argument = arguments[i];
            Object importedArgument = importNodes[i].executeWithTarget(argument);
            if (copy) {
                importedArguments[i] = importedArgument;
            } else if (importedArgument != argument) {
                importedArguments = new Object[importNodes.length];
                System.arraycopy(arguments, 0, importedArguments, 0, i);
                importedArguments[i] = importedArgument;
                copy = true;
            }
        }
        return importedArguments;
    }

    @Specialization(replaces = {"doZero", "doCached"})
    static Object[] doGeneric(Object[] arguments,
                    @Cached ImportValueNode importNode) {
        Object[] importedArguments = arguments;
        boolean copy = false;
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            Object importedArgument = importNode.executeWithTarget(argument);
            if (copy) {
                importedArguments[i] = importedArgument;
            } else if (importedArgument != argument) {
                importedArguments = new Object[arguments.length];
                System.arraycopy(arguments, 0, importedArguments, 0, i);
                importedArguments[i] = importedArgument;
                copy = true;
            }
        }
        return importedArguments;
    }

    @NeverDefault
    static ImportValueNode[] createImportNodes(int argumentCount) {
        ImportValueNode[] importNodes = new ImportValueNode[argumentCount];
        for (int i = 0; i < importNodes.length; i++) {
            importNodes[i] = ImportValueNode.create();
        }
        return importNodes;
    }
}
