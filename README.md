[![Build Status](https://travis-ci.org/skuzzle/TinyPlugz.svg)](https://travis-ci.org/skuzzle/TinyPlugz)

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
      
## Usage
Before TinyPlugz can be used, it needs to be configured using the 
`TinyPlugzConfigurator` class. This class is responsible for deploying an 
application wide single instance of `TinyPlugz` which can then be accessed
using the static `TinyPlugz.getDefault()` method.

In your host application (probably in its `void main`) write:

```java
final Path pluginFolder = ...; // folder containing plugins to be loaded
TinyPlugzConfigurator.setup()
    .withPluings(source -> source.addAll(pluginFolder))
    .deploy();
```

Now you can access services provided by your plugins anywhere in your 
application:

```java
Iterator<MyService> services = TinyPlugz.getDefault().getServices(MyService.class);
```

Services are then searched among all plugins and the host application using 
java's `ServiceLoader` class.

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