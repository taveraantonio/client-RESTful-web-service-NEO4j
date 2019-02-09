package it.polito.dp2.RNS.lab2.tests;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;


import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.polito.dp2.RNS.*;
import it.polito.dp2.RNS.lab2.BadStateException;
import it.polito.dp2.RNS.lab2.ModelException;
import it.polito.dp2.RNS.lab2.PathFinder;
import it.polito.dp2.RNS.lab2.PathFinderException;
import it.polito.dp2.RNS.lab2.PathFinderFactory;
import it.polito.dp2.RNS.lab2.ServiceException;
import it.polito.dp2.RNS.lab2.UnknownIdException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;


public class RNSTests {

	private static RnsReader referenceRnsReader;// reference data generator
	private static PathFinder testPathFinder;	// implementation under test
	private static TreeSet<GateReader> referenceInputGates;
	private static TreeSet<GateReader> referenceOutputGates;
	private static GateReader referenceInputGate=null;
	private static GateReader referenceOutputGate=null;
	private static long testcase;

	private static Client client;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Create reference data generator
		System.setProperty("it.polito.dp2.RNS.RnsReaderFactory", "it.polito.dp2.RNS.Random.RnsReaderFactoryImpl");
		referenceRnsReader = RnsReaderFactory.newInstance().newRnsReader();
		
		referenceInputGates = new TreeSet<GateReader> (new IdentifiedEntityReaderComparator());
		referenceInputGates.addAll(referenceRnsReader.getGates(GateType.IN));
		referenceInputGates.addAll(referenceRnsReader.getGates(GateType.INOUT));
		referenceInputGate = referenceInputGates.iterator().next();

		referenceOutputGates = new TreeSet<GateReader> (new IdentifiedEntityReaderComparator());
		referenceOutputGates.addAll(referenceRnsReader.getGates(GateType.OUT));
		referenceOutputGates.addAll(referenceRnsReader.getGates(GateType.INOUT));
		referenceOutputGate = referenceOutputGates.iterator().next();

//		// set referenceNFFGName and referenceNffgNodeSize
//		Set<NffgReader> referenceNffgs = referenceRnsReader.getNffgs(null);
//		if(referenceNffgs.size()!=0){
//			TreeSet<NffgReader> rts = new TreeSet<NffgReader>(new NamedEntityReaderComparator());
//			rts.addAll(referenceNffgs);
//			referenceNffgReader = rts.iterator().next();
//			referenceNodeReader = referenceNffgReader.getNodes().iterator().next();
//			referenceNffgName = referenceNffgReader.getName();
//			referenceNffgNodeSize = referenceNffgReader.getNodes().size();
//			referenceReachableHostReaders =  new TreeSet<HostReader>(new NamedEntityReaderComparator());
//			setReachableHosts(referenceNodeReader);
//
//			System.out.println("DEBUG: referenceNffgName: "+referenceNffgName);
//			System.out.println("DEBUG: referenceNffgNodeSize: "+referenceNffgNodeSize);
//		}
		
//		// set referenceHostNodeSize
//		
//		Set<HostReader> referenceHosts = new HashSet<>();
//		for (NodeReader nodeR : referenceNffgReader.getNodes()) {
//			referenceHosts.add(nodeR.getHost());
//		}
//		//Set<HostReader> referenceHosts = referenceRnsReader.getHosts();
//		referenceHostNodeSize = referenceHosts.size();		
//		System.out.println("DEBUG: referenceHostNodeSize: "+referenceHostNodeSize);

		// read testcase property
		Long testcaseObj = Long.getLong("it.polito.dp2.RNS.Random.testcase");
		if (testcaseObj == null)
			testcase = 0;
		else
			testcase = testcaseObj.longValue();

		client = ClientBuilder.newClient();
	}

