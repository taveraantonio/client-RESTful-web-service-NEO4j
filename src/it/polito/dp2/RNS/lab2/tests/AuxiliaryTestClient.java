package it.polito.dp2.RNS.lab2.tests;

import java.io.StringReader;
import java.math.BigInteger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import it.polito.dp2.RNS.lab2.lib.jaxb.ObjectFactory;
import it.polito.dp2.RNS.lab2.lib.jaxb.Results;
import it.polito.dp2.RNS.lab2.lib.jaxb.Statement;
import it.polito.dp2.RNS.lab2.lib.jaxb.Statements;

public class AuxiliaryTestClient {

	private static String base_url="http://localhost:7474/db";
	Client client;
	
	int initialNumberOfNodes;

	public AuxiliaryTestClient() throws AuxiliaryTestClientException{
		init(ClientBuilder.newClient());
	}

	public AuxiliaryTestClient(Client client) throws AuxiliaryTestClientException {
		init(client);
	}
	
	private void init(Client client) throws AuxiliaryTestClientException {
		this.client=client;
//		base_url = System.getProperty("it.polito.dp2.RNS.lab2.URL");
		if (base_url==null)
			throw new AuxiliaryTestClientException("it.polito.dp2.RNS.lab2.URL property not set");
		else
			base_url = base_url+"/data";
		initialNumberOfNodes = getCurrentNumberOfNodes();		
	}

	private int getCurrentNumberOfNodes() throws AuxiliaryTestClientException {
		try {
//			String requestBody = "{\"statements\" : [ {\"statement\" : \"MATCH (n) RETURN count(*)\"} ]}";
			ObjectFactory of = new ObjectFactory();
	        Statements statements = of.createStatements();
	        Statement stat1=of.createStatement();
	        stat1.setStatement("MATCH (n) RETURN count(*)");
	        statements.getStatements().add(stat1);
			Results response =client.target(base_url)
					.path("transaction/commit").request(MediaType.APPLICATION_JSON).post(Entity.entity(statements,MediaType.APPLICATION_JSON),Results.class);
			BigInteger retval = response.getResults().get(0).getData().get(0).getRow().get(0);
//			System.out.println("Response: "+response); // to be used with String response
//			JsonReader jsonReader = Json.createReader(new StringReader(response));
//			JsonObject jobj = jsonReader.readObject();
//			System.out.println(jobj);
//			JsonValue data = jobj.get("results");
//			System.out.println(data);
//			int retval =0;
//			if (data instanceof JsonArray) {
//				System.out.println("JsonArray found");
//				JsonValue obj = ((JsonArray)data).get(0);
//				if (obj instanceof JsonObject) {
//					System.out.println("JsonObject found");
//					JsonValue arr = ((JsonObject)obj).get("row");
//					if (arr instanceof JsonArray) {
//						System.out.println("JsonArray found");
//						JsonValue val = ((JsonArray)arr).get(0);
//						if (val instanceof JsonNumber) {
//							System.out.println("JsonNumber found");
//							retval = Integer.valueOf(val.toString());
//						}
//					}
//				}
//			}
			//return Integer.valueOf(response);
			return retval.intValue();
		} catch (Exception e) {
			// e.printStackTrace(System.out);
			throw new AuxiliaryTestClientException("Unable to get current number of nodes from service");
		}
	}

	public int getAddedNodes() throws AuxiliaryTestClientException {
		return getCurrentNumberOfNodes()-initialNumberOfNodes;
	}
	
	public static void main(String[] args) throws AuxiliaryTestClientException {
		AuxiliaryTestClient client = new AuxiliaryTestClient();
		System.out.println(client.initialNumberOfNodes);
	}


}
