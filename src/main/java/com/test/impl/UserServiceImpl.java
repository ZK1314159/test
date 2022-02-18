package com.test.impl;

import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.test.dao.UserMapper;
import com.test.entity.User;
import com.test.service.UserService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/27 17:01 <br>
 */
@DS("slave")
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    public List<User> userList(HttpServletRequest httpServletRequest, HttpServletResponse httpResponse) {
        HttpSession httpSession = httpServletRequest.getSession();
        if (httpSession.getAttribute("userId") == null) {
            //将在redis操作session
            httpSession.setAttribute("userId","123456");
            log.info("====session为空=======");
        } else {
            log.info("=========session========" + httpSession.getAttribute("userId"));
        }
        Cookie[] cookies = httpServletRequest.getCookies();
        String userInfo = getCookie(cookies, "userInfo");
        if (userInfo == null || userInfo.isEmpty()) {
            httpResponse.addCookie(new Cookie("userInfo", "dkfsdkf"));
        }
        return userMapper.userList();
    }

    @DS("master")
    @Transactional(rollbackFor = Exception.class)
    public void addUser(User user) {
        userMapper.addUser(user);
    }

    @DS("master")
    public void deleteUser(Integer userId) {
        userMapper.deleteUser(userId);
    }

    private String getCookie(Cookie[] cookies, String key) {
        if (cookies != null && cookies.length >= 1) {
            for (Cookie cookie : cookies) {
                if ("userInfo".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
