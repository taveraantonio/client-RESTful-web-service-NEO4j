package it.polito.dp2.RNS.sol2;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.validation.Validator; 
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import it.polito.dp2.RNS.PlaceReader;
import it.polito.dp2.RNS.RnsReader;
import it.polito.dp2.RNS.RnsReaderFactory;
import it.polito.dp2.RNS.lab2.BadStateException;
import it.polito.dp2.RNS.lab2.ModelException;
import it.polito.dp2.RNS.lab2.ServiceException;
import it.polito.dp2.RNS.lab2.UnknownIdException;
import it.polito.dp2.RNS.sol2.jaxb.Connection;
import it.polito.dp2.RNS.sol2.jaxb.Node;
import it.polito.dp2.RNS.sol2.jaxb.ObjectFactory;
import it.polito.dp2.RNS.sol2.jaxb.Place;
import it.polito.dp2.RNS.sol2.jaxb.Rns;
import it.polito.dp2.RNS.sol2.jaxb.ShortestPath;
import it.polito.dp2.RNS.sol2.jaxb.ShortestPath.Relationships;
import it.polito.dp2.RNS.sol2.jaxb.ShortestResult;


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
public class SolPathFinder implements it.polito.dp2.RNS.lab2.PathFinder {

	private boolean loaded;		// Boolean to indicate if the model is loaded or not
	private RnsReader monitor;	// Rns reader monitor to random generator
	Map<String, URI> mapNodes;	// Map the placeID to its URI in the neo4j 
	Map<URI, String> mapURIs; 	// Map the URI to the placeID 
	Set<URI> relationships; 	// Set of all the URIs of the created relationship
	private ObjectFactory ob; 	// Object factory for my RNS Schema
	private Client client;		// The client object  
	private WebTarget target;	// The target object 
	
