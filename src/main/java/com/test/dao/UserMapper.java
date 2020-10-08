package com.test.dao;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/6/16 14:32 <br>
 */

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import com.test.entity.User;

@Mapper
@Component
public interface UserMapper {
    List<User> userList();

    void addUser(User user);

    void deleteUser(Integer id);
}
