package generator.models;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.postgis.jdbc.geometry.Point;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class RegistroViagem {

	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	private Date dataHora;
	
	private int distanciaPercorrida;
	
	private Point coord;
	
	private int numeroOrdemVeiculo;
	
	private int velocidadeInstantanea;

	private int idLinha;
	
	@JsonIgnore
	private int index;
	
	private Double x;

	private Double y;
	
	public void setCoord(Point coord) {
		x = coord.x;
		y = coord.y;
		this.coord = coord;
	}
	
	public Double getX() {
		return x != null ? x : coord.getX();
	}
	
	public Double getY() {
		return y != null ? y : coord.getY();
	}
	
	@JsonIgnore
	public Point getCoord() {
		return coord != null ? coord : new Point(x, y);
	}

}
