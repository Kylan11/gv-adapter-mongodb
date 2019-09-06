package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.metadata.PropertiesHandlerException;
import org.bson.Document;
import org.w3c.dom.Node;

import java.util.Optional;
import java.util.function.Function;

public class MongoDBOFind extends MongoDBO {

	static final String NAME = "find";
	static final Function<Node, Optional<MongoDBO>> BUILDER = node -> {

		try {

			String query = XMLConfig.get(node, "./query/text()", "{}");
			String projection = XMLConfig.get(node, "./projection/text()", "{}");
			String sort = XMLConfig.get(node, "./sort/text()", "{}");
			
			String skip = XMLConfig.get(node, " @offset", "0");
			String limit = XMLConfig.get(node, "@limit", Integer.toString(Integer.MAX_VALUE));
			String isCount = XMLConfig.get(node, "@count", "false");
			String callOrder = XMLConfig.get(node, "@call-order", "0");
			
			return Optional.of(new MongoDBOFind(query, sort, projection, skip, limit, isCount, callOrder));

		} catch (Exception e) {

			return Optional.empty();

		}

	};
	private final String query;
	private final String sort;
	private final String projection;

	private final String skip;
	private final String limit;
	private final String isCount;

	MongoDBOFind(String query, String sort, String projection, String skip, String limit, String isCount, String callOrder) {
		this.query = query;
		this.sort = sort;
		this.projection = projection;
		this.skip = skip;
		this.limit = limit;
		this.isCount = isCount;
		this.callOrder = Integer.valueOf(callOrder);
	}

	@Override
	public String getDBOperationName() {
		return NAME;
	}

	@Override
	public String execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer)
			throws PropertiesHandlerException, GVException {

		// expand the content of children of find element from the GVBuffer
		String queryCommand = PropertiesHandler.expand(query, gvBuffer);
		String querySort = PropertiesHandler.expand(sort, gvBuffer);
		String queryProjection = PropertiesHandler.expand(projection, gvBuffer);
		
		// prepare the skip and limit parameters of the find element
		Integer querySkip = null;
		Integer queryLimit = null;
		boolean queryCount = false;

		try {

			// expand the the value of skip and limit parameters from the GVBuffer
			querySkip = Integer.valueOf(PropertiesHandler.expand(skip, gvBuffer));
			queryLimit = Integer.valueOf(PropertiesHandler.expand(limit, gvBuffer));
			queryCount = Boolean.valueOf(PropertiesHandler.expand(isCount, gvBuffer));
			
		} catch (NumberFormatException e) {

			// a non-integer value was found for either skip or limit parameter
			String exceptionMessage = "Non-integer parameter passed to <find> element: " + e.getCause();

			logger.error(exceptionMessage);

			throw new GVException(exceptionMessage);

		}

		Document commandDocument = Document.parse(queryCommand);
		Document sortDocument = Document.parse(querySort);
		Document projectionDocument = Document.parse(queryProjection);
		
		// if @count property is true it won't query the database, just return the row count as REC_READ
		
		if (queryCount) {
			logger.debug("Executing DBO Find (COUNT): {}", queryCommand);
			long count = mongoCollection.count(commandDocument);
			gvBuffer.setProperty("REC_READ", Long.toString(count));
			return Long.toString(count);
		} 
		
		// otherwise, query is executed in full
		else {
			
			logger.debug("Executing DBO Find: {}" + "; sort {}" + "; projection {}" + "; skip {}" + "; limit {}",
					queryCommand, querySort, queryProjection, querySkip, queryLimit);
			
			MongoCursor<String> resultSet = mongoCollection.find(commandDocument).projection(projectionDocument)
					.sort(sortDocument).skip(querySkip).limit(queryLimit).map(Document::toJson).iterator();
			int count = 0;
			StringBuilder jsonResult = new StringBuilder("[");
			while (resultSet.hasNext()) {
			
				count++;
				jsonResult.append(resultSet.next());
				if (resultSet.hasNext()) {
					jsonResult.append(",");
				} else {
					break;
				}
			}

			jsonResult.append("]");

			gvBuffer.setProperty("REC_READ", Integer.toString(count));
			//gvBuffer.setObject(jsonResult.toString());
			return jsonResult.toString();
		}
	}

}
