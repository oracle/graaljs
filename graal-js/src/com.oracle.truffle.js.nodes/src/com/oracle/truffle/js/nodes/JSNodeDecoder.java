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
package com.oracle.truffle.js.nodes;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.codec.BinaryDecoder;
import com.oracle.truffle.js.codec.NodeDecoder;
import com.oracle.truffle.js.nodes.control.BreakTarget;
import com.oracle.truffle.js.nodes.control.ContinueTarget;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class JSNodeDecoder {
    public enum Bytecode {
        ID_NOP,

        /** Create node using factory method. */
        ID_NODE,

        /** Return result. */
        ID_RETURN,
        /** Load integer constant into register. */
        ID_LDC_INT,
        /** Load long constant into register. */
        ID_LDC_LONG,
        /** Load boolean constant into register. */
        ID_LDC_BOOLEAN,
        /** Load double constant into register. */
        ID_LDC_DOUBLE,
        /** Load enum constant into register. */
        ID_LDC_ENUM,
        /** Load string constant into register. */
        ID_LDC_STRING,
        /** Load singleton constant into register. */
        ID_LDC_SINGLETON,

        /** Load argument into register. */
        ID_LD_ARG,
        /** Move register to register. */
        ID_MOV,

        /** Create dynamic array from registers. */
        ID_COLLECT_ARRAY,
        /** Create ArrayList from registers. */
        ID_COLLECT_LIST,
        /** Create CallTarget. */
        ID_CALL_TARGET,
        /** Create FrameDescriptor. */
        ID_FRAME_DESCRIPTOR,
        /** Add FrameSlot to FrameDescriptor. */
        ID_FRAME_SLOT,
        /** Create SourceSection. */
        ID_SOURCE_SECTION,

        /** Create JSFunctionData. */
        ID_FUNCTION_DATA,
        /** Fix up JSFunctionData. */
        ID_FUNCTION_DATA_NAME_FIXUP,

        /** Create {@link BreakTarget} or {@link ContinueTarget}. */
        ID_JUMP_TARGET,

        ID_CALL_EXTRACTED,
        ID_CALL_EXTRACTED_LAZY,

        ID_NODE_SOURCE_SECTION_FIXUP,
        ID_NODE_TAGS_FIXUP;

        static final Bytecode[] values = values();
    }

    public static final int BREAK_TARGET_LABEL = 1;
    public static final int BREAK_TARGET_SWITCH = 2;
    public static final int CONTINUE_TARGET_LOOP = 3;
    public static final int CONTINUE_TARGET_UNLABELED_LOOP = 4;

    private static final boolean VERBOSE = false;
    private static final NodeDecoder<NodeFactory> GEN = NodeFactoryDecoderGen.create();

    private static final Object[] SINGLETONS = new Object[]{null, Undefined.instance, Null.instance, Dead.instance()};

    public static int getSingletonIndex(Object singleton) {
        return Arrays.asList(SINGLETONS).indexOf(singleton);
    }

    public static int getChecksum() {
        return GEN.getChecksum();
    }

    public Object decodeNode(NodeDecoder.DecoderState state, NodeFactory nodeFactory, JSContext context, Source source) {
        while (state.hasRemaining()) {
            Bytecode bc = Bytecode.values[state.getBytecode()];
            switch (bc) {
                case ID_NOP:
                    break;
                case ID_NODE: {
                    Object node = GEN.decodeNode(state, nodeFactory);
                    int dest = state.getReg();
                    if (dest >= 0) {
                        state.setObjReg(dest, node);
                    }
                    break;
                }
                case ID_RETURN:
                    return state.getObject();
                case ID_LDC_INT:
                    storeResult(state, state.getInt());
                    break;
                case ID_LDC_LONG:
                    storeResult(state, state.getLong());
                    break;
                case ID_LDC_BOOLEAN:
                    storeResult(state, state.getBoolean());
                    break;
                case ID_LDC_DOUBLE:
                    storeResult(state, state.getDouble());
                    break;
                case ID_LDC_ENUM:
                    storeResult(state, GEN.getClasses()[state.getInt()].getEnumConstants()[state.getInt()]);
                    break;
                case ID_LDC_STRING:
                    storeResult(state, state.getString());
                    break;
                case ID_LDC_SINGLETON:
                    storeResult(state, SINGLETONS[state.getInt()]);
                    break;
                case ID_MOV:
                    state.setObjReg(state.getReg(), state.getObjReg(state.getReg()));
                    break;
                case ID_LD_ARG:
                    int argIndex = state.getInt();
                    Object argument;
                    if (argIndex == -1) {
                        argument = context;
                    } else if (argIndex == -2) {
                        argument = source;
                    } else {
                        argument = state.getArgument(argIndex);
                    }
                    storeResult(state, argument);
                    break;
                case ID_COLLECT_ARRAY: {
                    int componentTypeIndex = state.getInt();
                    int length = state.getInt();
                    Object array = Array.newInstance(GEN.getClasses()[componentTypeIndex], length);
                    if (array instanceof Object[]) {
                        Object[] objArray = (Object[]) array;
                        for (int i = 0; i < length; i++) {
                            Object value = state.getObject();
                            objArray[i] = value;
                        }
                    } else {
                        for (int i = 0; i < length; i++) {
                            Object value = state.getObject();
                            Array.set(array, i, value);
                        }
                    }
                    storeResult(state, array);
                    break;
                }
                case ID_COLLECT_LIST: {
                    int length = state.getInt();
                    ArrayList<Object> array = new ArrayList<>(length);
                    for (int i = 0; i < length; i++) {
                        array.add(state.getObject());
                    }
                    storeResult(state, array);
                    break;
                }

                case ID_CALL_TARGET:
                    storeResult(state, Truffle.getRuntime().createCallTarget((RootNode) state.getObject()));
                    break;
                case ID_FRAME_DESCRIPTOR:
                    storeResult(state, new FrameDescriptor(Undefined.instance));
                    break;
                case ID_FRAME_SLOT: {
                    FrameDescriptor frameDescriptor = (FrameDescriptor) state.getObject();
                    Object identifier = state.getObject();
                    int flags = state.getInt();
                    boolean findOrAdd = state.getBoolean();
                    FrameSlot frameSlot = findOrAdd ? frameDescriptor.findOrAddFrameSlot(identifier, flags, FrameSlotKind.Illegal)
                                    : frameDescriptor.addFrameSlot(identifier, flags, FrameSlotKind.Illegal);
                    storeResult(state, frameSlot);
                    break;
                }
                case ID_SOURCE_SECTION: {
                    Source src = (Source) state.getObject();
                    int charIndex = state.getInt();
                    int charLength = state.getInt();
                    SourceSection sourceSection;
                    if ((charIndex < 0 || charLength < 0) || (src.getCharacters().length() == 0 && charIndex + charLength > 0)) {
                        sourceSection = src.createUnavailableSection();
                    } else {
                        sourceSection = src.createSection(charIndex, charLength);
                    }
                    storeResult(state, sourceSection);
                    break;
                }
                case ID_FUNCTION_DATA: {
                    JSContext ctx = (JSContext) state.getObject();
                    int length = state.getInt();
                    String functionName = state.getString();
                    int flags = state.getInt32();
                    JSFunctionData functionData = JSFunctionData.create(ctx, null, null, null, length, functionName, flags);
                    storeResult(state, functionData);
                    break;
                }
                case ID_FUNCTION_DATA_NAME_FIXUP: {
                    JSFunctionData functionData = (JSFunctionData) state.getObject();
                    String name = state.getString();
                    functionData.setName(name);
                    break;
                }
                case ID_JUMP_TARGET:
                    storeResult(state, createJumpTarget(state.getInt()));
                    break;
                case ID_CALL_EXTRACTED: {
                    final int position = state.getInt32();
                    if (VERBOSE) {
                        System.err.println("callex pos:" + position);
                    }
                    final Object[] arguments = getObjectArray(state);
                    final ByteBuffer buffer = ((ByteBuffer) state.getBuffer().duplicate().position(position));
                    NodeDecoder.DecoderState extracted = new NodeDecoder.DecoderState(new BinaryDecoder(buffer), arguments);
                    storeResult(state, decodeNode(extracted, nodeFactory, context, source));
                    break;
                }
                case ID_CALL_EXTRACTED_LAZY: {
                    if (VERBOSE) {
                        System.err.println("callex-lazy@:" + state.getBuffer().position());
                    }
                    final int position = state.getInt32();
                    if (VERBOSE) {
                        System.err.println("callex-lazy pos:" + position);
                    }
                    JSFunctionData functionData = (JSFunctionData) state.getObject();
                    final Object[] arguments = getObjectArray(state);
                    functionData.setLazyInit(new JSFunctionData.Initializer() {
                        private final ByteBuffer buffer = ((ByteBuffer) state.getBuffer().duplicate().position(position));

                        @Override
                        public void initializeRoot(JSFunctionData fd) {
                            if (VERBOSE) {
                                System.out.println("Decoding: " + fd.getName());
                            }
                            NodeDecoder.DecoderState extracted = new NodeDecoder.DecoderState(new BinaryDecoder(buffer), arguments);
                            decodeNode(extracted, nodeFactory, context, source);
                        }
                    });
                    break;
                }
                case ID_NODE_SOURCE_SECTION_FIXUP: {
                    JavaScriptNode jsnode = (JavaScriptNode) state.getObject();
                    Source src = (Source) state.getObject();
                    int charIndex = state.getInt();
                    int charLength = state.getInt();
                    if ((charIndex < 0 || charLength < 0) || (src.getCharacters().length() == 0 && charIndex + charLength > 0)) {
                        jsnode.setSourceSection(src.createUnavailableSection());
                    } else {
                        jsnode.setSourceSection(src, charIndex, charLength);
                    }
                    break;
                }
                case ID_NODE_TAGS_FIXUP: {
                    JavaScriptNode jsnode = (JavaScriptNode) state.getObject();
                    boolean hasStatementTag = state.getBoolean();
                    boolean hasCallTag = state.getBoolean();
                    boolean hasExpressionTag = state.getBoolean();
                    boolean hasRootTag = state.getBoolean();
                    if (hasStatementTag) {
                        jsnode.addStatementTag();
                    }
                    if (hasCallTag) {
                        jsnode.addCallTag();
                    }
                    if (hasExpressionTag) {
                        jsnode.addExpressionTag();
                    }
                    if (hasRootTag) {
                        jsnode.addRootTag();
                    }
                    break;
                }
                default:
                    throw new IllegalStateException("invalid bytecode " + bc);
            }
        }
        throw new IllegalStateException("reached end of buffer without return");
    }

    private static void storeResult(NodeDecoder.DecoderState state, Object value) {
        state.setObjReg(state.getReg(), value);
    }

    private static BreakTarget createJumpTarget(int type) {
        switch (type) {
            case BREAK_TARGET_LABEL:
                return BreakTarget.forLabel(null, -1);
            case BREAK_TARGET_SWITCH:
                return BreakTarget.forSwitch();
            case CONTINUE_TARGET_LOOP:
                return ContinueTarget.forLoop(null, -1);
            case CONTINUE_TARGET_UNLABELED_LOOP:
                return ContinueTarget.forUnlabeledLoop();
        }
        throw new IllegalStateException("invalid jump target");
    }

    private static Object[] getObjectArray(NodeDecoder.DecoderState state) {
        int length = state.getInt();
        Object[] array = new Object[length];
        for (int i = 0; i < length; i++) {
            array[i] = state.getObject();
        }
        return array;
    }
}
