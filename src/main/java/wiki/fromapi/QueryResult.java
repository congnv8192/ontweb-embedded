package wiki.fromapi;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wiki.Article;

public class QueryResult {
	/**
	 * @see https://vi.wikipedia.org/w/api.php?action=query&format=json&prop=cirrusdoc|pageimages|pageterms&titles=Hồ Chí Minh
	 */
	public static Article toArticle(Reader reader) {
		Article article = new Article();
		
		JsonObject pages = JsonParser.parseReader(reader).getAsJsonObject()
			.getAsJsonObject("query").getAsJsonObject("pages");
		JsonObject page = pages.getAsJsonObject((String) pages.keySet().toArray()[0]);
		
		article.pageID = page.get("pageid").getAsLong();
		article.pageTitle = page.get("title").getAsString();
		
		// cirrusdoc
		JsonObject source = page.getAsJsonArray("cirrusdoc").get(0).getAsJsonObject().getAsJsonObject("source");
		
		article.cirrusDocTitle = source.get("title").getAsString();
		
		article.version = source.get("version").getAsLong();
		article.openingText = source.get("opening_text").getAsString();
			
		String sourceText = source.get("source_text").getAsString();
		article.sourceText = sourceText;
		
		if (page.has("thumbnail")) {
			article.thumbnail = page.getAsJsonObject("thumbnail").get("source").getAsString().replace("37px-", "200px-");
		}
			
		if (page.has("terms")) {
			JsonObject terms = page.getAsJsonObject("terms");
			article.termDescription = terms.get("description").getAsString();
			
			JsonArray alias = terms.getAsJsonArray("alias");
			List<String> termAlias = new ArrayList<>();
			for (int i = 0; i < alias.size(); i++) {
				termAlias.add(alias.get(i).getAsString());
			}
			article.termAlias = termAlias;
		}
		
		return article;
	}
}
