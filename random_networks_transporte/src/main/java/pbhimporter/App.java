package pbhimporter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import pbhimporter.resources.ResourcesPBH;

public class App {

	@SuppressWarnings("unchecked")
	@SneakyThrows
	public static void main(String... args) {
		var resourcePBH = ResourcesPBH.valueOf(args[0]);
		
		var url = "https://ckan.pbh.gov.br/api/3/action/datastore_search?resource_id=".concat(resourcePBH.getResourceId());
        var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();

        var httpClient = HttpClient.newHttpClient();
        var response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
         System.out.println(new ObjectMapper().readValue(response.body(), resourcePBH.getResponseTypeReference()));
	}

}
