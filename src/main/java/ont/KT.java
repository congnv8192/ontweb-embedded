package ont;


import com.github.owlcs.ontapi.jena.model.OntAnnotationProperty;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;

public class KT {
	public static final String URI = "http://ktgroup.vn";
	public static final String NS_PREFIX = "kt";
	
	public static AppOntology ontology = new AppOntology(URI, NS_PREFIX);
	
	public static final OntAnnotationProperty wiki = ontology.getOrCreateAnnotationProperty("wiki");
	public static final OntAnnotationProperty pageID = ontology.getOrCreateAnnotationProperty("pageID").addSuperProperty(wiki);
	public static final OntAnnotationProperty fetched = ontology.getOrCreateAnnotationProperty("fetched").addSuperProperty(wiki);
	public static final OntAnnotationProperty version = ontology.getOrCreateAnnotationProperty("version").addSuperProperty(wiki);
	
	public static final OntClass Wiki = ontology.getOrCreateClass("Wiki");
	public static final OntClass Category = ontology.getOrCreateClass("Category").addSuperClass(Wiki);
	public static final OntClass Infobox = ontology.getOrCreateClass("Infobox").addSuperClass(Wiki);
	
	public static final OntDataProperty openingText = ontology.getOrCreateDataProperty("openingText");
	public static final OntDataProperty thumbnail = ontology.getOrCreateDataProperty("thumbnail");
	public static final OntDataProperty termDescription = ontology.getOrCreateDataProperty("termDescription");
}
