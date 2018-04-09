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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

public abstract class GetPrototypeNode extends JavaScriptBaseNode {
    static final int MAX_SHAPE_COUNT = 1;

    GetPrototypeNode() {
    }

    protected abstract DynamicObject executeDynamicObject(DynamicObject obj);

    public abstract DynamicObject executeJSObject(Object obj);

    public static GetPrototypeNode create() {
        return GetPrototypeNodeGen.create();
    }

    public static JavaScriptNode create(JavaScriptNode object) {
        assert object instanceof RepeatableNode;
        class GetPrototypeOfNode extends JavaScriptNode implements RepeatableNode {
            @Child private JavaScriptNode objectNode = object;
            @Child private GetPrototypeNode getPrototypeNode = GetPrototypeNode.create();

            @Override
            public DynamicObject execute(VirtualFrame frame) {
                return getPrototypeNode.executeJSObject(objectNode.execute(frame));
            }

            @Override
            protected JavaScriptNode copyUninitialized() {
                return new GetPrototypeOfNode();
            }
        }
        return new GetPrototypeOfNode();
    }

    static Property getPrototypeProperty(Shape shape) {
        if (JSShape.getJSClass(shape) == JSProxy.INSTANCE) {
            return null;
        }
        return JSShape.getPrototypeProperty(shape);
    }

    @Specialization(guards = {"obj.getShape() == shape", "prototypeProperty != null"}, limit = "MAX_SHAPE_COUNT")
    public DynamicObject doCachedShape(DynamicObject obj,
                    @Cached("obj.getShape()") Shape shape,
                    @Cached("getPrototypeProperty(shape)") Property prototypeProperty) {
        assert !JSGuards.isJSProxy(obj);
        return (DynamicObject) prototypeProperty.get(obj, shape);
    }

    @Specialization(guards = "!isJSProxy(obj)", replaces = "doCachedShape")
    public DynamicObject doGeneric(DynamicObject obj,
                    @Cached("createClassProfile()") ValueProfile locationClass) {
        Location location = locationClass.profile(JSShape.getPrototypeProperty(obj.getShape()).getLocation());
        return (DynamicObject) location.get(obj, false);
    }

    @Specialization(guards = "isJSProxy(obj)")
    public DynamicObject doProxy(DynamicObject obj,
                    @Cached("create()") JSClassProfile jsclassProfile) {
        return JSObject.getPrototype(obj, jsclassProfile);
    }

    @Specialization(guards = "!isDynamicObject(obj)")
    public DynamicObject doNotObject(@SuppressWarnings("unused") Object obj) {
        return Null.instance;
    }
}
