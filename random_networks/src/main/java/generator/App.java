package generator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import generator.configuration.PostgisConfig;
import generator.models.RegistroViagem;
import generator.models.Viagem;
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
		final var mapper = new ObjectMapper();
		
		var linhas = postgisService.queryAll("select distinct id_linha\n"
				+ "from bh.bh.onibus_tempo_real otr\n"
				+ "where "
				+ "data_hora between '2022-12-16 6:00:00' and '2022-12-16 23:59:00'", rs -> rs.getInt(1));
		
		linhas
		.parallelStream()
		.map(idLinha -> 
			postgisService.queryAll("select data_hora, distancia_percorrida, coord, numero_ordem_veiculo, velocidade_instantanea, id_linha from bh.bh.onibus_tempo_real otr\n"
					+ "where id_linha = " + idLinha
					+ "\n and data_hora between '2022-12-16 6:00:00' and '2022-12-16 23:59:00'\n"
					+ "order by numero_ordem_veiculo, data_hora", rs -> RegistroViagem.builder()
						.dataHora(rs.getTimestamp(1))
						.distanciaPercorrida(rs.getInt(2))
						.coord((Point) ((PGgeometry) rs.getObject(3)).getGeometry())
						.numeroOrdemVeiculo(rs.getInt(4))
						.velocidadeInstantanea(rs.getInt(5))
						.idLinha(rs.getInt(6))
					.build())
		)
		.forEach(registros ->
			registros
			.stream()
			.collect(Collectors.groupingBy(RegistroViagem::getNumeroOrdemVeiculo))
			.entrySet()
			.parallelStream()
			.map(e -> getViagens(e.getValue()))
			.sequential()
			.forEach(viagens ->
					viagens
						.forEach(v -> {
							System.out.println(writeViagemAsString(mapper, v));
//							log.info("**************");
//							log.info("veiculo: {}", v.getPartida().getNumeroOrdemVeiculo());
//							log.info("distanciaPercorrida: {}", v.getDistanciaPercorrida());
//							log.info("viagem terminada: {}", v.isViagemCompleta());
//							log.info("pontos: {}", v.getPontosRota().size());
//							log.info("horarioPartida: {}", v.getPartida().getDataHora());
//							log.info("horarioChegada: {}", v.isViagemCompleta() ? v.getChegada().getDataHora() : "N/A");
//							log.info("**************");
						})
			)
		);
		
		
		config.close();
	}
	
	@SneakyThrows
	private static synchronized String writeViagemAsString(ObjectMapper mapper, Viagem v) {
		return mapper.writeValueAsString(v) + ",";
	}
	
	private static List<Viagem> getViagens(List<RegistroViagem> registros) {
		var viagens = new ArrayList<Viagem>();
		var viagem = new Viagem();
		
		int distanciaPercorrida = 0;
		RegistroViagem partida = null;
		RegistroViagem chegada = null;
		RegistroViagem anterior = null;
		
		for (RegistroViagem registro : registros) {
			
			if (registro.getDistanciaPercorrida() >= distanciaPercorrida) {
				
				if (chegada != null) {
					// comecou uma nova viagem
					viagem.setPartida(partida);
					viagem.setChegada(chegada);
					viagens.add(viagem);
					
					
					viagem = new Viagem();
					distanciaPercorrida = 0;
					partida = null;
					chegada = null;
				} else {
					distanciaPercorrida = registro.getDistanciaPercorrida();
				}
				
				if (partida == null || 
						registro.getDistanciaPercorrida() == partida.getDistanciaPercorrida()) {
					partida = registro;
				}
				
				viagem.getPontosRota().add(registro);
			} else {
				chegada = anterior;
				distanciaPercorrida = registro.getDistanciaPercorrida();
				viagem.getPontosRota().add(registro);
			}
			
			anterior = registro;
		}
		
		viagem.setPartida(partida);
		viagem.setChegada(chegada);
		viagens.add(viagem);
		
		
		return viagens;
	}

}
