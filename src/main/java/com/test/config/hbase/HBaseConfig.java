package com.test.config.hbase;

import com.test.service.direct.HBaseService;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HBase相关配置
 */
@Configuration
public class HBaseConfig {

  @Value("${hbase.zookeeper.quorum}")
  private String zookeeperQuorum;

  @Value("${hbase.zookeeper.port}")
  private String clientPort;

//  @Value("${zookeeper.znode.parent}")
//  private String znodeParent;

  @Bean
  public HBaseService getHbaseService(){
    org.apache.hadoop.conf.Configuration conf = HBaseConfiguration.create();
    conf.set("hbase.zookeeper.quorum", zookeeperQuorum);
    conf.set("hbase.zookeeper.property.clientPort", clientPort);
//    conf.set("zookeeper.znode.parent", znodeParent);
    return new HBaseService(conf);
  }

}
