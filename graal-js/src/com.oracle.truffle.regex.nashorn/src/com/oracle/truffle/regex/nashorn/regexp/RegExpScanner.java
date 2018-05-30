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
package com.oracle.truffle.regex.nashorn.regexp;

// @formatter:off

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;

import com.oracle.truffle.regex.nashorn.parser.Scanner;
import com.oracle.truffle.regex.tregex.parser.CodePointSet;
import com.oracle.truffle.regex.tregex.parser.UnicodeCharacterProperties;

/**
 * Scan a JavaScript regexp, converting to Java regex if necessary.
 *
 */
public final class RegExpScanner extends Scanner {

    /**
     * String builder used to rewrite the pattern for the currently used regexp factory.
     */
    private final StringBuilder sb;

    /** Expected token table */
    private final Map<Character, Integer> expected = new HashMap<>();

    /** Capturing parenthesis that have been found so far. */
    private final List<Capture> caps = new LinkedList<>();

    /** Map from named capture group names to their (1-based) indices. */
    private final Map<String, Integer> namedCaptureGroups = new HashMap<>();

    /** Forward references to capture groups to be resolved later. */
    private final Deque<ForwardReference> forwardReferences = new LinkedList<>();

    /** Current level of zero-width negative lookaround assertions. */
    private int negLookaroundLevel;

    /** Sequential id of current top-level zero-width negative lookaround assertion. */
    private int negLookaroundGroup;

    /** Are we currently inside a character class? */
    private boolean inCharClass = false;

    /** Are we currently inside a negated character class? */
    private boolean inNegativeClass = false;

    /** Was the last assertion a lookahead? Required for V8 compatibility. */
    private boolean lastAssertionWasLookahead = false;

    private static final String NON_IDENT_ESCAPES = "$^*+(){}[]|\\.?-";

    private static final CodePointSet ID_START = UnicodeCharacterProperties.getProperty("ID_Start");
    private static final CodePointSet ID_CONTINUE = UnicodeCharacterProperties.getProperty("ID_Continue");

    private static class Capture {
        /** Zero-width negative lookarounds enclosing the capture. */
        private final int negLookaroundLevel;
        /** Sequential id of top-level negative lookarounds containing the capture. */
        private  final int negLookaroundGroup;

        Capture(final int negLookaroundGroup, final int negLookaroundLevel) {
            this.negLookaroundGroup = negLookaroundGroup;
            this.negLookaroundLevel = negLookaroundLevel;
        }

        boolean isContained(final int group, final int level) {
            return negLookaroundLevel == 0 || (group == this.negLookaroundGroup && level >= this.negLookaroundLevel);
        }
    }

    private static class ForwardReference {
        /** The position in the output regex where the resolved reference should be put. */
        public final int outPos;

        protected ForwardReference(int outPos) {
            this.outPos = outPos;
        }
    }

    private static class IndexedForwardReference extends ForwardReference {
        /** The index of the capture group being referenced. */
        public final int index;

        IndexedForwardReference(int outPos, int index) {
            super(outPos);
            this.index = index;
        }
    }

    private static class NamedForwardReference extends ForwardReference {
        /** The name of the named capture group being referenced. */
        public final String name;
        /** The position in the input regex where the forward reference was found. */
        public final int inPos;

        NamedForwardReference(int outPos, int inPos, String name) {
            super(outPos);
            this.inPos = inPos;
            this.name = name;
        }
    }

    /**
     * Constructor
     * @param string the JavaScript regexp to parse
     */
    private RegExpScanner(final String string) {
        super(string);
        sb = new StringBuilder(limit);
        reset(0);
        expected.put(']', 0);
        expected.put('}', 0);
    }

