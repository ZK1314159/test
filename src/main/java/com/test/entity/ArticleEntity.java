package com.test.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/7/23 17:06
 */
@Data
//@Document(indexName = "test")
public class ArticleEntity {

    @Id
    private String id;

    private String name;

    private Long age;

    private String courseName;

    private Long number;

}
