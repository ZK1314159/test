package com.test.service.interfaces;

import com.test.config.config_bean.ShowConfig;
import com.test.entity.Show;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/11 22:39
 */
public interface OperationFlowService {

    void operate(ShowConfig.OperationFlow operationFlow, Show show);

    public void pushChannel(Show show);

    public void pushWencai(Show show);
}