    private void processForwardReferences() {
        while (!forwardReferences.isEmpty()) {
            final ForwardReference fwdRef = forwardReferences.pop();
            final int pos = fwdRef.outPos;
            if (fwdRef instanceof IndexedForwardReference) {
                final int num = ((IndexedForwardReference)fwdRef).index;
                if (num > caps.size()) {
                    // Non-existing backreference. If the number begins with a valid octal convert
                    // it to Unicode escape and append the rest to a literal character sequence.
                    final StringBuilder buffer = new StringBuilder();
                    octalOrLiteral(Integer.toString(num), buffer);
                    sb.insert(pos, buffer);
                }
            } else if (fwdRef instanceof NamedForwardReference) {
                final NamedForwardReference namedFwdRef = (NamedForwardReference)fwdRef;
                final String name = namedFwdRef.name;
                if (namedCaptureGroups.containsKey(name)) {
                    sb.insert(pos, "\\" + namedCaptureGroups.get(name));
                } else if (namedCaptureGroups.isEmpty()) {
                    // Reinterpret the string "\\k<groupName>" as a regular expression without
                    // recognizing named capture group references. We do so by removing the
                    // backslash preceding 'k'. The contents of 'groupName' are safe, since the only
                    // syntax character they can contain is '$', which has the same meaning in
                    // ECMAScript and Java regular expressions.
                    sb.insert(pos, String.format("k<%s>", name));
                } else {
                    // The regular expression contains named capture groups. According to the
                    // ECMAScript 2018 specification, this means it should be parsed with the [+N]
                    // option, which prohibits the \k identity escape. This means that we cannot
                    // reinterpret this reference as some other piece of RegExp syntax.
                    throw new PatternSyntaxException("unresolved named backreference", getContents(), namedFwdRef.inPos);
                }
            }
        }
    }

    /**
     * Scan a JavaScript regexp string returning a Java safe regex string.
     *
     * @param string
     *            JavaScript regexp string.
     * @return Java safe regex string.
     */
    public static RegExpScanner scan(final String string) {
        final RegExpScanner scanner = new RegExpScanner(string);

        try {
            scanner.disjunction();
        } catch (final Exception e) {
            throw new PatternSyntaxException(e.getMessage(), string, scanner.position);
        }

        scanner.processForwardReferences();

        // Throw syntax error unless we parsed the entire JavaScript regexp without syntax errors
        if (scanner.position != string.length()) {
            throw new PatternSyntaxException("cannot parse regular expression", string, scanner.position);
        }

        return scanner;
    }

    final StringBuilder getStringBuilder() {
        return sb;
    }

    public String getJavaPattern() {
        return sb.toString();
    }

    /**
     * Commit n characters to the builder and to a given token
     * @param n     Number of characters.
     * @return Committed token
     */
    private boolean commit(final int n) {
        switch (n) {
        case 1:
            sb.append(ch0);
            skip(1);
            break;
        case 2:
            sb.append(ch0);
            sb.append(ch1);
            skip(2);
            break;
        case 3:
            sb.append(ch0);
            sb.append(ch1);
            sb.append(ch2);
            skip(3);
            break;
        default:
            assert false : "Should not reach here";
        }

        return true;
    }

    /**
     * Restart the buffers back at an earlier position.
     *
     * @param startIn
     *            Position in the input stream.
     * @param startOut
     *            Position in the output stream.
     */
    private void restart(final int startIn, final int startOut) {
        reset(startIn);
        sb.setLength(startOut);
    }

    private void push(final char ch) {
        expected.put(ch, expected.get(ch) + 1);
    }

    private void pop(final char ch) {
        expected.put(ch, Math.min(0, expected.get(ch) - 1));
    }

    /*
     * Recursive descent tokenizer starts below.
     */

    /*
     * Disjunction ::
     *      Alternative
     *      Alternative | Disjunction
     */
    private void disjunction() {
        while (true) {
            alternative();

            if (ch0 == '|') {
                commit(1);
            } else {
                break;
            }
        }
    }

    /*
     * Alternative ::
     *      [empty]
     *      Alternative Term
     */
    private void alternative() {
        while (term()) {
            // do nothing
        }
    }

    /*
     * Term ::
     *      Assertion
     *      Atom
     *      Atom Quantifier
     */
    private boolean term() {
        final int startIn  = position;
        final int startOut = sb.length();

        if (assertion()) {
            // For compatability with JSC and ES3, V8 allows quantifiers after lookaheads
            // warning: this is out of the ES 262 spec
            if (lastAssertionWasLookahead) {
                quantifier();
            }
            return true;
        }

        if (atom()) {
            quantifier();
            return true;
        }

        restart(startIn, startOut);
        return false;
    }

