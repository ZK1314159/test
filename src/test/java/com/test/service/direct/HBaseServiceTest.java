package com.test.service.direct;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
class HBaseServiceTest {

  @Autowired
  private HBaseService hBaseService;

  @Test
  void test() {
    String tableName = "student";
    List<String> rowKeys = new ArrayList<>();
    rowKeys.add("row3");
    rowKeys.add("row4");
    String familyName = "desc";
    List<String> columns = new ArrayList<>();
    columns.add("name");
    columns.add("age");
    List<String> values = new ArrayList<>();
    values.add("zhangsan");
    values.add("18");
    hBaseService.batchPutOrUpdateData(tableName, rowKeys, familyName, columns, values);
  }

}