package wiki.fromapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import wiki.Article;

public class QueryAPI {
	public static final String URL_QUERY = "https://vi.wikipedia.org/w/api.php?action=query&format=json&prop=pageimages|pageterms|cirrusdoc&titles=%s";
	
	public static Article queryArticleByTitle(String title) {
		try {
			String url = String.format(URL_QUERY, URLEncoder.encode(title, "UTF-8"));
			
			try (InputStream is = new URL(url).openStream();
					Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				
				return QueryResult.toArticle(reader);
			}
		} catch (IOException e) {
			e.printStackTrace();
			
			return null;
		}
	}
	
	public static void main(String[] args) {
		queryArticleByTitle("Đài Tiếng nói Việt Nam");
	}
}