    /*
     * Assertion ::
     *      ^
     *      $
     *      \b
     *      \B
     *      ( ? = Disjunction )
     *      ( ? ! Disjunction )
     *      ( ? <= Disjunction )
     *      ( ? <! Disjunction )
     */
    private boolean assertion() {
        final int startIn  = position;
        final int startOut = sb.length();
        lastAssertionWasLookahead = false;

        switch (ch0) {
        case '^':
        case '$':
            return commit(1);

        case '\\':
            if (ch1 == 'b' || ch1 == 'B') {
                return commit(2);
            }
            break;

        case '(':
            if (ch1 != '?') {
                break;
            }
            commit(2); // commit "(?"
            final boolean lookbehind = ch0 == '<';
            if (lookbehind) {
                commit(1); // commit "<"
            }
            if (ch0 != '=' && ch0 != '!') {
                break;
            }
            final boolean isNegativeLookaround = (ch0 == '!');
            commit(1); // commit "=" or "!"

            if (isNegativeLookaround) {
                if (negLookaroundLevel == 0) {
                    negLookaroundGroup++;
                }
                negLookaroundLevel++;
            }
            disjunction();
            if (isNegativeLookaround) {
                negLookaroundLevel--;
            }

            if (ch0 == ')') {
                lastAssertionWasLookahead = !lookbehind;
                return commit(1);
            }
            break;
        default:
            break;
        }

        restart(startIn, startOut);
        return false;
    }

    /*
     * Quantifier ::
     *      QuantifierPrefix
     *      QuantifierPrefix ?
     */
    private boolean quantifier() {
        if (quantifierPrefix()) {
            if (ch0 == '?') {
                commit(1);
            }
            return true;
        }
        return false;
    }

    /*
     * QuantifierPrefix ::
     *      *
     *      +
     *      ?
     *      { DecimalDigits }
     *      { DecimalDigits , }
     *      { DecimalDigits , DecimalDigits }
     */
    private boolean quantifierPrefix() {
        final int startIn  = position;
        final int startOut = sb.length();

        switch (ch0) {
        case '*':
        case '+':
        case '?':
            return commit(1);

        case '{':
            commit(1);

            if (!decimalDigits()) {
                break; // not a quantifier - back out
            }
            push('}');

            if (ch0 == ',') {
                commit(1);
                decimalDigits();
            }

            if (ch0 == '}') {
                pop('}');
                commit(1);
            } else {
                // Bad quantifier should be rejected but is accepted by all major engines
                pop('}');
                restart(startIn, startOut);
                return false;
            }

            return true;

        default:
            break;
        }

        restart(startIn, startOut);
        return false;
    }

    /*
     * Atom ::
     *      PatternCharacter
     *      .
     *      \ AtomEscape
     *      CharacterClass
     *      ( GroupSpecifier Disjunction )
     *      ( ? : Disjunction )
     */
    private boolean atom() {
        final int startIn  = position;
        final int startOut = sb.length();

        if (patternCharacter()) {
            return true;
        }

        if (ch0 == '.') {
            return commit(1);
        }

        if (ch0 == '\\') {
            commit(1);

            if (atomEscape()) {
                return true;
            }
        }

        if (characterClass()) {
            return true;
        }

        if (ch0 == '(') {
            commit(1);
            if (ch0 == '?' && ch1 == ':') {
                commit(2);
            } else {
                caps.add(new Capture(negLookaroundGroup, negLookaroundLevel));
                groupSpecifier();
            }

            disjunction();

            if (ch0 == ')') {
                commit(1);
                return true;
            }
        }

        restart(startIn, startOut);
        return false;
    }

    /*
     * PatternCharacter ::
     *      SourceCharacter but not any of: ^$\.*+?()[]{}|
     */
    @SuppressWarnings("fallthrough")
    private boolean patternCharacter() {
        if (atEOF()) {
            return false;
        }

        switch (ch0) {
        case '^':
        case '$':
        case '\\':
        case '.':
        case '*':
        case '+':
        case '?':
        case '(':
        case ')':
        case '[':
        case '|':
            return false;

        case '}':
        case ']':
            final int n = expected.get(ch0);
            if (n != 0) {
                return false;
            }

       case '{':
           // if not a valid quantifier escape curly brace to match itself
           // this ensures compatibility with other JS implementations
           if (!quantifierPrefix()) {
               sb.append('\\');
               return commit(1);
           }
           return false;

        default:
            return commit(1); // SOURCECHARACTER
        }
    }

