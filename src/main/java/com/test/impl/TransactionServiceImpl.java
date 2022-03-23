package com.test.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

    @Transactional(rollbackFor = RuntimeException.class)
    public void transactionTest() throws Exception {

        User user1 = new User();
        user1.setUserName("2025");
        userService.addUser(user1);

//        try {
//            User user2 = new User();
////            user2.setUserName("2024");
//            userMapper.addUser(user2);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        try {
            User user2 = new User();
//        user2.setUserName("2024");
            userService.addUser(user2);
        } catch (Exception e) {
            log.info("test");
            e.printStackTrace();
        }

//        try {
//            User user4 = new User();
////        user2.setUserName("2021");
//            userService.addUser(user4);
//        } catch (Exception e) {
//            log.info("fail");
//        }
//
//        User user3 = new User();
//        user3.setUserName("2033");
//        userMapper.addUser(user3);
    }

    @Transactional
    @Override
    public void distribute() throws Exception {
        User user1 = new User();
        user1.setUserName("2022");
        userService.addUser(user1);
//        newUserService.addUser(user1);
        try {
            propagate(true);
        } catch (Exception e) {
            log.info("fail");
        }
    }

    @Override
    public void propagate(boolean exception) throws Exception {
        User user1 = new User();
        userService.addUser(user1);
//        if (exception) {
//            throw new Exception();
//        }
    }

}
