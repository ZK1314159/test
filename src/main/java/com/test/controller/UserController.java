package com.test.controller;

import com.test.entity.ResultDto;
import com.test.entity.User;
import com.test.service.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationContext applicationContext;

    @RequestMapping(method = RequestMethod.GET)
    public String list(Model model, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        List<User> userList = userService.userList(httpServletRequest, httpServletResponse);
        model.addAttribute("userList", userList);
        String userReview = "<script src=\"http://localhost:9000/xss.js\"></script>";
        model.addAttribute("userReview", userReview);
        return "user";
    }

    @RequestMapping(method = RequestMethod.POST, params = "userName")
    public String add(User user) {
        userService.addUser(user);
        return "redirect:/user";
    }

    // params参数意味着请求参数中必须包含该参数
    @RequestMapping(method = RequestMethod.POST, params = "userId")
    public String delete(@RequestParam("userId") Integer userId) {
        userService.deleteUser(userId);
        return "redirect:/user";
    }

    @GetMapping("/nacosMap")
    public ResultDto nacosMapTest() {
        AnnotationConfigServletWebServerApplicationContext context =
                (AnnotationConfigServletWebServerApplicationContext) applicationContext;
        DefaultListableBeanFactory defaultListableBeanFactory = context.getDefaultListableBeanFactory();
        ConcurrentHashMap<String, Object> singletonObjects =
                (ConcurrentHashMap<String, Object>) defaultListableBeanFactory.getSingletonMutex();
        List<Object> objectList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : singletonObjects.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("nacosConfigBean")) {
                objectList.add(entry.getValue());
            }
        }
        return new ResultDto();
    }

}