    private void handleBackReference(int groupIndex) {
        //  Captures inside a negative lookaround are undefined when referenced from the outside.
        if (!caps.get(groupIndex - 1).isContained(negLookaroundGroup, negLookaroundLevel)) {
            // Reference to capture in negative lookaround, omit from output buffer.
            sb.setLength(sb.length() - 1); // delete the backslash from the output
        } else {
            // Append backreference to output buffer.
            sb.append(groupIndex);
        }
    }

    /*
     * AtomEscape ::
     *      DecimalEscape
     *      CharacterClassEscape
     *      CharacterEscape
     *      k GroupName
     */
    private boolean atomEscape() {
        final int startIn  = position;
        final int startOut = sb.length();

        if (ch0 == 'k') {
            skip(1);
            StringBuilder nameSB = new StringBuilder();
            if (groupName(nameSB)) {
                String name = nameSB.toString();
                if (namedCaptureGroups.containsKey(name)) {
                    int groupIndex = namedCaptureGroups.get(name);
                    handleBackReference(groupIndex);
                } else {
                    sb.setLength(sb.length() - 1); // delete the backslash
                    forwardReferences.push(new NamedForwardReference(sb.length(), startIn - 1, name));
                }
                return true;
            } else {
                restart(startIn, startOut);
            }
        }

        return decimalEscape() || characterClassEscape() || characterEscape();
    }

    /*
     * CharacterEscape ::
     *      ControlEscape
     *      c ControlLetter
     *      0 [lookahead not in DecimalDigit]
     *      HexEscapeSequence
     *      UnicodeEscapeSequence
     *      LegacyOctalEscapeSequence
     *      IdentityEscape
     */
    private boolean characterEscape() {
        final int startIn  = position;
        final int startOut = sb.length();

        if (controlEscape()) {
            return true;
        }

        if (ch0 == 'c') {
            commit(1);
            if (controlLetter()) {
                return true;
            }
            restart(startIn, startOut);
        }

        if (ch0 == '0' && !isDecimalDigit(ch1)) {
            skip(1);
            unicode(0, sb);
            return true;
        }

        if (hexEscapeSequence() || unicodeEscapeSequence() || legacyOctalEscapeSequence() || identityEscape()) {
            return true;
        }

        restart(startIn, startOut);
        return false;
    }

    private boolean scanEscapeSequence(final char leader, final int length) {
        final int startIn  = position;
        final int startOut = sb.length();

        if (ch0 != leader) {
            return false;
        }

        commit(1);
        for (int i = 0; i < length; i++) {
            final char ch0l = Character.toLowerCase(ch0);
            if ((ch0l >= 'a' && ch0l <= 'f') || isDecimalDigit(ch0)) {
                commit(1);
            } else {
                restart(startIn, startOut);
                return false;
            }
        }

        return true;
    }

    private boolean hexEscapeSequence() {
        return scanEscapeSequence('x', 2);
    }

    private boolean unicodeEscapeSequence() {
        return scanEscapeSequence('u', 4);
    }

    /*
     * LegacyOctalEscapeSequence ::
     *      OctalDigit [lookahead not in OctalDigit]
     *      ZeroToThree OctalDigit [lookahead not in OctalDigit]
     *      FourToSeven OctalDigit
     *      ZeroToThree OctalDigit OctalDigit
     *
     * ZeroToThree :: one of
     *      0 1 2 3
     *
     * FourToSeven :: one of
     *      4 5 6 7
     */
    private boolean legacyOctalEscapeSequence() {
        final int startIn  = position;
        final int startOut = sb.length();

        int octalValue = 0;
        // Maximum value for octal escape is 377 (255) so we stop the loop at 32
        // (because 32 * 8 = 256)
        while (isOctalDigit(ch0) && octalValue < 32 && position < startIn + 3) {
            octalValue = octalValue * 8 + ch0 - '0';
            skip(1);
        }

        if (position > startIn) {
            unicode(octalValue, sb);
            return true;
        } else {
            restart(startIn, startOut);
            return false;
        }
    }

