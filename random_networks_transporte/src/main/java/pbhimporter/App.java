package pbhimporter;

import lombok.SneakyThrows;
import pbhimporter.configuration.PostgisConfig;
import pbhimporter.model.BasePbhResponse;
import pbhimporter.model.resources.PontoOnibus;
import pbhimporter.resources.ResourcesPBH;
import pbhimporter.services.PBHService;
import pbhimporter.services.PostgisService;

public class App {

	@SneakyThrows
	public static void main(String... args) {
		var resourcePBH = ResourcesPBH.valueOf(args[0]);
		
         var pbhService = new PBHService();
         
         BasePbhResponse<PontoOnibus> response = pbhService.getResource(resourcePBH);
         
         System.out.println(response);
         
         var config = new PostgisConfig("jdbc:postgresql://localhost:15432/bh", "bh", "bh");
         
         var postgisService = new PostgisService(config.getConn());
         
         postgisService.createDataset(response, PontoOnibus.class);
         
//         response.getResult()
//         	.getRecords()
//         	.stream()
//         	.map(p -> postgisService.pointFromEWKT(p.getGeometria()))
//         	.forEach(System.out::println);
         
         
         config.close();
         
	}

}
