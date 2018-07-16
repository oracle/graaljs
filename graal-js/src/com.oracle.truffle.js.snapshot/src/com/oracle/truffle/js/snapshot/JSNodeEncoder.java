/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.snapshot;

import static com.oracle.truffle.js.nodes.JSNodeDecoder.BREAK_TARGET_LABEL;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.BREAK_TARGET_SWITCH;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.CONTINUE_TARGET_LOOP;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.CONTINUE_TARGET_UNLABELED_LOOP;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.getSingletonIndex;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_CALL_EXTRACTED;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_CALL_EXTRACTED_LAZY;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_CALL_TARGET;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_COLLECT_ARRAY;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_COLLECT_LIST;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_FRAME_DESCRIPTOR;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_FRAME_SLOT;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_FUNCTION_DATA;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_FUNCTION_DATA_NAME_FIXUP;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_JUMP_TARGET;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_LDC_BOOLEAN;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_LDC_DOUBLE;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_LDC_ENUM;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_LDC_INT;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_LDC_LONG;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_LDC_SINGLETON;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_LDC_STRING;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_LD_ARG;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_MOV;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_NODE;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_NODE_SOURCE_SECTION_FIXUP;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_NODE_TAGS_FIXUP;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_RETURN;
import static com.oracle.truffle.js.nodes.JSNodeDecoder.Bytecode.ID_SOURCE_SECTION;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.codec.BinaryEncoder;
import com.oracle.truffle.js.codec.NodeDecoder;
import com.oracle.truffle.js.nodes.JSNodeDecoder;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.NodeFactoryDecoderGen;
import com.oracle.truffle.js.nodes.control.BreakTarget;
import com.oracle.truffle.js.nodes.control.ContinueTarget;
import com.oracle.truffle.js.parser.BinarySnapshotProvider;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;

public class JSNodeEncoder {
    private final BinaryEncoder encoder;
    private final HashMap<String, Integer> patchPositions = new HashMap<>();
    private final HashMap<String, Integer> resolvedPositions = new HashMap<>();

    private static final int FIRST_REG = 1;
    private final HashMap<Integer, Integer> valueIdToRegMap = new HashMap<>();
    private int nextReg;
    private int regsMax;

    private static final NodeDecoder<NodeFactory> GEN = NodeFactoryDecoderGen.create();

    private JSNodeEncoder(BinaryEncoder encoder) {
        this.encoder = encoder;
    }

    public JSNodeEncoder(BinaryEncoder encoder, CharSequence sourceCode) {
        this(encoder);
        putInt32(BinarySnapshotProvider.MAGIC);
        putInt32(JSNodeDecoder.getChecksum());
        putInt32(sourceCode.length());
        putInt32(sourceCode.hashCode());
    }

    private void putBytecode(JSNodeDecoder.Bytecode value) {
        encoder.putUInt(value.ordinal());
    }

    private void putInt(int value) {
        encoder.putInt(value);
    }

    private void putUInt(int value) {
        encoder.putUInt(value);
    }

    private void putLong(long value) {
        encoder.putLong(value);
    }

    private void putBoolean(boolean value) {
        encoder.putInt(value ? 1 : 0);
    }

    private void putDouble(double value) {
        encoder.putDouble(value);
    }

    private void putString(String value) {
        encoder.putUTF8(value);
    }

    private void putInt32(int value) {
        encoder.putInt32(value);
    }

    private static String getMethodSignature(Method method) {
        return method.getName() + Arrays.stream(method.getParameterTypes()).map(t -> getTypeSignature(t)).collect(Collectors.joining(",", "(", ")")) + getTypeSignature(method.getReturnType());
    }

    private static String getTypeSignature(Class<?> type) {
        return type.getCanonicalName();
    }

    public void encodeReturn(int value) {
        putBytecode(ID_RETURN);
        encodeReg(value);
    }

