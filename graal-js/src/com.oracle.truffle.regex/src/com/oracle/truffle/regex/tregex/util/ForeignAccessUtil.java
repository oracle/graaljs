/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.util;

import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.nodes.Node;

public class ForeignAccessUtil {

    public static Node createReadMessageNode() {
        return Message.READ.createNode();
    }

    public static Node createGetSizeMessageNode() {
        return Message.GET_SIZE.createNode();
    }
}
