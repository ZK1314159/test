package com.test.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import com.test.entity.User;
import com.test.service.SpelService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/4/24 17:27 <br>
 */
@Service
public class SpelServiceImpl implements SpelService {

    private static Logger logger = LoggerFactory.getLogger(SpelServiceImpl.class);

    public void spelTest() {
        User user = new User();
        user.setUserId(123);
        user.setUserName("zengkai");

        ExpressionParser parser = new SpelExpressionParser();
        EvaluationContext context = new StandardEvaluationContext(user);
        int userId = (Integer) parser.parseExpression("userId + 3").getValue(context);
        logger.info("userId : " + userId);

        List list = (List) parser.parseExpression("{1, 2, 3, 4}").getValue();
        Integer number = (Integer) list.get(0);
        logger.info(number.toString());
    }
}
