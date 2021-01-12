package wiki.parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import exceptions.InvalidWikiTextException;
import ont.Utils;
import wiki.InfoBox;

public class InfoBoxParser {
	public static final String[] INFOBOX_CONST_STRS = { "{{Infobox", "{{Hộp thông tin", "{{Thông tin" };
	
	public static List<InfoBox> parseInfoBoxes(String wikiText) {
		List<InfoBox> infoBoxes = new ArrayList<>();
		
		for (String infoBoxText : InfoBoxParser.extractInfoBoxTexts(wikiText)) {
			System.out.println("=====================");
			System.out.println(infoBoxText);
			System.out.println("=====================");
			InfoBox infoBox = InfoBoxParser.parseInfoBox(infoBoxText);
			infoBoxes.add(infoBox);
		}
		
		return infoBoxes;
	}
	
	public static InfoBox parseInfoBox(String infoBoxText) {
		InfoBox infoBox = new InfoBox();
		
		// template
		infoBox.setTemplate(parseTemplate(infoBoxText));
		
		// attributes
		infoBox.setAttributes(parseAttributes(infoBoxText));
		
		return infoBox;
	}
	
	/**
	 * @throws IndexOutOfBoundException if invalid infoBoxText
	 */
	public static String parseTemplate(String infoBoxText) {
		return infoBoxText.substring(2, infoBoxText.indexOf("\n")).trim(); // ignore {{
	}
	
	public static Map<String, String> parseAttributes(String infoBoxText) {
		Map<String, String> attributes = new HashMap<>();
		
		// remove {{ }}
		infoBoxText = infoBoxText.substring(2, infoBoxText.length()-2);
		
		// split by ^|
		String[] pairs = infoBoxText.split("(?m)^\\|");
		
		for (int i = 1; i < pairs.length; i++) { // ignore {{template
			String pair = pairs[i];
			
			String[] s = pair.split("=", 2);
			
			String value = s[1].trim();
			if (!value.isEmpty()) {
				attributes.put(s[0].trim(), value);				
			}
		}
		
		return attributes;
	}
	
	/**
	 * @effects extract list of strings representing infoboxes from wikiText
	 * @throws InvalidWikiTextException 
	 */
	public static List<String> extractInfoBoxTexts(String wikiText) {
		StringBuilder sb = new StringBuilder(wikiText);
		List<String> infoBoxTexts = new ArrayList<>();
		
		while (true) {
			String infoBoxText = extractInfoBoxText(sb);
			
			if (infoBoxText == null) {
				break;
			}
			
			infoBoxTexts.add(infoBoxText);
		}
		
		return infoBoxTexts;
	}
	
	/**
	 * @modifies wikiText
	 * @param wikiText source of article in wikitext form
	 * @throws InvalidWikiTextException 
	 */
	public static String extractInfoBoxText(StringBuilder wikiText)  {
		for (String INFOBOX_CONST_STR : INFOBOX_CONST_STRS) {
			int startPos = wikiText.indexOf(INFOBOX_CONST_STR);
			
			if (startPos < 0) {
				continue;
			}
			
			int bracketCount = 2;
			int endPos = startPos + INFOBOX_CONST_STR.length();

			for (; endPos < wikiText.length(); endPos++) {
				switch (wikiText.charAt(endPos)) {
				case '}':
					bracketCount--;
					break;
				case '{':
					bracketCount++;
					break;
				}
				if (bracketCount == 0)
					break;
			}
			
			String infoBoxText = wikiText.substring(startPos, endPos+1);
			// remove extracted infoBoxText
			wikiText.delete(startPos, endPos+1);
			
//			infoBoxText = clean(infoBoxText);
			
			return infoBoxText;
		}
		
		return null;
	}
	
	public static String clean(String infoBoxText) {
		infoBoxText = stripCite(infoBoxText); // strip clumsy {{cite}} tags
		
		// strip any html formatting
		infoBoxText = Utils.decodeHtmlEntities(infoBoxText);
		
		infoBoxText = infoBoxText.replaceAll("<timeline>(.|\\n)*<\\/timeline>", "");
		infoBoxText = infoBoxText.replaceAll("<ref.*?>.*?</ref>", " ");
		infoBoxText = infoBoxText.replaceAll("</?.*?>", " ");
		
		return infoBoxText;
	}
	
	private static String stripCite(String text) {
		String CITE_CONST_STR = "{{cite";
		int startPos = text.indexOf(CITE_CONST_STR);
		if (startPos < 0)
			return text;
		int bracketCount = 2;
		int endPos = startPos + CITE_CONST_STR.length();
		for (; endPos < text.length(); endPos++) {
			switch (text.charAt(endPos)) {
			case '}':
				bracketCount--;
				break;
			case '{':
				bracketCount++;
				break;
			default:
			}
			if (bracketCount == 0)
				break;
		}
		text = text.substring(0, startPos - 1) + text.substring(endPos);
		return stripCite(text);
	}
	
	public static String getLink(String wikiText) {
		Pattern p1 = Pattern.compile("\\[\\[(.*?)\\]\\]");
		Pattern p2 = Pattern.compile("(.*)\\|(.*?)");
		
	    Matcher m = p1.matcher(wikiText);
	    if (m.find()) {
	    	String text = m.group(1);
	    	
	    	m = p2.matcher(text);
	    	if (m.find()) {
	    		return m.group(1);
	    	}
	    	
	    	return text;
	    }
	    
	    return null;
	}
	
	public static String toPlainText(String wikiText) {
		wikiText = clean(wikiText);
		
		wikiText = wikiText.replaceAll("\\{\\{.*\\}\\}", "");
		wikiText = wikiText.replaceAll("\\[\\[(.*)\\|(.*?)\\]\\]", "$2");
		wikiText = wikiText.replaceAll("\\[\\[(.*?)\\]\\]", "$1");
		return wikiText;
	}
	
	public static void main(String[] args) {
		String test = "abc {{name|congnv}} [[congnv|nguyen van cong]] [[nghe an]] [[vietnam]] exit.";
		
//		System.out.println(toPlainText(test));
//		System.out.println(getLink(test));
		System.out.println(clean(test));
	}
}
