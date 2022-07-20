package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.FetchResponseFunctionBuiltins;
import com.oracle.truffle.js.builtins.FetchResponsePrototypeBuiltins;
import com.oracle.truffle.js.builtins.helper.FetchResponse;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSFetchResponse extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {
    public static final TruffleString CLASS_NAME = Strings.constant("Response");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Response.prototype");

    public static final JSFetchResponse INSTANCE = new JSFetchResponse();

    private JSFetchResponse() {
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public TruffleString getClassName(JSDynamicObject object) {
        return getClassName();
    }

    @Override
    public TruffleString getBuiltinToStringTag(JSDynamicObject object) {
        return getClassName(object);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, FetchResponseFunctionBuiltins.BUILTINS);
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSContext ctx = realm.getContext();

        JSObject responsePrototype;
        if (ctx.getEcmaScriptVersion() < 6) {
            Shape protoShape = JSShape.createPrototypeShape(realm.getContext(), INSTANCE, realm.getObjectPrototype());
            responsePrototype = JSFetchResponseObject.create(protoShape, new FetchResponse());
            JSObjectUtil.setOrVerifyPrototype(ctx, responsePrototype, realm.getObjectPrototype());
        } else {
            responsePrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        }

        JSObjectUtil.putConstructorProperty(ctx, responsePrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, responsePrototype, FetchResponsePrototypeBuiltins.BUILTINS);

        return responsePrototype;
    }

    public static JSFetchResponseObject create(JSContext context, JSRealm realm, FetchResponse response) {
        JSObjectFactory factory = context.getFetchResponseFactory();
        JSFetchResponseObject obj = JSFetchResponseObject.create(factory.getShape(realm), response);
        factory.initProto(obj, realm);
        return context.trackAllocation(obj);
    }
}