	public SolPathFinder() throws ServiceException{
		
		// Build the JAX-RS client object 
		this.client = ClientBuilder.newClient();
		// Set initially loaded to false 
		this.loaded = false;
		// Initialize the map of placeID, URIs 
		this.mapNodes = new TreeMap<String, URI>();
		// Initialize the map of URIs, placeID
		this.mapURIs = new TreeMap<URI, String>(); 
		// Initialize the set of relationship URIs
		this.relationships = new TreeSet<URI>();
		// Initialize the place object factory 
		this.ob = new ObjectFactory(); 
		// Set monitor to null
		this.monitor = null; 
		
		// Create a web target for the main URI 
		try{
			
			if(System.getProperty("it.polito.dp2.RNS.lab2.URL") == null){
				System.err.println("The URL system property is null");
				throw new ServiceException();
			}
			target = client.target(System.getProperty("it.polito.dp2.RNS.lab2.URL"));				
		
		}catch (Exception e){
			throw new ServiceException(); 
		}
	}

	
	/**
	 * Checks the current state
	 * @return true if the current state is the operating state (model loaded)
	 */
	@Override
	public boolean isModelLoaded() {
		if(this.loaded == true)
			return true;
		else 
			return false;
	}

	
	/**
	 * Loads the current version of the model so that, if the operation is successful,
	 * after the operation the PathFinder is in the operating state (model loaded) and
	 * it can compute shortest paths on the loaded model.
	 * @throws ServiceException if the operation cannot be completed because the remote service is not available or fails
	 * @throws ModelException if the operation cannot be completed because the current model cannot be read or is wrong (the problem is not related to the remote service)
	 */
	@Override
	public void reloadModel() throws ServiceException, ModelException {
		
		// Perform a delete of the already loaded model 
		if(this.loaded == true){
			this.performDelete();
			this.loaded = false; 
			this.mapNodes.clear();
			this.mapURIs.clear();
			this.relationships.clear();
			this.monitor = null; 
		}
		
		// Load model 
		// Instantiate a new RnsReader, Rns random generator 
		try{
			RnsReaderFactory factory = RnsReaderFactory.newInstance();
			this.monitor = factory.newRnsReader();	
		}catch(Exception e){
			System.out.println("Exception during RnsReaderFactory.newInstance()");
			throw new ModelException(); 
		}
		
		System.out.println("Loading data from Random Generator");
		Rns rns = this.ob.createRns();
		this.getPlaces(rns);
		System.out.println("Data Loaded");
		
		// Check data loaded from random generator, validate Rns against schema
		try {
			SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
			Schema schema = sf.newSchema(new File("custom/RnsSchema.xsd"));
			Validator validator = schema.newValidator();
	    	validator.setErrorHandler(new MyErrorHandler());
	    	JAXBContext jc = JAXBContext.newInstance("it.polito.dp2.RNS.sol2.jaxb"); 
	    	JAXBSource source = new JAXBSource(jc, rns); 
	    	validator.validate(source); 
	    	
		} catch (SAXException se) {
			System.out.println("Catched SAXException");
			se.printStackTrace();
			throw new ModelException();
		} catch (JAXBException je) {
			System.out.println("Catched JAXBException");
			je.printStackTrace();
			throw new ModelException();
		} catch (IOException ioe) {
			System.out.println("Catched IOException");
			ioe.printStackTrace();
			throw new ModelException();
		} catch (NullPointerException npe){
			System.out.println("Catched NullPointerException");
			npe.printStackTrace();
			throw new ModelException();
		} catch (IllegalArgumentException ie){
			System.out.println("Catched IllegalArgumentException");
			ie.printStackTrace();
			throw new ModelException();
		} catch (Exception e){
			System.out.println("Catched Exception");
			throw new ModelException();
		}
	
		// Upload places node to neo4j
		for(Place place : rns.getPlace()){
			URI uri = this.performPostNode(place); 
			this.mapNodes.put(place.getId(), uri);
			this.mapURIs.put(uri, place.getId()); 
		}
		
		// Load relationship to neoj4
		// I don't check if relationship are correct because i have already 
		// validate against the schema what i have read from the random generator
		for(Place place : rns.getPlace()){
			for(String connTo: place.getConnectedTo()){
				URI uri = this.performPostRelationship(place, connTo);
				this.relationships.add(uri); 
			}
		}
		
		this.loaded = true; 
	}
	
	

	/**
	 * Get all the places from the Rns random generator and save them into a Rns object
	 * @param rns The Rns object
	 * @throws ModelException 
	 */
	private void getPlaces(Rns rns) throws ModelException {
		
		try{
			Set<PlaceReader> places = this.monitor.getPlaces(null);
			for(PlaceReader place : places){
				Place p = this.ob.createPlace(); 
				p.setId(place.getId());
				for(PlaceReader next: place.getNextPlaces()){
					p.getConnectedTo().add(next.getId());
				}
				rns.getPlace().add(p); 
			}
		}catch (Exception e){
			throw new ModelException(); 
		}
	}


	/**
	 * Upload to the neo4j service the specified node
	 * @param place : is the place representing the node we want to upload
	 * @return location corresponding to the URI of the generated node
	 * @throws ServiceException 
	 */
	private URI performPostNode(Place place) throws ServiceException {
	
		System.out.println("\nPOST -> Creating Node for " + place.getId());
		Node n = this.ob.createNode(); 
	    n.setId(place.getId());
	    
	    //Perform POST  
	    try{
	    	Response response = target.path("data")
	    						.path("node")
	    						.request(MediaType.APPLICATION_JSON)
	    						.post(Entity.entity(n, MediaType.APPLICATION_JSON));
	    
	    	if(response.getStatus() == 201){
	    		System.out.println("POST Response <- 201 Created");
	    		URI location = response.getLocation();
	    		return location;
	    	}else{
	    		System.out.println("Post failed with status " + response.getStatus());
	    		throw new Exception();
	    	}	
	    } catch(RuntimeException re){
	    	throw new ServiceException("RuntimeException while creating Neo4j Node");
	    } catch(Exception e){
	    	throw new ServiceException("ServiceException while creating Neo4j Node");
	    }

	    
	}
	

