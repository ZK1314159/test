package com.test.show;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.test.entity.Show;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/13 11:17
 */
@Component
public class FlowClient {

    @Autowired
    private ApplicationContext applicationContext;

    public void operateFlow(OperateType type, Show show) {
        String beanName = type.getType() + "Flow";
        AbstractFlow abstractFlow = applicationContext.getBean(beanName, AbstractFlow.class);
        abstractFlow.operate(type.getType(), show);
    }

}
