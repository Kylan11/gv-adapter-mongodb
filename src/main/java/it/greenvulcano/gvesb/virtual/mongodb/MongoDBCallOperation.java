package it.greenvulcano.gvesb.virtual.mongodb;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.channel.mongodb.MongoDBChannel;
import it.greenvulcano.gvesb.virtual.*;
import it.greenvulcano.gvesb.virtual.mongodb.dbo.MongoDBO;
import it.greenvulcano.gvesb.virtual.mongodb.dbo.MongoDBOFactory;
import it.greenvulcano.util.metadata.PropertiesHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;

public class MongoDBCallOperation implements CallOperation {

	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBCallOperation.class);

	private OperationKey key = null;

	private String name;

	private String database;

	private String collection;

	private MongoClient mongoClient;

	private List<MongoDBO> dboList;

	private NodeList nodeList;

	private String rowsetBuilder;

	@Override
	public void init(Node node) throws InitializationException {

		logger.debug("Initializing mongodb-call...");

		try {

			name = XMLConfig.get(node, "@name");

			mongoClient = MongoDBChannel.getMongoClient(node).orElseThrow(
					() -> new NoSuchElementException("MongoClient instance not found for Operation " + name));

			database = XMLConfig.get(node, "@database");

			collection = XMLConfig.get(node, "@collection");

			rowsetBuilder = XMLConfig.get(node, "@rowset-builder", "json");

			nodeList = XMLConfig.getNodeList(node, "./*"); // extract children elements

			dboList = new ArrayList<MongoDBO>();

			// retrieves all the child nodes and puts them in a list to execute them in a
			// pipeline
			for (Node callNode : MongoDBO.iterable(nodeList))
				dboList.add(MongoDBOFactory.build(callNode));

			// sorts the list by call-order so operations are executed in the intended order
			dboList.sort(MongoDBO.sort());

			logger.debug("MongoDBO operations correctly configured: " + dboList.size());

		} catch (Exception e) {

			throw new InitializationException("GV_INIT_SERVICE_ERROR", new String[][] { { "message", e.getMessage() } },
					e);

		}

	}

	@Override
	public GVBuffer perform(GVBuffer gvBuffer) throws ConnectionException, CallException, InvalidDataException {

		try {

			String actualDatabase = PropertiesHandler.expand(database, gvBuffer);
			String actualCollection = PropertiesHandler.expand(collection, gvBuffer);

			MongoCollection<Document> mongoCollection = mongoClient.getDatabase(actualDatabase)
					.getCollection(actualCollection);

			for (MongoDBO dbo : dboList) {

				logger.debug("Preparing MongoDB operation " + dbo.getDBOperationName() + "  on database: "
						+ actualDatabase + " collection: " + actualCollection);

				// this way, every operation will get the input from the previous one in a
				// pipeline fashion
				// only the last result will persist in gvBuffer
				String resultSet = dbo.execute(mongoCollection, gvBuffer); // this is the actual operation call
				if (!resultSet.equals("")) {
					gvBuffer.setObject(buildResult(resultSet, rowsetBuilder));
				}
			}

		} catch (Exception exc) {
			throw new CallException("GV_CALL_SERVICE_ERROR",
					new String[][] { { "service", gvBuffer.getService() }, { "system", gvBuffer.getSystem() },
							{ "tid", gvBuffer.getId().toString() }, { "message", exc.getMessage() } },
					exc);
		}
		return gvBuffer;
	}

	public String buildResult(String resultSet, String rowsetBuilder) {
		if (rowsetBuilder.contentEquals("xml")) {
			Object output = null;
			try {
				output = new JSONObject(resultSet);
			} catch (JSONException e) {
				output = new JSONArray(resultSet);
			}
			return XML.toString(output, "Document");
		}
		return resultSet;
	}

	@Override
	public void cleanUp() {
		// do nothing
	}

	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public void setKey(OperationKey operationKey) {
		this.key = operationKey;
	}

	@Override
	public OperationKey getKey() {
		return key;
	}

	@Override
	public String getServiceAlias(GVBuffer gvBuffer) {
		return gvBuffer.getService();
	}

}
