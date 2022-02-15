/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime;

import java.util.Objects;
import java.util.OptionalInt;

import com.oracle.js.parser.ir.Symbol;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.InternalSlotId;

public final class JSFrameUtil {
    public static final MaterializedFrame NULL_MATERIALIZED_FRAME = Truffle.getRuntime().createMaterializedFrame(JSArguments.createNullArguments());
    public static final Object DEFAULT_VALUE = Undefined.instance;

    private static final String THIS_SLOT_ID = "<this>";
    private static final Class<? extends MaterializedFrame> MATERIALIZED_FRAME_CLASS = NULL_MATERIALIZED_FRAME.getClass();
    private static final int IS_LET = Symbol.IS_LET;
    private static final int IS_CONST = Symbol.IS_CONST;
    private static final int HAS_TDZ = IS_LET | IS_CONST;
    private static final int IS_HOISTABLE_DECLARATION = Symbol.IS_HOISTABLE_DECLARATION;
    private static final int IS_IMPORT_BINDING = Symbol.IS_IMPORT_BINDING;
    private static final int IS_PRIVATE_NAME = Symbol.IS_PRIVATE_NAME;
    private static final int IS_PRIVATE_NAME_STATIC = Symbol.IS_PRIVATE_NAME_STATIC;
    private static final int IS_PRIVATE_METHOD_OR_ACCESSOR = Symbol.IS_PRIVATE_NAME_METHOD | Symbol.IS_PRIVATE_NAME_ACCESSOR;
    private static final int IS_PARAM = Symbol.IS_PARAM;
    private static final int IS_ARGUMENTS = Symbol.IS_ARGUMENTS;
    public static final int SYMBOL_FLAG_MASK = HAS_TDZ | IS_HOISTABLE_DECLARATION | IS_IMPORT_BINDING | IS_PARAM | IS_ARGUMENTS |
                    IS_PRIVATE_NAME | IS_PRIVATE_NAME_STATIC | IS_PRIVATE_METHOD_OR_ACCESSOR | Symbol.IS_CLOSED_OVER;

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

    public static int getFlags(JSFrameSlot frameSlot) {
        return frameSlot.getFlags();
    }

    public static int getFlags(FrameDescriptor desc, int index) {
        return getFlagsFromInfo(desc.getSlotInfo(index));
    }

    public static int getFlagsFromInfo(Object info) {
        return info instanceof Integer ? (int) info : 0;
    }

    public static boolean hasTemporalDeadZone(JSFrameSlot frameSlot) {
        return (getFlags(frameSlot) & HAS_TDZ) != 0;
    }

    public static boolean hasTemporalDeadZone(FrameDescriptor desc, int index) {
        return (getFlags(desc, index) & HAS_TDZ) != 0;
    }

    public static boolean needsTemporalDeadZoneCheck(JSFrameSlot frameSlot, int frameLevel) {
        return hasTemporalDeadZone(frameSlot) && frameLevel != 0;
    }

    public static boolean isConst(JSFrameSlot frameSlot) {
        return (getFlags(frameSlot) & IS_CONST) != 0;
    }

    public static boolean isLet(JSFrameSlot frameSlot) {
        return (getFlags(frameSlot) & IS_LET) != 0;
    }

    public static boolean isConst(FrameDescriptor desc, int index) {
        return (getFlags(desc, index) & IS_CONST) != 0;
    }

    public static boolean isLet(FrameDescriptor desc, int index) {
        return (getFlags(desc, index) & IS_LET) != 0;
    }

    public static boolean isHoistable(FrameDescriptor desc, int index) {
        return (getFlags(desc, index) & IS_HOISTABLE_DECLARATION) != 0;
    }

    public static boolean isImportBinding(JSFrameSlot frameSlot) {
        return (getFlags(frameSlot) & IS_IMPORT_BINDING) != 0;
    }

    public static boolean isPrivateName(JSFrameSlot frameSlot) {
        return (getFlags(frameSlot) & IS_PRIVATE_NAME) != 0;
    }

    public static boolean needsPrivateBrandCheck(JSFrameSlot frameSlot) {
        return (getFlags(frameSlot) & IS_PRIVATE_METHOD_OR_ACCESSOR) != 0;
    }

    public static boolean isPrivateNameStatic(JSFrameSlot frameSlot) {
        return (getFlags(frameSlot) & IS_PRIVATE_NAME_STATIC) != 0;
    }

    public static boolean isParam(JSFrameSlot frameSlot) {
        return (getFlags(frameSlot) & IS_PARAM) != 0;
    }

    public static boolean isArguments(JSFrameSlot frameSlot) {
        return (getFlags(frameSlot) & IS_ARGUMENTS) != 0;
    }

    public static boolean isClosedOver(JSFrameSlot frameSlot) {
        return (getFlags(frameSlot) & Symbol.IS_CLOSED_OVER) != 0;
    }

    public static MaterializedFrame getParentFrame(Frame frame) {
        return JSArguments.getEnclosingFrame(frame.getArguments());
    }

    public static MaterializedFrame castMaterializedFrame(Object frame) {
        return CompilerDirectives.castExact(Objects.requireNonNull(frame), MATERIALIZED_FRAME_CLASS);
    }

    /**
     * Returns true if the frame slot is implementation-internal.
     */
    public static boolean isInternal(FrameDescriptor desc, int index) {
        return isInternalIdentifier(desc.getSlotName(index));
    }

    public static boolean isInternalIdentifier(Object identifier) {
        CompilerAsserts.neverPartOfCompilation();
        if (identifier instanceof String) {
            String name = (String) identifier;
            if (name.startsWith(":")) {
                return true;
            } else if (name.startsWith("<") && name.endsWith(">")) {
                return true;
            }
            return false;
        } else if (identifier instanceof InternalSlotId) {
            return true;
        }
        return true;
    }

    public static String getPublicName(Object identifier) {
        CompilerAsserts.neverPartOfCompilation();
        if (identifier instanceof String) {
            String name = (String) identifier;
            if (name.startsWith(":")) {
                return name.substring(1);
            } else if (name.startsWith("<") && name.endsWith(">")) {
                return name.substring(1, name.length() - 1);
            } else {
                return name;
            }
        } else {
            return identifier.toString();
        }
    }

    public static boolean isThisSlot(FrameDescriptor desc, int index) {
        return isThisSlotIdentifier(desc.getSlotName(index));
    }

    public static boolean isThisSlotIdentifier(Object identifier) {
        return THIS_SLOT_ID.equals(identifier);
    }

    public static int getThisSlotIndex(FrameDescriptor frameDescriptor) {
        for (int i = 0; i < frameDescriptor.getNumberOfSlots(); i++) {
            if (isThisSlotIdentifier(frameDescriptor.getSlotName(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int findFrameSlotIndex(FrameDescriptor frameDescriptor, Object identifier) {
        CompilerAsserts.neverPartOfCompilation();
        for (int i = 0; i < frameDescriptor.getNumberOfSlots(); i++) {
            if (identifier.equals(frameDescriptor.getSlotName(i))) {
                return i;
            }
        }
        return -1;
    }

    public static int findRequiredFrameSlotIndex(FrameDescriptor frameDescriptor, Object identifier) {
        int index = findFrameSlotIndex(frameDescriptor, identifier);
        assert index >= 0 : identifier + " not found in " + frameDescriptor;
        return index;
    }

    public static OptionalInt findOptionalFrameSlotIndex(FrameDescriptor frameDescriptor, Object identifier) {
        int index = findFrameSlotIndex(frameDescriptor, identifier);
        return index >= 0 ? OptionalInt.of(index) : OptionalInt.empty();
    }
}