	/**
	 * Upload to the neo4j service the relationship among two nodes 
	 * Node already exists because we have first validate model 
	 * @param place : is the place representing the starting point of the connection
	 * @param dest : is the string representing the destination point of the connection
	 * @return location of the URI of the generated relationship
	 * @throws ServiceException 
	 */
	private URI performPostRelationship(Place place, String dest) throws ServiceException {
		
		System.out.println("\nPOST -> Creating Relationship from: " + place.getId() + " to: " + dest);
		Connection co = this.ob.createConnection();
		co.setTo(this.mapNodes.get(dest).toString());
		co.setType("ConnectedTo");
	   
	    //Perform POST 
	    try{
	    	WebTarget wt = client.target(this.mapNodes.get(place.getId()));
	    	Response response = wt.path("relationships")
	    						.request(MediaType.APPLICATION_JSON)
	    						.post(Entity.entity(co, MediaType.APPLICATION_JSON));
	    	
	    	if(response.getStatus() == 201){
	    		System.out.println("POST Response <- 201 Created");
	    		URI location = response.getLocation();
	    		return location;
	    	}else{
	    		System.out.println("Post relationship failed with status " + response.getStatus());
	    		throw new Exception();
	    	}
	    }catch(RuntimeException re){
	    	throw new ServiceException("RuntimeException while creating Neo4j Relationship");
	    }catch(Exception e){
	    	throw new ServiceException("ServiceException while creating Neo4j Relationship");
	    }
		
	}

	
	/**
	 * Delete all the previously created relationships and nodes from the neo4j service
	 * First delete relationship and then node because it is not possible to delete
	 * nodes if there are existing relationships among them
	 * 
	 * @throws ServiceException 
	 */
	private void performDelete() throws ServiceException {
	    
		try{
			// Perform delete of the relationships 
			System.out.println("DELETE -> Deleting Relationships\n");
			for(URI uri : this.relationships){
				WebTarget wt = client.target(uri);
			    Response response = wt
			    					.request(MediaType.APPLICATION_JSON)
			    					.delete();	
			    
			    if(response.getStatus() != 204) {
			   		System.out.println("Deleted failed with status " + response.getStatus());
			   		throw new Exception(); 
			    }else{
			    	System.out.println("DELETE Response <- 204 No Content");
			    }
			}
			
			// Perform delete of the nodes
			System.out.println("DELETE -> Deleting Nodes\n");
			for(URI uri : this.mapNodes.values()){
				WebTarget wt = client.target(uri);
			    Response response = wt
			    					.request(MediaType.APPLICATION_JSON)
			    					.delete();	
			    
			    if(response.getStatus() != 204){
			   		System.out.println("Deleted failed with status "+response.getStatus());
			   		throw new Exception();
			   	}else {
			    	System.out.println("DELETE Response <- 204 No Content");
			   	}

			}
			return; 
		}catch(RuntimeException re){
	    	throw new ServiceException("RuntimeException while deleting graph from Neo4j");
	    }catch(Exception e){
			throw new ServiceException("ServiceException during Neo4j Graph Deleting");
		}	
	}


