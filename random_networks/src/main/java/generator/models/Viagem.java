package generator.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Viagem {

	private RegistroViagem partida;

	private RegistroViagem chegada;
	
	private List<RegistroViagem> registros;
	
	private List<PontoRota> pontos;
	
	
	public Viagem() {
		this.registros = new ArrayList<>();
		this.pontos = new ArrayList<>();
	}
	
	@JsonIgnore
	public RegistroViagem getPontoAtual() {
		return Optional.ofNullable(chegada)
				.orElseGet(() -> registros.get(registros.size() -1));
	}
	
	public int getVeiculo() {
		return partida.getNumeroOrdemVeiculo();
	}
	
	public int getIdLinha() {
		return partida.getIdLinha();
	}
	
	public boolean isViagemCompleta() {
		return chegada != null;
	}
	
	public int getDistanciaPercorrida() {
		return getPontoAtual().getDistanciaPercorrida();
	}
	
	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	public Date getHorarioPartida() {
		return partida.getDataHora();
	}
	
	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	public Date getHorarioChegada() {
		return isViagemCompleta() ? getChegada().getDataHora() : null;
	}
	
	public int getQuantidadeRegistros() {
		return registros.size();
	}
	
	@JsonIgnore
	public boolean isAnyNotCalculated() {
		return pontos
				.stream()
				.anyMatch(e -> !e.isCalculated());
	}
	
}
