/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser;

interface ErrorMessages {

    String CHAR_CLASS_RANGE_OUT_OF_ORDER = "Range out of order in character class";
    String ENDS_WITH_UNFINISHED_ESCAPE_SEQUENCE = "Ends with an unfinished escape sequence";
    String ENDS_WITH_UNFINISHED_UNICODE_PROPERTY = "Ends with an unfinished Unicode property escape";
    String INCOMPLETE_QUANTIFIER = "Incomplete quantifier";
    String INVALID_CHARACTER_CLASS = "Invalid character class";
    String INVALID_CONTROL_CHAR_ESCAPE = "Invalid control char escape";
    String INVALID_ESCAPE = "Invalid escape";
    String INVALID_UNICODE_ESCAPE = "Invalid Unicode escape";
    String INVALID_UNICODE_PROPERTY = "Invalid Unicode property escape";
    String QUANTIFIER_ON_LOOKAROUND_ASSERTION = "Quantifier on lookaround assertion";
    String QUANTIFIER_ON_POSITION_ASSERTION = "Quantifier on position assertion";
    String QUANTIFIER_ON_QUANTIFIER = "Quantifier on quantifier";
    String QUANTIFIER_OUT_OF_ORDER = "Numbers out of order in {} quantifier";
    String QUANTIFIER_WITHOUT_TARGET = "Quantifier without target";
    String UNMATCHED_LEFT_BRACKET = "Unmatched '['";
    String UNMATCHED_RIGHT_BRACKET = "Unmatched ']'";
    String UNMATCHED_RIGHT_PARENTHESIS = "Unmatched ')'";
    String UNMATCHED_RIGHT_BRACE = "Unmatched '}'";
    String UNTERMINATED_GROUP = "Unterminated group";
}