    /*
     * ControlEscape ::
     *      one of fnrtv
     */
    private boolean controlEscape() {
        switch (ch0) {
        case 'f':
        case 'n':
        case 'r':
        case 't':
        case 'v':
            return commit(1);

        default:
            return false;
        }
    }

    /*
     * ControlLetter ::
     *      one of abcdefghijklmnopqrstuvwxyz
     *      ABCDEFGHIJKLMNOPQRSTUVWXYZ
     */
    private boolean controlLetter() {
        // To match other engines we also accept '0'..'9' and '_' as control letters inside a character class.
        if ((ch0 >= 'A' && ch0 <= 'Z') || (ch0 >= 'a' && ch0 <= 'z')
                || (inCharClass && (isDecimalDigit(ch0) || ch0 == '_'))) {
            // for some reason java regexps don't like control characters on the
            // form "\\ca".match([string with ascii 1 at char0]). Translating
            // them to unicode does it though.
            sb.setLength(sb.length() - 1);
            unicode(ch0 % 32, sb);
            skip(1);
            return true;
        }
        return false;
    }

    /*
     * GroupSpecifier ::
     *      [empty]
     *      ? GroupName
     *
     * Note that this translation replaces named capture groups to unnamed capture groups (Java
     * regular expressions only allow alphanumeric ASCII characters, whereas JavaScript regular
     * expressions use Unicode identifers).
     */
    private void groupSpecifier() {
        final int startIn  = position;
        final int startOut = sb.length();

        if (ch0 == '?') {
            skip(1);
            StringBuilder name = new StringBuilder();
            if (groupName(name)) {
                namedCaptureGroups.put(name.toString(), caps.size());
            } else {
                restart(startIn, startOut);
            }
        }
    }

    /*
     * GroupName ::
     *      < RegExpIdentifierName >
     *
     * Instead of appending the group name to the resulting regular expression, we write it to the
     * provided StringBuilder instance.
     */
    private boolean groupName(StringBuilder nameBuilder) {
        final int startIn  = position;
        final int startOut = sb.length();

        if (ch0 == '<') {
            skip(1);
            if (regExpIdentifierName(nameBuilder)) {
                if (ch0 == '>') {
                    skip(1);
                    return true;
                }
            }
        }

        restart(startIn, startOut);
        return false;
    }

    /*
     * RegExpIdentifierName ::
     *      RegExpIdentifierStart
     *      RegExpIdentifierName RegExpIdentifierPart
     *
     * The name is appended to the provided StringBuilder instead of the resulting regex.
     */
    private boolean regExpIdentifierName(StringBuilder nameBuilder) {
        final int startIn  = position;
        final int startOut = sb.length();

        if (regExpIdentifierStart(nameBuilder)) {
            while (regExpIdentifierPart(nameBuilder)) {
                // do nothing
            }

            return true;
        }

        restart(startIn, startOut);
        return false;
    }

    /**
     * Helper method for {@link #regExpIdentifierStart} and {@link #regExpIdentifierPart}. Parses
     * an element of a RegExpIdentifierName, storing it in the supplied {@link StringBuilder}. Any
     * candidate character must pass the {@code allowedChars} {@link Predicate}.
     */
    private boolean regExpIdentifierElement(StringBuilder nameBuilder, Predicate<Character> allowedChars) {
        final int startIn  = position;
        final int startOut = sb.length();

        char codeUnit = '\0';
        if (ch0 == '\\') {
            skip(1);
            if (unicodeEscapeSequence()) {
                String hexSequence = sb.substring(sb.length() - 4);
                sb.setLength(sb.length() - 6); // delete "\\uXXXX"
                codeUnit = (char) Integer.parseInt(hexSequence, 16);
            }
        } else {
            codeUnit = ch0;
            skip(1);
        }

        if (allowedChars.test(codeUnit)) {
            nameBuilder.append(codeUnit);
            return true;
        } else {
            restart(startIn, startOut);
            return false;
        }
    }

