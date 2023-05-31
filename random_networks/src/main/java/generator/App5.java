package generator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
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
		
		var xGCompleted = mapper.readValue(new File("X_G.json"), 
				new TypeReference<Map<String, Map<String, Map<String, List<PontoRota>>>>>() {
		});
		
		var f = new SimpleDateFormat("yyyy-MM-dd");
		
		//idPonto -> [Map<Line, List<PontoRota>>]
		xGCompleted
			.entrySet()
			.stream()
			.sorted((o1, o2) -> parseDate(f, o1.getKey()).compareTo(parseDate(f, o2.getKey())) )
			.forEach(en -> {
				var weekday = en.getKey();
				var xG = en.getValue();
				var r = xG.get("00112364000410")
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
				log.info("Buses {} - {}", "00112364000410", weekday);
				r.forEach((line, entries) -> {
					entries
						.stream()
						.filter(d -> d.getDataHora().getHours() >= 10)
						.collect(Collectors.groupingBy(e -> e.getNumeroOrdemVeiculo()))
						.values()
						.stream()
						.flatMap(w -> w.stream())
						.sorted((o1, o2) -> o1.getDataHora().compareTo(o2.getDataHora()))
						.limit(3)
						.forEach(nextBus -> log.info("{} next arrival is {}", line, nextBus.getDataHora()))
						;
					});
				log.info("\n");
				});
	}
	
	@SneakyThrows
	private static Date parseDate(SimpleDateFormat f, String d) {
		return f.parse(d);
	}

}
