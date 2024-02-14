/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that an identifier reference in a switch expression declared in an outer function scope
 * is correctly resolved when a binding with the same name is declared in the switch block scope.
 */

load("assert.js");

const Type = {
    FLOAT: {},
    INTEGER: {},
    LONG: {},
    BIGDECIMAL: {},
    DATE: {},
    TIME: {},
    DATETIME: {},
    DURATION: {},
};

function createFormatter(type, options) {
    return {type, options};
}

var y = Type.BIGDECIMAL;
var O = {decimalSeparator: "."};

var F1 = (function getFormatterForType(t) {
    return (function () {
        switch (t) {
        case Type.FLOAT:
        case Type.INTEGER:
            return createFormatter("Number");
        case Type.LONG:
            return createFormatter("BigInt");
        case Type.BIGDECIMAL:
            const t = {
                digits: 34,
            };
            return createFormatter("Decimal", t);
        case Type.DATE:
            return createFormatter("PlainDate");
        case Type.TIME:
            return createFormatter("PlainTime");
        case Type.DATETIME:
            return createFormatter("Instant");
        case Type.DURATION:
            return createFormatter("Duration");
        default:
            return null;
        }
    });
})(y, O);

var F2 = (function getFormatterForType(t, y) {
    return (function () {
        switch (t) {
        case Type.FLOAT:
        case Type.INTEGER:
            return createFormatter("Number", y);
        case Type.LONG:
            return createFormatter("BigInt", y);
        case Type.BIGDECIMAL:
            const t = Object.assign({}, y, {
                digits: 34,
            });
            return createFormatter("Decimal", t);
        case Type.DATE:
            return createFormatter("PlainDate", y);
        case Type.TIME:
            return createFormatter("PlainTime", y);
        case Type.DATETIME:
            return createFormatter("Instant", y);
        case Type.DURATION:
            return createFormatter("Duration", y);
        default:
            return null;
        }
    });
})(y, O);

var F3 = (function getFormatterForType(r, y) {
    let s = r;
    {
        let t = s;
        return (function () {
            switch (t) {
            case Type.FLOAT:
            case Type.INTEGER:
                return createFormatter("Number", y);
            case Type.LONG:
                return createFormatter("BigInt", y);
            case Type.BIGDECIMAL:
                const t = Object.assign({}, y, {
                    digits: 34,
                });
                return createFormatter("Decimal", t);
            case Type.DATE:
                return createFormatter("PlainDate", y);
            case Type.TIME:
                return createFormatter("PlainTime", y);
            case Type.DATETIME:
                return createFormatter("Instant", y);
            case Type.DURATION:
                return createFormatter("Duration", y);
            default:
                return null;
            }
        });
    }
})(y, O);

assertSame('{"type":"Decimal","options":{"digits":34}}', JSON.stringify(F1()));
assertSame('{"type":"Decimal","options":{"decimalSeparator":".","digits":34}}', JSON.stringify(F2()));
assertSame('{"type":"Decimal","options":{"decimalSeparator":".","digits":34}}', JSON.stringify(F3()));
