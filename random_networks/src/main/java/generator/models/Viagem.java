package generator.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Viagem {

	private RegistroViagem partida;

	private RegistroViagem chegada;
	
	private List<RegistroViagem> pontosRota;
	
	
	public Viagem() {
		this.pontosRota = new ArrayList<>();
	}
	
	public boolean isViagemCompleta() {
		return chegada != null;
	}
	
	public RegistroViagem getPontoAtual() {
		return Optional.ofNullable(chegada)
				.orElseGet(() -> pontosRota.get(pontosRota.size() -1));
	}

	public int getDistanciaPercorrida() {
		return getPontoAtual().getDistanciaPercorrida();
	}
	
}
