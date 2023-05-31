package generator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import generator.App3.LineTripRecord;
import generator.models.PontoRota;
import generator.models.Viagem;
import lombok.SneakyThrows;

public class App4 {

	
	@SneakyThrows
	public static void main(String[] args) {
		
		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		
		
		Map<String, List<LineTripRecord>> tripsPerWeekdays = mapper.readValue(new File("tripsPerWeekdays.json"), 
				new TypeReference<Map<String, List<LineTripRecord>>>() {
		});
		
		
		var xGComplete = tripsPerWeekdays
		.entrySet()
		.parallelStream()
		.collect(Collectors.toMap(Entry<String, List<LineTripRecord>>::getKey, 
				entry -> {
					var weekday = entry.getKey();
					var trips = entry.getValue();
					
					//rotasPorOnibus
					var stopsByBusLine = trips.parallelStream()
							.collect(Collectors.toMap(t -> t.line(), t -> t.trips()
							.parallelStream()
							.map(Viagem::getPontos)
							.flatMap(List<PontoRota>::stream)
							.toList()))
							;
					
					// idPonto -> [Map<Line, List<PontoRota>>]
					var xG = trips.stream()
						.map(LineTripRecord::trips)
						.flatMap(List<Viagem>::stream)
						.map(Viagem::getPontos)
						.flatMap(List<PontoRota>::stream)
						.map(PontoRota::getIdPonto)
						.collect(Collectors.toSet())
						.parallelStream()
						.collect(Collectors.toMap(Function.identity(), 
								p -> stopsByBusLine.entrySet()
									.parallelStream()
									.collect(Collectors.toMap(Entry<String, List<PontoRota>>::getKey, 
											e -> e.getValue()
											.parallelStream()
											.filter(p1 -> p1.getIdPonto().equals(p))
											.toList()))
									.entrySet()
									.stream()
									.filter(e -> !e.getValue().isEmpty())
									.collect(Collectors.toMap(Entry<String, List<PontoRota>>::getKey, 
											Entry<String, List<PontoRota>>::getValue))
								)
								);
					
					try {
						mapper.writeValue(new File("./X_G_" + weekday + ".json"), xG);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					return xG;
					
				}
				));
		
		mapper.writeValue(new File("./X_G.json"), xGComplete);
		
	}
	
}
