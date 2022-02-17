package com.test.impl;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import com.example.dubbo.service.DubboProvider;
import com.test.service.DubboService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/7/9 10:14 <br>
 */
@Service
public class DubboServiceImpl implements DubboService {

    @DubboReference
    private DubboProvider provider;

    public String invokeProvider(String word) {
        String result = provider.say(word);
        return result;
    }
}
