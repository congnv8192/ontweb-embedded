package mappers;


import java.util.Map.Entry;

import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntIndividual;

import ont.AppOntology;
import ont.KT;
import wiki.Article;
import wiki.InfoBox;
import wiki.parsers.InfoBoxParser;

public class Wiki2Ont {
	private AppOntology ontology;
	
	public Wiki2Ont(AppOntology ontology) {
		this.ontology = ontology;
	}
	
	/**
	 * @modifies ontology
	 */
	public OntIndividual mapArticle(Article article) {
		OntIndividual individual = ontology.getOrCreateIndividual(article.pageTitle);
		
		// annotations
		individual.addAnnotation(KT.fetched, ontology.getModel().createTypedLiteral(true));
		individual.addAnnotation(KT.pageID, ontology.getModel().createTypedLiteral(article.pageID));
		
		// if type = redirect
		if (article.isRedirect()) {
			individual.addSameIndividual(ontology.getOrCreateIndividual(article.getRedirect()));
			return individual;
		}

		// annotation
		individual.addAnnotation(KT.version, ontology.getModel().createTypedLiteral(article.version));

		// properties
		individual.addProperty(KT.openingText, article.openingText);
		if (article.thumbnail != null) {
			individual.addProperty(KT.thumbnail, article.thumbnail);
		}
		if (article.termDescription != null) {
			individual.addProperty(KT.termDescription, article.termDescription);
		}
		
		// categories
		for (String category : article.getCategories()) {
			mapCategory(individual, category);
		}

		// infobox
		for (InfoBox infoBox : article.getInfoBoxes()) {
			mapInfoBox(individual, infoBox);
		}
		
		return individual;
	}
	
	/**
	 * @modifies individual
	 */
	public void mapCategory(OntIndividual individual, String category) {
		OntClass classCategory = ontology.getOrCreateClass(category);
		classCategory.addSuperClass(KT.Category);
		
		individual.addClassAssertion(classCategory);
	}
	
	/**
	 * @modifies individual
	 */
	public void mapInfoBox(OntIndividual individual, InfoBox infobox) {
		// template
		OntClass classInfobox = ontology.getOrCreateClass(infobox.getTemplate());
		classInfobox.addSuperClass(KT.Infobox);
		
		individual.addClassAssertion(classInfobox);
		
		// attributes
		for (Entry<String, String> pair : infobox.getAttributes().entrySet()) {
			if (!pair.getValue().isBlank())
				mapInfoBoxAttribute(individual, pair.getKey(), pair.getValue());
		}
	}
	
	public void mapInfoBoxAttribute(OntIndividual individual, String key, String wikiText) {
		String link = InfoBoxParser.getLink(wikiText);
		
		if (link == null) {
			String value = wikiText; // InfoBoxParser.toPlainText(wikiText);
			OntDataProperty prop = ontology.getOrCreateDataProperty(key);
			individual.addProperty(prop, value);
		} else {
			OntIndividual ref = ontology.getOrCreateIndividual(link);
			ontology.getOrCreateObjectProperty(key, individual, ref);
		}
	}
}