	/**
	 * Looks for the shortest paths connecting a source place to a destination place
	 * Each path is returned as a list of place identifiers, where the first place in the list is the source
	 * and the last place is the destination.
	 * @param source The id of the source of the paths to be found
	 * @param destination The id of the destination of the paths to be found
	 * @param maxlength The maximum length of the paths to be found (0 or negative means no bound on the length)
	 * @return the set of the shortest paths connecting source to destination
	 * @throws UnknownIdException if source or destination is not a known place identifier
	 * @throws BadStateException if the operation is called when in the initial state (no model loaded)
	 * @throws ServiceException if the operation cannot be completed because the remote service is not available or fails
	 */
	@Override
	public Set<List<String>> findShortestPaths(String source, String destination, int maxlength)
			throws UnknownIdException, BadStateException, ServiceException {
		
		//throw bad state exception if the model is not loaded yet
		if(!this.loaded){
			throw new BadStateException(); 
		}
		//throw UnknownIdException if the source is not a known place id
		if(!this.mapNodes.containsKey(source)){
			throw new UnknownIdException("Wrong source"); 
		}
		//thrown unknownIdException if the destination is not a known place id 
		if(!this.mapNodes.containsKey(destination)){
			throw new UnknownIdException("Wrong Destination");
		}
	
		//generate the entity to pass to the post request
		ShortestPath request = this.ob.createShortestPath();
		Relationships rel =  this.ob.createShortestPathRelationships();
		request.setRelationships(rel);
		String t = new String("ConnectedTo");
		request.getRelationships().setType(t);
		String d = new String("out");
		request.getRelationships().setDirection(d);
		request.setTo(this.mapNodes.get(destination).toString());
		String a = new String("shortestPath");
		request.setAlgorithm(a);
		if(maxlength>0){
			request.setMaxDepth(BigInteger.valueOf(maxlength));
		}else{				
			int maxPathDepth = this.mapNodes.size()-1; 
			request.setMaxDepth(BigInteger.valueOf(maxPathDepth));
		}
		/* DEBUG 
		System.out.println("From: " + this.mapNodes.get(source) );
		System.out.println("To: " + request.getTo()); 
		System.out.println("Relationship Type: " + request.getRelationships().getType()); 
		System.out.println("Relationship DIr: " + request.getRelationships().getDirection());
		System.out.println("Length: " + request.getMaxDepth()); 
		System.out.println("Algorithm " + request.getAlgorithm());
		*/
		
		//perform post request for shortest path
		try{
			System.out.println("POST -> Requesting Shortest Path");
			WebTarget wt = client.target(this.mapNodes.get(source)).path("paths");
	    	List<ShortestResult> paths = wt
	    			.request(MediaType.APPLICATION_JSON)
	    			.post(Entity.entity(request, MediaType.APPLICATION_JSON), new GenericType<List<ShortestResult>>() {});
	 
	    	if(paths == null){
    			System.out.println("Post shortest path failed");
	    		throw new ServiceException();
    		
	    	}else{
    			// If response is 200 OK
    			System.out.println("POST Response <- 200 OK Shortest Path Received");
	    		Set<List<String>> setList = new HashSet<List<String>>();
	    		for(ShortestResult result : paths){
	    			List<String> listPlaces = new ArrayList<String>();
	    			for(String str : result.getNodes()){
	    				URI uri = new URI(str);
	    				String id = this.mapURIs.get(uri);
	    				if(id == null){
	    					throw new UnknownIdException("Unknown URI received from server"); 
	    				}
	    				listPlaces.add(id);
	    			}
	    			setList.add(listPlaces);
	       		}
	    		paths.clear();
	    		return setList;
    		} 
	    	
	    } catch(UnknownIdException ie){
	    	System.out.println("UnknownID Exception");
	    	ie.printStackTrace();
	    	throw new UnknownIdException(ie.getMessage());
	    } catch(ProcessingException pe){
	    	System.out.println("Error during JAX-RS request processing");
			pe.printStackTrace();
			throw new ServiceException();
	    } catch(RuntimeException re){
			System.out.println("Runtime Exception");
	    	re.printStackTrace();
	    	throw new ServiceException();
	    } catch(ServiceException se){
	    	System.out.println("Service Exception");
	    	se.printStackTrace();
	    	throw new ServiceException();
	    } catch(Exception e){
	    	e.printStackTrace();
	    	throw new ServiceException("Exception while requesting Neo4j ShortesPath");
	    }	
	}

	
}
