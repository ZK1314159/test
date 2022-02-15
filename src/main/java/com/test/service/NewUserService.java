package com.test.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.test.entity.User;

public interface NewUserService {

    List<User> userList(HttpServletRequest httpServletRequest, HttpServletResponse httpResponse);

    void addUser(User user);

    void deleteUser(Integer userId);

}