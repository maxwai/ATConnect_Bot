package emoji;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class Emoji {
	
	public static final String VARIATION_SELECTOR = "\uFE0F";
	
	public static final String WASTEBASKET = "\uD83D\uDDD1";
	
	public static final String CALENDAR_SPIRAL = "\uD83D\uDDD3";
	public static final String CLOCK_2 = "\uD83D\uDD51";
	
	public static final String ZERO = "\u0030\u20E3";
	public static final String ONE = "\u0031\u20E3";
	public static final String TWO = "\u0032\u20E3";
	public static final String THREE = "\u0033\u20E3";
	public static final String FOUR = "\u0034\u20E3";
	public static final String FIVE = "\u0035\u20E3";
	public static final String SIX = "\u0036\u20E3";
	public static final String SEVEN = "\u0037\u20E3";
	public static final String EIGHT = "\u0038\u20E3";
	public static final String NINE = "\u0039\u20E3";
	public static final String KEYCAP_TEN = "\uD83D\uDD1F";
	
	public static final List<String> numbersList = Arrays
			.asList(ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, KEYCAP_TEN);
	
	public static String getCleanedUpEmoji(String emoji) {
		return emoji.replace(VARIATION_SELECTOR, "");
	}
	
}
