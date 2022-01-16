package com.test.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import com.test.config.YmlPropertyBeanConfig;
import com.test.service.TestService;

/**
 * @Description
 * @author zengkai
 * @date 2020/10/7 17:35
 */
@Slf4j
@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private YmlPropertyBeanConfig user;

    @Autowired
    private ApplicationContext applicationContext;

//    @Value("${test.value}")
//    private String value;

    @Value("${config.property.id}")
    private Integer id;

    @Value("${test.name}")
    private String name;

    private void test() {
        // 数组转集合
        // int[]无法直接转换为List<Integer>, 需要自己手动写循环
        // Integer[]可以直接转换为List<Integer>
        Integer[] ints = new Integer[] {1, 3, 5};
        List<Integer> tmpList = Arrays.asList(ints);
        List<Integer> list = new ArrayList<>(tmpList);

        // 集合转数组
        Integer[] tmpArray = new Integer[list.size()];
        Integer[] array = list.toArray(tmpArray);
    }

    @Override
    public void ymlPropertyTest() {
        log.info("info: " + user.getId() + " " + user.getName());
        TestService testService = applicationContext.getBean("testServiceImpl", TestService.class);
        log.info("5734tuiefer8" + testService);
    }

    @Override
    public void nacosTest() {
//        log.info("nacos property: " + value);

        for (int i = 0; i < 2; i++) {
            try {
//                throw new NullPointerException();
            } catch (Exception e) {
                continue;
            }
            break;
        }

    }


    public static void main(String[] args) throws Exception {
        String s = "fdjfkj";
        String s1 = s.replace("dj", "newchar");
        String s2 = s.replaceAll("dj", "newchar");
    }
}
