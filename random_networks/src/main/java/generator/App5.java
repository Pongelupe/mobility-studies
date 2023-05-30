package generator;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import generator.models.PontoRota;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App5 {

	@SneakyThrows
	public static void main(String[] args) {

		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		
		//idPonto -> [Map<Line, List<PontoRota>>]
		var xG = mapper.readValue(new File("X_G_2022-12-16.json"), 
				new TypeReference<Map<String, Map<String, List<PontoRota>>>>() {
		});
		
		var r = xG.get("00100314000686")
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Entry<String, List<PontoRota>>::getKey,
					e -> e.getValue()
						.stream()
						.map(w -> w.getRegistros())
						.flatMap(w -> w.stream())
						.sorted((o1, o2) -> o1.getDataHora().compareTo(o2.getDataHora()))
						.toList()
					)
					);
		
		r.forEach((line, entries) -> {
			var nextBus = entries
				.stream()
				.filter(d -> d.getDataHora().getHours() >= 10)
				.collect(Collectors.groupingBy(e -> e.getNumeroOrdemVeiculo()))
				.values()
				.stream()
				.flatMap(w -> w.stream())
				.min((o1, o2) -> o1.getDataHora().compareTo(o2.getDataHora()))
				.orElseThrow();
			
			log.info("{} next arrival is {}", line, nextBus.getDataHora());
			
		});
		
	}

}
