package ltg.commons;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

public class SimpleRESTClient {
	private HttpTransport httpTransport = null;
	private HttpRequestFactory reqFactory = null;
	private ObjectMapper jsonParser = null;


	public SimpleRESTClient() {
		httpTransport = new NetHttpTransport();
		reqFactory = httpTransport.createRequestFactory();
		jsonParser = new ObjectMapper();
	}


	public JsonNode get(String encodedUrl) throws IOException {
		HttpRequest request = reqFactory.buildGetRequest(new GenericUrl(encodedUrl));		
		return jsonParser.readTree(request.execute().parseAsString());
	}
}
