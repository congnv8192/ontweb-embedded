package wiki.fromapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SearchAPI {
	public static final String URL_SEARCH = "https://vi.wikipedia.org/w/api.php?action=query&format=json&list=search&utf8=1&srwhat=nearmatch&srsearch=%s";
	
	/**
	 * @see https://vi.wikipedia.org/w/api.php?action=query&format=json&list=search&utf8=1&srwhat=nearmatch&srsearch=h%E1%BB%93%20ch%C3%AD%20minh
	 */
	public static List<String> search(String keyword) {
		try {
			String url = String.format(URL_SEARCH, URLEncoder.encode(keyword, "UTF-8"));
	
			try (InputStream is = new URL(url).openStream();
					Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				
				return SearchResult.toArticleTitles(reader);
			}
		} catch (IOException e) {
			e.printStackTrace();
			
			return null;
		}
	}
}
