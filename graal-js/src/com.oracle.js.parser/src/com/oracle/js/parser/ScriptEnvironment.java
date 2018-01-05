/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.js.parser;

import java.io.PrintWriter;

/**
 * Parser environment consists of command line options, and output and error writers, etc.
 */
public final class ScriptEnvironment {
    /** Error writer for this environment */
    private final PrintWriter err;

    /** Top level namespace. */
    private final Namespace namespace;

    /** Accept "const" keyword and treat it as variable. Interim feature */
    final boolean constAsVar;

    /** Display stack trace upon error, default is false */
    final boolean dumpOnError;

    /** Invalid lvalue expressions should be reported as early errors */
    final boolean earlyLvalueError;

    /** Empty statements should be preserved in the AST */
    final boolean emptyStatements;

    /** Enable ECMAScript 6 features. */
    final boolean es6;

    /** Enable ECMAScript 8 (2017) features. */
    final boolean es8;

    /**
     * Behavior when encountering a function declaration in a lexical context where only statements
     * are acceptable (function declarations are source elements, but not statements).
     */
    public enum FunctionStatementBehavior {
        /**
         * Accept the function declaration silently and treat it as if it were a function expression
         * assigned to a local variable.
         */
        ACCEPT,
        /**
         * Log a parser warning, but accept the function declaration and treat it as if it were a
         * function expression assigned to a local variable.
         */
        WARNING,
        /**
         * Raise a {@code SyntaxError}.
         */
        ERROR
    }

    /**
     * Behavior when encountering a function declaration in a lexical context where only statements
     * are acceptable (function declarations are source elements, but not statements).
     */
    final FunctionStatementBehavior functionStatement;

    /** Do not support non-standard syntax extensions. */
    final boolean syntaxExtensions;

    /** is this environment in scripting mode? */
    final boolean scripting;

    /** does the environment support shebang? */
    final boolean shebang;

    /** is this environment in strict mode? */
    final boolean strict;

    private ScriptEnvironment(boolean strict, boolean es6, boolean es8, boolean earlyLvalueError, boolean emptyStatements, boolean syntaxExtensions, boolean scripting, boolean shebang,
                    boolean constAsVar, FunctionStatementBehavior functionStatementBehavior, PrintWriter dumpOnError) {
        this.namespace = new Namespace();
        this.err = dumpOnError;

        this.constAsVar = constAsVar;
        this.dumpOnError = dumpOnError != null;
        this.earlyLvalueError = earlyLvalueError;
        this.emptyStatements = emptyStatements;
        this.functionStatement = functionStatementBehavior;
        this.syntaxExtensions = syntaxExtensions;
        this.strict = strict;
        this.scripting = scripting;
        this.shebang = shebang;
        this.es6 = es6;
        this.es8 = es8;
    }

    /**
     * Get the error stream for this environment
     *
     * @return error print writer
     */
    PrintWriter getErr() {
        return err;
    }

    /**
     * Get the namespace for this environment
     *
     * @return namespace
     */
    Namespace getNamespace() {
        return namespace;
    }

    public boolean isStrict() {
        return strict;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("hiding")
    public static final class Builder {
        private boolean constAsVar;
        private boolean earlyLvalueError = true;
        private boolean emptyStatements;
        private boolean es6 = true;
        private boolean es8 = true;
        private boolean syntaxExtensions = true;
        private boolean scripting;
        private boolean shebang;
        private boolean strict;
        private FunctionStatementBehavior functionStatementBehavior = FunctionStatementBehavior.ERROR;
        private PrintWriter dumpOnError;

        private Builder() {
        }

        public Builder constAsVar(boolean constAsVar) {
            this.constAsVar = constAsVar;
            return this;
        }

        public Builder earlyLvalueError(boolean earlyLvalueError) {
            this.earlyLvalueError = earlyLvalueError;
            return this;
        }

        public Builder emptyStatements(boolean emptyStatements) {
            this.emptyStatements = emptyStatements;
            return this;
        }

        public Builder es6(boolean es6) {
            this.es6 = es6;
            return this;
        }

        public Builder es8(boolean es8) {
            this.es8 = es8;
            return this;
        }

        public Builder syntaxExtensions(boolean syntaxExtensions) {
            this.syntaxExtensions = syntaxExtensions;
            return this;
        }

        public Builder scripting(boolean scripting) {
            this.scripting = scripting;
            return this;
        }

        public Builder shebang(boolean shebang) {
            this.shebang = shebang;
            return this;
        }

        public Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        public Builder functionStatementBehavior(FunctionStatementBehavior functionStatementBehavior) {
            this.functionStatementBehavior = functionStatementBehavior;
            return this;
        }

        public Builder dumpOnError(PrintWriter dumpOnError) {
            this.dumpOnError = dumpOnError;
            return this;
        }

        public ScriptEnvironment build() {
            return new ScriptEnvironment(strict, es6, es8, earlyLvalueError, emptyStatements, syntaxExtensions, scripting, shebang, constAsVar,
                            functionStatementBehavior, dumpOnError);
        }
    }
}
