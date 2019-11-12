# GreenVulcano VCL Adapter for MongoDB

This is the implementation of a GreenVulcano VCL adapter for the MongoDB database platform. It's meant to run as an Apache Karaf bundle.

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [GreenVulcano VCL Adapter for MongoDB](#greenvulcano-vcl-adapter-for-mongodb)
	- [Getting started](#getting-started)
		- [Prerequisites](#prerequisites)
		- [Installing the VCL adapter bundle in Apache Karaf](#installing-the-vcl-adapter-bundle-in-apache-karaf)
	- [Using the VCL adapter in your GreenVulcano project](#using-the-vcl-adapter-in-your-greenvulcano-project)
		- [Declaring the System-Channel-Operation for the MongoDB database](#declaring-the-system-channel-operation-for-the-mongodb-database)
		- [Sample usage](#sample-usage)

<!-- /TOC -->

## Getting started

### Prerequisites

First, you need to have installed Java Development Kit (JDK) 1.7 or above.

Then, you need to have installed Apache Maven (3.5.4 or higher) and Apache Karaf 4.1.5. Please refer to the following links for further reference:
- Apache Maven 3.5.4:
    - [Download](http://mirror.nohup.it/apache/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz)
    - [Installation steps](https://maven.apache.org/install.html)
- Apache Karaf 4.2.6:
    - [Download](http://www.apache.org/dyn/closer.lua/karaf/4.2.6/apache-karaf-4.2.6.tar.gz)
    - Installation steps: simply extract the Apache Karaf directory in any path you want. Verify that the Apache Karaf installation is operational by running the executable ```./bin/karaf``` from the Apache Karaf root directory (a Karaf shell should be displayed).

Next, you need to install the GreenVulcano engine on the Apache Karaf container. Please refer to [this link](https://github.com/kylan11/gv-engine/blob/master/quickstart-guide.md) for further reference.

In order to install the bundle in Apache Karaf to use it for a GreenVulcano application project, you need to install its dependencies. Open the Apache Karaf terminal by running the Karaf executable and type the following command:

```shell
karaf@root()> bundle:install -s wrap:mvn:org.mongodb/mongo-java-driver/3.6.3
```

In case of success, this command will print the ID of the installed bundle:

```shell
Bundle ID: n
```

Before installing the VCL adapter bundle, you have to start the mongo-java-driver bundle by specifying its ID. In the Apache Karaf terminal, type (replacing n with the bundle ID):

```shell
karaf@root()> start n
```

In case you can't find the bundle ID, just type:

```shell
karaf@root()> list | grep mongo-java-driver
```

The bundle ID will be the first field of the row that will appear in the terminal.

Once you started the bundle, use the ```list | grep mongo-java-driver``` command to make sure the bundle is in ```Active``` status.

Then, you need to install the VCL adapter bundle itself in Apache Karaf.

### Installing the VCL adapter bundle in Apache Karaf

Clone or download this repository on your computer, and then run ```mvn install``` in its root folder:

```shell
git clone https://github.com/kylan11/gv-adapter-mongodb
cd gv-adapter-mongodb
mvn install
```

In case of success, the ```mvn install``` command will install the VCL adapter bundle in the local Maven repository folder.  
After this operation, you have to add the Maven repository project as an Apache Karaf bundle, telling Karaf to load it after the GreenVulcano core bundles, since the VCL adapter requires the GreenVulcano bundles in order to start correctly.  
This constraint can be enforced by properly configuring the *level* of the Karaf bundle: the lower the level number, the earlier the bundle will be loaded by Karaf.

For the VCL adapter bundle, we will use a bundle level higher than the GreenVulcano core bundles (i.e. ```80```). The following command will install the VCL adapter bundle and set its level to ```96``` by convention, using the ```-l``` attribute:

```shell
karaf@root()> bundle:install -l 96 mvn:it.greenvulcano.gvesb.adapter/gvvcl-mongo-unofficial/4.1.0-SNAPSHOT

Bundle ID: x
```

Make sure that the bundle ```GreenVulcano ESB VCL interface for MongoDB (UNOFFICIAL)``` appears in the ```list``` of installed bundles in ```Installed``` status and with bundle level (```Lvl```) equal to ```96``` (or at least strictly higher than ```80```).  
Then, use its ID to put the bundle in ```Active``` status by executing the following command:

```shell
karaf@root()> start x

list | grep GreenVulcano ESB VCL interface for MongoDB
```

## Using the VCL adapter in your GreenVulcano project

In order to use the features of the MongoDB VCL adapter in your GreenVulcano project, you need to define a proper System-Channel-Operation set of nodes. You can do that by manually editing the GVCore.xml file, or by using DeveloperStudio. In the latter case, you will have to update your ```dtds``` folder in order to make use of the new elements added by the extension.

### Declaring the System-Channel-Operation for the MongoDB database

Let's assume you want to interact with a MongoDB database called ```documents``` hosted on ```192.168.10.10``` on port ```27017```.

### Declaring the System-Channel-Operation for the MongoDB database

Insert the ```<mongodb-call>``` and ```<mongodb-list-collections-call>``` XML nodes in the ```<Systems></Systems>``` section of file ```GVCore.xml```. Here's an example:

```xml
<System id-system="mongodb">
    <Channel id-channel="mongodb_db1" enabled="true" endpoint="mongodb://192.168.10.10:27017/"
        type="MongoDBAdapter">
        <mongodb-call type="call" name="query" database="documents" collection="test">
            <query>@{{QUERY}}</query>
        </mongodb-call>
        <mongodb-list-collections-call type="call" name="list-collections" database="documents">
        </mongodb-list-collections-call>
    </Channel>
</System>
```

Some constraints apply to these XML nodes.

- The ```<Channel>``` XML node must comply with the following syntax:
    - ```endpoint``` must contain a URI string correctly referencing the hostname and the port of an operational MongoDB server; refer to [this link](https://docs.mongodb.com/manual/reference/connection-string/) for further reference.
- The ```<mongodb-call>``` XML node must comply with the following syntax:
    - ```type``` must be declared and set equal to ```"call"```;
    - ```name``` must be declared: it defines the name of the Operation node;
    - ```database``` must be declared: it defines the name of the MongoDB database to query;
    - ```collection``` must be declared: it defines the name of the MongoDB collection of the specified database to query;
    - the ```<query>``` element must contain the query to perform against the specified database and collection; it must comply the MongoDB query syntax.
- The ```<mongodb-list-collections-call>``` XML node must comply with the following syntax:
    - ```type``` must be declared and set equal to ```"call"```;
    - ```name``` must be declared: it defines the name of the Operation node;
    - ```database``` must be declared: it defines the name of the MongoDB database to query.
- The ```<mongodb-script-call>``` XML node must comply with the following syntax:
    - ```type``` must be declared and set equal to ```"call"```;
    - ```name``` must be declared: it defines the name of the Operation node;
    - ```database``` must be declared: it defines the name of the MongoDB database to run the script on.


When we're done defining our System node, we can now use it in a Service-Operation, such as:
```xml
<Services>
    <Description>This section contains a list of all services provided by
        GreenVulcano ESB</Description>
    <Service group-name="DEFAULT_GROUP" id-service="MONGODB" service-activation="on"
                statistics="off">
        <Operation class="it.greenvulcano.gvesb.core.flow.GVFlowWF" name="List Collections"
                    operation-activation="on" out-check-type="none"
                    type="operation">
            <Flow first-node="list_coll_operation">
                <GVOperationNode class="it.greenvulcano.gvesb.core.flow.GVOperationNode"
                                    id="list_coll_operation" id-channel="mongodb_db1"
                                    id-system="mongodb"
                                    next-node-id="end_step" op-type="call"
                                    operation-name="list-collections"
                                    output="output_query" point-x="254" point-y="150"
                                    type="flow-node"/>
                <GVEndNode class="it.greenvulcano.gvesb.core.flow.GVEndNode"
                            id="end_step" op-type="end" output="output_query"
                            point-x="510" point-y="150" type="flow-node"/>
            </Flow>
        </Operation>
        <Operation class="it.greenvulcano.gvesb.core.flow.GVFlowWF" name="Query"
                    operation-activation="on" out-check-type="none"
                    type="operation">
            <Flow first-node="query_operation">
                <GVOperationNode class="it.greenvulcano.gvesb.core.flow.GVOperationNode"
                                    id="query_operation" id-channel="mongodb_db1"
                                    id-system="mongodb" input="input_query"
                                    next-node-id="end_step" op-type="call"
                                    operation-name="query"
                                    output="output_query" point-x="254" point-y="150"
                                    type="flow-node"/>
                <GVEndNode class="it.greenvulcano.gvesb.core.flow.GVEndNode"
                            id="end_step" op-type="end" output="output_query"
                            point-x="510" point-y="150" type="flow-node"/>
            </Flow>
        </Operation>
    </Service>
</Services>
```
You can test these Services selecting them in the Execute section of the GreenVulcano dashboard:

- **List Collections**: no input Body is needed; after a successful execution, the output window will display the list of the collections defined in the MongoDB database referred by the application properties;
- **Script Call**: here, in the "script" element, you can run a JS script to be executed in that database. There is no need to run loadServerScripts() function: it's done automatically. Assuming our function is called responseAggregationDaily, the synthax is as follows:
 ```js
    aggregateDailyStats(@{{JSON_INPUT}});
 ``` 
 **Use caution when executing properties or any variable code, as it can potentially be target of code injection attacks**;
- **Query**: a string representation of a JSON, which must comply to the MongoDB syntax. To clarify, let's assume that our previously defined collection contains a list of Employees. We want to find a specific employee named "Mark" who's 25 years old. Our JSON string will look like this:
    ```json
    {
    	"name": "Mark",
		"age": 25
    }
    ```
Don't worry about escaping **"** quotes. Since the string is parsed as a whole, there's no need to use **\\**.

## Version 1.1 Features

- Added 'count' attribute to DBOFind. Optional, disabled by default. If checked true, will only return a REC_READ property which represents the number of found documents that match the query. The REC_READ property is returned regardless; however it's very useful performance wise if you're only looking to retrieve statistics since it doesn't actually get all the information on the documents you're looking for but just counts them;

- For each MongoDB call, you can now define multiple DBOs (find, insert, aggregate, delete, update) to be executed consecutively as opposed to just one. In order to do that, you will notice a new attribute has been added for each DBO: the **call-order** (must be an integer number). In case you only have one DBO in your call, you can leave it empty. 
Otherwise, you MUST specify it to avoid unwanted issues (like, per say, if you wanted to perform a find and then insert the result in another collection, you wanna make sure the find operation is executed before the insert;

- Added mongodb-script-call, which lets you call stored functions on MongoDB and retrieve the result set in a dynamic fashion.


