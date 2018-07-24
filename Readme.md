Features:
---------
1. The column name can be there in any order
2. Parser converts and assigns values based on the type.
3. Date format hardcoded with format 'dd/MM/yyyy' since it is POC. And not like to introduce configuration files for now.
4. CSVUtil will discard blank lines and empty records.
5. Can read up to a 1.0GB file - Using java streams.

Usage:
------
I have created the main method already in CSVUtil for testing.

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

Notes:
------
1. Java 1.8 version Required
2. Used HashSet to avoid duplicates.
3. Used java streams to process records faster (Ref: See "Section Reading a 1.0GB file" at https://www.java-success.com/processing-large-files-efficiently-java-part-1/)
4. Not splited into small chunks since it is poc code.
5. Header column is required(Made it mandatory )
6. hashCode and equals should be overridden since using HashSet to return parsed CSV file values as a collection
7. Error records will be discarded and error show in the console.
8. Avoided MappedByteBuffer to do more engineering for POC code.
9. For empty column values returning null(Added condition). If all the fields are required then condition should be removed from converFieldValue method.
10. jUnit test cases needs to be added.

Find the sample csv file below
------------------------------
id,name,dob,percentage
37,Agnes,01/02/1982,86.5
7,Alan,19/05/2011,99.5
5,Adrian,06/02/2014,99.8
36,Ashok,01/01/1982,80.5
33,Sundarshan,asdf,66.5
62,Mary,20/04/1956,87.5
38,Peter,01/02/1981,83.5
38,Peter,01/02/1981,83.5
,,,

71,Arputham,26/07/1947,90.5
,Will,01/01/1982,80.5
50,Anne,01/01/1982,
50,Joseph,,77.5
xxx,Arun,01/01/1982,77.5

Find the sample output below.
------------------------------

failed to parse line 33,Sundarshan,asdf,66.5 Exception: Unparseable date: "asdf"

failed to parse line xxx,Arun,01/01/1982,77.5 Exception: Unparseable number: "xxx"

Employee [name=Joseph, id=50, dob=null, percentage=77.5]
Employee [name=Ashok, id=36, dob=Fri Jan 01 00:00:00 PST 1982, percentage=80.5]
Employee [name=Arputham, id=71, dob=Sat Jul 26 00:00:00 PST 1947, percentage=90.5]
Employee [name=Alan, id=7, dob=Thu May 19 00:00:00 PDT 2011, percentage=99.5]
Employee [name=Will, id=null, dob=Fri Jan 01 00:00:00 PST 1982, percentage=80.5]
Employee [name=Peter, id=38, dob=Sun Feb 01 00:00:00 PST 1981, percentage=83.5]
Employee [name=Anne, id=50, dob=Fri Jan 01 00:00:00 PST 1982, percentage=null]
Employee [name=Agnes, id=37, dob=Mon Feb 01 00:00:00 PST 1982, percentage=86.5]
Employee [name=Adrian, id=5, dob=Thu Feb 06 00:00:00 PST 2014, percentage=99.8]
Employee [name=Mary, id=62, dob=Fri Apr 20 00:00:00 PST 1956, percentage=87.5]