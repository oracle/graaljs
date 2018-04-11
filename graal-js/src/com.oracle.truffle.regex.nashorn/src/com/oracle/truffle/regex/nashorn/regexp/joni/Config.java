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
/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.regex.nashorn.regexp.joni;

// @formatter:off

import java.io.PrintStream;

public interface Config {
    final int CHAR_TABLE_SIZE = 256;

    /* from jcodings */
    final boolean VANILLA = false;
    final int INTERNAL_ENC_CASE_FOLD_MULTI_CHAR = (1<<30);
    final int ENC_CASE_FOLD_MIN = INTERNAL_ENC_CASE_FOLD_MULTI_CHAR;
    final int ENC_CASE_FOLD_DEFAULT = ENC_CASE_FOLD_MIN;

    final boolean USE_MONOMANIAC_CHECK_CAPTURES_IN_ENDLESS_REPEAT = true; /* /(?:()|())*\2/ */
    final boolean USE_NEWLINE_AT_END_OF_STRING_HAS_EMPTY_LINE = true;     /* /\n$/ =~ "\n" */
    final boolean USE_WARNING_REDUNDANT_NESTED_REPEAT_OPERATOR = false;

    final boolean CASE_FOLD_IS_APPLIED_INSIDE_NEGATIVE_CCLASS = true;

    final boolean USE_MATCH_RANGE_MUST_BE_INSIDE_OF_SPECIFIED_RANGE = false;
    final boolean USE_VARIABLE_META_CHARS = true;
    final boolean USE_WORD_BEGIN_END = true;                                /* "\<": word-begin, "\>": word-end */
    final boolean USE_POSIX_API_REGION_OPTION = false;                           /* needed for POSIX API support */
    final boolean USE_FIND_LONGEST_SEARCH_ALL_OF_RANGE = true;


    final boolean USE_WARN = true;

    // internal config
    final boolean USE_PARSE_TREE_NODE_RECYCLE       = true;
    final boolean USE_OP_PUSH_OR_JUMP_EXACT         = true;
    final boolean USE_SHARED_CCLASS_TABLE           = false;
    final boolean USE_QTFR_PEEK_NEXT                = true;

    final int INIT_MATCH_STACK_SIZE                 = 64;
    final int DEFAULT_MATCH_STACK_LIMIT_SIZE        = 0;        /* unlimited */
    final int NUMBER_OF_POOLED_STACKS               = 4;



    final boolean DONT_OPTIMIZE                     = false;

    final boolean USE_STRING_TEMPLATES              = true; // use embeded string templates in Regex object as byte arrays instead of compiling them into int bytecode array

    final boolean NON_UNICODE_SDW                   = true;


    final PrintStream log = System.out;
    final PrintStream err = System.err;

    final boolean DEBUG_ALL                         = false;

    final boolean DEBUG                             = DEBUG_ALL;
    final boolean DEBUG_PARSE_TREE                  = DEBUG_ALL;
    final boolean DEBUG_PARSE_TREE_RAW              = true;
    final boolean DEBUG_COMPILE                     = DEBUG_ALL;
    final boolean DEBUG_COMPILE_BYTE_CODE_INFO      = DEBUG_ALL;
    final boolean DEBUG_SEARCH                      = DEBUG_ALL;
    final boolean DEBUG_MATCH                       = DEBUG_ALL;
}
