/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.array;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

@ImportStatic(JSArray.class)
@GenerateInline
@GenerateCached(false)
public abstract class JSArrayGetOwnPropertyNode extends JavaScriptBaseNode {

    protected static final int MAX_TYPE_COUNT = 4;

    protected JSArrayGetOwnPropertyNode() {
    }

    public abstract PropertyDescriptor execute(Node node, JSArrayObject target, long index,
                    boolean needValue, boolean needEnumerability, boolean needConfigurability, boolean needWritability);

    @Specialization(guards = {"arrayType.isInstance(arrayGetArrayType(arrayObject))"}, limit = "MAX_TYPE_COUNT")
    protected static PropertyDescriptor doCached(JSArrayObject arrayObject, long index,
                    boolean needValue, boolean needEnumerability, boolean needConfigurability, boolean needWritability,
                    @Cached("arrayGetArrayType(arrayObject)") ScriptArray arrayType) {
        if (arrayType.hasElement(arrayObject, index)) {
            PropertyDescriptor desc = PropertyDescriptor.createEmpty();
            if (needEnumerability) {
                desc.setEnumerable(true);
            }
            if (needConfigurability) {
                desc.setConfigurable(!arrayType.isSealed());
            }
            if (needWritability) {
                desc.setWritable(!arrayType.isFrozen());
            }
            if (needValue) {
                desc.setValue(arrayType.getElement(arrayObject, index));
            }
            return desc;
        }
        return null;
    }

    @Specialization(replaces = "doCached")
    protected static PropertyDescriptor doUncached(JSArrayObject target, long index,
                    boolean needValue, boolean needEnumerability, boolean needConfigurability, boolean needWritability) {
        ScriptArray arrayType = JSArray.arrayGetArrayType(target);
        return doCached(target, index, needValue, needEnumerability, needConfigurability, needWritability, arrayType);
    }

}