    /*
     * RegExpIdentifierStart ::
     *      UnicodeIDStart
     *      $
     *      _
     *      \ RegExpUnicodeEscapeSequence
     *
     * The character is appended to the provided StringBuilder instead of the resulting regex.
     */
    private boolean regExpIdentifierStart(StringBuilder nameBuilder) {
        return regExpIdentifierElement(nameBuilder, c -> ID_START.contains(c) || c == '$' || c == '_');
    }

    /*
     * RegExpIdentifierPart ::
     *      UnicodeIDContinue
     *      $
     *      \ RegExpUnicodeEscapeSequence
     *      <ZWNJ>
     *      <ZWJ>
     *
     * The character is appended to the provided StringBuilder instead of the resulting regex.
     */
    private boolean regExpIdentifierPart(StringBuilder nameBuilder) {
        return regExpIdentifierElement(nameBuilder, c -> ID_CONTINUE.contains(c) || c == '$' || c == '\u200c' || c == '\u200d');
    }

    /*
     * IdentityEscape ::
     *      SourceCharacter but not IdentifierPart
     *      <ZWJ>  (200c)
     *      <ZWNJ> (200d)
     */
    private boolean identityEscape() {
        if (atEOF()) {
            throw new RuntimeException("\\ at end of pattern"); // will be converted to PatternSyntaxException
        }
        // ES 5.1 A.7 requires "not IdentifierPart" here but all major engines accept any character here.
        if (ch0 == 'c') {
            sb.append('\\'); // Treat invalid \c control sequence as \\c
        } else if (NON_IDENT_ESCAPES.indexOf(ch0) == -1) {
            sb.setLength(sb.length() - 1);
        }
        return commit(1);
    }

    /*
     * DecimalEscape ::
     *      NonZeroDigit DecimalDigits_opt [lookahead not in DecimalDigit]
     */
    private boolean decimalEscape() {
        final int startIn  = position;
        final int startOut = sb.length();

        if (isDecimalDigit(ch0) && ch0 != '0') {
            // This should be a backreference, but could also be an octal escape or even a literal string.
            int decimalValue = 0;
            while (isDecimalDigit(ch0)) {
                decimalValue = decimalValue * 10 + ch0 - '0';
                skip(1);
            }

            if (inCharClass) {
                // No backreferences in character classes. Encode as unicode escape or literal char sequence
                sb.setLength(sb.length() - 1);
                octalOrLiteral(Integer.toString(decimalValue), sb);

            } else if (decimalValue <= caps.size()) {
                handleBackReference(decimalValue);
            } else {
                // Forward references to a capture group are always undefined so we can omit it from the output buffer.
                // However, if the target capture does not exist, we need to rewrite the reference as hex escape
                // or literal string, so register the reference for later processing.
                sb.setLength(sb.length() - 1);
                forwardReferences.push(new IndexedForwardReference(sb.length(), decimalValue));
            }
            return true;
        }

        restart(startIn, startOut);
        return false;
    }

    /*
     * CharacterClassEscape ::
     *  one of dDsSwW
     */
    private boolean characterClassEscape() {
        switch (ch0) {
        case 's':
        case 'S':
        case 'd':
        case 'D':
        case 'w':
        case 'W':
            return commit(1);

        default:
            return false;
        }
    }

    /*
     * CharacterClass ::
     *      [ [lookahead {^}] ClassRanges ]
     *      [ ^ ClassRanges ]
     */
    private boolean characterClass() {
        final int startIn  = position;
        final int startOut = sb.length();

        if (ch0 == '[') {
            try {
                inCharClass = true;
                push(']');
                commit(1);

                if (ch0 == '^') {
                    inNegativeClass = true;
                    commit(1);
                }

                if (classRanges() && ch0 == ']') {
                    pop(']');
                    commit(1);

                    // Substitute empty character classes [] and [^] that never or always match
                    if (position == startIn + 2) {
                        sb.setLength(sb.length() - 1);
                        sb.append("^\\s\\S]");
                    } else if (position == startIn + 3 && inNegativeClass) {
                        sb.setLength(sb.length() - 2);
                        sb.append("\\s\\S]");
                    }

                    return true;
                }
            } finally {
                inCharClass = false;  // no nested character classes in JavaScript
                inNegativeClass = false;
            }
        }

        restart(startIn, startOut);
        return false;
    }

