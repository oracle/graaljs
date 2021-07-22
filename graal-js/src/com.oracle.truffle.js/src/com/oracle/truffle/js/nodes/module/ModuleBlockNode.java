/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
