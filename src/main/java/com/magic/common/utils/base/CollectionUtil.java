package com.magic.common.utils.base;

import org.apache.commons.collections.CollectionUtils;

import java.util.*;

/**
 * 实用集合工具类
 * 
 * @author wuhoujian
 * 
 * @date 2019/3/1
 *
 */
public class CollectionUtil {
	/**
	 * 计算两个集合的交集
	 * 
	 * @param l1
	 * @param l2
	 * @return
	 */
	public static <T> List<T> getIntersection(List<T> l1, List<T> l2) {
		if (l1 == null) {
			l1 = Collections.emptyList();
		}

		if (l2 == null) {
			l2 = Collections.emptyList();
		}

		List<T> commonElements = new ArrayList<T>();
		commonElements.addAll(l1);
		commonElements.retainAll(l2);

		return commonElements;
	}

	/**
	 * 计算两个集合的差集
	 * 
	 * @param l1
	 * @param l2
	 * @param listNames
	 *            指定参与计算的两个集合的名字，要么都指定，要么都不指定（使用默认名称l1,l2）
	 * @return
	 */
	public static <T> Map<String, List<T>> getDifferenceList(List<T> l1, List<T> l2, String... listNames) {
		if (l1 == null) {
			l1 = Collections.emptyList();
		}

		if (l2 == null) {
			l2 = Collections.emptyList();
		}

		Map<String, List<T>> map = new HashMap<String, List<T>>();
		List<T> commonElements = getIntersection(l1, l2);

		l1.removeAll(commonElements);
		l2.removeAll(commonElements);

		if (listNames.length == 0) {
			map.put("l1", l1);
			map.put("l2", l2);
		} else if (listNames.length == 1) {
			map.put(listNames[0], l1);
			map.put("l2", l2);
		} else {
			map.put(listNames[0], l1);
			map.put(listNames[1], l2);
		}

		return map;
	}

	/**
	 * 去除两个集合中的共同元素，得到它们的差集
	 * 
	 * @param l1
	 * @param l2
	 * @return
	 */
	public static <T> void removeDuplicateElements(List<T> l1, List<T> l2) {
		if (l1 == null) {
			l1 = Collections.emptyList();
		}

		if (l2 == null) {
			l2 = Collections.emptyList();
		}

		List<T> commonElements = getIntersection(l1, l2);
		l1.removeAll(commonElements);
		l2.removeAll(commonElements);
	}

	/**
	 * 组合两个list
	 * 
	 * @param sourceList
	 *            源列表
	 * @param targetList
	 *            目标列表
	 */
	public static <T> void combineList(List<T> sourceList, List<T> targetList) {
		if (CollectionUtils.isNotEmpty(sourceList)) {
			targetList.addAll(sourceList);
		}
	}

	/**
	 * 将一个字符串转化为一个集合
	 * 
	 * @param str
	 * @return
	 */
	public static List<Long> convertString2List(String str) {
		List<String> list = Arrays.asList(str.split(","));

		List<Long> retList = new ArrayList<Long>();
		for (String each : list) {
			retList.add(Long.valueOf(each));
		}

		return retList;
	}
}
