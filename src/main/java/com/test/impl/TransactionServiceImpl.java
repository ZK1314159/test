package com.test.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.test.entity.User;
import com.test.service.TransactionService;
import com.test.service.UserService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/4/25 11:16 <br>
 */
@Transactional(rollbackFor = Exception.class)
@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private UserService userService;

    public void transactionTest() throws IndexOutOfBoundsException {

        User user1 = new User();
        user1.setUserName("1145");
        userService.addUser(user1);

        if (System.currentTimeMillis() > 100) {
            throw new IndexOutOfBoundsException();
        }

        User user2 = new User();
        user1.setUserName("1158");
        userService.addUser(user2);
    }
}