    private void encodeReg(int id) {
        int reg = valueIdToRegMap.computeIfAbsent(id, i -> nextReg++);
        assert reg < regsMax : id + " => " + reg;
        putUInt(reg);
    }

    public void encodeNode(Method method, int ret, int[] args) {
        putBytecode(ID_NODE);
        putUInt(GEN.getMethodIdFromSignature(getMethodSignature(method)));
        encodeRegs(args, false);
        encodeReg(ret);
    }

    private void encodeRegs(int[] args, boolean encodeLength) {
        if (encodeLength) {
            putInt(args.length);
        }
        for (int i = 0; i < args.length; i++) {
            encodeReg(args[i]);
        }
    }

    public void encodeConstant(int dest, Object value) {
        if (value instanceof Integer) {
            putBytecode(ID_LDC_INT);
            putInt((int) value);
        } else if (value instanceof Long) {
            putBytecode(ID_LDC_LONG);
            putLong((long) value);
        } else if (value instanceof Boolean) {
            putBytecode(ID_LDC_BOOLEAN);
            putBoolean((boolean) value);
        } else if (value instanceof Double) {
            putBytecode(ID_LDC_DOUBLE);
            putDouble((double) value);
        } else if (value instanceof String) {
            putBytecode(ID_LDC_STRING);
            putString((String) value);
        } else if (value instanceof Enum<?>) {
            putBytecode(ID_LDC_ENUM);
            int typeId = Arrays.asList(GEN.getClasses()).indexOf(value.getClass());
            if (typeId == -1) {
                throw new UnsupportedOperationException("Unsupported enum class: " + value.getClass());
            }
            putInt(typeId);
            putInt(((Enum<?>) value).ordinal());
        } else {
            int index = getSingletonIndex(value);
            if (index == -1) {
                throw new UnsupportedOperationException("Unsupported constant: " + value);
            }
            putBytecode(ID_LDC_SINGLETON);
            putInt(index);
        }
        encodeReg(dest);
    }

    public void encodeMove(int dest, int src) {
        assert dest >= 0 && src >= 0;
        putBytecode(ID_MOV);
        encodeReg(dest);
        encodeReg(src);
    }

    public void encodeLoadArg(int dest, int index) {
        putBytecode(ID_LD_ARG);
        putInt(index);
        encodeReg(dest);
    }

    public void encodeCollect(int dest, Class<?> type, int[] args) {
        if (type.isArray()) {
            putBytecode(ID_COLLECT_ARRAY);
            encodeClass(type.getComponentType());
            encodeRegs(args, true);
        } else if (type == ArrayList.class) {
            putBytecode(ID_COLLECT_LIST);
            encodeRegs(args, true);
        } else {
            throw new UnsupportedOperationException("Unsupported collection class: " + type);
        }
        encodeReg(dest);
    }

    private void encodeClass(Class<?> type) {
        int typeId = Arrays.asList(GEN.getClasses()).indexOf(type);
        if (typeId == -1) {
            throw new UnsupportedOperationException("Unsupported class: " + type);
        }
        putInt(typeId);
    }

    public void encodeCallTarget(int dest, int rootNodeArg) {
        putBytecode(ID_CALL_TARGET);
        encodeReg(rootNodeArg);
        encodeReg(dest);
    }

    public void encodeFrameDescriptor(int dest) {
        putBytecode(ID_FRAME_DESCRIPTOR);
        encodeReg(dest);
    }

    public void encodeFrameSlot(int dest, int frameDescriptorArg, int identifierArg, int flags, boolean findOrAdd) {
        putBytecode(ID_FRAME_SLOT);
        encodeReg(frameDescriptorArg);
        encodeReg(identifierArg);
        putInt(flags);
        putBoolean(findOrAdd);
        encodeReg(dest);
    }

    public void encodeSourceSection(int dest, int sourceArg, SourceSection sourceSection) {
        putBytecode(ID_SOURCE_SECTION);
        encodeReg(sourceArg);
        int charIndex = sourceSection.getCharIndex();
        int charLength = sourceSection.getCharLength();
        if (!sourceSection.isAvailable()) {
            charIndex = -1;
            charLength = -1;
        }
        putInt(charIndex);
        putInt(charLength);
        encodeReg(dest);
    }

