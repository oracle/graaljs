/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.helper;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;

public class JSONData {

    protected List<TruffleObject> stack = new ArrayList<>();
    private int indent;
    private final String gap;
    private final List<String> propertyList;
    private final DynamicObject replacerFnObj;

    private static final int MAX_STACK_SIZE = 1000;

    public JSONData(String gap, DynamicObject replacerFnObj, List<String> replacerList) {
        this.gap = gap;
        this.replacerFnObj = replacerFnObj;
        this.propertyList = replacerList;
    }

    public String getGap() {
        return gap;
    }

    public int getIndent() {
        return indent;
    }

    public void setIndent(int indentCount) {
        this.indent = indentCount;
    }

    public List<String> getPropertyList() {
        return propertyList;
    }

    public DynamicObject getReplacerFnObj() {
        return replacerFnObj;
    }

    public void pushStack(TruffleObject value) {
        stack.add(value);
    }

    public boolean stackTooDeep() {
        return stack.size() > MAX_STACK_SIZE;
    }

    public void popStack() {
        stack.remove(stack.size() - 1);
    }
}
