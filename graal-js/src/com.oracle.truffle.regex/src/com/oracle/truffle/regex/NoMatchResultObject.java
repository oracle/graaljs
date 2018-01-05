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
 * A TruffleObject that provides the no-match-result.
 */
class NoMatchResultObject implements TruffleObject {

    @Override
    public ForeignAccess getForeignAccess() {
        return NoMatchResultObjectMessageResolutionForeign.ACCESS;
    }

    public static boolean isInstance(TruffleObject obj) {
        return obj instanceof NoMatchResultObject;
    }

    @MessageResolution(receiverType = NoMatchResultObject.class)
    static final class NoMatchResultObjectMessageResolution {

        @Resolve(message = "KEYS")
        abstract static class NoMatchResultObjectKeysNode extends Node {

            @TruffleBoundary
            @SuppressWarnings("unused")
            public Object access(NoMatchResultObject o) {
                return new NoMatchResultNamesObject();
            }
        }

        @Resolve(message = "KEY_INFO")
        abstract static class NoMatchResultObjectKeyInfoNode extends Node {

            @SuppressWarnings("unused")
            public Object access(NoMatchResultObject o, String name) {
                if (RegexLanguage.NO_MATCH_RESULT_IDENTIFIER.equals(name)) {
                    return 3;
                } else {
                    return 0;
                }
            }
        }

        @Resolve(message = "READ")
        abstract static class NoMatchResultObjectReadNode extends Node {

            @SuppressWarnings("unused")
            public Object access(NoMatchResultObject o, String name) {
                if (RegexLanguage.NO_MATCH_RESULT_IDENTIFIER.equals(name)) {
                    return RegexLanguage.EXPORT_NO_MATCH_RESULT;
                }
                CompilerDirectives.transferToInterpreter();
                throw UnknownIdentifierException.raise(name);
            }
        }

        static final class NoMatchResultNamesObject implements TruffleObject {

            @Override
            public ForeignAccess getForeignAccess() {
                return NoMatchResultNamesMessageResolutionForeign.ACCESS;
            }

            public static boolean isInstance(TruffleObject obj) {
                return obj instanceof NoMatchResultNamesObject;
            }

            @MessageResolution(receiverType = NoMatchResultNamesObject.class)
            static final class NoMatchResultNamesMessageResolution {

                @Resolve(message = "HAS_SIZE")
                abstract static class NoMatchResultNamesHasSizeNode extends Node {

                    @SuppressWarnings("unused")
                    public Object access(NoMatchResultNamesObject namesObject) {
                        return true;
                    }
                }

                @Resolve(message = "GET_SIZE")
                abstract static class NoMatchResultNamesGetSizeNode extends Node {

                    @SuppressWarnings("unused")
                    public Object access(NoMatchResultNamesObject namesObject) {
                        return 1;
                    }
                }

                @Resolve(message = "READ")
                abstract static class NoMatchResultNamesReadNode extends Node {

                    @SuppressWarnings("unused")
                    public Object access(NoMatchResultNamesObject namesObject, int index) {
                        if (index >= 1) {
                            CompilerDirectives.transferToInterpreter();
                            throw UnknownIdentifierException.raise(Integer.toString(index));
                        }
                        return RegexLanguage.NO_MATCH_RESULT_IDENTIFIER;
                    }
                }

            }
        }
    }
}
