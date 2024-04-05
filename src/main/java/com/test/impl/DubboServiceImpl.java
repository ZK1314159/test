package com.test.impl;

import com.test.service.DubboService;
import org.springframework.stereotype.Service;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/7/9 10:14 <br>
 */
@Service
public class DubboServiceImpl implements DubboService {

//    @DubboReference
//    private DubboProvider provider;

    public String invokeProvider(String word) {
        return null;
//        String result = provider.say(word);
//        return result;
    }
}
