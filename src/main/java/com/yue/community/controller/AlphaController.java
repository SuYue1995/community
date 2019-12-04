package com.yue.community.controller;

import com.yue.community.service.AlphaService;
import com.yue.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Controller
@RequestMapping("/alpha") // 给类取访问名，浏览器通过名字访问类
public class AlphaController {

    @Autowired
    private AlphaService alphaService;

    //处理浏览器请求的方法
    @RequestMapping("/hello") //给方法取访问路径
    @ResponseBody //声明  不返回网页，返回字符串
    public String sayHello(){
        return "Hello Spring Boot";
    }

    @RequestMapping("/data") //给方法取访问路径
    @ResponseBody //声明  不返回网页，返回字符串
    public String getData(){ //处理查询请求
        return alphaService.find();
    }

    @RequestMapping("/http")
    //用request，response对象获取相关数据
    public void http(HttpServletRequest request, HttpServletResponse response){  //通过response对象可以直接向浏览器输出任何数据，不依赖返回值
        //如果想获取请求、响应对象，在方法上加以声明即可。
        // 声明了这两个类型以后，dispatcherServlet调取这个方法的时候就自动把两个对象传过来，在底层，两个对象是创建好的。
        //request,response对象常用接口:HttpServletRequest, HttpServletResponse.利用这两个对象处理请求，即处理请求中包含的数据。
        //获取请求数据
        System.out.println(request.getMethod());
        System.out.println(request.getServletPath());//请求的路径
        Enumeration<String> enumeration = request.getHeaderNames(); //消息头，请求行，若干行数据，被封装，key-value结构。得到的是一个迭代器
        while (enumeration.hasMoreElements()){ //通过循环遍历
            String name = enumeration.nextElement(); //得到的是一个key，即请求行的名字
            String value = request.getHeader(name);
            System.out.println(name + ": " + value);
        }
        System.out.println(request.getParameter("code"));//请求体包含业务数据，和各种参数

        //response 是用来向浏览器做出响应的对象，给浏览器返回响应数据
        //返回响应数据
        response.setContentType("text/html; charset=utf-8"); //设置返回响应数据的类型，网页的字符串，图片等等."text/html"网页类型的文本，字符集charset=utf-8，支持中文
        //response响应网页，就是用它所封装的输出流向浏览器输出
        PrintWriter writer = null;
        try {
            writer = response.getWriter(); //获取输出流
            writer.write("<h1>suyue</h1>");//输出一级标题
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            writer.close(); //关闭输出流
        }
    }

    //GET请求 一般用于获取服务器数据，默认发送的请求为GET请求

    //查询所有的学生，分页显示，告诉服务器当前第几页，每一页最对显示20条数据 /student?current=1&limit=20
    @RequestMapping(path = "/students", method = RequestMethod.GET) //声明请求路径，method声明请求方式，强制方法为GET请求才能访问到。如果没有限制，POST也可以访问到。
    //明确具体处理什么请求，更为合理
    @ResponseBody
    //public String getStudent(int current, int limit){ //只要参数名和传进来的参数名保持一致就可以得到。 //有的时候没有传进来参数，所以加上注解进一步处理
    public String getStudents(
            @RequestParam(name = "current", required = false, defaultValue = "1") int current, //通过加RequestParam注解对参数的注入做更详细的声明
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit){
        System.out.println(current);
        System.out.println(limit);
            return "some student";
    }

    //根据id查询一个学生 路径设置为 /student/123，直接把参数编排到路径当中，成为路径的一部分
    @RequestMapping(path = "/student/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String getStudent(@PathVariable("id") int id){ //注解PathVariable会将路径中的变量赋值给当前参数
        System.out.println(id);
        return "a student";
    }

    //POST请求 浏览器要向服务器提交数据，浏览器需要打开带有表单的网页，通过表单填写数据之后提交给服务器
    @RequestMapping(path = "/student", method = RequestMethod.POST)
    @ResponseBody
    public String saveSrudent(String name, int age){ //参数的名字与表单中数据的名字一致，即可自动传过来
        System.out.println(name);
        System.out.println(age);
        return "save successfully";
    }

    //响应动态HTML数据
    //浏览器查询一个老师，服务器将查询到老师的相关数据，并响应给浏览器，以网页形式
    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    //@ResponseBody 不加此注解，默认返回html
    public ModelAndView getTeacher(){ //返回类型为Model View两份数据。Spring mvc原理，所有组件都是由DispatcherServlet调度
        //DispatcherServlet会调controller的某个方法，这个方法需要给它返回model数据，和view视图相关数据，它将两者都提交给模板引擎。
        //由模板引擎进行渲染，生成动态HTML。ModelAndView 这个对象封装的就是要给DispatcherServlet返回的Model和View两份数据。
        ModelAndView modelAndView = new ModelAndView();//实例化对象
        modelAndView.addObject("name","张三");//传动态的值
        modelAndView.addObject("age",30); //model里需要多少的变量，就add多少数据
        modelAndView.setViewName("/demo/view");//对象需要设置模板，模板的路径和名字。需要写templates文件夹的下级目录。Thymeleaf默认文件为HTML，所以只写文文件名view，不用写扩展名
        return modelAndView;
    }

    //查询一个学校
    @RequestMapping(path = "/school", method = RequestMethod.GET)
    public String getSchool(Model model){ //返回类型为String，即返回view的路径。添加参数Model，DispatcherServlet调用该方法时检测到Model对象，自动实例化Model对象传过来
        //model是个bean，是个对象。DispatcherServlet持有这个对象的引用，所以在方法内部对model对象存数据，DispatcherServlet也可以得到。

        //第二种方法逻辑与第一种方法类似。第一种方法更为直观，直接把model和view的数据都装在一个对象里。
        //第二种方法将model数据装到参数里，把view的视图直接返回，返回的值给了DispatcherServlet，model的应用DispatcherServlet也持有着，所以两份数据都可得到，结果同第一种方法相同。
        model.addAttribute("name", "西北工业大学");
        model.addAttribute("age",81);
        return "/demo/view"; //返回类型为String，即返回view的路径。
    }

