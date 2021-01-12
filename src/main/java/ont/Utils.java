package ont;

public class Utils {
	public static String decodeHtmlEntities(String text) {
		text = text.replace("&gt;", ">");
		text = text.replace("&lt;", "<");
		text = text.replace("&nbsp;", " ");
		text = text.replace("&amp;", "&");
		text = text.replace("&quot;", "\"");
		
		return text;
	}
}
