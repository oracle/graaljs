/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

        public CustomClassWriter(ClassReader cr) {
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

        public InvCounterClassVisitor(final ClassVisitor cv, String className) {
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

        public InvCounterMethodVisitor(MethodVisitor mv, String owner, String name) {
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
