package com.magic.common.utils.base;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.locale.converters.DateLocaleConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

/**
 * Excel操作工具类
 * 
 * @author wuhoujian
 * 
 * @date 2019/3/1
 */
public class ExcelUtils {
	private final static String excel2003L =".xls";
	
	/**
	 * 内置类，用来配置Excel与Bean属性的映射关系
	 */
	public static class CellMapping {

		private String header;

		private String property;

		private String type;

		public CellMapping() {

		}

		public CellMapping(String header, String property) {
			this.header = header;
			this.property = property;
		}

		public String getHeader() {
			return header;
		}

		public void setHeader(String header) {
			this.header = header;
		}

		public String getProperty() {
			return property;
		}

		public void setProperty(String property) {
			this.property = property;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}

	public static void map2excel(List<Map<String, Object>> list, Sheet sheet, List<CellMapping> mappingList)
			throws Exception {
		int colPointer = 0;
		int headerRowNum = 0;
		Row row = accessRow(sheet, headerRowNum++);
		CellStyle dateStyle = null;
		for (CellMapping cm : mappingList) {
			accessCell(row, colPointer++).setCellValue(cm.getHeader());
		}
		for (Map<String, Object> d : list) {
			row = accessRow(sheet, headerRowNum++);
			colPointer = 0;
			for (CellMapping cm : mappingList) {
				Object o = d.get(cm.getProperty());

				Cell cell = accessCell(row, colPointer++);
				if (o == null) {
					continue;
				} else if (String.class.isAssignableFrom(o.getClass())) {
					cell.setCellValue((String) o);
				} else if (Date.class.isAssignableFrom(o.getClass())) {

					cell.setCellValue((Date) o);// 日期存储为Number，显示需求依赖CellStyle
					if (dateStyle == null) {
						dateStyle = sheet.getWorkbook().createCellStyle();
						if (o.toString().trim().length() == 10) {
							dateStyle.setDataFormat(
									sheet.getWorkbook().getCreationHelper().createDataFormat().getFormat("yyyy/m/d"));
						} else {
							dateStyle.setDataFormat(sheet.getWorkbook().getCreationHelper().createDataFormat()
									.getFormat("yyyy-MM-dd HH:mm:ss"));
						}
					}
					cell.setCellStyle(dateStyle);
				} else if (Number.class.isAssignableFrom(o.getClass())) {
					cell.setCellValue(((Number) o).doubleValue());
				} else if (Boolean.class.isAssignableFrom(o.getClass())) {
					cell.setCellValue(((Boolean) o).booleanValue());
				} else if (ByteArrayOutputStream.class.isAssignableFrom(o.getClass())) {// 处理图片
					HSSFPatriarch patriarch = ((HSSFSheet) sheet).createDrawingPatriarch();
					// anchor主要用于设置图片的属性
					HSSFClientAnchor anchor = new HSSFClientAnchor(0, 0, 255, 255, (short) 1, 1, (short) 1, 8);
					// 插入图片
					patriarch.createPicture(anchor, sheet.getWorkbook()
							.addPicture(((ByteArrayOutputStream) o).toByteArray(), HSSFWorkbook.PICTURE_TYPE_JPEG));
				} else {
					cell.setCellValue(o.toString());
				}
			}
		}
	}

	public static <T> List<T> excel2bean(Sheet sheet, Class<T> clazz, List<CellMapping> mappingList) throws Exception {
		return excel2bean(sheet, clazz, mappingList, 0);
	}

	public static <T> List<T> excel2bean(Sheet sheet, Class<T> clazz, List<CellMapping> mappingList, int headerRowNum)
			throws Exception {
		Map<String, Integer> configMap = new HashMap<String, Integer>();
		Row row = sheet.getRow(headerRowNum);
		for (int c = 0; c < row.getLastCellNum(); c++) {
			String key = getCellString(row.getCell(c));
			if (!configMap.containsKey(key)) {
				configMap.put(key, c);
			} else {
				throw new RuntimeException("表头第" + (configMap.get(key) + 1) + "列和第" + (c + 1) + "列重复");
			}
		}

		List<T> resultList = new ArrayList<T>();

		for (int r = headerRowNum + 1; r <= sheet.getLastRowNum(); r++) {
			row = sheet.getRow(r);
			if (row == null)
				break;// 遇空行，表示结束
			T t = clazz.newInstance();
			Map<String, Object> properties = new HashMap<String, Object>();
			boolean flag = true;// 判断整行属性全为空
			for (CellMapping cm : mappingList) {

				Integer index = configMap.get(cm.getHeader());
				if (index == null) {
					continue;
				}
				Object cellValue = getCellValue(row.getCell(index));
				if (cellValue != null && StringUtils.isNotBlank(cellValue.toString())) {
					properties.put(cm.getProperty(), cellValue);
					if (flag) {
						flag = false;// 有一列值不为空，则为false
					}
				}
			}
			if (flag)
				break;// 遇一行中所有值都为空，结束
			ConvertUtils.register(new DateLocaleConverter(), Date.class);
			BeanUtils.populate(t, properties);
			resultList.add(t);
		}

		return resultList;
	}

	public static String getCellString(Cell cell) {
		if (cell == null || cell.getCellTypeEnum() == CellType.BLANK) {
			return null;
		}
		if (cell.getCellTypeEnum() == CellType.NUMERIC || (cell.getCellTypeEnum() == CellType.FORMULA
				&& cell.getCachedFormulaResultTypeEnum() == CellType.NUMERIC)) {
			if (cell.getNumericCellValue() == (long) cell.getNumericCellValue()) {
				return String.valueOf((long) cell.getNumericCellValue());
			} else {
				return String.valueOf(cell.getNumericCellValue());
			}
		} else {
			return cell.toString();
		}
	}

	/**
	 * 获取单元格中的值
	 * 
	 * @param cell
	 * @return
	 */
	public static Object getCellValue(Cell cell) {
		Object result = null;

		if (cell != null) {
			CellType cellType = cell.getCellTypeEnum();
			switch (cellType) {
			case STRING:
				result = cell.getStringCellValue();
				break;
			case NUMERIC:
				if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
					result = cell.getDateCellValue();
				} else {
					if (cell.getNumericCellValue() == (long) cell.getNumericCellValue()) {
						return (long) cell.getNumericCellValue();
					} else {
						return cell.getNumericCellValue();
					}
				}
				break;
			case BOOLEAN:
				result = cell.getBooleanCellValue();
				break;
			default:
			}
		}
		return result;
	}

	private static Row accessRow(Sheet sheet, int rownum) {
		Row row = sheet.getRow(rownum);
		if (row == null) {
			row = sheet.createRow(rownum);
		}
		return row;
	}

	private static Cell accessCell(Row row, int column) {
		Cell cell = row.getCell(column);
		if (cell == null) {
			cell = row.createCell(column);
		}
		return cell;
	}

	public static Workbook getWorkbook(InputStream inStr,String fileName) throws Exception{
        Workbook wb = null;
        
        String fileType = fileName.substring(fileName.lastIndexOf("."));
        if(excel2003L.equals(fileType)){
            wb = new HSSFWorkbook(inStr);
        }else{
        	wb = new XSSFWorkbook(inStr);
        }
        
        return wb;
    }
}
