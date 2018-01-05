/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;

public final class JSFrameUtil {
    public static final MaterializedFrame NULL_MATERIALIZED_FRAME = Truffle.getRuntime().createMaterializedFrame(JSArguments.createNullArguments());

    private static final int IS_LET = 1 << 4;
    private static final int IS_CONST = 1 << 5;
    private static final int HAS_TDZ = IS_LET | IS_CONST;

    private JSFrameUtil() {
        // this utility class should not be instantiated
    }

    public static Object getThisObj(Frame frame) {
        return JSArguments.getThisObject(frame.getArguments());
    }

    public static DynamicObject getFunctionObject(Frame frame) {
        return (DynamicObject) JSArguments.getFunctionObject(frame.getArguments());
    }

    public static Object[] getArgumentsArray(Frame frame) {
        return JSArguments.extractUserArguments(frame.getArguments());
    }

    public static int getFlags(FrameSlot frameSlot) {
        return frameSlot.getInfo() instanceof Integer ? (int) frameSlot.getInfo() : 0;
    }

    public static boolean hasTemporalDeadZone(FrameSlot frameSlot) {
        return (getFlags(frameSlot) & HAS_TDZ) != 0;
    }

    public static boolean needsTemporalDeadZoneCheck(FrameSlot frameSlot, int frameLevel) {
        return hasTemporalDeadZone(frameSlot) && frameLevel != 0;
    }

    public static boolean isConst(FrameSlot frameSlot) {
        return (getFlags(frameSlot) & IS_CONST) != 0;
    }

    public static boolean isLet(FrameSlot frameSlot) {
        return (getFlags(frameSlot) & IS_LET) != 0;
    }

    public static MaterializedFrame getParentFrame(Frame frame) {
        return JSArguments.getEnclosingFrame(frame.getArguments());
    }

    /**
     * Returns true if the frame slot is implementation-internal.
     */
    public static boolean isInternal(FrameSlot frameSlot) {
        CompilerAsserts.neverPartOfCompilation();
        if (frameSlot.getIdentifier() instanceof String) {
            String name = (String) frameSlot.getIdentifier();
            if (name.startsWith(":")) {
                return true;
            } else if (name.startsWith("<") && name.endsWith(">")) {
                return true;
            }
            return false;
        }
        return true;
    }
}
