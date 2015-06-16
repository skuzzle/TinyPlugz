[![Build Status](https://travis-ci.org/skuzzle/TinyPlugz.svg)](https://travis-ci.org/skuzzle/TinyPlugz)
[SonarQube](https://www.serverd.de/sonar/dashboard/index/1106)

TinyPlugz
================

TinyPlugz is a very small (tiny!) library for building simple modular
applications. It is built around the facilities provided by the java
`ServiceLoader` and `URLClassLoader` classes.

Features:
* Access code from jar files dynamically during runtime of your application.
* Own ClassLoader implementation, allowing plugins to specify dependencies of their own.
* Plugins provide features for other plugins or for the host application
  using java service provider interfaces (SPI).
* Deploy time extensibility: The whole TinyPlugz implementation can be
  exchanged during deploy time of your application to provide additional
  features.
* Web application integration.

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


# Internals

## Classloading

There are several ClassLoaders involved with plugin loading which follow a strict 
delegation model to separate dependencies of different plugins.

* The `PluginClassLoader` extends java's own `URLClassLoader` and is responsible for 
  loading classes from a single plugin.
* The PluginClassLoader has a child URLClassLoader which is responsible for loading 
  dependent classes that are specified in the plugin's manifest file.
* The `DelegateClassLoader` connects all plugins with each other and thus allows plugins 
  and the application to access classes from other plugins. This is the loader returned by
  `TinyPlugz.getPluginClassLoader()`.
  
The parent ClassLoader of every ClassLoader mentioned above is the application's 
ClassLoader that has been specified in the setup phase (`TinyPlugzConfigurator.setup...`).
This delegation model has the following implications:

1. Classes of plugins can access classes of other plugins.
2. Classes of plugins can access classes of the application.
3. Classes of plugins can access classes of their own dependencies.
4. Classes of plugin dependencies can access classes of the application.
5. Classes of plugin dependencies can access classes of other dependencies of 
   the same plugin.
6. Classes of the application can access classes of plugins using the TinyPlugz 
   interface.
   
To seperate the concerns of single plugins, here is a list of relations that are not 
possible:

1. Classes of plugins can not access classes of other plugin's dependencies.
2. Classes of the application can not access classes from any plugin's dependencies.
3. Classes of a plugin's dependency can not access classes of that, or any other plugin.


# Plugin HowTo
A TinyPlugz plugin is no more than a single jar file containing custom code to extend your 
application. In order for your application to be extandable, it should specify interfaces 
that can be implemented as services by your plugins.

## Plugin Manifest
For full compatibility your plugins should specify a `META-INF/MANIFEST.mf` file, which at 
least contains the `Implementation-Title` attribute to specify the plugin's name. 
Additionally, if it specifies the `Class-Path` attribute, then the given entries are 
treated as relative paths to the plugin's own location and classes from the listed 
dependencies will be visible to the plugin during execution.