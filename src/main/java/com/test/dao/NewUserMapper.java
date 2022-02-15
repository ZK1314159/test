package com.test.dao;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/6/16 14:32 <br>
 */

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import com.test.entity.User;

@Mapper
@Repository
public interface NewUserMapper {

    @Select("select id as userId, name as userName from user")
    List<User> userList();

    @Insert("insert into user(name) values(#{userName})")
    void addUser(User user);

    @Delete("delete from user where id = #{id}")
    void deleteUser(Integer id);

}
