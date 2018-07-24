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

/**
 * This CSV file reader util created for reading csv and trying to address below
 * problem statement.
 * 
 * <li>Design and Code a CSV parser library, which can be used by multiple java
 * applications.
 * <li>The library has a single entry point as a "parse" function, which takes 2
 * parameters
 * <li>File - indicating the location of the CSV file
 * <li>Class - Data Class for which the CSV has to be mapped
 * <li>The return should be a Collection of Data Objects of Type class, which
 * has been passed as an argument.
 * <li>For example if the CSV is a employee.csv and the class which represents
 * employee is Employee then the function should look like
 * <li>Collection<Employee> parse(File csvFile, Employee.class)
 * <li>If we pass order then it should look like the following Collection<Order>
 * parse(File csvFile, Order.class)
 * <li>The library has to be generic.
 * <li>Assumption is that the header column names of the csv matches the member
 * variable of the Class.
 * <li>For example the csv can have the columns as "id", "age", "sal" and the
 * same member variable are available in the Employee class
 * <li>The library should also be able to remove duplicates from the CSV and
 * return collection of unit data objects
 * <li>Please use only core java and no third party CSV parser can be used.
 * <li>How will the library handle csv file which has a 1 million row.
 * <li>What will be the design element, which will handle 1 million row
 * processing.
 * <li>The code should be able to handle the scale problem as well.
 * 
 * @author peter_arputham
 *
 */
public class CSVUtil {

	/*
	 * Below Map holds column name and it is position.
	 */
	private Map<String, Integer> headerMap = new HashMap<>();

	/*
	 * This header arryay holds all the headers
	 */
	private String[] headers;

	/*
	 * Map which hold column and java class field mapping.
	 */
	private Map<String, Field> fieldColumnMapping = new HashMap<String, Field>();

	/*
	 * Variable for paring decimal points.
	 */
	private final DecimalFormat decimalFormat;

	/**
	 * Default constructor and initializes decimalFormat
	 * 
	 * @throws Exception
	 */
	public CSVUtil() throws Exception {
		NumberFormat numberFormat = NumberFormat.getInstance();
		// Checking number format is decimal format or not.
		if (!(numberFormat instanceof DecimalFormat)) {
			throw new Exception("Number format not a decimalformat");
		}
		decimalFormat = (DecimalFormat) numberFormat;
	}

