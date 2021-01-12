package wiki;

import java.util.List;

import wiki.parsers.ArticleParser;

public class Article {
	public long pageID;
	public long version;
	
	public String pageTitle;
	public String openingText;

	public String sourceText;
	public String cirrusDocTitle; // = redirect text
	
	// nullable
	public String thumbnail;
	public String  termDescription;
	public List<String> termAlias;
	
	private List<InfoBox> infoBoxes;
	private List<String> categories;
	
	public boolean isRedirect() {
		return !this.cirrusDocTitle.equals(pageTitle);
	}
	
	public String getRedirect() {
		return this.cirrusDocTitle;
	}
	
	public List<InfoBox> getInfoBoxes() {
		if (infoBoxes == null) {
			infoBoxes = ArticleParser.parseInfoBoxes(sourceText);
		}
		
		return infoBoxes;
	}
	
	public List<String> getCategories() {
		if (categories == null) {
			categories = ArticleParser.parseCategories(sourceText);
		}
		
		return categories;
	}

	@Override
	public String toString() {
		return "Article [pageID=" + pageID + ", version=" + version + ", pageTitle=" + pageTitle + ", openingText="
				+ openingText + ", cirrusDocTitle=" + cirrusDocTitle + ", thumbnail="
				+ thumbnail + ", termDescription=" + termDescription + ", termAlias=" + termAlias + ", infoBoxes="
				+ getInfoBoxes() + ", categories=" + getCategories() + "]";
	}
	
	
}
