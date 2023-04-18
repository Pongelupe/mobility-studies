package generator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import generator.configuration.PostgisConfig;
import generator.models.PontoRota;
import generator.models.RegistroViagem;
import generator.models.Viagem;
import generator.services.PostgisService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.postgis.jdbc.PGgeometry;
import net.postgis.jdbc.geometry.Point;

@Slf4j
public class App2 {
	
	private static final double DISTANCE_THRESHOLD = 0.0015d; // 150m
	

	@SneakyThrows
	public static void main(String... args) {
		var config = new PostgisConfig("jdbc:postgresql://localhost:15432/bh", "bh", "bh");
		var postgisService = new PostgisService(config.getConn());
		var rota = postgisService.queryAll("select st.stop_sequence, s.stop_id, ST_SetSRID(ST_MakePoint(ST_Y(s.the_geom),ST_X(s.the_geom)), 4326) from gtfs.stop_times st \n"
				+ "join gtfs.stops s on s.stop_id = st.stop_id \n"
				+ "where st.trip_id = '9202  031080602110' order by 1", rs -> {
					return PontoRota.builder()
					.sequenciaPonto(rs.getInt(1))
					.idPonto(rs.getString(2))
					.coord((Point) ((PGgeometry) rs.getObject(3)).getGeometry())
					.build();
				});
		
		
		var linhas = postgisService.queryAll("select distinct id_linha\n"
				+ "from bh.bh.onibus_tempo_real otr\n"
				+ "where id_linha = 629 and "
				+ "data_hora between '2022-12-16 6:00:00' and '2022-12-16 23:59:00'", rs -> rs.getInt(1));
		
		var viagens9202 = linhas
		.parallelStream()
		.map(idLinha -> 
			postgisService.queryAll("select data_hora, distancia_percorrida, coord, numero_ordem_veiculo, velocidade_instantanea, id_linha from bh.bh.onibus_tempo_real otr\n"
					+ "where id_linha = " + idLinha
					+ "\n and data_hora between '2022-12-16 6:00:00' and '2022-12-16 23:59:00'\n"
					+ "\n and numero_ordem_veiculo = 20246 "
					+ "order by numero_ordem_veiculo, data_hora", rs -> RegistroViagem.builder()
						.dataHora(rs.getTimestamp(1))
						.distanciaPercorrida(rs.getInt(2))
						.coord((Point) ((PGgeometry) rs.getObject(3)).getGeometry())
						.numeroOrdemVeiculo(rs.getInt(4))
						.velocidadeInstantanea(rs.getInt(5))
						.idLinha(rs.getInt(6))
					.build())
		)
		.map(registros ->
			registros
			.stream()
			.collect(Collectors.groupingBy(RegistroViagem::getNumeroOrdemVeiculo))
			.entrySet()
//			.parallelStream()
			.stream()
			.map(e -> getViagens(rota, e.getValue()))
			.sequential()
			.flatMap(List<Viagem>::stream)
			.filter(v -> v.isViagemCompleta() && v.getDistanciaPercorrida() > 3000) //TODO
			.collect(Collectors.toList())
		)
		.flatMap(List<Viagem>::stream)
		.collect(Collectors.toList());
		
		viagens9202.forEach(viagem -> {
			var pontoAtual = 0;
			var ponto = rota.get(pontoAtual);
			var registrosPonto = new ArrayList<RegistroViagem>();
			
			for (RegistroViagem r : viagem.getRegistros()) {
				var distancia = r.getCoord().distance(ponto.getCoord());
				
				if (distancia <= DISTANCE_THRESHOLD) { // onibus perto do ponto
					registrosPonto.add(r);
				} else if (!registrosPonto.isEmpty()) { // passou o ponto
					viagem.getPontos().add(PontoRota.builder()
							.sequenciaPonto(ponto.getSequenciaPonto())
							.idPonto(ponto.getIdPonto())
							.coord(ponto.getCoord())
							.registros(registrosPonto)
							.build());
					
					pontoAtual++;
					registrosPonto = new ArrayList<RegistroViagem>();
					ponto = rota.get(pontoAtual);
					
					distancia = r.getCoord().distance(ponto.getCoord());
					if (distancia <= DISTANCE_THRESHOLD) { // onibus perto do ponto
						registrosPonto.add(r);
					}
				}else {
					System.out.println("aaaaaaa");
				}
				
				
			}
			
			
		});
		
		viagens9202
			.forEach(v -> {
				log.info("**************");
				log.info("veiculo: {}", v.getPartida().getNumeroOrdemVeiculo());
				log.info("distanciaPercorrida: {}", v.getDistanciaPercorrida());
				log.info("viagem terminada: {}", v.isViagemCompleta());
				log.info("registros: {}", v.getRegistros().size());
				log.info("horarioPartida: {}", v.getPartida().getDataHora());
				log.info("horarioChegada: {}", v.isViagemCompleta() ? v.getChegada().getDataHora() : "N/A");
				log.info("**************");
			});
		
		
		config.close();
	}
	
	
	private static List<Viagem> getViagens(List<PontoRota> rota, List<RegistroViagem> registros) {
		var viagens = new ArrayList<Viagem>();
		var viagem = new Viagem();
		
		int distanciaPercorrida = 0;
		RegistroViagem partida = null;
		RegistroViagem chegada = null;
		RegistroViagem anterior = null;
		
		var pontoAtual = 0;
		var ponto = rota.get(pontoAtual);
		
		
		for (RegistroViagem registro : registros) {
			
			if (registro.getDistanciaPercorrida() >= distanciaPercorrida) {
				
				if (chegada != null) {
					// comecou uma nova viagem
					viagem.setPartida(partida);
					viagem.setChegada(chegada);
					viagens.add(viagem);
					
					
					viagem = new Viagem();
					distanciaPercorrida = 0;
					pontoAtual = 0;
					ponto = rota.get(pontoAtual);
					partida = null;
					chegada = null;
				} else {
					distanciaPercorrida = registro.getDistanciaPercorrida();
				}
				
				if (partida == null || 
						registro.getDistanciaPercorrida() == partida.getDistanciaPercorrida()) {
					partida = registro;
				}
				
				viagem.getRegistros().add(registro);
				
				if (registroPassandoPeloPonto(registro, ponto)) { // passando pelo ponto
					ponto.getRegistros().add(registro);
				} else if (!ponto.getRegistros().isEmpty() && pontoAtual < rota.size()) {
					viagem.getPontos().add(new PontoRota(ponto));
					ponto.getRegistros().clear();
					pontoAtual++;
					ponto = rota.get(pontoAtual);
				} 
				
			} else {
				// terminou uma rota
				chegada = anterior;
				distanciaPercorrida = registro.getDistanciaPercorrida();
				viagem.getRegistros().add(registro);
				viagem.getPontos().add(ponto);
			}
			
			anterior = registro;
		}
		
		viagem.setPartida(partida);
		viagem.setChegada(chegada);
		viagens.add(viagem);
		
		
		return viagens;
	}


	private static boolean registroPassandoPeloPonto(RegistroViagem registro, PontoRota ponto) {
		var coordRegistro = registro.getCoord();
		var coordPonto = ponto.getCoord();
		
		return coordRegistro.distance(coordPonto) <= DISTANCE_THRESHOLD;
	}

}
