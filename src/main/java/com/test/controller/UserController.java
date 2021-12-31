package com.test.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.test.entity.User;
import com.test.service.UserService;

@Controller
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(method = RequestMethod.GET)
    public String list(Model model, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        List<User> userList = userService.userList(httpServletRequest, httpServletResponse);
        model.addAttribute("userList", userList);
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

}