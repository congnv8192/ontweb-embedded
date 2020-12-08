package wiki2ont.wiki;

import java.nio.charset.StandardCharsets;

public class Utils {
	public static String toWikiUrl(String text) {
		text = decodeHtmlEntities(text);
		
		// handle ns prob
		text = text.replace("(", "");
		text = text.replace(")", "");
		text = text.replace("\"", "");
		text = text.replace(",", "");
		text = text.replace("&", "");
		
		text = text.replace(" ", "_");
		
		return text;
	}
	
	public static String paramToUTF8(String s) {
		byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
		return new String(bytes, StandardCharsets.UTF_8);
	}
	
	public static String decodeHtmlEntities(String text) {
		text = text.replace("&gt;", ">");
		text = text.replace("&lt;", "<");
		text = text.replace("&nbsp;", " ");
		text = text.replace("&amp;", "&");
		text = text.replace("&quot;", "\"");
		
		return text;
	}
	
	public static String stripWikiHtmlFormat(String text) {
		text = decodeHtmlEntities(text);
		
		text = text.replaceAll("<ref.*?>.*?</ref>", " ");
		text = text.replaceAll("</?.*?>", " ");
		
		return text;
	}
	
	public static void main(String[] args) {
		System.out.println(toWikiUrl("Câu lạc bộ bóng đá Hà Nội T&amp;T"));
	}
}
