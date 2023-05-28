package generator.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.postgis.jdbc.geometry.Point;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PontoRota {

	private int sequenciaPonto;
	
	private String idPonto;
	
	@JsonIgnore
	private Point coord;
	
	@Builder.Default
	private List<RegistroViagem> registros =  new ArrayList<>();
	
	private boolean calculated;
	
	private Double distance;
	
	private Double x;

	private Double y;
	
	
	public void setCoord(Point coord) {
		x = coord.x;
		y = coord.y;
		this.coord = coord;
	}
	
	@JsonIgnore
	public Point getCoord() {
		return coord != null ? coord : new Point(x, y);
	}
	
	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	public Date getArrivalTime() {
		return registros.get(0)
				.getDataHora();
	}

	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	public Date getDepartureTime() {
		return registros.get(registros.size() - 1)
				.getDataHora();
	}
	
	public Double getX() {
		return x != null ? x : coord.getX();
	}
	
	public Double getY() {
		return y != null ? y : coord.getY();
	}
	
	public long getArrivalDepartureSecondsDifference() {
		long diffInMillies = Math.abs(getDepartureTime().getTime() - getArrivalTime().getTime());
		return TimeUnit.SECONDS.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}
	
	public PontoRota(PontoRota ponto) {
		this.sequenciaPonto = ponto.getSequenciaPonto();
		this.idPonto = ponto.getIdPonto();
		this.coord = ponto.getCoord();
		this.registros = new ArrayList<>(ponto.getRegistros());
		this.calculated = ponto.calculated;
		this.distance = ponto.getDistance();
	}
	
}
