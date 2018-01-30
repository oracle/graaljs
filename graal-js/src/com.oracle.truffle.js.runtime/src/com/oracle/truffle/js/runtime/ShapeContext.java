/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.SIMDType;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDTypeFactory;

/**
 * Temporary interface for shape access.
 */
public interface ShapeContext {
    Shape getInitialUserObjectShape();

    Shape getEmptyShape();

    Shape getEmptyShapePrototypeInObject();

    DynamicObjectFactory getArrayFactory();

    DynamicObjectFactory getStringFactory();

    DynamicObjectFactory getBooleanFactory();

    DynamicObjectFactory getNumberFactory();

    DynamicObjectFactory getSymbolFactory();

    DynamicObjectFactory getArrayBufferViewFactory(TypedArrayFactory factory);

    DynamicObjectFactory getArrayBufferFactory();

    DynamicObjectFactory getDirectArrayBufferViewFactory(TypedArrayFactory factory);

    DynamicObjectFactory getDirectArrayBufferFactory();

    DynamicObjectFactory getRegExpFactory();

    DynamicObjectFactory getDateFactory();

    DynamicObjectFactory getModuleNamespaceFactory();

    DynamicObjectFactory getEnumerateIteratorFactory();

    DynamicObjectFactory getMapFactory();

    DynamicObjectFactory getWeakMapFactory();

    DynamicObjectFactory getSetFactory();

    DynamicObjectFactory getCollatorFactory();

    DynamicObjectFactory getNumberFormatFactory();

    DynamicObjectFactory getPluralRulesFactory();

    DynamicObjectFactory getDateTimeFormatFactory();

    DynamicObjectFactory getWeakSetFactory();

    DynamicObjectFactory getDataViewFactory();

    DynamicObjectFactory getProxyFactory();

    DynamicObjectFactory getSharedArrayBufferFactory();

    DynamicObjectFactory getJavaImporterFactory();

    DynamicObjectFactory getJSAdapterFactory();

    DynamicObjectFactory getErrorFactory(JSErrorType type, boolean withMessage);

    DynamicObjectFactory getSIMDTypeFactory(SIMDTypeFactory<? extends SIMDType> factory);

    DynamicObjectFactory getPromiseFactory();
}
