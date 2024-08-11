package com.test.entity;

import com.test.controller.TestController;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Description：result <br>
 *
 * @author zeng.kai <br>
 * test code
 * @see TestController#logTest()
 * CreateDate：2020/3/16 14:34 <br>
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ResultDto implements Serializable {
    private String result = "success";
}
