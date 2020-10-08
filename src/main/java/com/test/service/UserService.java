package com.test.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.test.entity.User;
import com.test.dao.UserMapper;

import java.util.List;

public interface UserService {

    List<User> userList();

    void addUser(User user);

    void deleteUser(Integer userId);
}