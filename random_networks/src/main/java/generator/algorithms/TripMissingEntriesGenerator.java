package generator.algorithms;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import generator.models.PontoRota;
import generator.models.RegistroViagem;
import generator.models.Viagem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TripMissingEntriesGenerator {

	private final EntryMerger merger;
	
	public void generateMissingEntries(Viagem viagem, List<PontoRota> rota) {
		var pontoFaltantes = rota
		.stream()
		.parallel()
		.filter(e -> e.getRegistros().isEmpty())
		.toList();
		
		for (var pontoFaltante : pontoFaltantes) {
			var indexPonto = pontoFaltante.getSequenciaPonto();
			var indexMaxRegistroPontoAnterior = getIndexMaxRegistroPontoAnterior(rota, indexPonto);
			var indexMinRegistroPontoSeguinte = getIndexMinRegistroPontoSeguinte(rota, indexPonto)
					.orElse(indexMaxRegistroPontoAnterior);
			
			var registrosCandidatos = viagem
				.getRegistros()
				.subList(indexMaxRegistroPontoAnterior, indexMinRegistroPontoSeguinte + 1)
				.stream()
				.sorted(Comparator.comparingDouble(r -> r.getCoord().distance(pontoFaltante.getCoord())))
				.distinct()
				.toList();
			
			var registroMerged = merger.merge(registrosCandidatos.get(0), 
					registrosCandidatos.get(registrosCandidatos.size() > 1 ? 1 : 0));
			
			var ponto = viagem.getPontos().get(indexPonto - 1);
			
			ponto.getRegistros().add(registroMerged);
			ponto.setCalculated(true);
		}
		
	}
	
	private Optional<Integer> getIndexMinRegistroPontoSeguinte(List<PontoRota> rota, int i) {
		return rota
				.stream()
				.parallel()
				.filter(e -> !e.getRegistros().isEmpty())
				.filter(e -> e.getSequenciaPonto() > i)
				.min(Comparator.comparingInt(e -> e.getSequenciaPonto()))
				.map(p -> p.getRegistros()
						.stream()
						.min(Comparator.comparing(RegistroViagem::getDataHora))
						.map(RegistroViagem::getIndex)
						.get())
				;
	}
	
	private int getIndexMaxRegistroPontoAnterior(List<PontoRota> rota, int i) {
		return rota
				.stream()
				.parallel()
				.filter(e -> !e.getRegistros().isEmpty())
				.filter(e -> e.getSequenciaPonto() < i)
				.max(Comparator.comparingInt(e -> e.getSequenciaPonto()))
				.orElse(rota.get(0))
				.getRegistros()
				.stream()
				.max(Comparator.comparing(RegistroViagem::getDataHora))
				.map(e -> e.getIndex())
				.orElse(0);
	}
	
}
