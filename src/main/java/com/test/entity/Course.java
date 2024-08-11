package com.test.entity;

import lombok.Data;

@Data
public class Course implements java.io.Serializable {
    private Integer number;
    private String courseName;
}
