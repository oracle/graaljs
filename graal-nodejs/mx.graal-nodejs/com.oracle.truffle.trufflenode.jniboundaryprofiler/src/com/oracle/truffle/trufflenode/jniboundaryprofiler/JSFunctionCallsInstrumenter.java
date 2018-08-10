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

        JSFunctionProfilerClassVisitor(final ClassVisitor cv) {
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

        JSFunctionMethodVisitor(MethodVisitor mv) {
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
