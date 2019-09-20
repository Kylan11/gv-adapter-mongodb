package it.greenvulcano.gvesb.virtual.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.gvesb.channel.mongodb.MongoDBChannel;
import it.greenvulcano.gvesb.virtual.*;
import it.greenvulcano.util.metadata.PropertiesHandler;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.w3c.dom.Node;

import java.util.NoSuchElementException;

public class MongoDBScriptCallOperation implements CallOperation {

	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBScriptCallOperation.class);

	private OperationKey key = null;

	private String name;

	private String database;

	private String js;

	private MongoClient mongoClient;

	private JSONObject jsonResult;

	private final BasicDBObject command = new BasicDBObject();

	@Override
	public void init(Node node) throws InitializationException {

		logger.debug("Initializing mongodb-script-call...");

		try {

			name = XMLConfig.get(node, "@name");

			mongoClient = MongoDBChannel.getMongoClient(node).orElseThrow(
					() -> new NoSuchElementException("MongoClient instance not foud for Operation " + name));

			database = XMLConfig.get(node, "@database");

			js = XMLConfig.get(node, "./script/text()", "{}");

			logger.debug("Initialization completed");

		} catch (Exception e) {

			throw new InitializationException("GV_INIT_SERVICE_ERROR", new String[][] { { "message", e.getMessage() } },
					e);

		}

	}

	@Override
	public GVBuffer perform(GVBuffer gvBuffer) throws ConnectionException, CallException, InvalidDataException {
		String result = "[]";
		try {

			String actualDatabase = PropertiesHandler.expand(database, gvBuffer);
			String codeJs = PropertiesHandler.expand(js, gvBuffer);

			MongoDatabase db = mongoClient.getDatabase(actualDatabase);

			command.put("eval", "function() { return db.loadServerScripts(); }");

			db.runCommand(command);

			command.put("eval", String.format("function() { return %s; }", codeJs));

			logger.debug("Executing code: " + codeJs + " on database: " + actualDatabase);
			result = db.runCommand(command).toJson();

			jsonResult = new JSONObject(result);
			
			logger.debug("Full response: " + jsonResult.toString(3));
			
			if (jsonResult.has("retval")) {
				
				//retval can be either an json object or a json array,
				//managing both possibilities here.
				
				try {
					jsonResult = jsonResult.getJSONObject("retval");
					result = jsonResult.toString();
				
				} catch (JSONException e) {
					result = jsonResult.getJSONArray("retval").toString(3);
				}

			}
			
			if (jsonResult.has("_batch")) {
				
				result = jsonResult.getJSONArray("_batch").toString(3);
			
			}
			
			gvBuffer.setObject(result);
			
		} catch (Exception exc) {
			throw new CallException("GV_CALL_SERVICE_ERROR",
					new String[][] { { "service", gvBuffer.getService() }, { "system", gvBuffer.getSystem() },
							{ "tid", gvBuffer.getId().toString() }, { "message", exc.getMessage() } },
					exc);

		}
		return gvBuffer;

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
