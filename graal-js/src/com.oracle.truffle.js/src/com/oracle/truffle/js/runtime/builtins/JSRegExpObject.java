/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSBasicObject;
import com.oracle.truffle.js.runtime.objects.JSCopyableObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

public final class JSRegExpObject extends JSBasicObject implements JSCopyableObject {
    private Object compiledRegex;
    private JSObjectFactory groupsFactory;
    private final JSRealm realm;
    private final boolean legacyFeaturesEnabled;

    protected JSRegExpObject(Shape shape, Object compiledRegex, JSObjectFactory groupsFactory, JSRealm realm, boolean legacyFeaturesEnabled) {
        super(shape);
        this.compiledRegex = compiledRegex;
        this.groupsFactory = groupsFactory;
        this.realm = realm;
        this.legacyFeaturesEnabled = legacyFeaturesEnabled;
    }

    public Object getCompiledRegex() {
        return compiledRegex;
    }

    public void setCompiledRegex(Object compiledRegex) {
        this.compiledRegex = compiledRegex;
    }

    public JSObjectFactory getGroupsFactory() {
        return groupsFactory;
    }

    public void setGroupsFactory(JSObjectFactory groupsFactory) {
        this.groupsFactory = groupsFactory;
    }

    public JSRealm getRealm() {
        return realm;
    }

    public boolean getLegacyFeaturesEnabled() {
        return legacyFeaturesEnabled;
    }

    @Override
    public String getClassName() {
        return JSRegExp.CLASS_NAME;
    }

    public static DynamicObject create(JSRealm realm, JSObjectFactory factory, Object compiledRegex, JSObjectFactory groupsFactory, boolean legacyFeaturesEnabled) {
        return factory.initProto(new JSRegExpObject(factory.getShape(realm), compiledRegex, groupsFactory, realm, legacyFeaturesEnabled), realm);
    }

    public static DynamicObject create(Shape shape, Object compiledRegex, JSRealm realm) {
        return new JSRegExpObject(shape, compiledRegex, null, realm, false);
    }

    @Override
    protected JSObject copyWithoutProperties(Shape shape) {
        return new JSRegExpObject(shape, compiledRegex, groupsFactory, realm, legacyFeaturesEnabled);
    }
}
