# Migration Guide from Rhino to GraalVM JavaScript

This document serves as migration guide for code previously targeted to the Rhino engine.
See the [Java Interoperability](JavaInteroperability.md) guide for an overview of supported features.

### Overview
Both Rhino and GraalVM JavaScript support a similar set of syntax and semantics for Java interoperability.
The most important differences relevant for migration are listed here.

### `Java.type(typename)` instead of `java.a.b.c.typename`
GraalVM JavaScript does not put available Java classes in the JavaScript scope.
You have to explicitly load the classes using `Java.type(typename)`.
GraalVM JavaScript supports the `Packages` global object, but loading the classes explicitly is still encouraged.
The following Java package globals are available in Nashorn compatibility mode (`js.nashorn-compat` option): `java`, `javafx`, `javax`, `com`, `org`, `edu`.

### Console Output of Java Classes and Java Objects
GraalVM JavaScript provides a `print` builtin function.
It tries to special-case its behavior on Java classes and Java objects to provide the most useful output.

Note that GraalVM JavaScript also provides a `console.log` function.
This is an alias for `print` in pure JavaScript mode, but uses an implementation provided by Node.js when in Node mode.
Behavior around interop objects differs for `console.log` in Node mode as it does not implement special treatment for such objects.

### JavaScript vs Java Strings
GraalVM JavaScript uses Java strings internally to represent JavaScript strings.
This makes it impossible to differentiate whether a specific string was created by JavaScript or by Java code.
In GraalVM JavaScript, the JavaScript properties take precedence over Java fields or methods.
For instance, you can query the `length` property (of JavaScript) but you cannot call the `length` function (of Java) on JavaScript strings - `length` behaves like a data property, not like a function.

### JavaImporter
The `JavaImporter` feature is available only in Nashorn compatibility mode (`js.nashorn-compat` option).
