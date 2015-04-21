package de.skuzzle.tinyplugz;


/**
 * Atomic action which can be executed with the plugin Classloader as the
 * current thread's context Classloader. This is useful to integrate with other
 * frameworks such as JPA which use the context Classloader for finding certain
 * classes or resources.
 *
 * <code>
 * <pre>
 * TinyPlugz.getDefault().contextClassLoaderScope(() -> {
 *     EntityManagerFactory emf = Persistence.createEntityManagerFactory("pu");
 *     // ...
 * });
 * </pre>
 * </code>
 *
 * @author Simon Taddiken
 * @see TinyPlugz#contextClassLoaderScope(ContextAction)
 */
@FunctionalInterface
public interface ContextAction {

    /**
     * Defines the action to perform.
     */
    public void perform();
}
