/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.runtime.JSContext;

public class GlobalConstantNode extends JSTargetableNode implements ReadNode {

    @Child private GlobalObjectNode globalObjectNode;
    @Child private JSConstantNode constantNode;
    private final String propertyName;

    protected GlobalConstantNode(JSContext context, String propertyName, JSConstantNode constantNode) {
        this.globalObjectNode = GlobalObjectNode.create(context);
        this.constantNode = constantNode;
        this.propertyName = propertyName;
    }

    public static JSTargetableNode createGlobalConstant(JSContext ctx, String propertyName, Object value) {
        return new GlobalConstantNode(ctx, propertyName, JSConstantNode.create(value));
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ReadPropertyExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("key", propertyName);
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        return execute(frame);
    }

    @Override
    public Object evaluateTarget(VirtualFrame frame) {
        return globalObjectNode.executeDynamicObject();
    }

    @Override
    public JavaScriptNode getTarget() {
        return globalObjectNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return constantNode.execute(frame);
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        return constantNode.executeInt(frame);
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return constantNode.executeDouble(frame);
    }

    public Object getValue() {
        return constantNode.getValue();
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return String.format("%s(property=%s, value=%s)", super.toString(), propertyName, constantNode.getValue());
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new GlobalConstantNode(globalObjectNode.getContext(), propertyName, cloneUninitialized(constantNode));
    }

    static final class LineNumberNode extends JSConstantNode {
        LineNumberNode() {
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return getLineNumber();
        }

        @Override
        public int executeInt(VirtualFrame frame) {
            return getLineNumber();
        }

        @Override
        public double executeDouble(VirtualFrame frame) {
            return getLineNumber();
        }

        private int getLineNumber() {
            return getEncapsulatingSourceSection().getStartLine();
        }

        @Override
        public Object getValue() {
            return getLineNumber();
        }
    }

    static final class FileNameNode extends JSConstantNode {
        FileNameNode() {
        }

        @Override
        public String execute(VirtualFrame frame) {
            return getFileName();
        }

        @Override
        public String executeString(VirtualFrame frame) {
            return getFileName();
        }

        private String getFileName() {
            return getEncapsulatingSourceSection().getSource().getName();
        }

        @Override
        public Object getValue() {
            return getFileName();
        }
    }

    static final class DirNameNode extends JSConstantNode {
        DirNameNode() {
        }

        @Override
        public String execute(VirtualFrame frame) {
            return getDirName();
        }

        @Override
        public String executeString(VirtualFrame frame) {
            return getDirName();
        }

        @TruffleBoundary
        private String getDirName() {
            Source source = getEncapsulatingSourceSection().getSource();
            if (source.isInternal() || source.isInteractive()) {
                return "";
            }
            String path = source.getName();
            if (File.separatorChar == '\\' && path.startsWith("/")) {
                // on Windows, remove first "/" from /c:/test/dir/ style paths
                path = path.substring(1);
            }
            Path filePath = Paths.get(path).toAbsolutePath();
            String dirPath = filePath.getParent().toString();
            if (!dirPath.isEmpty() && !(dirPath.charAt(dirPath.length() - 1) == '/' || dirPath.charAt(dirPath.length() - 1) == File.separatorChar)) {
                dirPath += File.separatorChar;
            }
            return dirPath;
        }

        @Override
        public Object getValue() {
            return getDirName();
        }
    }
}
