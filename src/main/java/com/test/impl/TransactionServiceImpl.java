package com.test.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.test.dao.NewUserMapper;
import com.test.dao.UserMapper;
import com.test.entity.User;
import com.test.service.NewUserService;
import com.test.service.TransactionService;
import com.test.service.UserService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/4/25 11:16 <br>
 */

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private UserService userService;

    @Autowired
    private NewUserService newUserService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NewUserMapper newUserMapper;

    @Transactional(rollbackFor = Exception.class)
    public void transactionTest() throws Exception {

        User user1 = new User();
        user1.setUserName("2032");
        userMapper.addUser(user1);

        try {
            User user2 = new User();
//            user2.setUserName("2024");
            userMapper.addUser(user2);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        User user2 = new User();
//        user2.setUserName("2029");
//        userService.addUser(user2);

        User user3 = new User();
        user3.setUserName("2033");
        userMapper.addUser(user3);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void distribute() throws Exception {
        User user1 = new User();
        user1.setUserName("2033");
        userService.addUser(user1);
        newUserService.addUser(user1);
    }

}
