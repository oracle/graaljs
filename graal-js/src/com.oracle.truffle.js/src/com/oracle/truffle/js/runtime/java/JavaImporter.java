/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.java;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JavaImporter extends JSNonProxy implements JSConstructorFactory.Default, PrototypeSupplier {
    public static final String CLASS_NAME = "JavaImporter";

    private static final JavaImporter INSTANCE = new JavaImporter();

    private JavaImporter() {
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    @Override
    public String toString() {
        return CLASS_NAME;
    }

    public static DynamicObject create(JSContext context, JSRealm realm, Object[] value) {
        JSObjectFactory factory = context.getJavaImporterFactory();
        JavaImporterObject obj = new JavaImporterObject(factory.getShape(realm), value);
        factory.initProto(obj, realm);
        assert isJavaImporter(obj);
        return context.trackAllocation(obj);
    }

    public static boolean isJavaImporter(Object obj) {
        return obj instanceof JavaImporterObject;
    }

    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object name) {
        return getOwnHelper(thisObj, thisObj, name, null) != null;
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object name, Node encapsulatingNode) {
        if (name instanceof String) {
            Object[] imports = getImports(store);
            JSRealm realm = JSRealm.get(null);
            // Nashorn searches the imports from the last one
            for (int i = imports.length - 1; i >= 0; i--) {
                Object anImport = imports[i];
                if (anImport instanceof JavaPackageObject) {
                    JavaPackageObject javaPackage = (JavaPackageObject) anImport;
                    Object found = JavaPackage.getClass(realm, javaPackage, (String) name, Object.class);
                    if (found != null) {
                        return found;
                    }
                } else {
                    TruffleLanguage.Env env = JavaScriptLanguage.getCurrentEnv();
                    Object clazz = env.asHostObject(anImport);
                    if (name.equals(((Class<?>) clazz).getSimpleName())) {
                        return anImport;
                    }
                }
            }
        }
        return null;
    }

    public static Object[] getImports(DynamicObject importer) {
        assert JavaImporter.isJavaImporter(importer);
        return ((JavaImporterObject) importer).getImports();
    }

    @Override
    public String toDisplayStringImpl(DynamicObject object, int depth, boolean allowSideEffects) {
        return "[JavaImporter]";
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext context = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);
        JSObjectUtil.putConstructorProperty(context, prototype, ctor);
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, instance(), context);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return instance().createConstructorAndPrototype(realm);
    }

    public static JavaImporter instance() {
        return INSTANCE;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getJavaImporterPrototype();
    }
}
