package xyz.suiwo.action.controller;

import xyz.suiwo.action.service.ModifyService;
import xyz.suiwo.action.service.QueryService;
import xyz.suiwo.mvcframework.annotation.SWAutowired;
import xyz.suiwo.mvcframework.annotation.SWController;
import xyz.suiwo.mvcframework.annotation.SWRequestMapping;
import xyz.suiwo.mvcframework.annotation.SWRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SWController
@SWRequestMapping("/get")
public class DemoController {

    @SWAutowired
    ModifyService modifyService;

    @SWAutowired
    QueryService queryService;

    @SWRequestMapping("/method")
    public void search(HttpServletRequest request,
                       HttpServletResponse response,
                       @SWRequestParam("name") String name,
                       @SWRequestParam("string") String str,
                       @SWRequestParam("password") Integer password) throws IOException {
        System.out.println(modifyService.get());
        System.out.println(queryService.get());
        System.out.println(name);
        System.out.println(str);
        System.out.println(password);
        String res = "name = " + name + ", str = " + str + ", password = " + password;
        response.getWriter().write(res);
    }

    @SWRequestMapping("/name/*")
    public void searchAll(HttpServletRequest request,
                       HttpServletResponse response,
                       @SWRequestParam("name") String name,
                       @SWRequestParam("string") String str,
                       @SWRequestParam("password") Integer password) throws IOException {
        System.out.println(modifyService.get());
        System.out.println(queryService.get());
        System.out.println(name);
        System.out.println(str);
        System.out.println(password);
        String res = "name = " + name + ", str = " + str + ", password = " + password;
        response.getWriter().write(res);
    }
}
