package wiki2ont.wiki;


public class InfoBox {
	private String text;
	
	public String getText() {
		return this.text;
	}
	
	public InfoBox(String text) {
		this.text = text;
	}
	
	public static InfoBox create(info.bliki.wiki.dump.InfoBox infobox) {
		return new InfoBox(infobox.dumpRaw());
	}
}
