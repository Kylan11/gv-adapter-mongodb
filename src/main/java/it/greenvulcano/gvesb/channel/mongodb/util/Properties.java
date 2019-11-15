package it.greenvulcano.gvesb.channel.mongodb.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import it.greenvulcano.gvesb.channel.mongodb.exception.PropertyInitializationException;
import it.greenvulcano.gvesb.channel.mongodb.exception.PropertyNotFoundException;

public class Properties {

    private static java.util.Properties properties;

    static {

        // declare the input stream object used to read application.properties file
        InputStream input = null;

        try {

            // initialize the properties object
            properties = new java.util.Properties();

            // open the application.properties file in read mode
            input = new FileInputStream("src/main/resources/application.properties");

            // load the properties defined in the file into the properties object
            properties.load(input);

        }

        catch (FileNotFoundException e) {

            System.out.println("application.properties file not found");

        }

        catch (IOException e) {

            System.out.println("An I/O error occurred while processing application.properties file: " + e.getMessage());

        }

        // close the input stream, if it was opened
        finally {

            if (input != null) {

                try { input.close(); }

                catch (IOException e) { System.out.println("An error occurred while closing the application.properties input stream: " + e.getMessage()); }

            }

        }

    }



    public static String getPropertyValueAsString(String key) throws IllegalArgumentException, PropertyInitializationException, PropertyNotFoundException {

        if (properties == null) {

            throw new PropertyInitializationException();

        }

        if (key == null || key.length() == 0) {

            String message = "Null-reference parameter is not allowed";

            System.out.println(message);

            throw new IllegalArgumentException(message);

        }

        try {

            return properties.getProperty(key);

        } catch (Exception e) {

            System.out.println("An error occurred while fetching the value of property " + key + " from the properties: " + e.getMessage());

            throw new PropertyNotFoundException(key);

        }

    }
    
    public static String formatScriptResponse(JSONObject jsonResult) throws JSONException {
    	
    	if (jsonResult.has("retval")) {
			
			//retval can be either an json object or a json array,
			//managing both possibilities here.
			
			try {
				jsonResult = jsonResult.getJSONObject("retval");
				if(!jsonResult.has("_batch"))
					return jsonResult.toString();
			
			} catch (JSONException e) {
				return jsonResult.getJSONArray("retval").toString();
			}

		}
		
		if (jsonResult.has("_batch")) {
			
			if(jsonResult.getJSONArray("_batch").length() == 1) {
				return jsonResult.getJSONArray("_batch").get(0).toString();
			}
			else {
				return jsonResult.getJSONArray("_batch").toString();
			}
		}
    			
		return "[]";
    }
}