    public void encodeFunctionData(int dest, int contextArg, JSFunctionData functionData) {
        putBytecode(ID_FUNCTION_DATA);
        encodeReg(contextArg);
        putInt(functionData.getLength());
        putString(functionData.getName());
        putInt32(functionData.getFlags());
        encodeReg(dest);
    }

    public void encodeFunctionDataNameFixup(int functionDataArg, String name) {
        putBytecode(ID_FUNCTION_DATA_NAME_FIXUP);
        encodeReg(functionDataArg);
        putString(name);
    }

    public void encodeBreakTarget(int dest, BreakTarget target) {
        int type;
        if (target instanceof ContinueTarget) {
            if (target.getId() != 0) {
                type = CONTINUE_TARGET_LOOP;
            } else {
                type = CONTINUE_TARGET_UNLABELED_LOOP;
            }
        } else {
            if (target.getId() != 0) {
                type = BREAK_TARGET_LABEL;
            } else {
                type = BREAK_TARGET_SWITCH;
            }
        }
        putBytecode(ID_JUMP_TARGET);
        putInt(type);
        encodeReg(dest);
    }

    public void encodeCallExtracted(String name, int dest, int[] methodArgs) {
        putBytecode(ID_CALL_EXTRACTED);
        putExtractedPosition(name);
        encodeRegs(methodArgs, true);
        encodeReg(dest);
    }

    public void encodeCallExtractedLazy(String name, int fd, int[] methodArgs) {
        putBytecode(ID_CALL_EXTRACTED_LAZY);
        putExtractedPosition(name);
        encodeReg(fd);
        encodeRegs(methodArgs, true);
    }

    private void putExtractedPosition(String name) {
        if (resolvedPositions.containsKey(name)) {
            assert false : "already resolved " + name;
            encoder.putInt32(resolvedPositions.get(name));
            return;
        }

        assert !patchPositions.containsKey(name) : name;
        patchPositions.put(name, encoder.getPosition());
        encoder.putInt32(-1);
    }

    public void markExtractedPosition(String name) {
        int mark = encoder.getPosition();
        resolvedPositions.put(name, mark);
        if (patchPositions.containsKey(name)) {
            int pos = patchPositions.get(name);
            ByteBuffer bb = encoder.getBuffer().duplicate().order(ByteOrder.LITTLE_ENDIAN);
            bb.position(pos);
            assert bb.getInt() == -1;
            Recording.logv(" -- %d: %d %s", pos, mark, name);
            bb.position(pos);
            bb.putInt(mark);
        } else {
            Recording.logv("nothing to patch: %s (%d)", name, mark);
        }
    }

    private void resetRegisterMapping() {
        valueIdToRegMap.clear();
        nextReg = FIRST_REG;
    }

    public void encodeRegisterArraySize(int regsSize) {
        resetRegisterMapping();
        int limit = FIRST_REG + regsSize;
        putUInt(limit);
        this.regsMax = limit;
    }

    public void encodeNodeSourceSectionFixup(int nodeArg, int sourceArg, int charIndex, int charLength) {
        putBytecode(ID_NODE_SOURCE_SECTION_FIXUP);
        encodeReg(nodeArg);
        encodeReg(sourceArg);
        putInt(charIndex);
        putInt(charLength);
    }

    public void encodeNodeTagsFixup(int nodeArg, boolean hasStatementTag, boolean hasCallTag, boolean hasExpressionTag, boolean hasRootTag) {
        putBytecode(ID_NODE_TAGS_FIXUP);
        encodeReg(nodeArg);
        putBoolean(hasStatementTag);
        putBoolean(hasCallTag);
        putBoolean(hasExpressionTag);
        putBoolean(hasRootTag);
    }
}
