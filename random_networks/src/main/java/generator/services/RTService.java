package generator.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import generator.models.RegistroViagem;
import lombok.RequiredArgsConstructor;
import net.postgis.jdbc.PGgeometry;
import net.postgis.jdbc.geometry.Point;

@RequiredArgsConstructor
public class RTService {
	
	private static final BiFunction<Integer, String, String> QUERY__ENTRIES_BUSLINE = """
			select data_hora, distancia_percorrida, coord, numero_ordem_veiculo, velocidade_instantanea, id_linha from bh.bh.onibus_tempo_real otr 
			where id_linha = %1$d 
			and data_hora between '%2$s 6:00:00' and '%2$s 23:59:00' 
			order by numero_ordem_veiculo, data_hora
			"""::formatted;
	
	private static final BinaryOperator<String> QUERY_LINES = """
			select id_linha from bh.bh.onibus_tempo_real otr 
			where id_linha in (select id_linha  from linha_onibus otr where codigo_linha like '%1$s%%')
			and data_hora between '%2$s 6:00:00' and '%2$s 23:59:00'
			"""::formatted;

	private final QueryExecutor queryExecutor;
		
	public Map<Integer, Map<Integer, List<RegistroViagem>>> getEntriesbyBusLine(String headSign, String date) {
		return queryExecutor.queryAllSet(QUERY_LINES.apply(headSign, date), rs -> rs.getInt(1))
			.stream()
			.map(line -> 
			Map.of(line, queryExecutor.queryAll(QUERY__ENTRIES_BUSLINE.apply(line, date), rs -> RegistroViagem.builder()
						.dataHora(rs.getTimestamp(1))
						.distanciaPercorrida(rs.getInt(2))
						.coord((Point) ((PGgeometry) rs.getObject(3)).getGeometry())
						.numeroOrdemVeiculo(rs.getInt(4))
						.velocidadeInstantanea(rs.getInt(5))
						.idLinha(rs.getInt(6))
					.build())
					.stream()
					.collect(Collectors.groupingBy(RegistroViagem::getNumeroOrdemVeiculo))))
			.reduce(new HashMap<>(), (acc, el) -> {
				acc.putAll(new HashMap<>(el));
				return acc;
			});
	}
	
}
