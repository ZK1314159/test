package com.test.controller;

import com.alibaba.fastjson.JSON;
import com.test.entity.ResultDto;
import com.test.service.direct.HBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/hbase")
public class HbaseController {

  @Autowired
  private HBaseService hBaseService;

  @GetMapping("/getData")
  public ResultDto getData(@RequestParam String tableName, @RequestParam String rowKey) {
    Map<String,String> rowData = hBaseService.getRowData(tableName, rowKey);
    return new ResultDto(JSON.toJSONString(rowData));
  }

  @GetMapping("/insertOrUpdateData")
  public ResultDto insertOrUpdateData(@RequestParam String tableName, @RequestParam String rowKey, @RequestParam String familyName,
                                      @RequestParam String[] columns, @RequestParam String[] values) {
    hBaseService.putData(tableName, rowKey, familyName, columns, values);
    return new ResultDto();
  }

}