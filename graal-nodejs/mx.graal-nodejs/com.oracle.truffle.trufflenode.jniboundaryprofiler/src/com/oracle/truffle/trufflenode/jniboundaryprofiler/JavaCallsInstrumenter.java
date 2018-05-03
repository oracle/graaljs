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
package com.oracle.truffle.trufflenode.jniboundaryprofiler;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

public class JavaCallsInstrumenter {

    public static byte[] maybeInstrumentClass(String className, byte[] classfileBuffer) {
        if (instrumentMethodInvocations(className)) {
            System.out.println("Instrumenting Java method calls in: " + className);
            ClassReader cr = new ClassReader(classfileBuffer);
            CustomClassWriter cw = new CustomClassWriter(cr);
            ClassVisitor cv = new InvCounterClassVisitor(cw, className);
            cr.accept(cv, 0);
            return cw.toByteArray();
        }
        return classfileBuffer;
    }

    private static boolean instrumentMethodInvocations(String klass) {
        return "com/oracle/truffle/trufflenode/GraalJSAccess".equals(klass);
    }

    private static class CustomClassWriter extends ClassWriter {

        CustomClassWriter(ClassReader cr) {
            super(cr, ClassWriter.COMPUTE_FRAMES);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            // ASM uses reflection to find the common super class for JS exceptions.
            // We provide the information statically to avoid reflection and changing the boot CP
            String jsExceptionKlass = "com/oracle/truffle/js/runtime/JSException";
            if (jsExceptionKlass.equals(type1) || jsExceptionKlass.equals(type2)) {
                assert type1.equals("java/lang/Throwable") || type2.equals("java/lang/Throwable");
                return "java/lang/Throwable";
            } else {
                return super.getCommonSuperClass(type1, type2);
            }
        }
    }

    private static class InvCounterClassVisitor extends ClassVisitor implements Opcodes {

        private final String className;

        InvCounterClassVisitor(final ClassVisitor cv, String className) {
            super(ASM5, cv);
            this.className = className;
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name,
                        final String desc, final String signature, final String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if (mv != null && !"<init>".equals(name) && !"<clinit>".equals(name) && (ACC_PUBLIC & access) != 0) {
                return new InvCounterMethodVisitor(mv, className, name);
            }
            return mv;
        }
    }

    private static class InvCounterMethodVisitor extends MethodVisitor implements Opcodes {

        private static final String ProfilerBegin = "jniCallBegin";
        private static final String ProfilerEnd = "jniCallEnd";

        private final String label;

        InvCounterMethodVisitor(MethodVisitor mv, String owner, String name) {
            super(ASM5, mv);
            this.label = "[" + owner + "] " + name;
        }

        Label tryBeginLbl = new Label();
        Label tryEndLbl = new Label();
        Label finallyBeginLbl = new Label();

        private void insertTryPreamble() {
            tryBeginLbl = new Label();
            tryEndLbl = new Label();
            finallyBeginLbl = new Label();
            mv.visitTryCatchBlock(tryBeginLbl, tryEndLbl, finallyBeginLbl, null);
            mv.visitLabel(tryBeginLbl);
        }

        private void insertProfilerCall(String method) {
            super.visitLdcInsn(label);
            super.visitMethodInsn(INVOKESTATIC, "com/oracle/truffle/trufflenode/jniboundaryprofiler/ProfilingAgent",
                            method,
                            "(Ljava/lang/String;)V", false);
        }

        private void insertFinallyBlock() {
            mv.visitLabel(tryEndLbl);
            /* ProfilingAgent.jniCallEnd(...); */
            insertProfilerCall(ProfilerEnd);
            Label finallyEndLbl = new Label();
            mv.visitJumpInsn(GOTO, finallyEndLbl);
            /* } finally { */
            mv.visitLabel(finallyBeginLbl);
            mv.visitVarInsn(ASTORE, 2);
            /* ProfilingAgent.jniCallEnd(...); */
            insertProfilerCall(ProfilerEnd);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ATHROW);
            mv.visitLabel(finallyEndLbl);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            /* try { */
            insertTryPreamble();
            /* ProfilingAgent.jniCallBegin(...); */
            insertProfilerCall(ProfilerBegin);
        }

        @Override
        public void visitInsn(int opcode) {
            if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
                insertFinallyBlock();
                /* } return/throw/... */
                mv.visitInsn(opcode);
                /* try { */
                insertTryPreamble();
            }
            mv.visitInsn(opcode);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            insertFinallyBlock();
            /* } */
            super.visitMaxs(maxStack, maxLocals);
        }
    }

}
