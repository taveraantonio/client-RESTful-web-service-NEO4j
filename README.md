# Client for RESTful Web Service of Neo4j Graph Oriented DB
The developed code consists of a client for the RESTful web service of the Neo4J graph-oriented DB, using the JAX-RS framework.
This code has been developed for the Distributed Programming II course held at the Politecnico di Torino

## How It Works

The client is able to: a) read the information about a set of places and about their connections, from the random generator already used in "road navigation system serialization and java library"; b) load the graph of these places with their connections into NEO4J by means of the Neo4J REST API; c) answer queries about shortest paths between places, by properly getting this information from the REST API as specified below.

The places are loaded into NEO4J as follows: a graph node has to be created for each place, with a property named “id” with the value that is the place id; a relationship is created for each connection, connecting the nodes of the corresponding places, with type “ConnectedTo”. Sections 21.8 and 21.9 of the Neo4j documentation explain how to create and read nodes and relationships while section 21.18 explains how to use the shortest path algorithm available in NEO4J.

The client classes are robust and portable, without dependencies on locales. However, these classes are meant for single-thread use only, which means there cannot be concurrent calls to the methods of these classes. When using the client, consider that the operation that finds paths can be time-consuming.

Before running the tests you must have started the NEO4J server by running the following ant command:
```
ant start-neo4j
```
Then, you can run the tests using the runFuncTest target, which also accepts the –Dseed and –Dtestcase options for controlling the random generation of data. The results of the junit tests can be displayed graphically by double clicking on the testout.xml file in Eclipse:
```
ant -Dseed=29289 -Dtestcase=1 runFuncTest
```
