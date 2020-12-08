package wiki2ont;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

import org.apache.jena.riot.Lang;

import com.github.owlcs.ontapi.jena.model.OntIndividual;

import info.bliki.wiki.dump.WikiArticle;

public class App {
	public static void main(String[] args) {

		try {
			Wiki2Ont app = new Wiki2Ont(AppConfig.URI);
			
//			try (InputStream is = new FileInputStream(new File(AppConfig.APP_PATH_ONTO))) {
//				// load ontology
//				app.loadOntology(is, Lang.RDFXML);
//			} 
		
			// load wiki dumps
//			app.processDumpFiles(AppConfig.APP_PATH_WIKI_DUMPS);

//			WikiArticle article = app.addArticleByUrl("Đại_học_Quốc_gia_Hà_Nội");
//			System.out.println(article);
//
//			List<OntIndividual> individuals = app.query("Đại học");
//
//			for (OntIndividual individual : individuals) {
//				System.out.println(individual);
//			}
			
			System.out.println(app.addArticleByUrl("Thành phố Hồ Chí Minh").getTitle());

//			try (OutputStream os = new FileOutputStream(AppConfig.APP_PATH_ONTO)) {
//				app.exportOntology(os, Lang.RDFXML);
//			}
			
//			app.exportOntology(System.out, Lang.RDFXML);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
