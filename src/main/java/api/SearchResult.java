package api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import com.github.owlcs.ontapi.jena.model.OntAnnotationProperty;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;
import com.google.gson.Gson;

import ont.KT;

public class SearchResult {
	public class Item {
		private String uri;
		private String label;
		
		private String openingText;
		private String thumbnail;
		private String termDescription;
		
		private Map<String, Object> meta = new HashMap<>();
		private Map<String, Object> infobox = new HashMap<>();
		
		private List<String> types = new ArrayList<>();
		private List<String> sameIndividuals = new ArrayList<>();
	}
	
	public enum PropType {
		entity,
		image
	};
	
	public class ObjectProp {
		private PropType type = PropType.entity;
		private String value;
		
		public ObjectProp() {
			
		}
		
		public ObjectProp(String value) {
			this.value = value;
		}
	}
	
	private List<Item> results;
	
	public List<Item> getResults() {
		return results;
	}

	public SearchResult(List<OntIndividual> individuals) {
		results = new ArrayList<>();
		
		for (OntIndividual individual : individuals) {
			results.add(mapIndividual(individual));
		}
	}
	
	public String toJson() {
		Gson gson = new Gson();
		return gson.toJson(results);
	}
	
	private Item mapIndividual(OntIndividual individual) {
		Item item = new Item();
	
		item.uri = individual.getURI();
		item.label = individual.getLabel();
		
		individual.listProperties().forEachRemaining(s -> {
			Property predicate = s.getPredicate();
			RDFNode object = s.getObject();
			
			if (predicate instanceof OntAnnotationProperty) { // annotations
				OntAnnotationProperty annotationProperty  = (OntAnnotationProperty) predicate;
				String label = annotationProperty.getLabel();
				if (label != null) {
					item.meta.put(label, object.asLiteral().getLexicalForm());
				}
			} else if (predicate instanceof OntDataProperty) { // data properties
				OntDataProperty dataProperty = (OntDataProperty) predicate;
				
				if (predicate.equals(KT.openingText)) { // openingText
					item.openingText = object.toString();
				} else if (predicate.equals(KT.thumbnail)) { // thumbnail
					item.thumbnail = object.toString();
				} else if (predicate.equals(KT.termDescription)) { // term description
					item.termDescription = object.toString();
				} else { // infobox
					item.infobox.put(dataProperty.getLabel(), object.toString());
				}
			} else if (predicate instanceof OntObjectProperty) { // object properties
				OntObjectProperty objectProperty = (OntObjectProperty) predicate;
				Statement _s = object.asResource().getProperty(RDFS.label);
				if (_s != null) { // infobox
					item.infobox.put(objectProperty.getLabel(), new ObjectProp().value = _s.getObject().toString());
				}
			} else if (predicate.equals(RDF.type)) { // types
				Statement _s = object.asResource().getProperty(RDFS.label);
				if (_s != null) { 
					item.types.add(_s.getObject().toString());
				}
			}
		});
		
		// similar individuals
		individual.sameIndividuals().forEach(s -> {
			item.sameIndividuals.add(s.getLabel());
		});
		
		return item;
	}

//	public static JsonObject toJson(OntIndividual individual) {
//		JsonObject jsonObject = new JsonObject();
//		jsonObject.addProperty("uri", individual.getURI());
//		jsonObject.addProperty("label", individual.getLabel());
//		
//		// image
//		
//		// openingText
//		
//		// properties
//		JsonObject annotations = new JsonObject(); jsonObject.add("annotations", annotations);
//		JsonObject properties = new JsonObject(); jsonObject.add("properties", properties);
//		
//		individual.listProperties().forEachRemaining(s -> {
//			Property predicate = s.getPredicate();
//			RDFNode object = s.getObject();
//	
//			if (predicate instanceof OntAnnotationProperty) { // annotations
//				OntAnnotationProperty annotationProperty  = (OntAnnotationProperty) predicate;
//
//				annotations.add(annotationProperty.getLabel(), new JsonPrimitive(object.asLiteral().getString()));
//			} else if (predicate instanceof OntDataProperty) { // data properties
//				OntDataProperty dataProperty = (OntDataProperty) predicate;
//				
//				properties.add(dataProperty.getLabel(), new JsonPrimitive(object.asLiteral().getString()));
//			} else if (predicate instanceof OntObjectProperty) { // object properties
//				OntObjectProperty objectProperty = (OntObjectProperty) predicate;
//				
//				JsonObject _jsonObject = new JsonObject();
//				_jsonObject.add("type", new JsonPrimitive("entity"));
//				_jsonObject.add(objectProperty.getLabel(), new JsonPrimitive(object.asLiteral().getString()));
//			}
//		});
//		
//		// similar individuals
//		individual.sameIndividuals().forEach(s -> {
//			System.out.println(s);
//		});
//		
//		return jsonObject;
//	}
}
