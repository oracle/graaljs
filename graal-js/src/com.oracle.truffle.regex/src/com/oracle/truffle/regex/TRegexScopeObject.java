/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;

/**
 * A TruffleObject that provides the TRegex globals: the no-match-result and the engine
 * configurator.
 */
class TRegexScopeObject implements TruffleObject {

    private RegexLanguage language;

    TRegexScopeObject(RegexLanguage language) {
        this.language = language;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return TRegexScopeObjectMessageResolutionForeign.ACCESS;
    }

    public static boolean isInstance(TruffleObject obj) {
        return obj instanceof TRegexScopeObject;
    }

    @MessageResolution(receiverType = TRegexScopeObject.class)
    static final class TRegexScopeObjectMessageResolution {

        @Resolve(message = "KEYS")
        abstract static class TRegexScopeObjectKeysNode extends Node {

            @TruffleBoundary
            @SuppressWarnings("unused")
            public Object access(TRegexScopeObject o) {
                return new TRegexScopeNamesObject();
            }
        }

        @Resolve(message = "KEY_INFO")
        abstract static class TRegexScopeObjectKeyInfoNode extends Node {

            @SuppressWarnings("unused")
            public Object access(TRegexScopeObject o, String name) {
                if (RegexLanguage.NO_MATCH_RESULT_IDENTIFIER.equals(name)) {
                    return 3;
                } else if (RegexLanguage.ENGINE_BUILDER_IDENTIFIER.equals(name)) {
                    return 3;
                } else {
                    return 0;
                }
            }
        }

        @Resolve(message = "READ")
        abstract static class TRegexScopeObjectReadNode extends Node {

            public Object access(TRegexScopeObject o, String name) {
                if (RegexLanguage.NO_MATCH_RESULT_IDENTIFIER.equals(name)) {
                    return RegexLanguage.EXPORT_NO_MATCH_RESULT;
                }
                if (RegexLanguage.ENGINE_BUILDER_IDENTIFIER.equals(name)) {
                    return o.language.engineBuilder;
                }
                CompilerDirectives.transferToInterpreter();
                throw UnknownIdentifierException.raise(name);
            }
        }

        static final class TRegexScopeNamesObject implements TruffleObject {

            @Override
            public ForeignAccess getForeignAccess() {
                return TRegexScopeNamesMessageResolutionForeign.ACCESS;
            }

            public static boolean isInstance(TruffleObject obj) {
                return obj instanceof TRegexScopeNamesObject;
            }

            @MessageResolution(receiverType = TRegexScopeNamesObject.class)
            static final class TRegexScopeNamesMessageResolution {

                @Resolve(message = "HAS_SIZE")
                abstract static class TRegexScopeNamesHasSizeNode extends Node {

                    @SuppressWarnings("unused")
                    public Object access(TRegexScopeNamesObject namesObject) {
                        return true;
                    }
                }

                @Resolve(message = "GET_SIZE")
                abstract static class TRegexScopeNamesGetSizeNode extends Node {

                    @SuppressWarnings("unused")
                    public Object access(TRegexScopeNamesObject namesObject) {
                        return 2;
                    }
                }

                @Resolve(message = "READ")
                abstract static class TRegexScopeNamesReadNode extends Node {

                    @SuppressWarnings("unused")
                    public Object access(TRegexScopeNamesObject namesObject, int index) {
                        if (index == 0) {
                            return RegexLanguage.NO_MATCH_RESULT_IDENTIFIER;
                        }
                        if (index == 1) {
                            return RegexLanguage.ENGINE_BUILDER_IDENTIFIER;
                        }
                        CompilerDirectives.transferToInterpreter();
                        throw UnknownIdentifierException.raise(Integer.toString(index));
                    }
                }

            }
        }
    }
}
