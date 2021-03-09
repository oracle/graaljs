package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

import static com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey.TemporalCalendarId;

public class JSTemporalCalendar extends JSNonProxy implements JSConstructorFactory.Default.WithSpecies,
        PrototypeSupplier {

    public static final JSTemporalCalendar INSTANCE = new JSTemporalCalendar();

    public static final String CLASS_NAME = "TemporalCalendar";
    public static final String PROTOTYPE_NAME = "TemporalCalendar.prototype";

    public static final String ID = "id";

    private JSTemporalCalendar() {

    }

    public static DynamicObject create(JSContext context, String id) {
        if (!isBuiltinCalendar(id)) {
            throw Errors.createRangeError("Given calendar id not supported.");
        }

        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalCalendarFactory();
        DynamicObject obj = factory.initProto(new JSTemporalCalendarObject(factory.getShape(realm), id), realm);
        return context.trackAllocation(obj);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.Calendar";
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static DynamicObject getIdFunction(JSRealm realm) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(TemporalCalendarId, (c) -> {
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(c.getLanguage(), null, null) {
                private final BranchProfile errorBranch = BranchProfile.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = frame.getArguments()[0];
                    if (JSTemporalCalendar.isJSTemporalCalendar(obj)) {
                        JSTemporalCalendarObject temporalCalendar = (JSTemporalCalendarObject) obj;
                        return temporalCalendar.getId();
                    } else {
                        errorBranch.enter();
                        throw Errors.createTypeErrorTemporalCalenderExpected();
                    }
                }
            });
            return JSFunctionData.createCallOnly(c, callTarget, 0, "get id");
        });
        DynamicObject getter = JSFunction.create(realm, getterData);
        return getter;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, ID, getIdFunction(realm), Undefined.instance);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalCalendarPrototypeBuiltins.INSTANCE);
        JSObjectUtil.putToStringTag(prototype, "Temporal.Calendar");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalCalendar.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalCalendarPrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static boolean isJSTemporalCalendar(Object obj) {
        return obj instanceof JSTemporalCalendarObject;
    }

    // 12.1.1
    public static boolean isBuiltinCalendar(String id) {
        return id.equals("iso8601");
    }
}
