package nyx.collections.test;

/**
 * Utility methods to generate random alphanumeric strings.
 * 
 * @author varlou@gmail.com
 */
public class StrTestUtils {

	/**
	 * Populates given char array with random string using characters from
	 * specified character set. Can be used for memory conservation purposes.
	 * 
	 * @param str
	 * @param charSet  
	 */
	public static void fillWithRandomChars(char[] str, String charSet) {
		char[] ALPHA_NUM = charSet.toCharArray();
		for (int i = 0; i < str.length; i++) {
			str[i] = ALPHA_NUM[(int) (Math.random() * ALPHA_NUM.length)];
		}
	}

	/**
	 * Populates given char array with random string. Can be used for memory
	 * conservation purposes.
	 * 
	 * @param str
	 */
	public static void fillWithRandomAlphaNum(char[] str) {
		char[] ALPHA_NUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
		for (int i = 0; i < str.length; i++) {
			str[i] = ALPHA_NUM[(int) (Math.random() * ALPHA_NUM.length)];
		}
	}

	/**
	 * Generates random alphanumeric String of the specified length.
	 * 
	 * @param length
	 */
	public static String randomAlphaNum(int length) {
		char[] result = new char[length];
		fillWithRandomAlphaNum(result);
		return new String(result);
	}

	/**
	 * Generates random alphanumeric String of the specified length using characters specified in charSet.
	 * 
	 * @param length
	 * @param charSet character set 
	 */
	public static String randomStr(int length, String charSet) {
		char[] result = new char[length];
		fillWithRandomChars(result, charSet);
		return new String(result);
	}

}
