package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import java.util.Optional;
import java.util.function.Function;

import org.bson.BsonValue;
import org.bson.Document;
import org.w3c.dom.Node;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.metadata.PropertiesHandlerException;

public class MongoDBOUpdate extends MongoDBO {
	
	static final String NAME = "update";
	static final Function<Node, Optional<MongoDBO>> BUILDER = node ->{
		
		try {
		
			String filter = XMLConfig.get(node, "./filter/text()", "{}");
			String statement = XMLConfig.get(node, "./statement/text()");
			boolean upsert = XMLConfig.getBoolean(node, "@upsert", false);
			String callOrder = XMLConfig.get(node, "@call-order", "0");
			return Optional.of(new MongoDBOUpdate(filter, statement,  upsert, callOrder));
			
		} catch (Exception e) {
			
			return Optional.empty();
		}
		
	};	
	
	private final String filter;
	private final String statement;	
	private final boolean upsert;

	public MongoDBOUpdate(String filter, String statement, boolean upsert, String callOrder) {
		this.filter = filter;
		this.statement = statement;
		this.upsert = upsert;
		this.callOrder = Integer.valueOf(callOrder);
	}

	@Override
	public String getDBOperationName() {		
		return NAME;
	}

	@Override
	public String execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException {
		
		String actualFilter = PropertiesHandler.expand(filter, gvBuffer);
		String actualStatement = PropertiesHandler.expand(statement, gvBuffer);
		
		UpdateOptions updateOptions = new UpdateOptions();
		updateOptions.upsert(upsert);
		
		UpdateResult updateResult = mongoCollection.updateMany(Document.parse(actualFilter), Document.parse(actualStatement), updateOptions);
		
		gvBuffer.setProperty("REC_READ", Long.toString(updateResult.getMatchedCount()));
		gvBuffer.setProperty("REC_UPDATE", Long.toString(updateResult.getModifiedCount()));

		if (upsert) {
			Optional.ofNullable(updateResult.getUpsertedId())
			        .map(BsonValue::asObjectId)
			        .ifPresent(id->{
						try {
							gvBuffer.setProperty("REC_IDS", id.toString());
						} catch (GVException e) {}
					});
			
		}
		return "";
	}

}
