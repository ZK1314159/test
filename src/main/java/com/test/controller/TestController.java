package com.test.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.test.config.NewKafka;
import com.test.config.ShowConfig;
import com.test.entity.FrontPostTest;
import com.test.entity.FrontTestDTO;
import com.test.entity.PostRequestDto;
import com.test.entity.ResultDto;
import com.test.entity.Show;
import com.test.feign.TestFeignClient;
import com.test.service.AopTestService;
import com.test.service.CommonService;
import com.test.service.OperationFlowService;
import com.test.service.OperationFlowServiceNew;
import com.test.service.TestService;
import com.test.show.decorator.BackOperateFlowClient;
import com.test.show.strategy.AbstractFlow;
import com.test.show.strategy.FlowClient;
import com.test.show.strategy.OperateType;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/7/3 15:36
 */
@RestController("testController")
@RequestMapping("/test")
@Slf4j
@Data
public class TestController {

    private int id = 3;

    @Autowired
    private TestFeignClient testFeignClient;

    @Autowired
    private CommonService commonService;

    @Autowired
    private TestService testService;

    @Autowired
    private OperationFlowService operationFlowService;

    @Autowired
    private AopTestService aopTestService;

    @Autowired
    private FlowClient flowClient;

    @Autowired
    private BackOperateFlowClient backOperateFlowClient;

    @Autowired
    private OperationFlowServiceNew operationFlowServiceNew;

    @Autowired
    private AbstractFlow abstractFlow;
    
    @Value("${test.name}")
    private String name;

    @Value("${spring.datasource.dynamic.datasource.master.username}")
    private String username;

    @Value("${spring.datasource.dynamic.datasource.master.password}")
    private String password;

    @Value("${test.newdata}")
    private String test;

    @Autowired
    private NewKafka newKafka;

    @GetMapping("/aop")
    public ResultDto aopTest() {
        aopTestService.test();
        ResultDto result = new ResultDto();
        return result;
    }

//    ThreadPoolExecutor threadPoolExecutor =
//            new ThreadPoolExecutor(3, 6, 100, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10),
//                    new ThreadPoolExecutor.CallerRunsPolicy());

//    @Autowired
//    private KafkaProperties kafkaProperties;

    @GetMapping("/thread")
    public ResultDto threadTest() {
        log.info(Thread.currentThread().getName() + " start");
        TestController testController = new TestController();
        testController.id = 5;
//        try {
//            Thread.sleep(10000);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        System.out.println(Thread.currentThread().getName() + " finish");
//        for (int i = 0; i < 30; i++) {
//            threadPoolExecutor.submit(() -> {
//                try {
//                    Thread.sleep(2000);
//                    System.out.println(Thread.currentThread().getName() + " finish");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            });
//        }
        return new ResultDto();
    }

    @GetMapping("/kafka")
    public ResultDto kafkaTest() {
        KafkaProperties postscan = newKafka.getPostscan();
        ResultDto result = new ResultDto();
//        Map<String, String> test = kafkaProperties.getProperties();
        return result;
    }

    @RequestMapping(value = "/log", produces = "application/json", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ResultDto logTest() {
        log.info("test log!");
        ResultDto result = new ResultDto();
        return result;
    }

    @RequestMapping(value = "/properties", produces = "application/json", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ResultDto propertiesTest() {
        log.info("test.name={}", name);
        log.info("username={}", username);
        log.info("password={}", password);
        ResultDto result = new ResultDto();
        return result;
    }

    @GetMapping(value = "/get")
    public FrontTestDTO getTest(@RequestParam Integer userId, @RequestParam List<Integer> uidList,
                                @RequestParam Double price, @RequestParam Boolean judge) {
        log.info("get test userId : " + userId);
        log.info("get test uidList : " + uidList);
        log.info("get test price : " + price);
        log.info("get test judge : " + judge);
        FrontTestDTO frontTestDTO = new FrontTestDTO();
        frontTestDTO.setTestByte((byte) 1);
        frontTestDTO.setTestShort((short) 2);
        frontTestDTO.setTestLong(7998L);
        frontTestDTO.setTestFloat(7.8f);
        frontTestDTO.setTestBoolean(true);
        frontTestDTO.setCharacter('d');
        return frontTestDTO;
    }

    @RequestMapping(value = "/post", method = RequestMethod.POST)
    public FrontTestDTO postTest(@RequestBody FrontPostTest frontPostTest) {
        log.info("post");
        log.info("user.userId : " + frontPostTest.getUserId());
        FrontTestDTO frontTestDTO = new FrontTestDTO();
        frontTestDTO.setTestLong(7998L);
        return frontTestDTO;
    }

    @GetMapping("/starter")
    public void staterTest() {
        commonService.print();
    }

    @GetMapping("/property")
    public ResultDto propertyTest() {
        commonService.testProperty();
        return new ResultDto();
    }

    @GetMapping("/yml")
    public ResultDto ymlPropertyTest() {
        testService.ymlPropertyTest();
        return new ResultDto();
    }

    @GetMapping("/reflect")
    public ResultDto reflectTest() {
        ShowConfig.OperationFlow operationFlow = new ShowConfig.OperationFlow();
        operationFlow.setPushChannel(false);
        operationFlow.setPushWencai(true);
        Show show = new Show();
        show.setSid(123);
        show.setTitle("dfjdfj");
        operationFlowService.operate(operationFlow, show);
        return new ResultDto();
    }

    @GetMapping("/show")
    public ResultDto showTest() {
        Show show = new Show();
        show.setSid(123);
        show.setTitle("dfjdfj");
        flowClient.operateFlow(OperateType.ADDSHOW, show);
        return new ResultDto();
    }

    @GetMapping("/decorator")
    public ResultDto decorator() {
        Show show = new Show();
        show.setSid(123);
        show.setTitle("dfjdfj");
        backOperateFlowClient.operate(OperateType.ADDSHOW, show);
        return new ResultDto();
    }

    @GetMapping("/newReflect")
    public ResultDto newReflect() {
        Show show = new Show();
        show.setSid(123);
        show.setTitle("dfjdfj");
        operationFlowServiceNew.operate("addShow", show);
        return new ResultDto();
    }

    @GetMapping("/abstract")
    public ResultDto abstractTest() {
        Show show = new Show();
        show.setSid(123);
        show.setTitle("dfjdfj");
        abstractFlow.operate("addShow", show);
        return new ResultDto();
    }

    @GetMapping("/feign")
    public ResultDto feign() {
//        String result = testFeignClient.test(1234);
        PostRequestDto requestDto = new PostRequestDto();
        requestDto.setId(123);
        requestDto.setName("gdhdfh");
        PostRequestDto postRequestDto = testFeignClient.post(requestDto);
        return new ResultDto();
    }

    @GetMapping("/nacosTest")
    public ResultDto nacosTest() {
        String value = test;
        log.info(value);
        return new ResultDto();
    }

}
