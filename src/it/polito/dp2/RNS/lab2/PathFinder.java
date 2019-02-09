package it.polito.dp2.RNS.lab2;

import java.util.List;
import java.util.Set;


/**
 * PathFinder is an interface that has to be implemented in DP2 Assignment 2.
 * A PathFinder lets users find shortest paths in a set of interconnected places (the model).
 * A PathFinder exploits a remote service, capable of finding shortest paths in
 * a directed graph (in Assignment 2 this service is NEO4J).
 * A PathFinder must know how to get the current version of the model,
 * which may change from time to time.
 * Only when requested by the user, through the reloadModel operation, a PathFinder gets
 * (and loads) the current version of the model, which replaces any previously loaded model.
 * During this operation, the PathFinder also uploads the graph of this model to the remote
 * service. After this operation, the loaded model can be used to compute shortest paths,
 * by means of the findShortestPaths operation, until the next reloadModel, which may
 * cause the loaded model to change.
 * A PathFinder has 2 states:
 * 1. initial state: no model loaded (and no graph yet uploaded to remote service).
 * 2. operating state: model loaded (and graph uploaded to remote service and service ready
 * to respond to queries).
 * The current state can be checked by means of the isModelLoaded operation.
 *
 */
public interface PathFinder {
	
	/**
	 * Checks the current state
	 * @return true if the current state is the operating state (model loaded)
	 */
	boolean isModelLoaded();
	
	/**
	 * Loads the current version of the model so that, if the operation is successful,
	 * after the operation the PathFinder is in the operating state (model loaded) and
	 * it can compute shortest paths on the loaded model.
	 * @throws ServiceException if the operation cannot be completed because the remote service is not available or fails
	 * @throws ModelException if the operation cannot be completed because the current model cannot be read or is wrong (the problem is not related to the remote service)
	 */
	void reloadModel() throws ServiceException, ModelException;
	
	/**
	 * Looks for the shortest paths connecting a source place to a destination place
	 * Each path is returned as a list of place identifiers, where the first place in the list is the source
	 * and the last place is the destination.
	 * @param source The id of the source of the paths to be found
	 * @param destination The id of the destination of the paths to be found
	 * @param maxlength The maximum length of the paths to be found (0 or negative means no bound on the length)
	 * @return the set of the shortest paths connecting source to destination
	 * @throws UnknownIdException if source or destination is not a known place identifier
	 * @throws BadStatedException if the operation is called when in the initial state (no model loaded)
	 * @throws ServiceException if the operation cannot be completed because the remote service is not available or fails
	 */
	Set<List<String>> findShortestPaths(String source, String destination, int maxlength) throws UnknownIdException, BadStateException, ServiceException;

}
