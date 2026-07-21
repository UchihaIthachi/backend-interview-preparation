# Basic Questions

## Question 1: What is JDBC ?

JDBC is a persistence technology which is used to develop persistence logic having the capability to perform persistence operations on persistence data of a persistence store.

## Question 2: How many steps are there to develop jdbc application?

There are six steps are there to develop jdbc application.
1)Register JDBC driver with DriverManager service.
2)Establish the connection with database software.
3)Create statement object
4)Sends and executes SQL query in database software.
5)Gather the result from database software to result.
6)Close all jdbc connection objects.

## Question 3: How many drivers are there in jdbc?

We have four types of drivers in jdbc?
1) Type1 JDBC driver (JDBC-ODBC bridge driver)
2) Type2 JDBC driver (Native API)
3) Type3 JDBC driver (Net Protocol)
4) Type4 JDBC driver (Native Protocol)

## Question 4: What is DatabaseMetaData?

DatabaseMetaData is an interface which is present in java.sql package.
DatabaseMetaData provides metadata of a database.
DatabaseMetaData gives information about database product name, database product version, database driver name, database driver version, database username and etc.
We can create DatabaseMetaData object by using getMetaData() method of Connection obj.
ex:
DatabaseMetaData dbmd=con.getMetaData();

## Question 5: Types of Queries in jdbc?

We have two types of queries in jdbc.
1)Select query
It will give bunch of records from database table.
ex:select * from student order by sno;
To execute select query we need to executeQuery() method.
2)Non-Select query
It will give numeric value represent number of records effected in a
database table.
ex: delete from student;
To execute non-select query we need to use executeUpdate() method.

## Question 6: What is JDBC Connection pool?

It is a factory containing a set of readily available JDBC connection objects before actual being used.
JDBC Connection pool represent connectivity with same database software.
A programmer is not responsible to create, manage or destroy JDBC connection objects in jdbc connection pool. A jdbc connection pool is responsible to create,manage and destroy jdbc connection objects in jdbc connection pool.

## Question 7: Write a jdbc application to perform aggregate function?

import java.sql.*;
class CreateTableApp
{
public static void main(String[] args)throws Exception
{
Class.forName("oracle.jdbc.driver.OracleDriver");
Connecton con=DriverManager.getConnection ("jdbc:oracle:thin:@localhost:1521:XE","system","admin");
String qry="create table student(sno number(3),
sname varchar2(10),sadd varchar2(12))";
PreparedStatement ps=con.prepareStatement(qry)
int result=ps.executeUpdate();
if(result==0)
System.out.println("Table not created");
else
System.out.println("Table created");
ps.close();
con.close();
}
}
10)Write a jdbc application to insert the record into student table
import java.sql.*;
import java.util.*;
class CreateTableApp
{
public static void main(String[] args)throws Exception
{
Scanner sc=new Scanner(System.in);
System.out.println("Enter the student no: ");
int no=sc.nextInt();
System.out.println("Enter the student name :");
String name=sc.next();
System.out.println("Enter the student address :");
String add=sc.next();
Class.forName("oracle.jdbc.driver.OracleDriver");
Connecton con=DriverManager.getConnection ("jdbc:oracle:thin:@localhost:1521:XE","system","admin");
String qry="insert into student values(?,?,?)";
PreparedStatement ps=con.prepareStatement(qry);
//set the values
ps.setInt(1,no);
ps.setString(2,name);
ps.setString(3,add);
int result=ps.executeUpdate();
if(result==0)
System.out.println("Table not created");
else
System.out.println("Table created");
ps.close();
con.close();
}
}

