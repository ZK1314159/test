package com.test.config;

import com.test.config.config_bean.ShowConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/11 22:24
 */
@SpringBootTest
class ShowConfigTest {

    @Autowired
    private ShowConfig showConfig;

    @Test
    void test() {
        ShowConfig.OperationFlow addShow = showConfig.getAddShow();
        ShowConfig.OperationFlow editShow = showConfig.getEditShow();
    }

}