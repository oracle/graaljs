/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public interface JSConstructorFactory {
    JSConstructor createConstructorAndPrototype(JSRealm realm);

    interface Default extends JSConstructorFactory {
        @Override
        default JSConstructor createConstructorAndPrototype(JSRealm realm) {
            JSContext ctx = realm.getContext();
            DynamicObject constructor = createConstructorObject(realm);
            DynamicObject prototype = createPrototype(realm, constructor);
            JSObjectUtil.putConstructorPrototypeProperty(ctx, constructor, prototype);
            fillConstructor(realm, constructor);
            return new JSConstructor(constructor, prototype);
        }

        String getClassName();

        DynamicObject createPrototype(JSRealm realm, DynamicObject constructor);

        default DynamicObject createConstructorObject(JSRealm realm) {
            return realm.lookupFunction(JSConstructor.BUILTINS, getClassName());
        }

        @SuppressWarnings("unused")
        default void fillConstructor(JSRealm realm, DynamicObject constructor) {
        }

        interface WithFunctions extends Default {
            @Override
            default void fillConstructor(JSRealm realm, DynamicObject constructor) {
                JSObjectUtil.putFunctionsFromContainer(realm, constructor, getClassName());
            }
        }

        interface WithFunctionsAndSpecies extends Default {
            @Override
            default void fillConstructor(JSRealm realm, DynamicObject constructor) {
                JSObjectUtil.putFunctionsFromContainer(realm, constructor, getClassName());
                JSBuiltinObject.putConstructorSpeciesGetter(realm, constructor);
            }
        }
    }
}