	/**
	 * Main method for testing reading csv file.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// Creating new Instance for the CSVUtil
		CSVUtil csvUtil = new CSVUtil();

		// Calling csvToCollection method with args path of the csv file and class type.
		Collection employeeSet = csvUtil.csvToCollection("/Users/peter_arputham/Desktop/employee.csv", Employee.class);

		// Reading parsed csv collection values.
		for (Object object : employeeSet) {
			System.out.println(object);
		}
	}

	/**
	 * Reads csv file and converts csv file into give class type.
	 * 
	 * @param path
	 *            path of the csv file.
	 * @param type
	 *            of the entity class mapped for the csv file.
	 * @return Collection of type with parsed data.
	 * @throws Exception
	 *             if any conversion error.
	 */
	public Collection<?> csvToCollection(String path, Class<?> type) throws Exception {
		File filePath = new File(path);
		Set collectionSet = new HashSet<>();
		// Using java 7 try resource to avoid leak
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)))) {
			// Getting header information from the first line.
			getHeader(reader.readLine());

			// loads fields from the bean and sets fieldColumnMapping
			loadFields(type);

			// Using java 8 stream for reading file faster
			reader.lines().forEach(line -> {
				try {
					// Checking for blank line and all blank values
					if (StringUtils.isNotBlank(line) && StringUtils.isNotBlank(line.replace(",", "")))
						collectionSet.add(populateBean(line.split("\\,", -1), Employee.class));
				} catch (Exception e1) {
					System.out.println("failed to parse line " + line + " Exception: " + e1.getMessage() + "\n");
				}
			});
		}
		return collectionSet;
	}

	/**
	 * Parses line into java bean
	 * 
	 * @param line
	 *            which needs to be parsed.
	 * @param type
	 *            of the class
	 * @return respective bean object for line
	 * @throws Exception
	 *             in case of any Unparseable error.
	 */
	public <T> T populateBean(String[] line, Class<T> type) throws Exception {
		T bean = createBean(type);
		// Using old for loop since have to reterive header column for each line column
		for (int col = 0; col < line.length; col++) {
			setFieldValue(bean, line[col], headers[col].trim());
		}
		return bean;
	}

	/**
	 * Reads header line and creates map with it is position.
	 * 
	 * @param headLine
	 */
	private void getHeader(String headLine) {
		headers = headLine.split("\\,", -1);
		headerMap.clear();
		int i = 0;
		while (i < headers.length) {
			headerMap.put(headers[i].trim(), i);
			i++;
		}
	}

	/**
	 * Sets fieldvalue to the bean property
	 * 
	 * @param bean
	 *            instance of the bean which holds value
	 * @param value
	 *            column value which needs to be set to bean property
	 * @param column
	 *            csv file column name
	 * @throws Exception
	 *             in case of any Unparseable error.
	 */
	private void setFieldValue(Object bean, String value, String column) throws Exception {
		Field columnField = fieldColumnMapping.get(column);
		if (columnField != null) {
			assignValueToField(bean, value, columnField);
		}
	}

	/**
	 * Sets fieldvalue to the bean property and calls converter to cast the values.
	 * 
	 * @param bean
	 *            instance of the bean which holds value
	 * @param obj
	 *            column value which needs to be set to bean property
	 * @param field
	 *            bean field which holds csv the value.
	 * @throws Exception
	 *             in case of any Unparseable error.
	 */
	private void assignValueToField(Object bean, Object obj, Field field) throws Exception {
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

	/**
	 * Uses java reflection for setting values to bean propertis.
	 * 
	 * @param bean
	 *            instance of the bean which holds value
	 * @param obj
	 *            column value which needs to be set to bean property
	 * @param field
	 *            bean field which holds csv the value.
	 * @throws Exception
	 *             in case of any wrong method name error.
	 */
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

	/**
	 * Returns setter method of the field for bean property.
	 * 
	 * @param bean
	 *            instance of the bean which holds value
	 * @param field
	 *            bean field which holds csv the value.
	 * @return setter method for given field.
	 * @throws NoSuchMethodException
	 *             in case of any wrong method error.
	 */
	private Method getWriteMethod(Object bean, Field field) throws NoSuchMethodException {
		String setterName = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
		return bean.getClass().getMethod(setterName, field.getType());
	}

	/**
	 * Loads all the fields to fieldColumnMapping with respective column name value.
	 * 
	 * @param cls
	 *            name of the class which needs to be mapped with csv file.
	 * @return of fields of the given class.
	 */
	private List<Field> loadFields(Class<?> cls) {
		List<Field> fields = new LinkedList<>();
		for (Field field : FieldUtils.getAllFields(cls)) {
			fields.add(field);
			fieldColumnMapping.put(field.getName(), field);
		}
		return fields;
	}

	/**
	 * Converts csv column value to bean field type.
	 * 
	 * @param field
	 *            bean field which will hold csv column value.
	 * @param value
	 *            which need to be converted and set into field.
	 * @return converted value for the bean field.
	 * @throws Exception
	 *             incase of Unparseable error.
	 */
	private Object converFieldValue(Field field, Object value) throws Exception {
		// If value is blank then it return null value. Code should be removed incase if
		// error should be thrown.
		if (value == null || StringUtils.isBlank(value.toString()))
			return null;

		// Checking for string field.
		if (field.getType() == String.class) {
			return value;
			// Checking for Date field.
		} else if (field.getType() == Date.class) {
			//Hardcode dateformat since it is poc.
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			return sdf.parse(value.toString());
			// Checking for numeric field.
		} else
			return convertToNumber(field, value);
	}

	/**
	 * Converts csv file column values into number.
	 * 
	 * @param field
	 *            which can accept numeric value
	 * @param value
	 *            which needs to be converted for passed field.
	 * @return converted numeric value.
	 * @throws Exception
	 *             incase of Unparseable error.
	 */
	private Object convertToNumber(Field field, Object value) throws Exception {
		Class<?> type = field.getType();
		Number n;
		try {
			synchronized (decimalFormat) {
				n = decimalFormat.parse(value.toString());
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

	/**
	 * Creates new bean class for the give class type using java reflection.
	 * 
	 * @param type
	 *            of the class.
	 * @return new instance of the class.
	 * @throws Exception
	 *             in case object creation error using java reflection api.
	 */
	public <T> T createBean(Class<T> type) throws Exception {
		if (type == null) {
			throw new Exception("Type Cannot be null");
		}
		return type.newInstance();
	}
}
