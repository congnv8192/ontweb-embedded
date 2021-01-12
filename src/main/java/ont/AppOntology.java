package ont;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.OntAnnotationProperty;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;

public class AppOntology {
	
	private String uri;
	private String nsPrefix;
	private OntModel model;
	
	public AppOntology(String uri, String nsPrefix) {
		this.uri = uri;
		this.nsPrefix  = nsPrefix;
		
		OntologyManager ontologyManager = OntManagers.createConcurrentONT();
		model = ontologyManager.createGraphModel(uri);
	}
	
	
	public void loadOntology(InputStream is, Lang lang) {
		RDFDataMgr.read(model, is, lang);
	}
	
	public void newOntology(String version) {
		model.getID().setVersionIRI(uri + "/" + version);
		model.setNsPrefixes(OntModelFactory.STANDARD);
		model.setNsPrefix(nsPrefix, uri + "#");
	}
	
	public OntModel getModel() {
		return model;
	}
	
	/**
	 * save in RDF/XML
	 */
	public void save(OutputStream os) {
		model.write(os, Lang.RDFXML.getName());
	}
	
	public String createURI(String text) {
		// handle ns problem
		if (Character.isDigit(text.charAt(0))) {
			text = '_' + text;
		}
		
//		text = URLEncoder.encode(text, StandardCharsets.UTF_8);
		
		text = text.replaceAll(" ", "_");
		text = text.replaceAll("#", "_");
		text = text.replaceAll("\\.", "_");
		text = text.replaceAll(":", "_");
		text = text.replaceAll("\\(", "_");
		text = text.replaceAll("\\)", "_");
		text = text.replaceAll("-", "_");
		
//		System.out.println(uri + "#" + text);
		return uri + "#" + text;
	}
	
	public List<OntIndividual> search(String keyword) {
		QueryExecution qexec = QueryExecutionFactory
				.create(QueryFactory.create("PREFIX rdf:	<http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
						+ "PREFIX owl:     <http://www.w3.org/2002/07/owl#> \n"
						+ "PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#> \n"
						+ "PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#> \n"
						+ "PREFIX wo:     <http://example.com#> \n" +

						"SELECT * WHERE { " 
						+ "?x rdfs:label ?label . FILTER regex(?label, \"" + keyword + "\", \"i\") " 
//						+ "?x ?p ?o"
						+ "}"), model);

		ResultSet res = qexec.execSelect();

		List<OntIndividual> individuals = new ArrayList<OntIndividual>();
		while (res.hasNext()) {
			QuerySolution querySolution = res.next();
			
			RDFNode node = querySolution.get("x");
			OntIndividual individual = model.getIndividual(node.toString());

			individuals.add(individual);
		}

		return individuals;
	}
	
	public OntClass getOrCreateClass(String name) {
		String uri = createURI(name);
		
		OntClass c = model.getOntClass(uri);
		
		if (c == null) {
			c = model.createOntClass(uri);
			c.addLabel(name);
		}
		
		return c;
	}
	
	public OntIndividual getOrCreateIndividual(String name) {
		String uri = createURI(name);
		
		OntIndividual individual = model.getIndividual(uri);
		
		if (individual == null) {
			individual = model.createIndividual(uri);
			individual.addLabel(name);
		}
		
		return individual;
	}
	
	public OntDataProperty getOrCreateDataProperty(String name) {
		String uri = createURI(name);
		
		OntDataProperty p = model.getDataProperty(uri);
		
		if (p == null) {
			p = model.createDataProperty(uri);
			p.addLabel(name);
		}
		
		return p;
	}
	
	public OntObjectProperty getOrCreateObjectProperty(String name, OntIndividual individual, OntIndividual ref) {
		String uri = createURI(name);
		
		OntObjectProperty p = model.getObjectProperty(uri);
		
		if (p == null) {
			p = model.createObjectProperty(uri);
			p.addLabel(name);
			
			model.add(individual, (Property) p, ref);
		}
		
		return p;
	}
	
	public OntAnnotationProperty getOrCreateAnnotationProperty(String name) {
		String uri = createURI(name);
		
		OntAnnotationProperty p = model.getAnnotationProperty(uri);
		
		if (p == null) {
			p = model.createAnnotationProperty(uri);
			p.addLabel(name);
		}
		
		return p;
	}
	
}