    // 响应JSON数据（异步请求）
    //java为面向对象语言，时刻都可以得到java对象。返回给一个java对象给浏览器，浏览器用js解析这个对象，js也是面向对象语言，也希望得到一个js对象。
    //然而java对象无法直接转为js对象，这是两种语言，不兼容。
    //JSON可以实现两者的兼容，JSON是具有特定格式的字符串。将java对象转成JSON字符串给浏览器传过去，浏览器可以将JSON字符串转换为JS对象
    //因为任何语言都有字符串类型，而且JSON字符串的格式是比较通用的格式，任何语言都可以进行解析，并将其转换为对象。所以JSON起到衔接的作用
    //通过JSON可以将java对象转成其他任何语言的对象。在跨语言的环境下，JSON是非常常用的字符串形式，
    // 尤其是在异步请求当中：客户端需要返回一个局部验证的结果，是否，成功失败等结果

    @RequestMapping(path = "/emp", method = RequestMethod.GET) //声明访问路径
    @ResponseBody //向浏览器返回JSON，需要加此注解。如果不加，，加上以后才能返回JSON字符串
    public Map<String, Object> getEmp(){ //DispatcherServlet调用该方法时，该方法加了ResponseBody注解而且声明返回Map类型，会自动将map转换成JSON字符串发送给浏览器
        Map<String, Object> emp = new HashMap<>();//方法内，实例化map，存一些值再返回
        emp.put("name", "suyue");
        emp.put("age", 24);
        emp.put("salary",15000.00);
        return emp;
    }

    //查询所有员工
    @RequestMapping(path = "/emps", method = RequestMethod.GET) //声明访问路径
    @ResponseBody //向浏览器返回JSON，需要加此注解。如果不加，，加上以后才能返回JSON字符串
    public List<Map<String, Object>> getEmps(){ //返回类型是一个集合，每一个员工是一个map
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> emp = new HashMap<>();//方法内，实例化map，存一些值再返回
        emp.put("name", "suyue");
        emp.put("age", 24);
        emp.put("salary",15000.00);
        list.add(emp);

        emp = new HashMap<>();//方法内，实例化map，存一些值再返回
        emp.put("name", "julian");
        emp.put("age", 25);
        emp.put("salary",10.00);
        list.add(emp);

        emp = new HashMap<>();//方法内，实例化map，存一些值再返回
        emp.put("name", "Josh");
        emp.put("age", 22);
        emp.put("salary",1.00);
        list.add(emp);

        emp = new HashMap<>();//方法内，实例化map，存一些值再返回
        emp.put("name", "Howard");
        emp.put("age", 24);
        emp.put("salary",0.00);
        list.add(emp);
        return list;
    }

    //Cookie示例
    //第一步，浏览器第一次访问服务器，服务器创建cookie给浏览器
    @RequestMapping(path = "/cookie/set", method = RequestMethod.GET)
    @ResponseBody
    public String setCookie(HttpServletResponse response){
        //创建Cookie存在Response里
        Cookie cookie = new Cookie("code", CommunityUtil.generateUUID());//Cookie只能存储少量字符串数据，因为在两端之间来回传输，大量数据会影响性能，另外客户端只能识别字符串，无法识别其他Java类型。
        //设置Cookie生效范围
        //再次访问服务器时，浏览器会自动把cookie发送给服务器，指定访问哪些路径会发送cookie。有些路径不需要，浪费网络资源，使其无效
        cookie.setPath("/community/alpha"); //cookie在该路径及其子路径下有效
        //设置cookie生存时间
        //cookie默认存在浏览器的内存里，关闭浏览器会被清除。设置生存时间后，cookie会被存在硬盘里，长期有效直到超过生存时间
        cookie.setMaxAge(60 * 10);//单位为s, 设置为10min
        //发送cookie，把cookie对象添加到response里
        response.addCookie(cookie);
        return  "set cookie";
    }

    //第一步，浏览器再次访问服务器，携带之前创建好的cookie
    @RequestMapping(path = "/cookie/get", method = RequestMethod.GET)
    @ResponseBody
    public String getCookie(@CookieValue("code") String code){ //从众多cookies中获取key为code的值，然后赋值给code参数
        System.out.println(code); //code加入model中带给模板即可在模板中使用
        return "get cookie";
    }

    //session示例
    //第一步，浏览器第一次请求访问服务器，创建session，存储数据
    @RequestMapping(path = "/session/set", method = RequestMethod.GET)
    @ResponseBody
    public String setSession(HttpSession session){ //session使用和request，response，model一样，SpringMVC自动创建session。只需要声明，SpringMVC就会注入进来。
        //Session存在服务端，可存储任何数据
        session.setAttribute("id",1);
        session.setAttribute("name","test");
        return "set session";
    }


    @RequestMapping(path = "/session/get", method = RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session){
        System.out.println(session.getAttribute("id"));
        System.out.println(session.getAttribute("name"));
        return "get session";
    }

    // Ajax示例
    @RequestMapping(path = "/ajax", method = RequestMethod.POST)//页面通过异步方式向服务器提交数据，所以为POST
    @ResponseBody // 服务器不返回网页，返回字符串
    public String testAjax(String name, int age){
        System.out.println(name);
        System.out.println(age);
        return CommunityUtil.getJSONString(0, "操作成功");
    }
}
