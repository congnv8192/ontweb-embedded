package wiki2ont;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.riot.Lang;

public class Wiki2OntFactory {
	private static Wiki2Ont app;
	
	public static Wiki2Ont get() {
		
		if (app == null) {
			app = new Wiki2Ont(AppConfig.URI);
			
			try (InputStream is = Wiki2OntFactory.class.getClassLoader().getResourceAsStream(AppConfig.WEB_PATH_ONTO)) {
				System.out.println("loading...");
				// load ontology
				app.loadOntology(is, Lang.RDFXML);
				System.out.println("done.");
			} catch (IOException e) {
				System.out.println("error!");
				e.printStackTrace();
			}
		}
		
		return app;
	}
}
