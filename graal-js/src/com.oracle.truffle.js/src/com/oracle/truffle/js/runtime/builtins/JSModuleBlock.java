package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltins;
import com.oracle.truffle.js.builtins.ModuleBlockPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public class JSModuleBlock extends JSNonProxy implements JSConstructorFactory.Default.WithSpecies, PrototypeSupplier {

    public static final JSModuleBlock INSTANCE = new JSModuleBlock();

    public static final String CLASS_NAME = "ModuleBlock";
    public static final String PROTOTYPE_NAME = "ModuleBlock.prototype";

    private JSModuleBlock() {
    }

    public static DynamicObject create(JSContext context) {

        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getModuleBlockFactory();
        DynamicObject obj = factory.initProto(new JSModuleBlockObject(factory.getShape(realm)), realm);
        assert isJSModuleBlock(obj);
        return context.trackAllocation(obj);
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
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);

        JSObjectUtil.putFunctionsFromContainer(realm, prototype, ModuleBlockPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);

        return prototype;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getModuleBlockPrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    // TODO
    public static boolean isJSModuleBlock(Object obj) {

        return true;
        // return obj instanceof JSModuleBlockObject;
    }

}
