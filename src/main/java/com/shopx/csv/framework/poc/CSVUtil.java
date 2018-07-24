package com.shopx.csv.framework.poc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import com.shopx.csv.framework.poc.sample.bean.Employee;

public class CSVUtil {
	private Map<String, Integer> headerMap = new HashMap<>();
	private String[] headers;
	private Map<String, Field> fieldColumnMapping = new HashMap<String, Field>();
	private final DecimalFormat df;

	public CSVUtil() throws Exception {
		NumberFormat nf = NumberFormat.getInstance();
		if (!(nf instanceof DecimalFormat)) {
			throw new Exception("Number format not a decimalformat");
		}
		df = (DecimalFormat) nf;
	}

	public static void main(String[] args) throws Exception {
		CSVUtil csvUtil = new CSVUtil();
		System.out.println(csvUtil.csvToCollection("/Users/peter_arputham/Desktop/employee.csv", Employee.class));
	}

	public Collection<?> csvToCollection(String path, Class<?> type) throws Exception {
		File filePath = new File(path);
		Set collectionSet = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)))) {
			getHeader(reader.readLine());
			loadFields(type);
			reader.lines().forEach(line -> {
				try {
					if (StringUtils.isNotBlank(line))
						collectionSet.add(populateNewBean(line.split("\\,", -1), Employee.class));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
		}
		return collectionSet;
	}

	public <T> T populateNewBean(String[] line, Class<T> type) throws Exception {
		T bean = createBean(type);
		for (int col = 0; col < line.length; col++) {
			setFieldValue(bean, line[col], headers[col].trim());
		}
		return bean;
	}

	private void getHeader(String headLine) {
		headers = headLine.split("\\,", -1);
		headerMap.clear();
		int i = 0;
		while (i < headers.length) {
			headerMap.put(headers[i].trim(), i);
			i++;
		}
	}

	private void setFieldValue(Object bean, String value, String column) throws IllegalArgumentException, Exception {
		Field columnField = fieldColumnMapping.get(column);
		if (columnField != null) {
			assignValueToField(bean, value, columnField);
		}
	}

	private void assignValueToField(Object bean, Object obj, Field field) throws IllegalArgumentException, Exception {
		// obj == null means that the source field was empty.
		if (obj != null) {
			// Find and use a setter method if one is available.
			try {
				Method setterMethod = getWriteMethod(bean, field);
				try {
					setterMethod.invoke(bean, converFieldValue(field, obj));
				} catch (IllegalAccessException e) {
					// Can't happen, because we've already established that the
					// method is public through the use of getMethod().
				} catch (InvocationTargetException e) {
					throw e;
				}
			} catch (NoSuchMethodException | SecurityException e1) {
				// Otherwise set the field directly.
				writeWithoutSetter(bean, obj, field);
			}
		}
	}

	private void writeWithoutSetter(Object bean, Object obj, Field field) throws Exception {
		try {
			FieldUtils.writeField(field, bean, converFieldValue(field, obj), true);
		} catch (IllegalAccessException e) {
			// The Apache Commons Lang Javadoc claims this can be thrown
			// if the field is final, but it's not true if we override
			// accessibility. This is never thrown.
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	private Method getWriteMethod(Object bean, Field field) throws NoSuchMethodException {
		String setterName = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
		return bean.getClass().getMethod(setterName, field.getType());
	}

	private List<Field> loadFields(Class<?> cls) {
		List<Field> fields = new LinkedList<>();
		for (Field field : FieldUtils.getAllFields(cls)) {
			fields.add(field);
			fieldColumnMapping.put(field.getName(), field);
		}
		return fields;
	}

	private Object converFieldValue(Field field, Object value) throws Exception {
		if (field.getType() == String.class) {
			return value;
		} else if (field.getType() == Date.class) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			return sdf.parse(value.toString());
		} else
			return convertToNumber(field, value);
	}

	private Object convertToNumber(Field field, Object value) throws Exception {
		Class<?> type = field.getType();
		Number n;
		try {
			synchronized (df) {
				n = df.parse(value.toString());
			}
		} catch (ParseException e) {
			throw e;
		}

		if (type == Byte.class || type == Byte.TYPE) {
			n = n.byteValue();
		} else if (type == Short.class || type == Short.TYPE) {
			n = n.shortValue();
		} else if (type == Integer.class || type == Integer.TYPE) {
			n = n.intValue();
		} else if (type == Long.class || type == Long.TYPE) {
			n = n.longValue();
		} else if (type == Float.class || type == Float.TYPE) {
			n = n.floatValue();
		} else if (type == Double.class || type == Double.TYPE) {
			n = n.doubleValue();
		} else if (type == BigInteger.class) {
			n = ((BigDecimal) n).toBigInteger();
		}
		// else: Either it's already a BigDecimal and nothing need be done,
		// or it's some derivative of java.lang.Number that we couldn't be
		// expected to know and accommodate for. In the latter case, a class
		// cast exception will be thrown later on assignment.
		return n;
	}

	public <T> T createBean(Class<T> type) throws Exception {
		if (type == null) {
			throw new Exception("Type Cannot be null");
		}
		return type.newInstance();
	}
}
