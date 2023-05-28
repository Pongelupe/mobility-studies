package generator;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import generator.App3.LineTripRecord;
import generator.App4.StopRecord;
import generator.models.PontoRota;
import generator.models.Viagem;
import lombok.SneakyThrows;

public class App4 {

	public record StopRecord(String line, PontoRota pontoRota) {}

	
	@SneakyThrows
	public static void main(String[] args) {
		
		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		
		
		Map<String, List<LineTripRecord>> tripsPerWeekdays = mapper.readValue(new File("tripsPerWeekdays.json"), 
				new TypeReference<Map<String, List<LineTripRecord>>>() {
		});
		
		
		tripsPerWeekdays.forEach((weekday, trips) -> {
			Map<Object, List<StopRecord>> xG = trips
				.stream()
				.collect(Collectors.toMap(t -> t.line(), t -> t.trips()
						.stream()
						.map(Viagem::getPontos)
						.flatMap(List<PontoRota>::stream)
						.map(e -> new StopRecord(t.line(), e))
						.collect(Collectors.groupingBy(t1 -> t1.pontoRota().getIdPonto())
						)))
				.entrySet()
				.stream()
				.map(Entry<String, Map<Object, List<StopRecord>>>::getValue)
				.reduce(new HashMap<>(), (acc, el) -> {
					acc.putAll(el);
					return acc;
				});
			
			// TODO merge ponto rota
			
			System.out.println(xG.size());
		});
		
	}
	
}
