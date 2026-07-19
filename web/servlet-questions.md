# Servlet Questions

### What is Servlet ?

Servlet is a dynamic web resource program which enhanced the functionality of
web server , proxy server ,HTTP server and etc.
or
Servlet is a java based dynamic web resource program which is used to generate
dynamic web pages.
or
Servlet is a single instance multi-thread java based web resource program which is used
to develop web applications.

### What is Web container

It is a software application or program which is used to manage whole life cycle of
web resource program i.e from birth to death.
Servlet container manages whole life cycle of servlet program.
Similarly , JSP container manages whole life cycle of jsp program.
Some part of industry considers servlet container and jsp container are web containers.
5)ServletConfig object
ServletConfig is an interface which is present in javax.servlet package.
ServletConfig object will be created by the web container for every servlet.
ServletConfig object is used to read configuration information from web.xml file
We can create ServletConfig object by using getServletConfig() method.
ex:
ServletConfig config=getServletConfig();
6)ServletContext object
ServletContext is an interface which is present in javax.servlet package.
ServletContext object is created by the web container for every web application i.e it is one per web application.
ServletContext object is used to read configuration information from web.xml file and it is for all servlets.
We can create ServletContext object by using getServletContext() method.
ex: ServletContext context=getServletContext():
Or ServletConfig config=getServletConfig();
ServletContext context=config.getServletContext();

### what is Servlet Filters?

Filter is an object which is executed at the time of preprocessing and
postprocessing of the request.
The main advantages of using filter is to perform filter task such as
1) Counting number of request
2) To perform validation
3) Encyrption and Decryption
and etc.
Like Servlet, Filter is having it's own Filter API.
The javax.servlet package contains thre interfaces of Filter API.
1)Filter
2)FilterChain
3)FilterConfig

### Differences between GET And POST methodology?

GET POST
It is a default methodology. It is not a default methodology.
It sends the request fastly. It sends the request bit slow
It will carry limited amount of data. It will carry unlimited amount of data.
It is not suitable for secure data. It is suitable for secure data.
It is not suitable to perform It is suitable to perform encyrption and
encryption or fileuploading. file uploading.
To process GET methodology we need To process POST methodology we need to use
to use doGet(-,-) method. doPost(-,-) method.

### Explain Servlet Life cycle methods?

We have three life cycle methods in Servlets
1)public void init(ServletConfig config)throws ServletException
It is used for instantiation event.
This method will execute just before Servlet object creation.
2)public void service(ServletRequest req,ServletResponse res)throws ServletException,IOException
It is used for request processing event.
This method will execute when request goes to servlet program.
3)public void destroy
It is used for destruction event.
This method will execute just before Servlet object destruction.
10)Limitations with servlets
> To work with servlets strong java knowledge is required.
> It is suitable for only java programmers.
> Configuration of servlet program in web.xml file is mandatory.
> Handling exceptions are mandatory.
> It does not give any implicit object.
(Object which can be used directly without any configuration is called implicit object).
> We can't maintain html code and java code sperately.
> It does not support tag based language.

1)Advantages of JSP
> To work with JSP strong java knowledge is not required.
> It is suitable for java and non-java programs.
> It supports tag based language.
> It allows us to create custom tags in jsp.
> Configuration of jsp program in web.xml file is optional.
> Handling exceptions are optional.
> It gives 9 implicit objects.
> We can maintain html code and java code sperately.
> It gives all the features of servlets.

