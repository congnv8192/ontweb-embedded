package wiki.fromapi;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SearchResult {
	public static List<String> toArticleTitles(Reader reader) {
		List<String> titles = new ArrayList<String>();

		JsonArray search = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("query")
				.getAsJsonArray("search");

		for (int i = 0; i < search.size(); i++) {
			JsonObject page = search.get(i).getAsJsonObject();
			titles.add(page.get("title").getAsString());
		}

		return titles;
	}

}
