/**
 * 
 */
package it.polito.dp2.RNS.lab2;

import it.polito.dp2.RNS.FactoryConfigurationError;

/**
 * Defines a factory API that enables applications to obtain one or more objects
 * implementing the {@link PathFinder} interface.
 *
 */
public abstract class PathFinderFactory {

	private static final String propertyName = "it.polito.dp2.RNS.lab2.PathFinderFactory";
	
	protected PathFinderFactory() {}
	
	/**
	 * Obtain a new instance of a <tt>PathFinderFactory</tt>.
	 * 
	 * <p>
	 * This static method creates a new factory instance. This method uses the
	 * <tt>it.polito.dp2.RNS.lab2.PathFinderFactory</tt> system property to
	 * determine the PathFinderFactory implementation class to load.
	 * </p>
	 * <p>
	 * Once an application has obtained a reference to a
	 * <tt>PathFinderFactory</tt> it can use the factory to obtain a new
	 * {@link PathFinder} instance.
	 * </p>
	 * 
	 * @return a new instance of a <tt>PathFinderFactory</tt>.
	 * 
	 * @throws FactoryConfigurationError if the implementation is not available 
	 * or cannot be instantiated.
	 */
	public static PathFinderFactory newInstance() throws FactoryConfigurationError {
		
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		
		if(loader == null) {
			loader = PathFinderFactory.class.getClassLoader();
		}
		
		String className = System.getProperty(propertyName);
		if (className == null) {
			throw new FactoryConfigurationError("cannot create a new instance of a PathFinderFactory"
												+ "since the system property '" + propertyName + "'"
												+ "is not defined");
		}
		
		try {
			Class<?> c = (loader != null) ? loader.loadClass(className) : Class.forName(className);
			return (PathFinderFactory) c.newInstance();
		} catch (Exception e) {
			throw new FactoryConfigurationError(e, "error instantiatig class '" + className + "'.");
		}
	}
	
	
	/**
	 * Creates a new instance of a {@link PathFinder} implementation.
	 * 
	 * @return a new instance of a {@link PathFinder} implementation.
	 * @throws PathFinderException if an implementation of {@link PathFinder} cannot be created.
	 */
	public abstract PathFinder newPathFinder() throws PathFinderException;
}