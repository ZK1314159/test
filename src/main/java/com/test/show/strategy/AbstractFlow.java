package com.test.show.strategy;

import com.test.entity.Show;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/13 10:51
 */
public abstract class AbstractFlow {

    abstract void pushChannel(Show show);

    abstract void pushWencai(Show show);

    void deleteCache(Show show) {
        System.out.println("deleteCache");
    }

}
