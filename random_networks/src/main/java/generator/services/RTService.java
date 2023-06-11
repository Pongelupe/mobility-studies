package generator.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import generator.models.RegistroViagem;
import lombok.RequiredArgsConstructor;
import net.postgis.jdbc.PGgeometry;
import net.postgis.jdbc.geometry.Point;

@RequiredArgsConstructor
public class RTService {
	
	private static final BiFunction<Integer, String, String> QUERY__ENTRIES_BUSLINE = """
			select data_hora, distancia_percorrida, coord, numero_ordem_veiculo, velocidade_instantanea, id_linha from bh.bh.onibus_tempo_real otr 
			where id_linha = %1$d and numero_ordem_veiculo = 20887
			and data_hora between '%2$s 00:00:00' and '%2$s 3:00:00' 
			order by numero_ordem_veiculo, data_hora
			"""::formatted;
	
	private static final BiFunction<String, String, String> QUERY2__ENTRIES_BUSLINE = """
			with linhas as (select replace(codigo_linha, '-', '  ') as route_id, id_linha 
			from bh.bh.linha_onibus lo where codigo_linha like '%s%%')
			select distinct data_hora, distancia_percorrida, coord, numero_ordem_veiculo, velocidade_instantanea, l.id_linha from bh.bh.onibus_tempo_real otr 
			join linhas l on l.id_linha = otr.id_linha
			and data_hora between '%2$s 00:00:00' and '%2$s 23:59:00' 
			order by numero_ordem_veiculo, data_hora
			"""::formatted;
	
	private static final UnaryOperator<String> QUERY_LINES = """
			with linhas as ( select replace(codigo_linha, '-', '  ') as route_id, id_linha 
			from bh.bh.linha_onibus lo where codigo_linha like '%s%%')
			select id_linha from linhas l
			join gtfs.routes r on r.route_id = l.route_id"""::formatted;

	private final QueryExecutor queryExecutor;
		
	public Map<Integer, Map<Integer, List<RegistroViagem>>> getEntriesbyBusLine(String headSign, String date) {
		return queryExecutor.queryAllSet(QUERY_LINES.apply(headSign), rs -> rs.getInt(1))
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
	
	public Map<Integer, List<RegistroViagem>> getEntriesbyBusLine2(String headSign, String date) {
		return queryExecutor.queryAll(QUERY2__ENTRIES_BUSLINE.apply(headSign, date), rs -> RegistroViagem.builder()
					.dataHora(rs.getTimestamp(1))
					.distanciaPercorrida(rs.getInt(2))
					.coord((Point) ((PGgeometry) rs.getObject(3)).getGeometry())
					.numeroOrdemVeiculo(rs.getInt(4))
					.velocidadeInstantanea(rs.getInt(5))
					.idLinha(rs.getInt(6))
				.build())
				.stream()
				.collect(Collectors.groupingBy(RegistroViagem::getNumeroOrdemVeiculo));
	}
	
}
