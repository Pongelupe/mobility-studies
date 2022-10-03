package pbhimporter.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import pbhimporter.model.BasePbhResponse;
import pbhimporter.resources.ResourcesPBH;

public class PBHService {

	private static final String BASE_RESOURCE_URL = "https://ckan.pbh.gov.br/api/3/action/datastore_search?resource_id=";
	
	@SuppressWarnings("unchecked")
	@SneakyThrows
	public <T> BasePbhResponse<T> getResource(ResourcesPBH resource) {
		var request = HttpRequest.newBuilder()
                .uri(new URI(BASE_RESOURCE_URL.concat(resource.getResourceId())))
                .GET()
                .build();

        var httpClient = HttpClient.newHttpClient();
        var response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return (BasePbhResponse<T>) mapper.readValue(response.body(), resource.getResponseTypeReference());
	}
	
}
