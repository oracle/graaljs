/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.snapshot;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.NodeFactoryProxyGen;

public class RecordingProxy {
    public static NodeFactory createRecordingNodeFactory(final Recording rec, final NodeFactory nodeFactory) {
        InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
                rec.recordCall(method, args != null ? args : new Object[0]);
                Object result = method.invoke(nodeFactory, args);
                rec.recordReturn(method, result);
                return result;
            }
        };
        if (NodeFactory.class.isInterface()) {
            return (NodeFactory) Proxy.newProxyInstance(RecordingProxy.class.getClassLoader(), new Class<?>[]{NodeFactory.class}, invocationHandler);
        } else {
            return NodeFactoryProxyGen.create(invocationHandler);
        }
    }
}
