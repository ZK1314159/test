package com.test.other.show.strategy;

import org.springframework.stereotype.Component;

import com.test.entity.Show;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/13 11:19
 */
@Component
public class AddShowFlow extends AbstractFlow {

    @Override
    void pushChannel(Show show) {
        System.out.println("pushChannel");
    }

    @Override
    void pushWencai(Show show) {
        System.out.println("pushWencai");
    }

}
