package generator.configuration;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;

import lombok.Getter;
import lombok.SneakyThrows;
import net.postgis.jdbc.PGbox3d;
import net.postgis.jdbc.PGgeometry;

public class PostgisConfig implements Closeable {

	@Getter
	private final Connection conn;
	
	@SneakyThrows
	public PostgisConfig(String url, String username, String password) {
	    conn = DriverManager.getConnection(url, username, password);
	    /*
	    * Add the geometry types to the connection. Note that you
	    * must cast the connection to the pgsql-specific connection
	    * implementation before calling the addDataType() method.
	    */
	    ((org.postgresql.PGConnection)conn).addDataType("geometry",PGgeometry.class);
	    ((org.postgresql.PGConnection)conn).addDataType("box3d", PGbox3d.class);
	}

	@Override
	@SneakyThrows
	public void close() throws IOException {
		conn.close();
	}	
	
}
