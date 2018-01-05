/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser.foreign;

import com.oracle.truffle.api.interop.*;

abstract class JSForeignAccessExtraMessages extends Message {

    @Override
    public final int hashCode() {
        return System.identityHashCode(getClass());
    }

    public static final class GetOwnKeysMessage extends JSForeignAccessExtraMessages {

        @Override
        public boolean equals(Object message) {
            return message instanceof GetOwnKeysMessage;
        }
    }

    public static final class IsStringifiableMessage extends JSForeignAccessExtraMessages {

        @Override
        public boolean equals(Object message) {
            return message instanceof IsStringifiableMessage;
        }
    }

    public static final class IsArrayMessage extends JSForeignAccessExtraMessages {

        @Override
        public boolean equals(Object message) {
            return message instanceof IsArrayMessage;
        }
    }

    public static final class GetJSONConvertedMessage extends JSForeignAccessExtraMessages {

        @Override
        public boolean equals(Object message) {
            return message instanceof GetJSONConvertedMessage;
        }
    }

    public static final class DoubleToStringMessage extends JSForeignAccessExtraMessages {

        @Override
        public boolean equals(Object message) {
            return message instanceof DoubleToStringMessage;
        }
    }

    public static final class HasPropertyMessage extends JSForeignAccessExtraMessages {

        @Override
        public boolean equals(Object message) {
            return message instanceof HasPropertyMessage;
        }
    }

    public static final class TryConvertMessage extends JSForeignAccessExtraMessages {

        @Override
        public boolean equals(Object message) {
            return message instanceof TryConvertMessage;
        }
    }

    public static final class AllocateTypedArrayMessage extends JSForeignAccessExtraMessages {

        @Override
        public boolean equals(Object message) {
            return message instanceof AllocateTypedArrayMessage;
        }
    }

}
