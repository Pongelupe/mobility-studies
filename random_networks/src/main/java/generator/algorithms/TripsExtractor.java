package generator.algorithms;

import java.util.ArrayList;
import java.util.List;

import generator.models.RegistroViagem;
import generator.models.Viagem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TripsExtractor {

	public List<Viagem> extract(List<RegistroViagem> registros) {
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
