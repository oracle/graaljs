/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.jniboundaryprofiler;

import com.oracle.truffle.trufflenode.NativeAccess;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

public class JSFunctionCallsInstrumenter {

    public static byte[] maybeInstrumentClass(String className, byte[] classfileBuffer) {
        if (instrumentJSFunctionCalls(className)) {
            System.out.println("Instrumenting JSFunction calls in: " + className);
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new JSFunctionProfilerClassVisitor(cw);
            cr.accept(cv, 0);
            return cw.toByteArray();
        }
        return classfileBuffer;
    }

    private static boolean instrumentJSFunctionCalls(String klass) {
        return "com/oracle/truffle/trufflenode/node/ExecuteNativeFunctionNode".equals(klass);
    }

    private static class JSFunctionProfilerClassVisitor extends ClassVisitor implements Opcodes {

        public JSFunctionProfilerClassVisitor(final ClassVisitor cv) {
            super(ASM5, cv);
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name,
                        final String desc, final String signature, final String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if (mv != null && !"<init>".equals(name) && !"<clinit>".equals(name)) {
                return new JSFunctionMethodVisitor(mv);
            }
            return mv;
        }
    }

    private static class JSFunctionMethodVisitor extends MethodVisitor implements Opcodes {

        public JSFunctionMethodVisitor(MethodVisitor mv) {
            super(ASM5, mv);
        }

        private static boolean isNativeMethodCall(String mname, String klass) {
            if (NativeAccess.class.getName().replace('.', '/').equals(klass)) {
                switch (mname) {
                    case "executeFunction0":
                    case "executeFunction1":
                    case "executeFunction2":
                    case "executeFunction3":
                    case "executeFunction4":
                    case "executeFunction5":
                    case "executeFunction6":
                    case "executeFunction":
                        return true;
                }
            }
            return false;
        }

        private static void instrumentNativeMethodRecordTimeBegin(MethodVisitor mv, Label tryBeginLbl, Label trEndLbl, Label finallyLbl, String mname, String klass) {
            assert isNativeMethodCall(mname, klass);
            /* try { ... */
            mv.visitTryCatchBlock(tryBeginLbl, trEndLbl, finallyLbl, null);
            mv.visitLabel(tryBeginLbl);
            /* ProfilingAgent.bindingCallBegin(...) */
            mv.visitLdcInsn(mname);
            switch (mname) {
                case "executeFunction0":
                case "executeFunction1":
                case "executeFunction2":
                case "executeFunction3":
                case "executeFunction4":
                case "executeFunction5":
                case "executeFunction6":
                    mv.visitVarInsn(Opcodes.ALOAD, 3); // The 3rd argument is the JSFunction object
                    break;
                case "executeFunction":
                    mv.visitVarInsn(Opcodes.ALOAD, 1); // The 1st argument is Object[] arguments
                    break;
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                            "com/oracle/truffle/trufflenode/jniboundaryprofiler/ProfilingAgent", "bindingCallBegin",
                            "(Ljava/lang/String;Ljava/lang/Object;)V", false);
        }

        private static void instrumentNativeMethodRecordTimeEnd(MethodVisitor mv, Label tryEndLbl, Label finallyLbl, String mname, String klass) {
            assert isNativeMethodCall(mname, klass);
            mv.visitLabel(tryEndLbl);
            /* ProfilingAgent.bindingCallEnd() */
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                            "com/oracle/truffle/trufflenode/jniboundaryprofiler/ProfilingAgent", "bindingCallEnd",
                            "()V", false);
            Label finallyEndLbl = new Label();
            mv.visitJumpInsn(Opcodes.GOTO, finallyEndLbl);
            /* } finally { */
            mv.visitLabel(finallyLbl);
            mv.visitVarInsn(Opcodes.ASTORE, 2);
            /* ProfilingAgent.bindingCallEnd() */
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                            "com/oracle/truffle/trufflenode/jniboundaryprofiler/ProfilingAgent", "bindingCallEnd",
                            "()V", false);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitInsn(Opcodes.ATHROW);
            /* } */
            mv.visitLabel(finallyEndLbl);
        }

        @Override
        public void visitMethodInsn(int opcode, String mowner, String mname, String desc, boolean itf) {
            Label tryBeginLbl = new Label();
            Label tryEndLbl = new Label();
            Label finallyLbl = new Label();
            if (isNativeMethodCall(mname, mowner)) {
                /* try { ProfilingAgent.bindingCallBegin(...); */
                instrumentNativeMethodRecordTimeBegin(mv, tryBeginLbl, tryEndLbl, finallyLbl, mname, mowner);
            }
            /* do call */
            mv.visitMethodInsn(opcode, mowner, mname, desc, itf);
            if (isNativeMethodCall(mname, mowner)) {
                /* } finally { ProfilingAgent.bindingCallEnd(); } */
                instrumentNativeMethodRecordTimeEnd(mv, tryEndLbl, finallyLbl, mname, mowner);
            }
        }

    }
}
