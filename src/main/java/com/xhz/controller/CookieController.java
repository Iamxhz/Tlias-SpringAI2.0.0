package com.xhz.controller;

import com.xhz.pojo.Result;
import jakarta.servlet.http.Cookie; // <- 就是补上这一行
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class CookieController {

    //设置Cookie
    @GetMapping("/c1")
    public Result cookie1(HttpServletResponse response){
        response.addCookie(new Cookie("login_username","itheima")); //设置Cookie/响应Cookie
        return Result.success();
    }

    //获取Cookie
    @GetMapping("/c2")
    public Result cookie2(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if (cookies != null) { // 建议加一个判空，防止未携带Cookie时报空指针异常
            for (Cookie cookie : cookies) {
                if(cookie.getName().equals("login_username")){
                    System.out.println("login_username: "+cookie.getValue()); //输出name为login_username的cookie
                }
            }
        }
        return Result.success();
    }
}