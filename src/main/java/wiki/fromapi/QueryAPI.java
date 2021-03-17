package wiki.fromapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
	
	public static final String URL_CATEGORY_MEMBERS = "https://vi.wikipedia.org/w/api.php?action=query&format=json&list=categorymembers&utf8=1&cmtitle=%s&cmlimit=20";
	
	/**
	 * @see https://vi.wikipedia.org/w/api.php?action=query&format=json&list=categorymembers&utf8=1&cmtitle=Th%E1%BB%83%20lo%E1%BA%A1i%3A%20Vua%20nh%C3%A0%20T%C3%A2y%20S%C6%A1n&cmlimit=20
	 */
	public static List<String> categoryMembers(String category) {
		try {
			String url = String.format(URL_CATEGORY_MEMBERS, URLEncoder.encode("Thể loại:"+category, "UTF-8"));
			
			try (InputStream is = new URL(url).openStream();
					Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				
				return QueryResult.toArticleTitles(reader);
			}
		} catch (IOException e) {
			e.printStackTrace();
			
			return null;
		}
	}

	
	public static void main(String[] args) {
//		queryArticleByTitle("Đài Tiếng nói Việt Nam");
		
		categoryMembers("Vua nhà Tây Sơn");
	}
}
