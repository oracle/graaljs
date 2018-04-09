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

"use strict";

(function(){

// String methods

Internal.CreateMethodProperty(String, "raw",
    function raw(template, ...substitutions) {
        var cooked = Internal.ToObject(template);
        var raw = Internal.ToObject(cooked.raw);
        var literalSegments = Internal.ToLength(raw.length);
        if (literalSegments <= 0) {
            return "";
        }
        var numberOfSubstitutions = substitutions.length;
        var stringElements = "";
        for (var i = 0;; i++) {
            stringElements += Internal.ToString(raw[i]);
            if (i + 1 == literalSegments) {
                break;
            }
            if (i < numberOfSubstitutions) {
                stringElements += Internal.ToString(substitutions[i]);
            }
        }
        return stringElements;
    }
);


if (Internal.AnnexB) {
// HTML generation methods of String.prototype

function CreateHTML(string, tag, attribute, value) {
    Internal.RequireObjectCoercible(string);
    var S = Internal.ToString(string);
    var p1 = "<" + tag;
    if (attribute !== "") {
        p1 += " " + attribute + '="' + Internal.StringReplace(Internal.ToString(value), '"', "&quot;") + '"';
    }
    return p1 + ">" + S + "</" + tag + ">";
}

Internal.CreateMethodProperty(String.prototype, "anchor", function anchor(name) { return CreateHTML(this, "a", "name", name); });
Internal.CreateMethodProperty(String.prototype, "big", function big() { return CreateHTML(this, "big", "", ""); });
Internal.CreateMethodProperty(String.prototype, "blink", function blink() { return CreateHTML(this, "blink", "", ""); });
Internal.CreateMethodProperty(String.prototype, "bold", function bold() { return CreateHTML(this, "b", "", ""); });
Internal.CreateMethodProperty(String.prototype, "fixed", function fixed() { return CreateHTML(this, "tt", "", ""); });
Internal.CreateMethodProperty(String.prototype, "fontcolor", function fontcolor(clr) { return CreateHTML(this, "font", "color", clr); });
Internal.CreateMethodProperty(String.prototype, "fontsize", function fontsize(size) { return CreateHTML(this, "font", "size", size); });
Internal.CreateMethodProperty(String.prototype, "italics", function italics() { return CreateHTML(this, "i", "", ""); });
Internal.CreateMethodProperty(String.prototype, "link", function link(url) { return CreateHTML(this, "a", "href", url); });
Internal.CreateMethodProperty(String.prototype, "small", function small() { return CreateHTML(this, "small", "", ""); });
Internal.CreateMethodProperty(String.prototype, "strike", function strike() { return CreateHTML(this, "strike", "", ""); });
Internal.CreateMethodProperty(String.prototype, "sub", function sub() { return CreateHTML(this, "sub", "", ""); });
Internal.CreateMethodProperty(String.prototype, "sup", function sup() { return CreateHTML(this, "sup", "", ""); });
}

})();
