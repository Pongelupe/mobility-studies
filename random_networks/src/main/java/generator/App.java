package generator;

import java.util.Date;
import java.util.List;

import generator.configuration.PostgisConfig;
import generator.models.RegistroViagem;
import generator.services.PostgisService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.postgis.jdbc.PGgeometry;
import net.postgis.jdbc.geometry.Point;

@Slf4j
public class App {

	@SneakyThrows
	public static void main(String... args) {
		var config = new PostgisConfig("jdbc:postgresql://localhost:15432/bh", "bh", "bh");
		var postgisService = new PostgisService(config.getConn());
		
		var registros = postgisService.queryAll("select data_hora, distancia_percorrida, coord, numero_ordem_veiculo, velocidade_instantanea from bh.bh.onibus_tempo_real otr\n"
				+ "where id_linha = 629\n"
				+ "and data_hora between '2022-12-16 11:00:00' and '2022-12-16 14:00:00'\n"
				+ "and numero_ordem_veiculo = 20246\n"
				+ "order by data_hora", rs -> RegistroViagem.builder()
					.dataHora(rs.getTimestamp(1))
					.distanciaPercorrida(rs.getInt(2))
					.coord((Point) ((PGgeometry) rs.getObject(3)).getGeometry())
					.numeroOrdemVeiculo(rs.getInt(4))
					.velocidadeInstantanea(rs.getInt(5))
				.build());
		
		var horarioPartida = getHorarioPartida(registros);
		var chegada = getRegistroChegada(registros);
		
		log.info("horarioPartida: {}", horarioPartida);
		log.info("horarioChegada: {}", chegada.getDataHora());
		log.info("distanciaPercorrida: {}", chegada.getDistanciaPercorrida());
		log.info("veiculo: {}", chegada.getNumeroOrdemVeiculo());
		
		config.close();
	}
	

	private static RegistroViagem getRegistroChegada(List<RegistroViagem> registros) {
		var seek = true;
		var registroViagem = registros.get(0);
		
		for (RegistroViagem r : registros) {
			if (seek && r.getDistanciaPercorrida() >= registroViagem.getDistanciaPercorrida()) {
				registroViagem = r;
			} else {
				seek = false;
			}
		}
		
		return registroViagem;
	}

	private static Date getHorarioPartida(List<RegistroViagem> registros) {
		var seek = true;
		var horarioPartida = registros.get(0).getDataHora();
		
		for (RegistroViagem registroViagem : registros) {
			if (registroViagem.getDistanciaPercorrida() == 0 && seek) {
				horarioPartida = registroViagem.getDataHora();
			} else {
				seek = false;
			}
		}
		
		return horarioPartida;
	}

}
