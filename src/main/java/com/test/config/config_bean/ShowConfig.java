package com.test.config.config_bean;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import com.test.config.annotatin.FieldOrder;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/11 22:17
 */
@Configuration
@ConfigurationProperties("show.flow")
@Data
public class ShowConfig {

    private OperationFlow addShow;
    private OperationFlow editShow;

    @Data
    public static class OperationFlow {
        @FieldOrder(1)
        private boolean deleteCache;
        @FieldOrder(2)
        private boolean pushChannel;
        @FieldOrder(3)
        private boolean pushWencai;
    }
}
