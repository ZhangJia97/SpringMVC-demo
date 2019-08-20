package xyz.suiwo.mvcframework.servlet;

import xyz.suiwo.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SWDispatcherServlet extends HttpServlet {

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    // 重点理解
    private List<Handler> handlerMapping = new ArrayList<>();

//    private Map<Pattern, Handler> handlerMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            boolean isMatch = pattern(req, resp);
            if(!isMatch){
                resp.getWriter().write("404");
            }
        }catch (Exception e){
            resp.getWriter().write("500 - " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1. 读取配置文件
        String scanPackage = config.getInitParameter("scanPackage");

        //2. 扫描包路径下的类
        scanClass(scanPackage);

        // 3.把这些被扫描出来的类进行实例化
        instance();

        // 4.建立依赖关系，自动依赖注入
        autowired();

        // 5.建立URL和Method的映射关系HandlerMapping
        handlerMapping();

        System.out.println("SW MVC 已经准备就绪");
    }

    private void scanClass(String packageName){
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.","/"));
        File dir = new File(Objects.requireNonNull(url).getFile());
        for(File file : Objects.requireNonNull(dir.listFiles())){
            if(file.isDirectory()){
                scanClass(packageName + "." + file.getName());
            }else{
                String className = packageName + "." + file.getName().replace(".class","");
                classNames.add(className);
            }
        }

    }

    private void instance(){
        //利用反射机制将扫描到的类名全部实例化
        if(classNames.isEmpty()){
            return;
        }
        for(String className : classNames){
            try {
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(SWController.class)){
                    ioc.put(lowerFirstChar(clazz.getName()),clazz.newInstance());
                }else if(clazz.isAnnotationPresent(SWService.class)){
                    SWService swService = clazz.getAnnotation(SWService.class);
                    String beanName = swService.value();
                    if(!"".equals(beanName.trim())){
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    }
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for(Class<?> i : interfaces){
                        ioc.put(i.getName(), clazz.newInstance());
                    }

                }
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    private void autowired(){
        if(ioc.isEmpty()){
            return;
        }
        for(Map.Entry<String, Object> entry : ioc.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field : fields){
                if(field.isAnnotationPresent(SWAutowired.class)){
                    SWAutowired swAutowired = field.getAnnotation(SWAutowired.class);
                    String beanName = swAutowired.value().trim();
                    if("".equals(beanName)){
                        beanName = field.getType().getName();
                    }
                    field.setAccessible(true);
                    try {
                        field.set(entry.getValue(), ioc.get(beanName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void handlerMapping(){
        if(ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(SWController.class)){
                continue;
            }
            String baseUrl = "";
            if(clazz.isAnnotationPresent(SWRequestMapping.class)){
                SWRequestMapping swRequestMapping = clazz.getAnnotation(SWRequestMapping.class);
                baseUrl += swRequestMapping.value();
            }

            Method[] methods = clazz.getMethods();
            for(Method method : methods){
                if(!method.isAnnotationPresent(SWRequestMapping.class)){
                    continue;
                }
                SWRequestMapping requestMapping = method.getAnnotation(SWRequestMapping.class);
                String regex = baseUrl + requestMapping.value();
                regex = regex.replaceAll("\\*",".*");

                Map<String, Integer> paramMapping = new HashMap<>();

                //提取自己命名的索引
                Annotation[][] pa = method.getParameterAnnotations();
                for(int i = 0; i < pa.length; i++){
                    for(Annotation a : pa[i]){
                        if(a instanceof SWRequestParam){
                            String paramName = ((SWRequestParam) a).value();
                            if(!"".equals(paramName)){
                                paramMapping.put(paramName, i);
                            }
                        }
                    }
                }

                //提取request和response的索引
                Class<?>[] parameterTypes = method.getParameterTypes();
                for(int i = 0; i < parameterTypes.length; i++){
                    Class<?> type = parameterTypes[i];
                    if(type == HttpServletRequest.class || type == HttpServletResponse.class){
                        paramMapping.put(type.getName(), i);
                    }
                }

                handlerMapping.add(new Handler(Pattern.compile(regex), entry.getValue(), method, paramMapping));
                System.out.println("Mapping " + regex + ", " + method);
            }
        }

    }

    private boolean pattern(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if(handlerMapping.isEmpty()){
            return false;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        for(Handler handler: handlerMapping){
            try {

                Matcher matcher = handler.pattern.matcher(url);

                if(!matcher.matches()){
                    continue;
                }
                Method method = handler.method;
                Class<?>[] parameterTypes = method.getParameterTypes();
                Object[] parameterValues = new Object[parameterTypes.length];
                Map<String, String[]> params = req.getParameterMap();
                for(Map.Entry<String, String[]> param : params.entrySet()){

                    String value = Arrays.toString(param.getValue()).replaceAll("\\]|\\[","").replaceAll(",\\s",",");
                    if(!handler.paramMapping.containsKey(param.getKey())){
                        continue;
                    }
                    int index = handler.paramMapping.get(param.getKey());

                    parameterValues[index] = castStringValue(value, parameterTypes[index]);

                }
                int reqIndex = handler.paramMapping.get(HttpServletRequest.class.getName());
                parameterValues[reqIndex] = req;

                int respIndex = handler.paramMapping.get(HttpServletResponse.class.getName());
                parameterValues[respIndex] = resp;

                handler.method.invoke(handler.controller, parameterValues);
                return true;

            }catch (Exception e){
                throw e;
            }
        }
        return true;
    }

    private Object castStringValue(String value, Class clazz){
        // todo 未支持所有类型
        if(clazz == String.class) {
            return value;
        }else if(clazz == Integer.class || clazz == int.class){
            return Integer.valueOf(value);
        }else if(clazz == Boolean.class || clazz == boolean.class){
            return Boolean.valueOf(value);
        }else {
            return "";
        }
    }

    private String lowerFirstChar(String str) {
        char[] ch = str.toCharArray();
        ch[0] += 32;
        return String.valueOf(ch);
    }

    private class Handler{
        protected Pattern pattern;
        protected Object controller;
        protected Method method;
        protected Map<String, Integer> paramMapping;

        protected Handler(Pattern pattern, Object controller, Method method, Map<String, Integer> paramMapping){
            this.pattern = pattern;
            this.controller = controller;
            this.method = method;
            this.paramMapping = paramMapping;
        }
    }
}
