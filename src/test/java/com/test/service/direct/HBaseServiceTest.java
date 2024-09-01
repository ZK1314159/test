package com.test.service.direct;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
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
    rowKeys.add("row4");
    String familyName = "desc";
    List<String> columns = new ArrayList<>();
    columns.add("name");
    columns.add("age");
    columns.add("name");
    List<String> values = new ArrayList<>();
    values.add("zhangsan");
    values.add("18");
    values.add("kai");
    hBaseService.batchPutOrUpdateData(tableName, rowKeys, familyName, columns, values);
  }

  @Test
  void test2() {
    String tableName = "student";
    String familyName = "desc";
    Table<String, String, String> data = HashBasedTable.create();
    data.put("row5", "name", "zhangsan");
    data.put("row5", "age", "18");
    data.put("row6", "name", "lisi");
    data.put("row6", "age", "20");
    data.put("row7", "name", "wangwu");
    data.put("row7", "age", "22");
    hBaseService.batchPutOrUpdateTableData(tableName, familyName, data);
  }

}