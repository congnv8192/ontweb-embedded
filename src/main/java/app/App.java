package app;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;

import api.SearchResult;
import mappers.Wiki2Ont;
import ont.AppOntology;
import ont.KT;
import wiki.Article;
import wiki.fromapi.QueryAPI;
import wiki.fromapi.SearchAPI;

public class App {
	private AppOntology ontology;
	private Wiki2Ont wiki2Ont;

	public App() {
		ontology = KT.ontology;
		 
		try {
			try (InputStream is = new FileInputStream(AppConfig.PATH_ONT)) {
				ontology.loadOntology(is, Lang.RDFXML);
			}
		} catch (IOException e) {
			ontology.newOntology("20201215");
		}

		wiki2Ont = new Wiki2Ont(ontology);
	}

	public static void main(String[] args)
			throws MalformedURLException, IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		App app = new App();
		
		List<OntIndividual> individuals = app.search("Quang Trung");
		SearchResult result = new SearchResult(individuals);
		System.out.println(result.toJson());
		
//		SearchResult.toJson(individuals);
//		System.out.println(SearchResult.toJson(individuals));

	}

	private static List<Map<String, Object>> toMap(List<OntIndividual> individuals) {
		List<Map<String, Object>> data = new ArrayList<>();
		for (OntIndividual individual : individuals) {
			Map<String, Object> result = new HashMap<>();

			result.put("label", individual.getLabel());
			result.put("uri", individual.getURI());

			// annotations
			Map<String, String> annotations = new HashMap<String, String>();
			individual.annotations().forEach((a) -> {
				annotations.put(a.getPredicate().getLocalName(), a.getObject().toString());
			});
			result.put("annotation", annotations);

			// props
			Map<String, Object> props = new HashMap<String, Object>();
			individual.listProperties().forEachRemaining((p) -> {
				Resource subject = p.getSubject();
				Property predicate = p.getPredicate();
				RDFNode object = p.getObject();

				if (predicate instanceof OntDataProperty) {
					OntDataProperty dataProperty = (OntDataProperty) predicate;
					
					props.put(dataProperty.getLabel(), object.toString());
				}
				
				if (predicate instanceof OntObjectProperty) {
					OntObjectProperty objectProperty = (OntObjectProperty) predicate;
					OntIndividual in = (OntIndividual) object;
					
					Map<String, String> tmp = new HashMap<>();
					tmp.put("type", "entity");
					tmp.put("data", in.getLabel());
					
					props.put(objectProperty.getLabel(), tmp);
				}
			});
			result.put("props", props);

			// same as
			List<Map<String, String>> sameIndividuals = new ArrayList<>();
			individual.sameIndividuals().forEach(in -> {
				Map<String, String> tmp = new HashMap<>();

				tmp.put("label", in.getLabel());
				tmp.put("uri", in.getURI());

				sameIndividuals.add(tmp);
			});
			result.put("sameIndividuals", sameIndividuals);

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

	public List<OntIndividual> search(String keyword) {
		List<OntIndividual> individuals = searchLocal(keyword);

		if (individuals.isEmpty()) {
			OntIndividual individual = searchAPI(keyword);
			if (individual != null) {
				individuals.add(individual);

				// export ontology
				try {
					ontology.save(new FileOutputStream(AppConfig.PATH_ONT));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					System.out.println("Failed to saved file ontology.");
				}
			}
		}

		return individuals;
	}

	public List<OntIndividual> searchLocal(String keyword) {
		return ontology.search(keyword);
	}

	public OntIndividual searchAPI(String keyword) {
		// search
		List<String> titles = SearchAPI.search(keyword);

		if (titles == null || titles.isEmpty()) {
			// return NO RESULT
			return null;
		}

		// get article by title
		Article article = QueryAPI.queryArticleByTitle(titles.get(0));
		System.out.println(article.sourceText);

		if (article == null) {
			return null;
		}

		return wiki2Ont.mapArticle(article);
	}
}
