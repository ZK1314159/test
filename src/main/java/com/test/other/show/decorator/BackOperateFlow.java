package com.test.other.show.decorator;

import com.test.entity.Show;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/13 17:10
 */
public abstract class BackOperateFlow {

    abstract void setBackOperateFlow(BackOperateFlow backOperateFlow);

    abstract void operate(Show show);
}
