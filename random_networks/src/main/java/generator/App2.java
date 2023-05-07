package generator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongBiFunction;
import java.util.stream.Collectors;

import generator.configuration.PostgisConfig;
import generator.models.PontoRota;
import generator.models.RegistroViagem;
import generator.models.Viagem;
import generator.services.PostgisService;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.WayPoint;
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
		
		var queryRota = """
				select st.stop_sequence, s.stop_id, ST_SetSRID(ST_MakePoint(ST_Y(s.the_geom),ST_X(s.the_geom)), 4326) from gtfs.stop_times st
				join gtfs.stops s on s.stop_id = st.stop_id 
				where st.trip_id = '9202  031080602110' order by 1
				""";
		var rota = postgisService.queryAll(queryRota, rs ->
					PontoRota.builder()
					.sequenciaPonto(rs.getInt(1))
					.idPonto(rs.getString(2))
					.coord((Point) ((PGgeometry) rs.getObject(3)).getGeometry())
					.build()
				);
		
		
		var queryLinhas = """
				select distinct id_linha from bh.bh.onibus_tempo_real otr 
				where id_linha = 629 
				and data_hora between '2022-12-16 6:00:00' and '2022-12-16 23:59:00'
				""";
		var linhas = postgisService.queryAll(queryLinhas, rs -> rs.getInt(1));
		
		var queryRegistrosLinha = """
				select data_hora, distancia_percorrida, coord, numero_ordem_veiculo, velocidade_instantanea, id_linha from bh.bh.onibus_tempo_real otr 
				where id_linha = %d 
				and numero_ordem_veiculo = 20481
				and data_hora between '2022-12-16 6:00:00' and '2022-12-16 23:59:00' 
				order by numero_ordem_veiculo, data_hora
				""";
		
		var viagens9202 = linhas
		.stream()
		.map(queryRegistrosLinha::formatted)
		.map(query -> 
			postgisService.queryAll(query, rs -> RegistroViagem.builder()
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
			.toList()
		)
		.flatMap(List<Viagem>::stream)
		.toList();
		
		viagens9202
			.forEach(viagem -> {
				associarPontos(viagem, rota);
				
				Function<RegistroViagem, WayPoint> registro2Waypoint = p ->
				WayPoint.builder()
	    			.lat(p.getCoord().getX())
	    			.lon(p.getCoord().getY())
	    			.build();
				Function<List<WayPoint>, GPX> waypoints2GPX = w -> GPX.builder()
						.wayPoints(w)
						.build();
				
				var waypointsPontos = viagem.getPontos()
				    	.stream()
				    	.map(PontoRota::getRegistros)
				    	.flatMap(List<RegistroViagem>::stream)
				    	.map(registro2Waypoint::apply)
				    	.toList();
				
				var waypointsRegistros = viagem.getRegistros()
						.stream()
						.map(registro2Waypoint::apply)
						.toList();
				
				
				try {
					var filename = viagem.getVeiculo() + "_" +
							viagem.getIdLinha() + "_"
							+ viagem.getPartida().getDataHora()
							+ ".gpx";
					
					GPX.write(waypoints2GPX.apply(waypointsPontos), Paths.get("./" + "pontos_" + filename));
					GPX.write(waypoints2GPX.apply(waypointsRegistros), Paths.get("./" + "registros_" +filename));
				} catch (IOException e1) {
					e1.printStackTrace();
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
	
	private static void associarPontos(Viagem viagem, List<PontoRota> rota) {
		var registroAtual = 0;
		
//		for (var ponto : rota) {
		for (var pontoAtual = 0; pontoAtual < rota.size() - 1; pontoAtual++) {
			var ponto = rota.get(pontoAtual);
			
			var registrosPontoAnterior = pontoAtual > 0 ? rota.get(pontoAtual -1) : null;
			var menorMaiorDistancia = registrosPontoAnterior != null
					? registrosPontoAnterior.getDistance(): 0d;
			var procurando = true;
			var direcaoPonto = rota.size() / 2 >= ponto.getSequenciaPonto();
			
			for (var i = registroAtual; i < viagem.getRegistros().size() - 1; i++) {
				var registro = viagem.getRegistros().get(i);
				var distancia = registro.getCoord().distance(ponto.getCoord());

				var direcaoregistro = viagem.getRegistros().size() / 2 >= i;
				
				if (distancia <= DISTANCE_THRESHOLD && direcaoPonto == direcaoregistro) {
					ponto.getRegistros().add(registro);
					ponto.setDistance(distancia);
					registroAtual = i;
					menorMaiorDistancia = distancia;
				} else if (!ponto.getRegistros().isEmpty()) {
					registroAtual = i - 1;
					i = viagem.getRegistros().size();
				} else if (menorMaiorDistancia < distancia && procurando) {
					menorMaiorDistancia = distancia;
					registroAtual++;
				} else if (menorMaiorDistancia > distancia) {
					procurando = false;
				}
				
			}
			
			if (ponto.getRegistros().isEmpty()) {
				ponto.setCalculated(true);
				var registroMerged = mergeRegistros(viagem.getRegistros(), registroAtual);
				ponto.getRegistros().add(registroMerged);
				ponto.setDistance(registroMerged.getCoord().distance(ponto.getCoord()));
			}
			
			log.info("{} registros para a sequencia {}", 
					ponto.getRegistros().size(), ponto.getSequenciaPonto());
			viagem.getPontos().add(new PontoRota(ponto));
			registroAtual = ponto.isCalculated() ? registroAtual : viagem.getRegistros().indexOf(ponto.getRegistros().get(ponto.getRegistros().size() - 1));
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
				.map(r -> {
					var point = new Point(avgDouble.applyAsDouble(getXCoord.applyAsDouble(registroAtual), getXCoord.applyAsDouble(registroSeguinte)), 
							avgDouble.applyAsDouble(getYCoord.applyAsDouble(registroAtual), getYCoord.applyAsDouble(registroSeguinte)),
							r.getCoord().getZ());
					point.dimension = r.getCoord().dimension;
					return RegistroViagem
							.builder()
							.distanciaPercorrida(avg.applyAsInt(registroAtual.getDistanciaPercorrida(), 
									registroSeguinte.getDistanciaPercorrida()))
							.dataHora(new Date(avgDate.applyAsLong(registroAtual.getDataHora(), registroSeguinte.getDataHora())))
							.coord(point)
							.numeroOrdemVeiculo(r.getNumeroOrdemVeiculo())
							.velocidadeInstantanea(avg.applyAsInt(registroAtual.getVelocidadeInstantanea(), 
									registroSeguinte.getVelocidadeInstantanea()))
							.idLinha(r.getIdLinha())						
						.build();
				})
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