    /*
     * ClassRanges ::
     *      [empty]
     *      NonemptyClassRanges
     */
    private boolean classRanges() {
        nonemptyClassRanges();
        return true;
    }

    /*
     * NonemptyClassRanges ::
     *      ClassAtom
     *      ClassAtom NonemptyClassRangesNoDash
     *      ClassAtom - ClassAtom ClassRanges
     */
    private boolean nonemptyClassRanges() {
        final int startIn  = position;
        final int startOut = sb.length();

        if (classAtom()) {

            if (ch0 == '-') {
                commit(1);

                if (classAtom() && classRanges()) {
                    return true;
                }
            }

            nonemptyClassRangesNoDash();

            return true;
        }

        restart(startIn, startOut);
        return false;
    }

    /*
     * NonemptyClassRangesNoDash ::
     *      ClassAtom
     *      ClassAtomNoDash NonemptyClassRangesNoDash
     *      ClassAtomNoDash - ClassAtom ClassRanges
     */
    private boolean nonemptyClassRangesNoDash() {
        final int startIn  = position;
        final int startOut = sb.length();

        if (classAtomNoDash()) {

            // need to check dash first, as for e.g. [a-b|c-d] will otherwise parse - as an atom
            if (ch0 == '-') {
               commit(1);

               if (classAtom() && classRanges()) {
                   return true;
               }
               //fallthru
           }

            nonemptyClassRangesNoDash();
            return true; // still a class atom
        }

        if (classAtom()) {
            return true;
        }

        restart(startIn, startOut);
        return false;
    }

    /*
     * ClassAtom : - ClassAtomNoDash
     */
    private boolean classAtom() {

        if (ch0 == '-') {
            return commit(1);
        }

        return classAtomNoDash();
    }

    /*
     * ClassAtomNoDash ::
     *      SourceCharacter but not one of \ or ] or -
     *      \ ClassEscape
     */
    private boolean classAtomNoDash() {
        if (atEOF()) {
            return false;
        }
        final int startIn  = position;
        final int startOut = sb.length();

        switch (ch0) {
        case ']':
        case '-':
            return false;

        case '[':
            // unescaped left square bracket - add escape
            sb.append('\\');
            return commit(1);

        case '\\':
            commit(1);
            if (classEscape()) {
                return true;
            }

            restart(startIn, startOut);
            return false;

        default:
            return commit(1);
        }
    }

    /*
     * ClassEscape ::
     *      b
     *      CharacterClassEscape
     *      CharacterEscape
     */
    private boolean classEscape() {
        if (ch0 == 'b') {
            sb.setLength(sb.length() - 1);
            sb.append('\b');
            skip(1);
            return true;
        }

        return characterClassEscape() || characterEscape();
    }

    /*
     * DecimalDigits
     */
    private boolean decimalDigits() {
        if (!isDecimalDigit(ch0)) {
            return false;
        }

        while (isDecimalDigit(ch0)) {
            commit(1);
        }

        return true;
    }

    private static void unicode(final int value, final StringBuilder buffer) {
        final String hex = Integer.toHexString(value);
        buffer.append('u');
        for (int i = 0; i < 4 - hex.length(); i++) {
            buffer.append('0');
        }
        buffer.append(hex);
    }

    // Convert what would have been a backreference into a unicode escape, or a number literal, or both.
    private static void octalOrLiteral(final String numberLiteral, final StringBuilder buffer) {
        final int length = numberLiteral.length();
        int octalValue = 0;
        int pos = 0;
        // Maximum value for octal escape is 0377 (255) so we stop the loop at 32
        while (pos < length && octalValue < 0x20) {
            final char ch = numberLiteral.charAt(pos);
            if (isOctalDigit(ch)) {
                octalValue = octalValue * 8 + ch - '0';
            } else {
                break;
            }
            pos++;
        }
        if (octalValue > 0) {
            buffer.append('\\');
            unicode(octalValue, buffer);
            buffer.append(numberLiteral.substring(pos));
        } else {
            buffer.append(numberLiteral);
        }
    }

    private static boolean isOctalDigit(final char ch) {
        return ch >= '0' && ch <= '7';
    }

    private static boolean isDecimalDigit(final char ch) {
        return ch >= '0' && ch <= '9';
    }
}
