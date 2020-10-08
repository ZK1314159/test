package com.test.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.test.dao.UserMapper;
import com.test.entity.User;
import com.test.service.UserService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/27 17:01 <br>
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    public List<User> userList() {
        return userMapper.userList();
    }

    public void addUser(User user) {
        userMapper.addUser(user);
    }

    public void deleteUser(Integer userId) {
        userMapper.deleteUser(userId);
    }
}
