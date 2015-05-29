package de.skuzzle.tinyplugz.guice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import de.skuzzle.tinyplugz.ExchangeClassLoader;

final class TinyPlugzContextInterceptor implements MethodInterceptor {

    @Override
    public final Object invoke(MethodInvocation invocation) throws Throwable {
        try (ExchangeClassLoader exchange = ExchangeClassLoader.forTinyPlugz()) {
            return invocation.proceed();
        }
    }

}
