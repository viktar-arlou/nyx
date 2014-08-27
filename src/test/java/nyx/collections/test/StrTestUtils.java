package nyx.collections.test;

/**
 * Utility methods to generate random alphanumeric strings.
 * 
 * @author varlou@gmail.com
 */
public class StrTestUtils {

	/**
	 * Populates given char array with random string. Can be used for memory conservation purposes.
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

}
