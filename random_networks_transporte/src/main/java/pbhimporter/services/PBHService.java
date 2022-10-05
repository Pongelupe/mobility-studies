package pbhimporter.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import pbhimporter.model.BasePbhResponse;
import pbhimporter.resources.ResourcesPBH;

@RequiredArgsConstructor
public class PBHService {
	
	private static final String RESOURCE_ID = "{{RESOURCE_ID}}";

	private static final String OFFSET = "{{OFFSET}}";
	
	private static final BigDecimal MAX_PAGE_SIZE = BigDecimal.valueOf(100);
	
	private final ObjectMapper mapper;

	private static final String BASE_RESOURCE_URL = "https://ckan.pbh.gov.br/api/3/action/datastore_search?resource_id=" + RESOURCE_ID + "&offset=" + OFFSET;
	
	
	@SuppressWarnings("unchecked")
	public <T> BasePbhResponse<T> getResource(ResourcesPBH resource) {
		var primeiraRequest = (BasePbhResponse<T>) doRequest(resource, 0);
		
		var total = BigDecimal.valueOf(primeiraRequest.getResult().getTotal());
		int totalInterations = total.divide(MAX_PAGE_SIZE, RoundingMode.UP).intValue();
		
		System.out.println("Buscando " + total + " registros em " + totalInterations + " interacoes");
		
		for (int i = 1; i <= totalInterations; i++) {
			var currentOffset = i * MAX_PAGE_SIZE.intValue();
			
			System.out.println("Loading resource " + resource.name() + " (" + i * 100 / totalInterations + "%)");
			var request = (BasePbhResponse<T>) doRequest(resource, currentOffset);
			primeiraRequest.getResult().getRecords().addAll(request.getResult().getRecords());
		}
		
		return primeiraRequest;
	}
	
	@SuppressWarnings("unchecked")
	@SneakyThrows
	private <T> BasePbhResponse<T> doRequest(ResourcesPBH resource, int offset) {
		var request = HttpRequest.newBuilder()
                .uri(new URI(BASE_RESOURCE_URL
                		.replace(RESOURCE_ID, resource.getResourceId())
                		.replace(OFFSET, String.valueOf(offset))))
                .GET()
                .build();

        var httpClient = HttpClient.newHttpClient();
        var response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        return (BasePbhResponse<T>) mapper.readValue(response.body(), resource.getResponseTypeReference());
	}
	
}
