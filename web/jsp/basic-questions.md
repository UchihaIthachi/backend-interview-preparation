# Basic Questions

## Question 1: What is jsp?

JavaServer Pages (JSP) is a technology for developing Webpages that supports dynamic content.
This helps developers insert java code in HTML pages by making use of special JSP tags,
most of which start with <% and end with %>.

## Question 2: Types of JSP Tags/Elements?

1)Scripting tags
i)Scriptlet tag
ex: <% code %
ii)Expression tag
ex: <%= code %>
iii)Declaration tag
ex: <%! code %>
2)Directive Tags
i)Page directive tag
ex: <%@page attribute=value %>
ii)include directive tag
ex: <%@include attribute=value %>
3)Standard Tags
<jsp:include>
<jsp:forward>
<jsp:setProperty>
<jsp:getProperty>
<jsp:useBean>
and etc.
4)JSP comment
<%-- jsp comment --%>
i)Scriptlet tag
It is used to declare java code.
syntax: <% code %>
5)JSP life cycle methods
JSP contains three life cycle methods.
1)_jspInit()
It is used for instantiation event
This method will execute just before JES class object creation.
JES class stands for Java Equivalent Servlet class.
2)_jspService()
It is used for request arrival event
This method will execute when request goes to JSP program.
3)_jspDestroy()
It is used for destruction event.
This method will execute just before JES class object destruction

## Question 3: Phases in JSP?

In JSP , we have two phases.
1)Translation phase
In this phase, our JSP program converts to JES class
(ABC_jsp.class and ABC_jsp.java) object.
2)Request processing phase
In this phase, our JES class will be execute and result will send
to browser window as dynamic response.

## Question 4: MVC in JSP?

MVC stands for Model View Controller.
It is a design partern which seperates business logic , persistence logic and data.
Controller acts like an interface between Model and View.
Controller is used to intercept with all the incoming requests.
Model contains data.
View represent User interface i.e UI.

## Question 5: What is JES clSass?

