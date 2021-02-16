/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test for checking the order of properties in AggregateError.
 *
 * @option ecmascript-version=2022
 */

function toAccessor(name) {
    return function(field) {
        if(field.kind == "field") {
            field.extras = [{
                kind:"accessor",
                key:name,
                placement: field.placement,
                get: function(){
                    return this[field.key];
                },
                set: function(value){
                    this[field.key] = value;
                }
            }];
        }
        return field;
    }
}

class C {
    @toAccessor("test")
    x = 5;
}

let c = new C();
c.test = 10;
c.test == 10;

