/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.js.nodes.*;

public interface WriteNode extends NodeInterface {
    Object executeWrite(VirtualFrame frame, Object value);

    JavaScriptNode getRhs();
}
