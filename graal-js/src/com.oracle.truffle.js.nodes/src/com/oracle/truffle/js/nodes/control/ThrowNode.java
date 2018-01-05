/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * 12.13 The throw Statement.
 */
@NodeInfo(shortName = "throw")
public class ThrowNode extends StatementNode {
    @Child private JavaScriptNode exceptionNode;
    private final ConditionProfile isError = ConditionProfile.createBinaryProfile();

    protected ThrowNode(JavaScriptNode exceptionNode) {
        this.exceptionNode = exceptionNode;
    }

    public static ThrowNode create(JavaScriptNode exceptionNode) {
        return new ThrowNode(exceptionNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object exceptionObject = exceptionNode.execute(frame);
        if (isError.profile(JSError.isJSError(exceptionObject))) {
            DynamicObject jsobject = (DynamicObject) exceptionObject;
            if (JSTruffleOptions.NashornCompatibilityMode) {
                SourceSection sourceSection = getSourceSection();
                JSContext context = JSObject.getJSContext(jsobject);
                JSError.setLineNumber(context, jsobject, sourceSection.getStartLine());
                JSError.setColumnNumber(context, jsobject, sourceSection.getStartColumn());
            }
            throw JSError.getException(jsobject);
        }
        throw UserScriptException.create(exceptionObject, this);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(exceptionNode));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return true;
    }
}
