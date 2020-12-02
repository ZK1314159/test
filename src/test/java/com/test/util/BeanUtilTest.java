package com.test.util;

import org.junit.jupiter.api.Test;

import com.test.entity.SrcObject;
import com.test.entity.TargetObject;

/**
 * Description
 *
 * @author zengkai
 * Date: 2020/12/2 23:19
 */
class BeanUtilTest {

    @Test
    void test() {
        SrcObject src = new SrcObject();
        src.setId(123);
        src.setName("test");
        src.setHobby("game");
        SrcObject.SrcFamily family = new SrcObject.SrcFamily();
        family.setFather("father");
        family.setMother("mother");
        family.setFatherAge(52);
        src.setFamily(family);
        TargetObject target = new TargetObject();
        target = BeanUtil.copyProperties(src, target);
//        TargetObject target = BeanUtil.copyProperties(src, TargetObject.class);
    }
}