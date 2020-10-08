package com.test.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import com.example.dubbo.impl.DubboProviderImpl;
import com.example.dubbo.service.DubboProvider;
import com.test.service.DubboService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/7/9 10:14 <br>
 */
@Service
@Import(DubboProviderImpl.class)
public class DubboServiceImpl implements DubboService {

    //@Reference
    @Autowired
    private DubboProvider provider;

    public String invokeProvider(String word) {
        String result = provider.say(word);
//        DubboProviderImpl test = new DubboProviderImpl();
//        String result = test.say(word);
        return result;
    }
}
