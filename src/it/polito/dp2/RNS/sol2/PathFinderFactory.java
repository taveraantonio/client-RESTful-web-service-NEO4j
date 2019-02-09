package it.polito.dp2.RNS.sol2;

import it.polito.dp2.RNS.lab2.PathFinder;
import it.polito.dp2.RNS.lab2.PathFinderException;
import it.polito.dp2.RNS.lab2.ServiceException;

public class PathFinderFactory extends it.polito.dp2.RNS.lab2.PathFinderFactory {

	@Override
	public PathFinder newPathFinder() throws PathFinderException {
		
		SolPathFinder pathfinder = null;
		
		try{
			pathfinder = new SolPathFinder();
			
		}catch(ServiceException se){
			System.out.println("Error while creating SolPathFinder");
			throw new PathFinderException();
		}catch(Exception e){
			System.out.println("Catched Exception");
			throw new PathFinderException(); 
		}
		
		return pathfinder;
	}

}
