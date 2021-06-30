package com.oracle.truffle.js.nodes.module;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.runtime.JSContext;

public class ModuleBlockNode extends JavaScriptNode {

    public static final String TYPE_NAME = "moduleBlock";
    public static final String CLASS_NAME = "ModuleBlock";
    public static final String PROTOTYPE_NAME = "ModuleBlock.prototype";

    static final HiddenKey SOURCE_KEY = new HiddenKey("SourceText");
    static final HiddenKey MODULE_BODY_KEY = new HiddenKey("ModuleBlockBody");
    static final HiddenKey HOST_DEFINED_SLOT = new HiddenKey("HostDefinedSlot");

    private final JSContext context;
    private final String moduleBlockSourceName;

    @Child private JavaScriptNode body;
    @Child private CreateObjectNode.CreateObjectWithPrototypeNode moduleBlockCreateNode;
    @Child private PropertySetNode setHostDefinedSlot;
    @Child private PropertySetNode setModuleBlockBody;
    @Child private PropertySetNode setSourceText;

    private ModuleBlockNode(JSContext context, JavaScriptNode body, String moduleBlockSourceName) {
        this.context = context;
        this.body = body;
        this.moduleBlockSourceName = moduleBlockSourceName;

        // for step 1 of runtime semantics
        this.moduleBlockCreateNode = CreateObjectNode.createOrdinaryWithPrototype(context);
        // prepare setting steps for runtime semantic steps 2,3,4
        this.setSourceText = PropertySetNode.createSetHidden(SOURCE_KEY, context);
        this.setModuleBlockBody = PropertySetNode.createSetHidden(MODULE_BODY_KEY, context);
        this.setHostDefinedSlot = PropertySetNode.createSetHidden(HOST_DEFINED_SLOT, context);

    }

    public static ModuleBlockNode create(JSContext context, JavaScriptNode body, String moduleBlockSourceName) {
        return new ModuleBlockNode(context, body, moduleBlockSourceName);
    }

    public static HiddenKey getModuleSourceKey() {
        return SOURCE_KEY;
    }

    public static HiddenKey getModuleBodyKey() {
        return MODULE_BODY_KEY;
    }

    public static HiddenKey getHostDefinedSlotKey() {
        return HOST_DEFINED_SLOT;
    }

    @Override
    public Object execute(VirtualFrame frame) {

        // 1 create
        DynamicObject newModuleBlockInstance = moduleBlockCreateNode.execute(context.getRealm().getModuleBlockPrototype());

        // 2 set source text to the source text matched by ModuleBlockExpression
        setSourceText.setValue(newModuleBlockInstance, body.getSourceSection().getCharacters());
        // 3 specification says to parse now but it is already parsed and also will be parsed
        // when imported
        // DO NOTHING

        // 4 store text
        setModuleBlockBody.setValue(newModuleBlockInstance, body);

        // 5 perform hostInitializeModuleBlock(myNewInstance)
        hostInitializeModuleBlock(newModuleBlockInstance);

        setHostDefinedSlot.setValue(newModuleBlockInstance, moduleBlockSourceName);

        // 6 return module Block
        return newModuleBlockInstance;
    }

    /**
     * Not implemented yet as no specification is set on it yet.
     *
     * @param myNewInstance module object
     */
    private void hostInitializeModuleBlock(DynamicObject myNewInstance) {

    }

}
