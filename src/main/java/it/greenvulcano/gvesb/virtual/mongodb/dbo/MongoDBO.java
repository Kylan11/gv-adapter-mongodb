package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mongodb.client.MongoCollection;

import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandlerException;

public abstract class MongoDBO {

	protected static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBO.class);

	public int callOrder = 0;

	public abstract String getDBOperationName();

	public abstract String execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer)
			throws PropertiesHandlerException, GVException;

	public boolean isValidJSON(String json) throws IllegalArgumentException {

		// first, check the parameters
		if (json == null) {

			String message = "Null reference mandatory parameter";

			logger.error(message);

			throw new IllegalArgumentException(message);

		}

		// first, determine whether the specified JSON string represents a single JSON
		// element
		try {
			new JSONObject(json);
		}

		// if an exception occurred, first determine whether the specified JSON string
		// represents a JSON array
		catch (JSONException e) {

			try {
				new JSONArray(json);
			}

			catch (JSONException e1) {
				return false;
			}

		}

		// the specified string is a valid JSON string
		return true;

	}

	public boolean isValidJSONArray(String json) throws IllegalArgumentException {

		// first, check the parameters
		if (json == null) {

			String message = "Null reference mandatory parameter";

			logger.error(message);

			throw new IllegalArgumentException(message);

		}

		// first, determine whether the specified JSON string represents a JSON array
		try {
			new JSONArray(json);
		}

		// if an exception occurred, then the specified string does not represent a JSON
		// array
		catch (JSONException e) {
			return false;
		}

		// the specified string is a valid JSON array
		return true;

	}

	public boolean isValidJSONObject(String json) throws IllegalArgumentException {

		// first, check the parameters
		if (json == null) {

			String message = "Null reference mandatory parameter";

			logger.error(message);

			throw new IllegalArgumentException(message);

		}

		// first, determine whether the specified JSON string represents a JSON object
		try {
			new JSONObject(json);
		}

		// if an exception occurred, then the specified string does not represent a JSON
		// object
		catch (JSONException e) {
			return false;
		}

		// the specified string is a valid JSON object
		return true;

	}

	// utility method to iterate over a NodeList
	public static Iterable<Node> iterable(final NodeList nodeList) {
		return () -> new Iterator<Node>() {

			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < nodeList.getLength();
			}

			@Override
			public Node next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return nodeList.item(index++);
			}
		};
	}

	// utility method to sort a MongoDBO list by callOrder
	public static Comparator<MongoDBO> sort() {
		return new Comparator<MongoDBO>() {
			public int compare(MongoDBO dbo1, MongoDBO dbo2) {
				return dbo1.callOrder - dbo2.callOrder;
			}
		};
	}
}