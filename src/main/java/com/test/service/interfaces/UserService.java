package com.test.service.interfaces;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.test.entity.User;

public interface UserService {

    List<User> userList(HttpServletRequest httpServletRequest, HttpServletResponse httpResponse);

    void addUser(User user);

    void deleteUser(Integer userId);

}