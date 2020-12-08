package wiki2ont.wiki;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.bliki.wiki.dump.WikiPatternMatcher;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.WikiModel;
import wiki2ont.AppConfig;

public class PatternMatcher {
	private WikiPatternMatcher matcher;
	private InfoBox infoBox;

	private List<String> templates;
	private String content;

	private boolean redirect = false;
	private String redirectString = null;
	private List<String> pageCats = null;

	private final static Pattern CATEGORY_PATTERN = Pattern.compile("\\[\\[Thể loại:(.*?)\\]\\]", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	public PatternMatcher(String text) {
		this.matcher = new WikiPatternMatcher(text);

		// redirect
		parseRedirect();

	}

	private void parseRedirect() {
		final Pattern[] REDIRECT_PATTERNS = { 
				Pattern.compile("#đổi[\\w:\\s]*\\[\\[(.*?)\\]\\]", Pattern.CASE_INSENSITIVE),
				Pattern.compile("#redirect[\\w:\\s]*\\[\\[(.*?)\\]\\]", Pattern.CASE_INSENSITIVE) 
		};

		for (Pattern REDIRECT_PATTERN : REDIRECT_PATTERNS) {
			Matcher matcher = REDIRECT_PATTERN.matcher(this.matcher.getText());
			if (matcher.find()) {
				redirect = true;
				if (matcher.groupCount() == 1)
					redirectString = matcher.group(1);
			}
		}
	}

	public String getContent() {
		if (content == null) {
			parseContentAndTemplates();
		}

		return content;
	}

	public List<String> getTemplates() {
		if (templates == null) {
			parseContentAndTemplates();
		}

		return this.templates;
	}

	/**
	 * parse wiki text for templates & content
	 */
	private void parseContentAndTemplates() {
		StringBuilder sb = new StringBuilder(this.matcher.getText());
		String WIKI_TAG_CONST_STR = "{{";
		templates = new ArrayList<>();

		int startPos = sb.indexOf(WIKI_TAG_CONST_STR);
		while (startPos >= 0) {
			int bracketCount = 2;
			int endPos = startPos + WIKI_TAG_CONST_STR.length();

			for (; endPos < sb.length(); endPos++) {
				switch (sb.charAt(endPos)) {
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

			try {
				String template = sb.substring(startPos, endPos + 1);

				template = Utils.stripWikiHtmlFormat(template);
				templates.add(template);
				sb.delete(startPos, endPos + 1);

			} catch (IndexOutOfBoundsException e) {
				// bugfix: prob invalid format: end of string without }}
				break;
			}

			// update startPos
			startPos = sb.indexOf(WIKI_TAG_CONST_STR);
		}

		content = Utils.stripWikiHtmlFormat(sb.toString()).trim();
	}

	public boolean isRedirect() {
		// en || vi
		return this.matcher.isRedirect() || this.redirect;
	}

	public String getRedirectText() {
		// en
		if (this.matcher.isRedirect()) {
			return this.matcher.getRedirectText();
		}

		// vi
		return this.redirectString;
	}

	public InfoBox getInfoBox() {
		if (infoBox == null) {
			info.bliki.wiki.dump.InfoBox en = this.matcher.getInfoBox();
			if (en != null) {
				infoBox = InfoBox.create(this.matcher.getInfoBox());
			} else {
				infoBox = parseInfoBox();
			}
		}

		return this.infoBox;
	}

	private InfoBox parseInfoBox() {
		final String[] INFOBOX_CONST_STRS = { "{{Hộp thông tin", "{{Thông tin" };
		String wikiText = this.matcher.getText();

		for (String INFOBOX_CONST_STR : INFOBOX_CONST_STRS) {
			int startPos = wikiText.indexOf(INFOBOX_CONST_STR);
			if (startPos < 0)
				continue;
			int bracketCount = 2;
			int endPos = startPos + INFOBOX_CONST_STR.length();

			if (endPos >= wikiText.length()) {
				return null;
			}
			for (; endPos < wikiText.length(); endPos++) {
				switch (wikiText.charAt(endPos)) {
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
			String infoBoxText;
			if (endPos >= wikiText.length()) {
				infoBoxText = wikiText.substring(startPos);
			} else {
				infoBoxText = wikiText.substring(startPos, endPos + 1);
			}

			infoBoxText = stripCite(infoBoxText); // strip clumsy {{cite}} tags
			// strip any html formatting
			infoBoxText = Utils.decodeHtmlEntities(infoBoxText);

			infoBoxText = infoBoxText.replaceAll("<ref.*?>.*?</ref>", " ");
			infoBoxText = infoBoxText.replaceAll("</?.*?>", " ");

			return new InfoBox(infoBoxText);
		}

		return null;
	}

	private String stripCite(String text) {
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

	public List<String> getCategories() {
		if (pageCats == null) {
			parseCategories();
		}

		return pageCats;
	}

	private void parseCategories() {
		pageCats = new ArrayList<>();

		// en
		pageCats.addAll(this.matcher.getCategories());

		// vi
		Matcher matcher = CATEGORY_PATTERN.matcher(this.matcher.getText());

		while (matcher.find()) {
			String[] temp = matcher.group(1).split("\\|");
			pageCats.add(temp[0]);
		}
	}

	public String getSummary() {
		String content = getContent();
		content = toPlainText(content);

		Scanner scanner = new Scanner(content);
		String line = scanner.nextLine();
		scanner.close();

		return trim(line, AppConfig.SUMMARY_LENGTH);
	}

	public String trim(String src, int size) {
		if (src.length() <= size)
			return src;
		int pos = src.lastIndexOf(" ", size - 3);
		if (pos < 0)
			return src.substring(0, size);
		return src.substring(0, pos) + "...";
	}

	public String toPlainText(String text) {
		WikiModel wikiModel = new WikiModel(new Configuration(), Locale.ENGLISH, "${image}", "${title}");
		wikiModel.setUp();
		try {
			return wikiModel.render(new PlainTextConverter(), text, false).trim();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			wikiModel.tearDown();
		}

		return text;
	}

//	@Deprecated
	public String getPlainText(String text) {
		text = Utils.decodeHtmlEntities(text);

		text = text.replaceAll("<ref>.*?</ref>", " ");
		text = text.replaceAll("</?.*?>", " ");
        text = text.replaceAll("\\{\\{.*?\\}\\}", " ");
		text = text.replaceAll("\\[\\[.*?:.*?\\]\\]", " ");
		text = text.replaceAll("\\[\\[(.*?)\\]\\]", "$1");
        text = text.replaceAll("\\s(.*?)\\|(\\w+\\s)", " $2");
		text = text.replaceAll("\\[.*?\\]", " ");
		text = text.replaceAll("\\'+", "");
		return text;
	}

	/**
	 * @requires this.infobox neq null
	 */
	public String getInfoBoxTemplate() {
		Scanner scanner = new Scanner(this.infoBox.getText().trim());// remove empty lines
		String line = scanner.nextLine(); // first line

		line = line.replace("{{", "");

		// in case of 1-line infobox
		if (line.indexOf('|') > 0) {
			line = line.substring(0, line.indexOf('|'));
		}

		scanner.close();

		return line.trim();
	}

	// TODO: improve in case of 1-line infobox
	public Map<String, String> getInfoBoxAttributes() {
		Scanner scanner = new Scanner(getPlainText(this.infoBox.getText()));
		
		Map<String, String> attributes = new HashMap<>();
		String attribute = null;
		String value = null;

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine().trim();

			if (line.trim().isEmpty()) {
				continue;
			}

			if (line.equals("}}")) {
				break;
			}
			
			try {
				if (line.startsWith("|")) { // new
					int index = line.indexOf('=');
					attribute = line.substring(1, index).trim();
					value = line.substring(index + 1).trim();
					attributes.put(attribute, value);
				} else if (attribute != null) { // multiple line attribute values
					value += ", " + line;
					attributes.put(attribute, value);
				}
			} catch (Exception e) {
				continue;
			}
		}

		scanner.close();
		
		
		// remove empty attributes
		Map<String, String> result = new HashMap<String, String>();
		for (String key : attributes.keySet()) {
			String v = attributes.get(key);
			
			if (attributes.get(key).trim().isEmpty()) {
				result.put(key, v);
			}
		}
		
		return result;
	}

	public static void main(String[] args) {
		String test = "{{Infobox animanga/Header\n" + "| name            = Denpa Kyōshi\n"
				+ "| image           = [[Tập tin:Denpa cover.jpg|230px]]\n"
				+ "| caption         = Bìa của tập 1, các nhân vật chính Jun'ichirō và Suzune Kagami\n"
				+ "| ja_kanji        = 電波教師\n" + "| ja_romaji       = Denpa Kyōshi\n"
				+ "| genre           = &lt;!-- NEED SOURCE [[Comedy]], [[Romance manga|Romance]], [[Slice of Life]], [[Harem (genre)|Harem]], [[Action (fiction)|Action]] --&gt;\n"
				+ "}}\n" + "{{Infobox animanga/Print\n" + "| type            = Manga\n"
				+ "| author          = Takeshi Azuma\n" + "| illustrator     = \n"
				+ "| publisher       = [[Shogakukan]] \n" + "| demographic     = ''[[Shōnen manga|Shōnen]]''\n"
				+ "| imprint           = Shōnen Sunday Comics\n" + "| magazine        = [[Weekly Shōnen Sunday]]\n"
				+ "| first           = 2 tháng 11 năm 2011\n" + "| last            = 29 tháng 3 năm 2017\n"
				+ "| volumes         = 26\n" + "| volume_list     = \n" + "}}\n" + "{{Infobox animanga/Video\n"
				+ "| type         = TV series\n" + "| director     = Masato Sato\n" + "| producer     = \n"
				+ "| writer       = Atsushi Maekawa\n" + "| music        = Ryuuichi Takada (Monaca)\n"
				+ "| studio       = [[A-1 Pictures]]\n" + "| licensee     = {{English anime licensee\n"
				+ "  | NA=[[Funimation]]\n" + "}}\n"
				+ "| network      = [[Nippon Television Network System|NNS]] ([[Yomiuri Telecasting Corporation|ytv]])\n"
				+ "| network_en   = {{English anime network|US=[[Funimation]]}}\n"
				+ "| first        = 4 tháng 4 năm 2015\n" + "| last         = 26 tháng 9 năm 2015\n"
				+ "| episodes     = 24\n" + "| episode_list = \n" + "}}\n" + "{{Infobox animanga/Footer}}\n" + "\n"
				+ "{{Nihongo|'''Denpa Kyōshi'''|{{ruby|電波教師|でんぱきょうし}}||hanviet=Điện ba Giáo sư|kyu=|hg=|kk=|&quot;Thầy giáo Sóng điện&quot;, tựa [[tiếng Anh]]: ''He is an Ultimate Teacher''}} là một loạt [[Manga|truyện tranh]] [[Nhật Bản]] năm 2011, được viết và minh họa bởi Takeshi Azuma.&lt;ref name=&quot;ANN1&quot;&gt;&lt;cite class=&quot;citation web&quot;&gt;[http://www.animenewsnetwork.com/news/2013-02-17/denpa-kyoshi-manga-2nd-anime-ad-by-a-1-pictures-aired &quot;''Denpa Kyōshi'' Manga's 2nd Anime Ad by A-1 Pictures Aired&quot;]. &lt;/cite&gt;&lt;/ref&gt; Hai clip [[anime]] ngắn, được dùng để quảng bá cho bộ truyện này, đã được phát hành, và một loạt [[anime]] truyền hình được sản xuất, tất cả đều bởi [[A-1 Pictures]].&lt;ref name=&quot;ANN1&quot;/&gt;&lt;ref&gt;&lt;cite class=&quot;citation web&quot;&gt;Green, Scott (ngày 3 tháng 10 năm 2012). &lt;/cite&gt;&lt;/ref&gt;\n"
				+ "\n" + "== Cốt truyện ==\n"
				+ "Câu chuyện kể về Kagami Jun'ichirō, với một người em gái là Suzune đang tức giận anh ta vì của anh ta hoàn toàn không quan tâm gì đến thế giới thực. Do Jun'ichirō chỉ quan tâm đến anime, manga và games, Suzune bắt anh phải đi làm việc và trở thành một thầy giáo vật lý thay thế ở ngôi trường mà anh từng học. Jun'ichirō chứng minh mình là một giáo viên có năng lực và chăm chỉ, một người coi các phương pháp chính thống để dạy dỗ học sinh dựa trên kiến thức là vô ích, anh dạy dỗ và động viên học sinh của mình theo cái cách của một [[otaku]] chính hiệu.\n"
				+ "\n" + "== Nhân vật ==\n" + "\n" + "=== Nhân vật chính ===\n"
				+ "; {{Nihongo|Kagami Jun'ichirō|鑑 純一郎||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Ono Daisuke (Drama CD), Kamiya Hiroshi (anime)(Japanese), Anthony Bowling (English)&lt;ref&gt;&lt;cite class=&quot;citation web&quot;&gt;[http://www.animenewsnetwork.com/news/2015-02-16/hiroshi-kamiya-leads-denpa-kyoshi-tv-anime-cast/.85034 &quot;Hiroshi Kamiya Leads Denpa Kyōshi TV Anime's Cast&quot;]. &lt;/cite&gt;&lt;/ref&gt;\n"
				+ ": Nhân vật chính là một [[otaku]] 24 tuổi, suốt ngày chỉ ngồi ở nhà chăm sóc blog [[anime]] của mình cho đến khi anh bị cô em gái bắt phải trở thành giáo viên Vật lý ở trường cũ của mình. Jun'ichirō là người đã xuất bản một bài báo Vật lý làm chấn động cả giới khoa học khi mới 17 tuổi. Anh đã đề xuất một lý thuyết để tạo ra một thiết bị dịch chuyển không gian như &quot;Cánh cửa thần kì&quot; của [[Doraemon]]. Mặc dù bị các học giả chế nhạo, nhưng cho đến nay, mọi nỗ lực bác bỏ lý thuyết này đều thất bại, cho dù có thể mất hàng thế kỉ cho đến khi con người phát triển được công nghệ cần thiết để ráp được nó.\n"
				+ ": Mặc dù được mời về làm việc tại các trung tâm nghiên cứu quan trọng nhất trên thế giới, Jun'ichirō tuyên bố bị một bệnh gọi là&quot;YD&quot;, &quot;chỉ cho phép anh làm những gì anh muốn làm&quot;, vì thế anh từ chối tất cả mọi lời mời của họ. Tuy nhiên, sau vài tuần làm việc như một giáo viên, anh ta bị thuyết phục bởi Hiiragi Koyomi và trở thành giáo viên ở trường học của mình. Kể từ đó, Jun'ichirō chia thời gian của mình giữa việc sử dụng các phương pháp khác lạ để thay đổi những học sinh bỏ học với tập trung vào những sở thích của mình và đôi khi tìm ra một cách để kết hợp cả hai thứ với nhau, thường thông qua việc sử dụng các video games cạnh tranh. Anh có một thói quen đặt biệt danh cho mọi người anh gặp trong suốt cuộc phiêu lưu giảng dạy của mình.\n"
				+ "; {{Nihongo|Kagami Suzune|鑑 純音||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Kana Asumi (Drama CD), Rena matsui (anime)(Japanese), Mikaela Krantz (English)&lt;ref name=&quot;engcast&quot;&gt;&lt;cite class=&quot;citation web&quot;&gt;Justin Rojas (ngày 5 tháng 6 năm 2015). &lt;/cite&gt;&lt;/ref&gt;&lt;ref&gt;&lt;cite class=&quot;citation web&quot;&gt;[http://www.animenewsnetwork.com/news/2015-03-02/denpa-kyoshi-anime-casts-ske48-idol-rena-matsui-as-suzune/.85517 &quot;Denpa Kyōshi Anime Casts SKE48 Idol Rena Matsui as Suzune&quot;]. &lt;/cite&gt;&lt;/ref&gt;\n"
				+ ": Em gái của Jun'ichirō's, là một người luôn luôn nổi giận vì những trò hề của anh trai mình. Sở thích của cô là luyện  đấu vật, và cô là người duy nhất mà Jun'ichirō thực sự lo ngại. Cô cũng là người quản lý tài chính cho cả nhà và cấm Jun'ichirō mua bất cứ thứ gì mà không có sự đồng ý của cô. Mặc dù anh gây cho cô rất nhiều rắc rối, Suzune rất thích anh và muốn được chăm sóc anh mãi mãi. Cô có thói quen đánh người khác, thường là Jun'ichirō, khi cô buồn hay giận họ. \n"
				+ "\n" + "=== Cao trung Đông Shinmei ===\n"
				+ "; {{Nihongo|Kanō Minako|叶 美奈子||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Sora Amamiya (anime)(Japanese), Sarah Wiedenheft (English)&lt;ref name=&quot;engcast&quot;/&gt;&lt;ref name=&quot;cast1&quot;&gt;&lt;cite class=&quot;citation web&quot;&gt;[http://www.animenewsnetwork.com/news/2015-02-17/sora-amamiya-aki-toyosaki-suzuko-mimori-more-join-denpa-kyoshi/.85101 &quot;Sora Amamiya, Aki Toyosaki, Suzuko Mimori, More Join Denpa Kyōshi&quot;]. &lt;/cite&gt;&lt;/ref&gt;\n"
				+ ": Từng là người đứng đầu băng đảng Black Oracle người ghét thế giới, vì nó không có sự tồn tại của anh hùng. Tuy nhiên, sau khi nhận được lời động viên từ Jun'ichirō trong một cuộc trò chuyện trực tuyến &quot;nếu thế giới không có anh hùng, hãy tự mình trở thành một người như thế&quot;, cô quyết định làm lại bản thân và trở thành một diễn viên lồng tiếng. Jun'ichirō đặt biệt danh cô là &quot;Ganmen-Punch&quot;(顔面パンチ ''Ganmen-panchi'').\n"
				+ "; {{Nihongo|Kitō Miho|鬼頭 ミホ||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Yuiko Tatsumi&amp;#x20;(Japanese); Jamie Marchi (English)\n"
				+ ": Một thành viên khác trong băng Black Oracle luôn nỗ lực để mang Kano trở về con đường tội phạm trước đây. Sau khi nhận được &quot;bài học bắt nạt&quot; của Jun'ichirō, cô ấy đã kết thúc tình bạn với Kano, và theo đuổi ước mơ trở thành một đầu bếp nổi tiếng như bà của cô. Biệt danh Jun'ichirō đặt cho cô là &quot;Wicked Blondie&quot;.\n"
				+ "; {{Nihongo|Kuribayashi Yukino|栗林 雪乃||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Yumi Uchiyama (Japanese), Kristin Sutton (English)\n"
				+ ": Một nhân vật [[anime]] cổ điển, là lớp trưởng lớp của Minako. Một Meganekko ban đầu ghét Kagami do anh không chịu giảng dạy đàng hoàng, cho đến khi cô hiểu anh hơn.\n"
				+ "\n" + "=== Học viện Hiiragi ===\n" + "\n" + "==== Icho Branch ====\n"
				+ "; {{Nihongo|Hiiragi Koyomi|柊 暦||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Suzuko Minori (anime)(Japanese), Jād Saxton (English)&lt;ref name=&quot;engcast&quot; /&gt;&lt;ref name=&quot;cast1&quot; /&gt;\n"
				+ ": Koyomi là chủ tịch của Icho Academy. Cô tin rằng Nhật Bản là một nơi rất nhàm chán đối với cô, và mục tiêu của cô là biến nó trở thành một nơi thú vị hơn. Cô bắt đầu thuê Jun'ichirō sau khi thấy được cách dạy của anh, và luôn ủng hộ những ý tưởng của Jun'ichirō nếu như nó làm cô thích thú, cho dù chúng ngớ ngẩn như thế nào đi chăng nữa. Biệt danh của Jun'ichirō cho cô là {{Nihongo|&quot;Option tsuki&quot;|オプション付き|Opushon-tsuki|hanviet=|kyu=|hg=|kk=|}}.\n"
				+ "; {{Nihongo|Momozono Makina|桃園 マキナ||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Yōko Hikasa (Drama CD), Saori Ōnishi (anime) (Japanese); Caitlin Glass (English)&lt;ref name=&quot;cast1&quot;/&gt;\n"
				+ ": Makina là chủ tịch hội học sinh và là đội trưởng của câu lạc bộ Kendo tại Icho Academy. Cô có thể đuổi việc bất kì giáo viên nào không đáp ứng dược các tiêu chuẩn của mình. Mặc dù vậy, cô lại hay bị Jun'ichirō lôi kéo vào những trò hề của anh ta, cho dù điều đó làm cô rất bực mình. Jun'ichirō đặt biệt danh cho cô là {{Nihongo|&quot;Hensoku Twintails&quot;|変則ツインテール|Hensoku Tsuintēru|hanviet=|kyu=|hg=|kk=|}}. Cô có một người em trai tên là Kazuya.\n"
				+ "; {{Nihongo|Nanami Seijūrō|七海 征十郎||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Yoshimasa Hosoya (anime) (Japanese); Jarrod Greene (English)&lt;ref name=&quot;cast1&quot;/&gt;\n"
				+ ": Nanami là một người to con, hung hăng đã mất đi lý trí của mình trong cuộc sống sau khi không thể chơi bóng chày được nữa do chấn thương. Jun'ichirō đã giúp cho Nanami thích chơi bóng đá và trở thành một thành viên của câu lạc bộ bóng đá Icho Academy. Anh hay cãi nhau với Makina và hay động viên Kōtarō. Anh là thành viên cũ của Black Oracle. Do thân hình cơ bắp, Jun'ichirō gọi anh là {{Nihongo|&quot;Kaizō Ningen&quot;|改造人間||hanviet=|kyu=|hg=|kk=|}}.\n"
				+ "; {{Nihongo|Tanaka Sachiko|田中 さち子||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Marina Inoue (Drama CD), Ayane Sakura (anime) (Japanese); Apphia Yu (English)&lt;ref name=&quot;cast1&quot;/&gt;\n"
				+ ": Sachiko là một học sinh không bao giờ xuất hiện ở trường cho đến khi Jun'ichirō bị Makina bắt mang cô ấy trở về trường học. Cô ấy là một trong số những tác giả [[manga]] mà anh thích, người đã viết ''Shuumatsu Gakuen.'' Bút danh của cô là {{Nihongo|''Tenjōin Kisaki''|天上院 騎咲||hanviet=|kyu=|hg=|kk=|}}, nên Jun'ichirō gọi cô là &quot;Kisaki-sensei&quot;.\n"
				+ "; {{Nihongo|Araki Kōtarō|荒木 光太郎||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Miyuki Sawashiro (Drama CD), Aki Toyosaki (anime) (Japanese); Tia Ballard (English)&lt;ref name=&quot;cast1&quot;/&gt;\n"
				+ ": Bạn đồng hành của Jun'ichirō's trong MMORPG &quot;Ouroboros&quot; dưới cái tên {{Nihongo|&quot;Luce&quot;|ルーチェ|Rūche}}. Họ không biết danh tính thật của nhau cho đến khi Jun'ichirō phát hiện ra anh chính là một học sinh bỏ học. Kōtarō thích trap, và khi anh ta mặc đồ con gái, tất cả mọi con trai biết bí mật của anh ta đều vờ như không biết, đến mức Koyomi cấm anh ăn mặc như con trai ở trường. Ban đầu anh từ chối trở lại trường do lo sợ sẽ bị chối bỏ, nhưng sau khi đấu với Jun'ichirō mà sau đó đã phải từ bỏ tài khoản của mình, anh đã có lại sự tự tin để chấp nhận chính mình. Nhờ sự động viên của Jun'ichirō, Kōtarō đã có dược những người bạn chấp nhận con người thật của anh và anh trở lại trường.\n"
				+ "; {{Nihongo|Shikishima Kiriko|式島 切子||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Azusa Tadokoro (anime)&lt;ref name=&quot;cast1&quot;/&gt; (Japanese); Dawn M. Bennett (English)\n"
				+ ": Kiriko làm việc ở một maid cafe tên Heart-On-Heart, quán cà phê yêu thích của Kagami, nhưng do học sinh trường Icho không được làm thêm bên ngoài, cô đã phải nghỉ việc khi Makina phát hiện ra. Jun'ichirō đã thuyết phục Makina cho cô ấy là một ngoại lệ. Jun'ichirō gọi cô là {{Nihongo|&quot;Potato&quot;|ポテト|Poteto|hanviet=|kyu=|hg=|kk=|}} do cô thường phục vụ khoai tây chiên cho anh.\n"
				+ "; {{Nihongo|Chinami Kanan|千波 花音||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Rina Hidaka (anime)&lt;ref name=&quot;cast1&quot;/&gt; (Japanese); Monica Rial&lt;ref&gt;&lt;cite class=&quot;citation web&quot;&gt;johnalmoete7324 (ngày 18 tháng 7 năm 2015). &lt;/cite&gt;&lt;/ref&gt; (English)\n"
				+ ": Kanan là một người bạn của Suzune, người Suzune muốn Jun'ichirō giúp trong môn vật lý. Cô là một người rất ít nói do có một giọng nói nghe giống như một nhân vật [[anime]], do đó biệt danh của cô là {{Nihongo|&quot;Animekko&quot;|アニメっ娘||hanviet=|kyu=|hg=|kk=|}}. Cô giao tiếp bằng cách nhắn tin điện thoại cho đến khi Jun'ichirō giúp cô vượt qua nỗi sợ bị chế nhạo vì giọng nói của mình.\n"
				+ "; {{Nihongo|Kuramachi Madoka|蔵持 円||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: [[Ichimichi Mao|Mao Ichimichi]] (M·A·O)&lt;ref name=&quot;cast2&quot;&gt;&lt;cite class=&quot;citation web&quot;&gt;[http://www.animenewsnetwork.com/news/2015-06-09/ultimate-otaku-teacher-casts-daisuke-namikawa-m.a.o-ayahi-takagaki/.89067 &quot;Ultimate Otaku Teacher Casts Daisuke Namikawa, Mao Ichimichi., Ayahi Takagaki&quot;]. &lt;/cite&gt;&lt;/ref&gt; (Japanese); Alexis Tipton (English)\n"
				+ ": Cô là thủ quỹ của hội học sinh Icho Academy, nhờ có khả năng định giá mọi thứ mà cô nhìn thấy (trừ Jun'ichirō, do đó cô rất có hứng thú với anh). Cô cũng là con gái của một công ty game nổi tiếng là KMC, và vì thế cô bị dính vào một cuộc hôn nhân giữa công ty của cô với một công ty khác đang muốn mua lấy KMC. Jun'ichirō đặt cho cô là ''Sukoshi no miss okane'' (少しのミスお金 Sukoshi no misu okane)\n"
				+ "; {{Nihongo|Kushinada Moemi|櫛名田 萌美||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Shiina Natsukawa&lt;ref name=&quot;cast2&quot;/&gt; (Japanese); Leah Clark (English)\n"
				+ ": Là một thần tượng đối thủ của Taki trong các sự kiện của Familin Ranger Brothers Squadron, có một số điểm hoàn hảo, nhưng bị đánh bại bởi Nagaru (người thay thế cho Taki) người cũng đạt số điểm tuyệt đối, dẫn đến việc Nagaru chiến thắng do đồng hạng. Sau đó, Moemi cũng được tiết lộ là học sinh của Icho Academy khi Jun'ichirō đi ngang qua cô trong trại huấn luyện của Icho Academy tại bãi biển. biệt danh của cô là &quot;Wild Dollar&quot;.\n"
				+ "\n" + "=== Nhân vật khác ===\n"
				+ "; {{Nihongo|Tim Bernards Lynn|ティム・バーナーズ・リン|Timu Bānāzu Rin|hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Ayahi Takagaki&lt;ref name=&quot;cast2&quot;/&gt; (Japanese); Colleen Clinkenbeard (English)\n"
				+ ": Một đồng nghiệp cũ của Jun'ichirō, người đã thành lập và chỉ đạo CERM, một phòng thí nghiệm nghiên cứu Vật Lý Hạt thành lập tại Geneva, Thụy Sĩ, trong một nỗ lực để bác bỏ lý thuyết về việc tạo ra một thiết bị dịch chuyển của Jun'ichirō.\n"
				+ "; {{Nihongo|Tōne Yamato|刀祢 大和||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Aya Hirano&lt;ref name=&quot;cast2&quot;/&gt; (Japanese); Mallorie Rodak (English)\n"
				+ ": Một cựu học sinh cao trung cùng lớp với Jun'ichirō.  Trong quá khứ, Yamato là thành viên duy nhất của câu lạc bộ khoa học của trường cho đến khi Jun'ichirō quyết định tham gia nó để tìm hiểu làm thế nào để tạo ra một cánh cửa Dokodemo, và kết thúc là việc giảng dạy vật lý bởi Yamato trong quá trình này.\n"
				+ "; {{Nihongo|Hell Gates|ヘル・ゲイツ|Heru Geitsu|hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Daisuke Namikawa&amp;#x20;(Japanese);&lt;ref name=&quot;cast2&quot;/&gt; Eric Vale (English)\n"
				+ ": CEO của công ty game lớn nhất thế giới, Activision Frigate và là chồng chưa cưới bị ép buộc của Madoka.\n"
				+ "; {{Nihongo|Momozono Kazuya|桃園 一也||hanviet=|kyu=|hg=|kk=|}}: Lồng tiếng bởi: Tomoko Tsuzuki&lt;ref name=&quot;cast2&quot;/&gt; (Japanese); Terri Doty (English)\n"
				+ ": Em trai của Makina Momozono.\n"
				+ "; {{Nihongo|Nishikujō Matome|西九条 まとめ||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Risa Taneda&lt;ref name=&quot;cast2&quot;/&gt; (Japanese); Kristi Kang (English)\n"
				+ ": Một con ma ám trong trại huấn luyện của Hiragi Academy, và thích đọc [[manga]] để giết thời gian. Cô được đặt cho biệt danh &quot;Ghost Artist&quot; do ước muốn được tạo ra manga của riêng mình. Matome gặp Jun'ichirō khi anh tình cờ thấy cô trên đỉnh một đống manga trong thư viện của trại huấn luyện, và cô hỏi anh cách để được &quot;siêu thoát&quot; sang thế giới bên kia, Jun'ichirō nghĩ rằng có vấn đề gì đó với đống manga mà cô đã đọc suốt thời gian qua.\n"
				+ "; {{Nihongo|Komiya Nagare|小宮 流||hanviet=|kyu=|hg=|kk=|}} và {{Nihongo|Komiya Taki|小宮 滝||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Satsumi Matsuda&amp;#x20;(Japanese); Bryn Apprill (English) (Nagare) &amp; Voiced by: Risae Matsuda&amp;#x20;(Japanese); Felecia Angelle (English) (Taki) &lt;ref name=&quot;cast2&quot;/&gt;\n"
				+ ": Cặp sinh đôi 15 tuổi. Jun'ichirō gặp người chị, Taki, tại một game arcade trong kì nghỉ hè. Tại đây, Taki mời Jun'ichirō chơi ở arcade với cô ấy như một lời cảm ơn vì đã trở thành Momozono, người đã đưa cô về nhà, cắt đuôi một nhóm côn đồ trong khu vực. Nhưng thật ra, họ đều là bạn bè của Taki, và cô ấy là thủ lĩnh của băng Black Oracle Neo; tách ra từ băng Black Oracle của Kanou. Người em gái, Nagare, là ca sĩ thần tượng của nhóm nhạc 5th Queens, nơi mà sau đó cô gọi Jun'ichirō ra để gặp riêng, nhờ anh đưa Taki ra khỏi băng đảng. Biệt danh của Taki là &quot;Leader&quot;, còn của Nagare là &quot;Idol-Sis&quot;.\n"
				+ "; {{Nihongo|Kitayama Minami|北山 みなみ||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Lồng tiếng bởi: Akemi Kanda (Japanese); Megan Emerick (English)\n"
				+ ": Maid trưởng của quán Heart-On-Heart café.\n"
				+ "; {{Nihongo|Kuon Kasane|久遠 重音||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Kasane là bạn thời thơ ấu của Jun'ichirō và họ còn thân hơn thế. Cô ấy bị bệnh từ nhỏ và đã chết khi cô 17 tuổi, nhưng Jun'ichirō nghĩ rằng anh phải chịu trách nhiệm cho cái chết của cô ấy.\n"
				+ "; {{Nihongo|Kuon Nayuta|久遠 那由他||hanviet=|kyu=|hg=|kk=|}}\n"
				+ ": Em gái của Kasane, người muốn giết Jun'ichirō để trả thù cho chị mình, do anh chịu trách nhiệm cho cái chết của chị cô. Cô đặt một chiếc vòng lên cổ tay của giáo viên và nó không thể tháo ra nếu như anh không thắng hết tất cả các game mà cô đã chuẩn bị sẵn cho anh, và sẽ bơm một chất độc chết người vào anh nếu anh thua.Để cuẩn bị cho sự trả thù của mình, cô không chỉ nghiên cứu hồ sơ của Jun'ichirō's profile, mà của cả những người liên quan đến tất cả học sinh và bạn bè của anh.\n"
				+ "\n" + "== Truyền thông ==\n" + "\n" + "=== Manga ===\n"
				+ "Loạt truyện được đăng tuần tự trên tạp chí [[Shogakukan]]'s ''[[Shōnen Sunday|Weekly Shōnen Sunday]]'', được công bố vào ngày 2 tháng 10 năm 2011.&lt;ref&gt;&lt;cite class=&quot;citation web&quot;&gt;[http://natalie.mu/comic/news/59020 &lt;bdi lang=&quot;ja&quot;&gt;「超弩級少女」の東毅、新連載の主役はギークな高校教師&lt;/bdi&gt;] (in Japanese). &lt;/cite&gt;&lt;/ref&gt; Tính đến 17 tháng 6 năm 2016, đã có tổng cộng 20 tập truyện được xuất bản.&lt;ref&gt;&lt;cite class=&quot;citation web&quot;&gt;[http://www.amazon.co.jp/dp/4091272681 &lt;bdi lang=&quot;ja&quot;&gt;電波教師 22 (少年サンデーコミックス)&lt;/bdi&gt;] (in Japanese). &lt;/cite&gt;&lt;/ref&gt;\n"
				+ "\n" + "=== Anime ===\n" + "Bài hát mở đầu &quot;Youthful Dreamer&quot; bởi TrySail (tập 1-12)\n"
				+ "\n" + "Bài hát kết thúc &quot;DREAMIN&quot; bởi Tokyo Performance Doll (tập 1-12)\n" + "\n"
				+ "Bài hát mở đầu &quot;Vivid Brilliant door&quot; bởi Sphere (tập 13-24)\n" + "\n"
				+ "Bài hát kết thúc &quot;MY ONLY ONE&quot; bởi 9nine (episodes 13-24)\n" + "\n" + "== Ghi chú ==\n"
				+ "&lt;references group=&quot;lower-alpha&quot;/&gt;\n" + "==Tham khảo==\n"
				+ "&lt;div class=&quot;reflist&quot; style=&quot; list-style-type: decimal;&quot;&gt;\n"
				+ "{{tham khảo}}&lt;/div&gt;\n" + "\n" + "== Liên kết ngoài ==\n"
				+ "* {{Trang web chính thức|http://websunday.net/rensai/denpa}} (Japanese) \n"
				+ "* {{Ann|manga|14837}}\n" + "\n"
				+ "[[Thể loại:Articles with Japanese-language external links|Category:Articles with Japanese-language external links]]\n"
				+ "[[Thể loại:Manga năm 2011]]\n" + "[[Thể loại:Anime và manga hài]]\n" + "[[Thể loại:Funimation]]\n"
				+ "[[Thể loại:Manga dài tập]]";

		PatternMatcher matcher = new PatternMatcher(test);

//		String content = matcher.getContent();
//		System.out.println(matcher.toPlainText(content));

		System.out.println(matcher.getSummary());

	}
}
