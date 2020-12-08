package servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;

import com.github.owlcs.ontapi.jena.model.OntIndividual;

import info.bliki.wiki.dump.WikiArticle;
import wiki2ont.AppConfig;
import wiki2ont.Wiki2Ont;
import wiki2ont.Wiki2OntFactory;
import wiki2ont.wiki.Utils;

@WebServlet("/query")
public class QueryServlet extends Servlet {
	private Wiki2Ont app;

	@Override
	public void init() throws ServletException {
		app = Wiki2OntFactory.get();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		request.setCharacterEncoding("UTF-8");

		String q = Utils.paramToUTF8(request.getParameter("q"));
		
		// wiki search
		String page = app.search(q);
		
		List<OntIndividual> individuals = app.query(page);
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		// no local result
		if (individuals.isEmpty()) {
			try {
				// wiki article
				WikiArticle article = app.addArticleByUrl(page);
				
				System.out.println(article.getText());
				
				// save
				URL url = getClass().getClassLoader().getResource(AppConfig.WEB_PATH_ONTO);
				OutputStream os = new FileOutputStream(new File(url.toURI()));
				app.exportOntology(os, Lang.RDFXML);
				result.put("onSave", true);
				
				// retry
				individuals = app.query(article.getTitle());
				
			} catch (UnsupportedEncodingException e) {
				result.put("onArticle", false);
				result.put("onArticleError", e.getMessage());
			} catch (FileNotFoundException | URISyntaxException e) {
				result.put("onSave", false);
				result.put("onSaveError", e.getMessage());
			}
		}
		
		result.put("data", toMap(individuals));
		
		json(response, result);
	}
	
	private List<Map<String, Object>> toMap(List<OntIndividual> individuals) {
		List<Map<String, Object>> data = new ArrayList<>();
		for(OntIndividual individual : individuals) {
			Map<String, Object> result = new HashMap<>();
			
			result.put("label", individual.getLabel());
			result.put("uri", individual.getURI());
			
			// summary
			Statement propSummary = individual.getProperty(app.getPropSummary());
			if (propSummary != null) {
				result.put("summary", propSummary.getString());
			}
			
			// infobox
			Statement propInfobox = individual.getProperty(app.getPropInfobox());
			if (propInfobox != null) {
				result.put("infobox", propInfobox.getString());
			}

			// same as
			List<Map<String, String>> sameIndividuals = new ArrayList<>();
			individual.sameIndividuals().forEach(in -> {
				Map<String, String> props = new HashMap<>();
				
				props.put("label", in.getLabel());
				props.put("uri", in.getURI());
				
				sameIndividuals.add(props);
			});
			
			// types
			List<String> types = new ArrayList<>();
			individual.types().forEach((type) -> {
				types.add(type.getURI());
			});
			result.put("types", types);
			
			data.add(result);
		}
		
		return data;
	}
}
