package com.test.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.test.repository.mysql.NewUserMapper;
import com.test.entity.User;
import com.test.service.interfaces.NewUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/27 17:01 <br>
 */
@DS("backupSlave")
@Service
@Slf4j
public class NewUserServiceImpl implements NewUserService {

    @Autowired
    private NewUserMapper newUserMapper;

    public List<User> userList(HttpServletRequest httpServletRequest, HttpServletResponse httpResponse) {
        return newUserMapper.userList();
    }

    @DS("backupMaster")
    public void addUser(User user) {
        newUserMapper.addUser(user);
    }

    @DS("backupMaster")
    public void deleteUser(Integer userId) {
        newUserMapper.deleteUser(userId);
    }

}