//	private static void setReachableHosts(NodeReader nodeReader) {
//		Set<NodeReader> visitedNodes = new HashSet<NodeReader>();
//		visit(nodeReader, visitedNodes);
//	}
//
//	// recursively visits reachable nodes and collects reachable hosts
//	private static void visit(NodeReader nodeReader, Set<NodeReader> visitedNodes) {
//		if (!visitedNodes.contains(nodeReader)) {
//			visitedNodes.add(nodeReader);
//			HostReader hr = nodeReader.getHost();
//			if (hr!=null)
//				referenceReachableHostReaders.add(hr);
//			Set<LinkReader> links = nodeReader.getLinks();
//			for (LinkReader lr:links) {
//				visit(lr.getDestinationNode(), visitedNodes);
//			}
//		}	
//	}

	@Before
	public void setUp() throws Exception {
		assertNotNull(referenceInputGate);
		assertNotNull(referenceOutputGate);
	}

	private void createClient() throws PathFinderException {
		// Create client under test
		try {
			testPathFinder = PathFinderFactory.newInstance().newPathFinder();
		} catch (FactoryConfigurationError fce) {
			fce.printStackTrace();
		}
		assertNotNull("Internal tester error during test setup: null reference", referenceRnsReader);
		assertNotNull("Could not run test: the implementation under test generated a null PathFinder", testPathFinder);
	}

	@Test
	public final void testReloadModel() {
		System.out.println("DEBUG: starting testReloadModel");
		try {
			// create client under test
			createClient();
			
			// create additional client for tracking added nodes
			AuxiliaryTestClient ct = new AuxiliaryTestClient(client);
			
			// check initially model is not loaded
			assertEquals("Initially model should not be loaded", false, testPathFinder.isModelLoaded());
			
			// reload model
			testPathFinder.reloadModel();

			// check right number of nodes has been created
			assertEquals("Wrong number of nodes", referenceRnsReader.getPlaces(null).size(), ct.getAddedNodes());
			
			// check finally model is loaded
			assertEquals("Finally model should be loaded", true, testPathFinder.isModelLoaded());

		} catch (PathFinderException | AuxiliaryTestClientException | ServiceException | ModelException e) {
			fail("Unexpected exception thrown: "+e.getClass().getName());
		}
	}
	
	@Test
	public final void testFindShortestPaths() {
		System.out.println("DEBUG: starting testFindShortestPath");
		try {
			// create client under test
			createClient();
			
			// reload model
			testPathFinder.reloadModel();
			
			String source = referenceInputGate.getId();

			Iterator<GateReader> outGatesIt = referenceOutputGates.iterator();
			if(!outGatesIt.hasNext()) {
				System.out.println("Warning: no output gate, nothing to test");
				return;
			}

			boolean foundAtLeastOne = false;
			while (outGatesIt.hasNext()) {
				String destination = outGatesIt.next().getId();
				Set<List<String>> testPaths = testPathFinder.findShortestPaths(source, destination, 20);
				if (testPaths.size()>0) {
					foundAtLeastOne = true;
					for (List<String> testPath:testPaths) {
						checkPath(source, destination, testPath);
					}
				}
			}
			// check that at least one path has been found (this is guaranteed in the reference random generator)
			assertTrue("Wrong result of shortest paths search", foundAtLeastOne);
			
		} catch (PathFinderException | ServiceException | ModelException | UnknownIdException | BadStateException e) {
			fail("Unexpected exception thrown: "+e.getClass().getName());
		}
	}

	private void checkPath(String source, String destination, List<String> testPath) {
		assertTrue("Wrong number of elements in path", testPath.size()>=2);
		assertEquals("Wrong source in path", source, testPath.get(0));
		Iterator<String> listIt = testPath.iterator();
		String previous = listIt.next();
		while (listIt.hasNext()) {
			String id = listIt.next();
			assertNotNull("Wrong place name in path", referenceRnsReader.getPlace(previous));
			// check that id is the id of one of the next places of the previous place
			boolean found = false;
			for (PlaceReader p:referenceRnsReader.getPlace(previous).getNextPlaces()) {
				if (p.getId().equals(id)) {
					found=true;
					break;
				}
			}
			assertTrue("Wrong path returned",found);
			if (!listIt.hasNext()) // if is last
				assertEquals("Wong destination in path", destination, id);
			previous = id;
		}
	}

	@Test(expected = UnknownIdException.class)
	public final void testUnknownSource() throws PathFinderException, UnknownIdException, ServiceException, ModelException, BadStateException {
		System.out.println("DEBUG: starting testWrongReachability");
			// create client under test
			createClient();
			
			// reload model
			testPathFinder.reloadModel();

			// try to execute findShortestPaths using a wrong source id
			testPathFinder.findShortestPaths("Wrong", referenceOutputGate.getId() , 20);
	}

	@Test(expected = UnknownIdException.class)
	public final void testUnknownDestination() throws PathFinderException, UnknownIdException, ServiceException, ModelException, BadStateException {
		System.out.println("DEBUG: starting testWrongReachability");
			// create client under test
			createClient();
			
			// reload model
			testPathFinder.reloadModel();

			// try to execute findShortestPaths using a wrong destination id
			testPathFinder.findShortestPaths(referenceInputGate.getId(), "Wrong", 20);
	}

	@Test(expected = BadStateException.class)
	public final void testBadState() throws PathFinderException, UnknownIdException, BadStateException, ServiceException {
		System.out.println("DEBUG: starting testWrongLoad");
			// create client under test
			createClient();
			
			// try to execute findShortestPath before having loaded the model
			testPathFinder.findShortestPaths(referenceInputGate.getId(), referenceOutputGate.getId(), 20);			

	}

}

class IdentifiedEntityReaderComparator implements Comparator<IdentifiedEntityReader> {
    public int compare(IdentifiedEntityReader f0, IdentifiedEntityReader f1) {
    	return f0.getId().compareTo(f1.getId());
    }
}
