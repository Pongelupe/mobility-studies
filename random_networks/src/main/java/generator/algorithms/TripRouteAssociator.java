package generator.algorithms;

import java.util.List;

import generator.models.PontoRota;
import generator.models.Viagem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TripRouteAssociator {
	
	private static final double DISTANCE_THRESHOLD = 0.0005d; // 50m

	private final TripMissingEntriesGenerator missingEntriesGenerator;
	
	public void associate(Viagem viagem, List<PontoRota> rota) {
		associateTripToRoute(viagem, rota);
		missingEntriesGenerator.generateMissingEntries(viagem, rota);
		
		rota.forEach(p -> {
			p.setCalculated(false);
			p.getRegistros().clear();
		});
	}
	
	private void associateTripToRoute(Viagem viagem, List<PontoRota> rota) {
		var registroAtual = 0;
		
		for (var pontoAtual = 0; pontoAtual < rota.size(); pontoAtual++) {
			var ponto = rota.get(pontoAtual);
			
			var registrosPontoAnterior = pontoAtual > 0 ? rota.get(pontoAtual -1) : null;
			var menorMaiorDistancia = registrosPontoAnterior != null
					? registrosPontoAnterior.getCoord().distance(ponto.getCoord()): 0d;
			var procurando = true;
			var direcaoPonto = rota.size() / 2 >= ponto.getSequenciaPonto();
			
			var distanciaPercorridaViagem = viagem.getDistanciaPercorrida();
			
			for (var i = registroAtual; i < viagem.getRegistros().size() - 1; i++) {
				var registro = viagem.getRegistros().get(i);
				var distancia = registro.getCoord().distance(ponto.getCoord());

				var direcaoregistro = distanciaPercorridaViagem / 2 >= registro.getDistanciaPercorrida();
				
				if (distancia <= DISTANCE_THRESHOLD && 
						((direcaoPonto == direcaoregistro)
								|| (ponto.getRegistros().isEmpty() && !direcaoPonto))) {
					registro.setIndex(i);
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
			
			viagem.getPontos().add(new PontoRota(ponto));
		}
	}
}
