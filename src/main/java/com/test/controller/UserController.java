package com.test.controller;

import java.util.List;

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
    public String list(Model model) {
        List<User> userList = userService.userList();
        model.addAttribute("userList", userList);
        return "user";
    }

    @RequestMapping(method = RequestMethod.POST, params = "userName")
    public String add(User user) {
        userService.addUser(user);
        return "redirect:/user";
    }

    @RequestMapping(method = RequestMethod.POST, params = "userId")
    public String delete(@RequestParam("userId") Integer userId) {
        userService.deleteUser(userId);
        return "redirect:/user";
    }

}