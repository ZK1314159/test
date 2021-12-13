package com.test.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/11 22:17
 */
@Configuration
@ConfigurationProperties("show.flow-new")
@Data
public class ShowConfigNew {

    private OperationFlowNew addShow;
    private OperationFlowNew editShow;

    @Data
    public static class OperationFlowNew {
        private Integer deleteCache;
        private Integer pushChannel;
        private Integer pushWencai;
    }
}
