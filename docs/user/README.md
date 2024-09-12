---
layout: docs
toc_group: js
link_title: GraalJS
permalink: /reference-manual/js/
---

# GraalJS

GraalJS is a fast JavaScript language implementation built on top of GraalVM. 
It is ECMAScript-compliant, provides interoperability with Java and other Graal languages, common tooling, and, if run on the GraalVM JDK, provides the best performance with the Graal JIT compiler by default.
You can also use GraalJS with Oracle JDK or OpenJDK. 

GraalJS is a suitable replacement for projects wanting to [migrate from Nashorn or Rhino](#migration-guides) to a JavaScript engine that supports new ECMAScript standards and features.
You can easily add GraalJS to your Java application as shown below.

## Getting Started with GraalJS on the JVM

To embed JavaScript in a Java host application, enable GraalJS by adding it as a project dependency.
All necessary artifacts can be downloaded directly from Maven Central. 
All artifacts relevant to embedders can be found in the Maven dependency group [org.graalvm.polyglot](https://central.sonatype.com/namespace/org.graalvm.polyglot). 

Below is the Maven configuration for a JavaScript embedding:
```xml
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>polyglot</artifactId>
    <version>${graaljs.version}</version>
</dependency>
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>js</artifactId>
    <version>${graaljs.version}</version>
    <type>pom</type>
</dependency>
```
This enables GraalJS which is built on top of Oracle GraalVM and licensed under the [GraalVM Free Terms and Conditions (GFTC)](https://www.oracle.com/downloads/licenses/graal-free-license.html).
Use `js-community` if you want to use GraalJS built on GraalVM Community Edition.

Go step-by-step to create a Maven project, embedding JavaScript in Java, and run it.
This example application was tested with GraalVM for JDK 22 and the GraalVM Polyglot API version 24.0.2.
See how to install GraalVM on the [Downloads page](https://www.graalvm.org/downloads/).

1. Create a new Maven Java project named "app" in your favorite IDE or from your terminal with the following structure:
    ```
    ├── pom.xml
    └── src
        ├── main
        │   └── java
        │       └── com
        │           └── example
        │               └── App.java
    ```
    For example, you can run this command to create a new Maven project using the quickstart archetype:
    ```bash
    mvn archetype:generate -DgroupId=com.example -DartifactId=app -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
    ```

2. Replace the contents of _App.java_ with the following code:
    ```java
    package com.example;

    import org.graalvm.polyglot.*;
    import org.graalvm.polyglot.proxy.*;

    public class App {

        static String JS_CODE = "(function myFun(param){console.log('hello '+param);})";

        public static void main(String[] args) {
            System.out.println("Hello JavaScript from Java");
            try (Context context = Context.create()) {
                Value value = context.eval("js", JS_CODE);
                value.execute(args[0]);
            }
        }
    }
    ```

3. Add the regular Maven plugins for compiling and assembling the project into a JAR file with all dependencies to your _pom.xml_ file:
    ```xml
    <build>
        <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>${maven-compiler-plugin.version}</version>
            <configuration>
            <fork>true</fork>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-assembly-plugin</artifactId>
            <version>${maven-assembly-plugin.version}</version>
            <configuration>
            <archive>
                <manifest>
                <mainClass>com.example.App</mainClass>
                </manifest>
            </archive>
            <descriptorRefs>
                <descriptorRef>jar-with-dependencies</descriptorRef>
            </descriptorRefs>
            </configuration>
            <executions>
            <execution>
                <id>make-assembly</id>
                <phase>package</phase>
                <goals>
                <goal>single</goal>
                </goals>
            </execution>
            </executions>
        </plugin>
        </plugins>
    </build>
    ```

4. Add the following dependencies to _pom.xml_ to include the JavaScript engine (GraalJS):
    ```xml
    <dependencies>
        <dependency>
            <groupId>org.graalvm.polyglot</groupId>
            <artifactId>polyglot</artifactId>
            <version>${graaljs.version}</version>
        </dependency>
        <dependency>
            <groupId>org.graalvm.polyglot</groupId>
            <artifactId>js</artifactId>
            <version>${graaljs.version}</version>
            <type>pom</type>
        </dependency>
    </dependencies>
    ```
    Set the `${graaljs.version}` property to the GraalVM Polyglot API version. For this example, use `24.0.2`.

5. Package the project and run the application:
    ```bash
    mvn clean package
    ```
    ```bash
    java -jar target/helloworld-1.0-SNAPSHOT-jar-with-dependencies.jar GraalVM
    ```
    
    This example application uses the [Polyglot API](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/package-summary.html) and returns a JavaScript function as a Java value. 
    A single JAR with all dependencies was created from language libraries. 
    However, we recommend splitting and using Java modules on the module path, especially if you would like to compile this application ahead of time with GraalVM Native Image. 
    Learn more in the [Guide to Embedding Languages](https://www.graalvm.org/reference-manual/embed-languages/#dependency-setup).

The source code unit can be represented with a String, as in the example, a file, read from URL, and [other means](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Source.html). 
By wrapping the function definition (`()`), you return the function immediately:
```java
Value f = context.eval("js", "(function f(x, y) { return x + y; })");
Value result = f.execute(19, 23);
```

You can also lookup Java types from JavaScript and instantiate them, as demonstrated below:
```java
try (Context context = Context.newBuilder()
                           .allowHostAccess(HostAccess.newBuilder(HostAccess.ALL).build())
                           .allowHostClassLookup(className -> true)
                       .build()) {
    java.math.BigDecimal v = context.eval("js",
            "var BigDecimal = Java.type('java.math.BigDecimal');" +
            "BigDecimal.valueOf(10).pow(20)")
        .asHostObject();
    assert v.toString().equals("100000000000000000000");
}
```

The Polyglot API offers many other ways to access a guest language code from Java, for example, by directly accessing JavaScript objects, numbers, strings, and arrays. 
Learn more about JavaScript to Java interoperability and find more examples in the [Java Interoperability guide](JavaInteroperability.md).

### Related Documentation

GraalJS is also available as a standalone distribution that you can download from [GitHub](https://github.com/oracle/graaljs/releases). 
Learn more [here](https://github.com/oracle/graaljs/blob/master/README.md#standalone-distributions).

We provide the following documentation for GraalJS users:
* [JavaScript Compatibility](JavaScriptCompatibility.md)
* [Java Interoperability](JavaInteroperability.md)
* [Options of the JavaScript Engine](Options.md)
* [Multithreading Support](Multithreading.md)
* [Execute GraalJS on a Stock JDK](RunOnJDK.md)

#### Migration Guides

Learn more about migration from legacy environments:
* [Migration Guide from Nashorn](NashornMigrationGuide.md)
* [Migration Guide from Rhino](RhinoMigrationGuide.md)
* [Work with ScriptEngine](ScriptEngine.md)