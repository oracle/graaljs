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
package com.oracle.truffle.js.nodes.function;

import java.util.ArrayList;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

public class JSFunctionArgumentsNode extends AbstractFunctionArgumentsNode {

    @Children protected final JavaScriptNode[] args;

    public static AbstractFunctionArgumentsNode create(JavaScriptNode[] args) {
        for (JavaScriptNode arg : args) {
            if (arg instanceof SpreadArgumentNode) {
                return new SpreadFunctionArgumentsNode(args);
            }
        }
        if (args.length == 0) {
            return new JSFunctionZeroArgumentsNode();
        } else if (args.length == 1) {
            return JSFunctionOneArgumentNode.create(args[0]);
        }
        return new JSFunctionArgumentsNode(args);
    }

    protected JSFunctionArgumentsNode(JavaScriptNode[] args) {
        this.args = args;
        if (args.length > JSTruffleOptions.MaxFunctionArgumentsLength) {
            throw Errors.createSyntaxError("function has too many parameters");
        }
    }

    @Override
    public int getCount(VirtualFrame frame) {
        return args.length;
    }

    @Override
    @ExplodeLoop
    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta) {
        for (int i = 0; i < args.length; i++) {
            arguments[i + delta] = args[i].execute(frame);
        }
        return arguments;
    }

    @Override
    protected AbstractFunctionArgumentsNode copyUninitialized() {
        return new JSFunctionArgumentsNode(JavaScriptNode.cloneUninitialized(args));
    }
}

class JSFunctionZeroArgumentsNode extends AbstractFunctionArgumentsNode {
    protected JSFunctionZeroArgumentsNode() {
    }

    @Override
    public int getCount(VirtualFrame frame) {
        return 0;
    }

    @Override
    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta) {
        return arguments;
    }

    @Override
    protected AbstractFunctionArgumentsNode copyUninitialized() {
        return new JSFunctionZeroArgumentsNode();
    }
}

class JSFunctionOneArgumentNode extends AbstractFunctionArgumentsNode {
    @Child private JavaScriptNode child;

    protected JSFunctionOneArgumentNode(JavaScriptNode child) {
        this.child = child;
    }

    public static AbstractFunctionArgumentsNode create(JavaScriptNode child) {
        return new JSFunctionOneArgumentNode(child);
    }

    public static AbstractFunctionArgumentsNode create(JavaScriptNode child, boolean optimizeConstantArguments) {
        if (optimizeConstantArguments) {
            return create(child);
        } else {
            return new JSFunctionOneArgumentNode(child);
        }
    }

    @Override
    public int getCount(VirtualFrame frame) {
        return 1;
    }

    @Override
    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta) {
        arguments[delta] = child.execute(frame);
        return arguments;
    }

    @Override
    protected AbstractFunctionArgumentsNode copyUninitialized() {
        return new JSFunctionOneArgumentNode(JavaScriptNode.cloneUninitialized(child));
    }
}

class SpreadFunctionArgumentsNode extends JSFunctionArgumentsNode {
    protected SpreadFunctionArgumentsNode(JavaScriptNode[] args) {
        super(args);
    }

    @Override
    public int getCount(VirtualFrame frame) {
        return args.length;
    }

    @Override
    @ExplodeLoop
    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta) {
        ArrayList<Object> argList = new ArrayList<>(args.length + delta);
        for (int i = 0; i < delta; i++) {
            Boundaries.listAdd(argList, arguments[i]);
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof SpreadArgumentNode) {
                ((SpreadArgumentNode) args[i]).executeToList(frame, argList);
            } else {
                Boundaries.listAdd(argList, args[i].execute(frame));
            }
        }
        return Boundaries.listToArray(argList);
    }

    @Override
    protected AbstractFunctionArgumentsNode copyUninitialized() {
        return new SpreadFunctionArgumentsNode(JavaScriptNode.cloneUninitialized(args));
    }
}
