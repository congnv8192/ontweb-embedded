package wiki;

import java.util.Map;

public class InfoBox {
	private String template;
	private Map<String, String> attributes;
	
	public String getTemplate() {
		return template;
	}
	
	public void setTemplate(String template) {
		this.template = template;
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
	
	@Override
	public String toString() {
		return "InfoBox [template=" + template + ", attributes=" + attributes + "]";
	}
}