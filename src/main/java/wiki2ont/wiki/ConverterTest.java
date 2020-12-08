package wiki2ont.wiki;

import java.io.IOException;

import info.bliki.wiki.BlikiConverter;
import info.bliki.wiki.model.WikiModel;

public class ConverterTest {
	public static void main(String[] args) throws IOException {
//		WikiModel.toHtml("{{ngày sinh và tuổi|1970|4|11}}", System.out);
		BlikiConverter.main(args);
	}
}
