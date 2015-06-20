package de.skuzzle.tinyplugz.guice;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

import de.skuzzle.tinyplugz.util.ElementIterator;
import de.skuzzle.tinyplugz.util.Iterators;

/**
 * Strategies for obtaining the iterator that is returned by
 * {@link TinyPlugzGuice#getServices(Class)}.
 *
 * @author Simon Taddiken
 * @since 0.3.0
 */
enum GetServicesStrategy {
    /**
     * Services created by this strategy will be eagerly instantiated by the
     * time they are requested. That is, they will be created before the
     * iterator is consumed.
     */
    EAGER {

        @Override
        <T> Iterator<T> getServices(Injector injector, Class<T> type) {
            try {
                final TypeLiteral<Collection<T>> t = setOf(type);
                final Collection<T> c = injector.getInstance(Key.get(t));
                if (c.isEmpty()) {
                    return getSingleEager(injector, type);
                } else {
                    return c.iterator();
                }
            } catch (final ConfigurationException e) {
                return getSingleEager(injector, type);
            }
        }
    },

    /**
     * Services created by this strategy will be lazily instantiated. That is,
     * each service will be constructed by the time it is requested from the
     * iterator.
     */
    LAZY {

        @Override
        <T> Iterator<T> getServices(Injector injector, Class<T> type) {
            try {
                final TypeLiteral<Collection<Provider<T>>> t = setOfProviderOf(type);
                final Collection<Provider<T>> c = injector.getInstance(Key.get(t));
                if (c.isEmpty()) {
                    return getSingleLazy(injector, type);
                } else {
                    return new ProviderIterator<>(c.iterator());
                }
            } catch (final ConfigurationException e) {
                return getSingleLazy(injector, type);
            }
        }

    };

    abstract <T> Iterator<T> getServices(Injector injector, Class<T> type);

    protected <T> Iterator<T> getSingleEager(Injector injector, Class<T> type) {
        try {
            final T single = injector.getInstance(type);
            return Iterators.singleIterator(single);
        } catch (final ConfigurationException e1) {
            return Collections.emptyIterator();
        }
    }

    protected <T> Iterator<T> getSingleLazy(Injector injector, Class<T> type) {
        try {
            final Provider<T> single = injector.getProvider(type);
            final Iterator<Provider<T>> providerIt = Iterators.singleIterator(single);
            return new ProviderIterator<>(providerIt);
        } catch (final ConfigurationException e1) {
            return Collections.emptyIterator();
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> TypeLiteral<Collection<T>> setOf(Class<T> type) {
        final Type providerSet = Types.setOf(type);
        return (TypeLiteral<Collection<T>>) TypeLiteral.get(providerSet);
    }

    @SuppressWarnings("unchecked")
    protected <T> TypeLiteral<Collection<Provider<T>>> setOfProviderOf(Class<T> type) {
        final Type providerType = Types.providerOf(type);
        final Type providerSet = Types.newParameterizedType(Collection.class, providerType);
        return (TypeLiteral<Collection<Provider<T>>>) TypeLiteral.get(providerSet);
    }

    private static final class ProviderIterator<T> implements ElementIterator<T> {

        private final Iterator<Provider<T>> wrapped;

        private ProviderIterator(Iterator<Provider<T>> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public final boolean hasNext() {
            return this.wrapped.hasNext();
        }

        @Override
        public final T next() {
            return this.wrapped.next().get();
        }
    }
}
