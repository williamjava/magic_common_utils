package com.magic.common.utils.base;

import java.util.Random;

/**
 * 随机码生成工具类
 * 
 * @author wuhoujian
 * @date 2019/3/1
 *
 */
public class CodeGeneratorUtil {
	//码是字母
	public static final String CODE_GENERATE_TYPE_CHAR = "char";

	//码是数字
	public static final String CODE_GENERATE_TYPE_NUM = "num";

	/**
	 * 生成随机码方法
	 * 
	 * @param length
	 *            随机码的长度
	 * @return 随机码
	 */
	public static String genCodes(int length) {
		StringBuffer code = new StringBuffer();

		Random random = new Random();
		for (int i = 0; i < length; i++) {
			String charOrNum = random.nextInt(2) % 2 == 0 ? CODE_GENERATE_TYPE_CHAR
					: CODE_GENERATE_TYPE_NUM; // 输出字母还是数字

			if (CODE_GENERATE_TYPE_CHAR.equalsIgnoreCase(charOrNum)){ // 字符串
				int choice = random.nextInt(2) % 2 == 0 ? 65 : 97; // 取得大写字母还是小写字母
				code.append((char) (choice + random.nextInt(26)));
			} else if (CODE_GENERATE_TYPE_NUM.equalsIgnoreCase(charOrNum)){ // 数字
				code.append(String.valueOf(random.nextInt(10)));
			}
		}

		return code.toString().toUpperCase();
	}

	/**
	 * 生成指定长度的数字随机码
	 * 
	 * @param length
	 *            随机码的长度
	 * @return 数字随机码
	 */
	public static String genNumCodes(int length) {
		StringBuffer code = new StringBuffer();

		Random random = new Random();
		for (int i = 0; i < length; i++) {
			code.append(String.valueOf(random.nextInt(10)));
		}

		return code.toString();
	}
}
