[![Build Status](https://travis-ci.org/skuzzle/TinyPlugz.svg)](https://travis-ci.org/skuzzle/TinyPlugz)
[SonarQube](https://www.serverd.de/sonar/dashboard/index/1106)

TinyPlugz
================

TinyPlugz is a very small (tiny!) library for building simple modular
applications. It is built around the facilities provided by the java
`ServiceLoader` and `URLClassLoader` classes.

Features:
* Access code from jar files dynamically during runtime of your application
* Plugins provide features for other plugins or for the host application
  using java service provider interfaces (SPI).
* Deploy time extensibility: The whole TinyPlugz implementation can be
  exchanged during deploy time of your application to provide additional
  features.

## Maven Dependency
TinyPlugz is available through maven central:
```xml
<dependency>
    <groupId>de.skuzzle.tinyplugz</groupId>
    <artifactId>tiny-plugz</artifactId>
    <version>0.1.0</version>
</dependency>
```

TinyPlugz guice extension:
```xml
<dependency>
    <groupId>de.skuzzle.tinyplugz</groupId>
    <artifactId>tiny-plugz-guice</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Documentation
JavaDoc is available here:
* [TinyPlugz](http://www.skuzzle.de/tiny-plugz/0.1.0/main/doc)
* [TinyPlugzGuice](http://www.skuzzle.de/tiny-plugz/0.1.0/guice/doc)

## Usage
Before TinyPlugz can be used, it needs to be configured using the
`TinyPlugzConfigurator` class. This class is responsible for deploying an
application wide single instance of `TinyPlugz` which can then be accessed
using the static `TinyPlugz.getInstance()` method.

In your host application (probably in its `void main`) write:

```java
final Path pluginFolder = ...; // folder containing plugins to be loaded
TinyPlugzConfigurator.setup()
    .withPluings(source -> source.addAllPluginJars(pluginFolder))
    .deploy();
```

If you use TinyPlugz in a web application, you can use the TinyPlugzServletContextListener
for deploying TinyPlugz.

Now you can access services provided by your plugins anywhere in your
application:

```java
Iterator<MyService> services = TinyPlugz.getInstance().getServices(MyService.class);
```

Services are then searched among all plugins and the host application using
java's `ServiceLoader` class.

## Logging
TinyPlugz only has a single dependency on a 3rd party library. It uses _slf4j_
as logging abstraction to support multiple logging frameworks if desired.

## Extension
As mentioned above, the whole TinyPlugz behavior can be exchanged during deploy
time of your application. This is achieved by loading the TinyPlugz
implementation itself using the `ServiceLoader`: if the configurator class
finds a service providing an instance of `TinyPlugz` it will use just that
instance. Otherwise, the default implementation will be used.

The _tiny-plugz-guice_ extension takes advantage of this features to provide
a plugin system with supporting dependency injection: while deploying, it pulls
implementations of Guice modules from plugins using the `ServiceLoader` and
then implements the TinyPlugz interface using a Guice Injector.

## FAQ

F: Why is the test-coverage so low?

A: Because of the many static methods involved in the TinyPlugz implementation
  (like `ServiceLoader.load`, `Files.isDirectory` or `Guice.createInjector`)
  TinyPlugz uses _PowerMockito_ for testing. Sadly, PowerMockito does not play
  very well together with the _jacoco_ code coverage plugin, which is the reason
  that the reported coverage is much lower than the actual coverage.