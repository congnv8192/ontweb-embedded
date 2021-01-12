package wiki.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wiki.InfoBox;

public class ArticleParser {
	private final static Pattern[] CATEGORY_PATTERNS = {
		Pattern.compile("\\[\\[Category:(.*?)\\]\\]", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
		Pattern.compile("\\[\\[Thể loại:(.*?)\\]\\]", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE)
	};
	public static final String[] CATEGORY_CONST_STRS = { "{{Infobox", "{{Hộp thông tin", "{{Thông tin" };
	
	public static List<String> parseCategories(String sourceText) {
		System.out.println("parsing...");
		List<String> categories = new ArrayList<>();

		for (Pattern CATEGORY_PATTERN : CATEGORY_PATTERNS) {
			Matcher matcher = CATEGORY_PATTERN.matcher(sourceText);
			
			while (matcher.find()) {
				String[] temp = matcher.group(1).split("\\|");
				categories.add(temp[0].trim());
			}
		}
		
		return categories;
	}
	
	public static List<InfoBox> parseInfoBoxes(String sourceText) {
		return InfoBoxParser.parseInfoBoxes(sourceText);
	}
}
