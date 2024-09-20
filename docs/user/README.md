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
This example application was tested with GraalVM for JDK 23 and the GraalVM Polyglot API version 24.1.0.
See how to install GraalVM on the [Downloads page](https://www.graalvm.org/downloads/).

1. Create a new Maven Java project named "helloworld" in your favorite IDE or from your terminal with the following structure:
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
    mvn archetype:generate -DgroupId=com.example -DartifactId=helloworld -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.5 -DinteractiveMode=false
    ```

2. Replace the contents of _App.java_ with the following code:
    ```java
    package com.example;

    import org.graalvm.polyglot.*;
    import org.graalvm.polyglot.proxy.*;

    public class App {

        static String JS_CODE = "(function myFun(param){console.log('Hello ' + param + ' from JS');})";

        public static void main(String[] args) {
            String who = args.length == 0 ? "World" : args[0];
            System.out.println("Hello " + who + " from Java");
            try (Context context = Context.create()) {
                Value value = context.eval("js", JS_CODE);
                value.execute(who);
            }
        }
    }
    ```
    This example application uses the [Polyglot API](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/package-summary.html) and returns a JavaScript function as a Java value.

3. Add the following dependencies to _pom.xml_ to include the JavaScript engine (GraalJS):
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
    Set the GraalJS and GraalVM Polyglot API versions by adding a `graaljs.version` property to the `<properties>` section.
    Alternatively, you can replace `${graaljs.version}` with the version string directly.
    For this example, use `24.1.0`:
    ```xml
    <properties>
        <graaljs.version>24.1.0</graaljs.version>
    </properties>
    ```

4. Add the Maven plugins for compiling the project into a JAR file and copying all runtime dependencies into a directory to your _pom.xml_ file:
    ```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <fork>true</fork>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.4.2</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.example.App</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>3.8.0</version>
                <executions>
                    <execution>
                        <id>copy-dependencies</id>
                        <phase>package</phase>
                        <goals>
                            <goal>copy-dependencies</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>${project.build.directory}/modules</outputDirectory>
                            <includeScope>runtime</includeScope>
                            <includeTypes>jar</includeTypes>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    ```

5. (Optional.) Add _module-info.java_ to your application.
    If you would like to run your application on the module path, create a _module-info.java_ file in _src/main/java_ with the following contents:
    ```java
    module com.example {
        requires org.graalvm.polyglot;
    }
    ```

6. Compile and package the project:
    ```bash
    mvn clean package
    ```

7.  Run the application using GraalVM or another compatible JDK.
    If you've included _module-info.java_ in your project (step 5), you can now run the application on the module path, using one of the following commands:
    ```bash
    java --module-path target/modules:target/helloworld-1.0-SNAPSHOT.jar --module com.example/com.example.App "GraalVM"
    java -p target/modules:target/helloworld-1.0-SNAPSHOT.jar -m com.example/com.example.App "GraalVM"
    ```
    Otherwise, you can run with the dependencies on the module path and the application on the class path:
    ```bash
    java --module-path target/modules --add-modules=org.graalvm.polyglot -cp target/helloworld-1.0-SNAPSHOT.jar com.example.App "GraalVM"
    java --module-path target/modules --add-modules=org.graalvm.polyglot -jar target/helloworld-1.0-SNAPSHOT.jar "GraalVM"
    ```
    Alternatively, you can run with everything on the class path as well (in this case you need to use `*` or specify all JAR files):
    ```bash
    java -cp "target/modules/*:target/helloworld-1.0-SNAPSHOT.jar" com.example.App "GraalVM"
    # or using shell expansion:
    java -cp "$(find target/modules -name '*.jar' | tr '\n' :)target/helloworld-1.0-SNAPSHOT.jar" com.example.App "GraalVM"
    java -cp "$(printf %s: target/modules/*.jar)target/helloworld-1.0-SNAPSHOT.jar" com.example.App "GraalVM"
    ```

    > Note: We discourage bundling all dependencies into a single "fat" JAR (for example, using the Maven Assembly plugin) as it can cause issues and prevent ahead-of-time compilation with [GraalVM Native Image](https://www.graalvm.org/reference-manual/native-image/).
    > Instead, we recommend using the original, separate JAR files for all `org.graalvm.*` dependencies, preferably on the module path.
    Learn more in the [Guide to Embedding Languages](https://www.graalvm.org/reference-manual/embed-languages/#dependency-setup).

The source code unit can be represented with a String, as shown in the example, a file, read from URL, and [other means](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Source.html).
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