package generator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongBiFunction;
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
	
	private static final double DISTANCE_THRESHOLD = 0.0005d; // 50m
	

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
			.map(e -> getViagens(e.getValue()))
			.sequential()
			.flatMap(List<Viagem>::stream)
			.filter(v -> v.isViagemCompleta() && v.getDistanciaPercorrida() > 3000) //TODO
			.collect(Collectors.toList())
		)
		.flatMap(List<Viagem>::stream)
		.collect(Collectors.toList());
		
		viagens9202
			.forEach(viagem -> associarPontos(viagem, rota));
		
		//TODO importar no qgis
		
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
	
	private static void associarPontos(Viagem viagem, List<PontoRota> rota) {
		var registroAtual = 0;
		
		for (var ponto : rota) {
			
			var menorMaiorDistancia = 0d;
			var procurando = true;
			
			for (var i = registroAtual; i < viagem.getRegistros().size() - 1; i++) {
				var registro = viagem.getRegistros().get(i);
				var distancia = registro.getCoord().distance(ponto.getCoord());
				
				log.debug("{} - {}", i, registro.getDataHora() + " - " +  distancia);
				
				if (distancia <= DISTANCE_THRESHOLD) {
					ponto.getRegistros().add(registro);
					registroAtual = i;
				} else if (!ponto.getRegistros().isEmpty()) {
					registroAtual = i --;
					i = viagem.getRegistros().size();
				} else if (menorMaiorDistancia <= distancia && procurando) {
					menorMaiorDistancia = distancia;
					registroAtual++;
				} else if (menorMaiorDistancia > distancia) {
					procurando = false;
				}
				
			}
			
			if (ponto.getRegistros().isEmpty()) {
				ponto.setCalculated(true);
				ponto.getRegistros().add(mergeRegistros(viagem.getRegistros(), registroAtual));
			}
			
			log.info("{} registros para a sequencia {}", 
					ponto.getRegistros().size(), ponto.getSequenciaPonto());
			viagem.getPontos().add(new PontoRota(ponto));
		}
		
		rota.forEach(p -> {
			p.setCalculated(false);
			p.getRegistros().clear();
		});
		
	}
	
	
	private static RegistroViagem mergeRegistros(List<RegistroViagem> registros, int countRegistro) {
		var registroAtual = registros.get(countRegistro);
		var registroSeguinte = registros
				.stream()
				.filter(r -> r.getDataHora().after(registroAtual.getDataHora()))
				.findFirst()
				.orElse(null);
		
		IntBinaryOperator avg = (o1, o2) -> (o1 + o2) / 2;
		
		ToDoubleFunction<RegistroViagem> getXCoord = o1 -> o1.getCoord().getX();
		ToDoubleFunction<RegistroViagem> getYCoord = o1 -> o1.getCoord().getY();
		DoubleBinaryOperator avgDouble = (o1, o2) -> (o1 + o2) / 2;
		ToLongBiFunction<Date, Date> avgDate = (o1, o2) -> (o1.getTime() + o2.getTime()) / 2;
		
		log.info(countRegistro + " merge registro {}, {}", registroAtual,
				registroSeguinte);
		
		return Optional.ofNullable(registroSeguinte)
				.map(r -> RegistroViagem
						.builder()
						.distanciaPercorrida(avg.applyAsInt(registroAtual.getDistanciaPercorrida(), 
								registroSeguinte.getDistanciaPercorrida()))
						.dataHora(new Date(avgDate.applyAsLong(registroAtual.getDataHora(), registroSeguinte.getDataHora())))
						.coord(new Point(avgDouble.applyAsDouble(getXCoord.applyAsDouble(registroAtual), getXCoord.applyAsDouble(registroSeguinte)), 
								avgDouble.applyAsDouble(getYCoord.applyAsDouble(registroAtual), getYCoord.applyAsDouble(registroSeguinte)),
								r.getCoord().getZ()))
						.numeroOrdemVeiculo(r.getNumeroOrdemVeiculo())
						.velocidadeInstantanea(avg.applyAsInt(registroAtual.getVelocidadeInstantanea(), 
								registroSeguinte.getVelocidadeInstantanea()))
						.idLinha(r.getIdLinha())						
					.build())
				.orElse(registroAtual);
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
				
				viagem.getRegistros().add(registro);
				
			} else {
				// terminou uma rota
				chegada = anterior;
				distanciaPercorrida = registro.getDistanciaPercorrida();
				viagem.getRegistros().add(registro);
			}
			
			anterior = registro;
		}
		
		viagem.setPartida(partida);
		viagem.setChegada(chegada);
		viagens.add(viagem);
		
		
		return viagens;
	}

}
