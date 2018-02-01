/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.interop;

import java.util.EnumSet;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBuiltinObject;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JavaImporter extends JSBuiltinObject implements JSConstructorFactory.Default {
    public static final String CLASS_NAME = "JavaImporter";

    private static final HiddenKey PACKAGES_ID = new HiddenKey("packages");
    static final Property PACKAGES_PROPERTY;

    static class LazyState {
        static final JavaImporter INSTANCE = new JavaImporter();
    }

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        PACKAGES_PROPERTY = JSObjectUtil.makeHiddenProperty(PACKAGES_ID, allocator.locationForType(DynamicObject[].class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JavaImporter() {
        assert JSTruffleOptions.NashornJavaInterop;
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

    public static DynamicObject create(JSContext context, DynamicObject[] value) {
        if (JSTruffleOptions.NashornJavaInterop) {
            return doCreate(context, value);
        } else {
            /*
             * This path should never be reached when JavaInterop is disabled. To help the static
             * analysis of Substrate VM, we throw an exception.
             */
            throw new UnsupportedOperationException();
        }
    }

    /* In a separate method for Substrate VM support. */
    private static DynamicObject doCreate(JSContext context, DynamicObject[] value) {
        DynamicObject obj = JSObject.create(context, context.getJavaImporterFactory(), new Object[]{value});
        assert isJavaImporter(obj);
        return obj;
    }

    public static boolean isJavaImporter(Object obj) {
        return JSObject.isDynamicObject(obj) && isJavaImporter((DynamicObject) obj);
    }

    public static boolean isJavaImporter(DynamicObject obj) {
        if (JSTruffleOptions.NashornJavaInterop) {
            return isJavaImporter0(obj);
        } else {
            return false;
        }
    }

    /* In a separate method for Substrate VM support. */
    private static boolean isJavaImporter0(DynamicObject obj) {
        return isInstance(obj, LazyState.INSTANCE);
    }

    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object name) {
        return getOwnHelper(thisObj, thisObj, name) != null;
    }

    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object name) {
        if (name instanceof String) {
            DynamicObject[] packages = getPackages(store);
            for (DynamicObject pkg : packages) {
                JavaClass found = JavaPackage.getClass(pkg, (String) name, JavaClass.class);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public static DynamicObject[] getPackages(DynamicObject importer) {
        if (JSTruffleOptions.NashornJavaInterop) {
            return getWrapped0(importer);
        } else {
            /*
             * This path should never be reached when JavaInterop is disabled. To help the static
             * analysis of Substrate VM, we throw an exception.
             */
            throw new UnsupportedOperationException();
        }
    }

    /* In a separate method for Substrate VM support. */
    private static DynamicObject[] getWrapped0(DynamicObject importer) {
        assert JavaImporter.isJavaImporter(importer);
        return (DynamicObject[]) PACKAGES_PROPERTY.get(importer, isJavaImporter(importer));
    }

    @Override
    public String safeToString(DynamicObject object) {
        return "[JavaImporter]";
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        DynamicObject prototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putDataProperty(realm.getContext(), prototype, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putConstructorProperty(realm.getContext(), prototype, ctor);
        return prototype;
    }

    public static Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        assert JSShape.getProtoChildTree(prototype.getShape(), LazyState.INSTANCE) == null;
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, LazyState.INSTANCE, context);
        initialShape = initialShape.addProperty(PACKAGES_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return LazyState.INSTANCE.createConstructorAndPrototype(realm);
    }
}
