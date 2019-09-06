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
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MongoDBOAggregate extends MongoDBO {

	static final String NAME = "aggregate";
	static final Function<Node, Optional<MongoDBO>> BUILDER = node -> {

		try {

			NodeList stagesConfig = XMLConfig.getNodeList(node, "./stage");

			List<String> stages = IntStream.range(0, stagesConfig.getLength()).mapToObj(stagesConfig::item)
					.map(Node::getTextContent).collect(Collectors.toList());
			String callOrder = XMLConfig.get(node, "@call-order", "0");
			return Optional.of(new MongoDBOAggregate(stages, callOrder));

		} catch (Exception e) {

			return Optional.empty();

		}

	};

	private final List<String> stages;

	public MongoDBOAggregate(List<String> stages, String callOrder) {
		this.callOrder = Integer.valueOf(callOrder);
		this.stages = stages;

	}

	@Override
	public String getDBOperationName() {
		return NAME;
	}

	@Override
	public String execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer)
			throws PropertiesHandlerException, GVException {

		List<Document> stagesBson = new ArrayList<>();
		for (String s : stages) {

			try {

				String statement = PropertiesHandler.expand(s, gvBuffer);

				logger.debug("Adding stage to aggregation: {}", statement);
				stagesBson.add(Document.parse(statement));
			} catch (IllegalArgumentException e) {

				logger.error("Error adding stage to aggregation, invalid parameter", e);
				return "";

			} catch (Exception e) {

				logger.error("Error adding stage to aggregation", e);

				throw new GVException("Error adding stage to aggregation" + e.getClass().getName());

			}
		}
		MongoCursor<String> resultSet = mongoCollection.aggregate(stagesBson).map(Document::toJson).iterator();

		StringBuilder jsonResult = new StringBuilder("[");

		int count = 0;
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
		return jsonResult.toString();
	}

}
