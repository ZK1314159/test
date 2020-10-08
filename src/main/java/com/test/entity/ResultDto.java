package com.test.entity;

import java.io.Serializable;

import com.test.controller.TestController;

/**
 * Description：result <br>
 *
 * @author zeng.kai <br>
 * test code
 * @see TestController#logTest()
 * CreateDate：2020/3/16 14:34 <br>
 */
public class ResultDto implements Serializable {

    private String result = "success";

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
