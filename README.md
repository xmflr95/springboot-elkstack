# Spring Boot와 ELKStack, Filebeat 연동 정리
스프링부트 로그를 엘라스틱서치 및 키바나로 연동하기 위해서 공부했던 내용들을 기록하기 위해 작성하였습니다.

## 설정에 참고할 파일
* [logback-spring.xml](https://github.com/xmflr95/springboot-elkstack/blob/main/src/main/resources/logback-spring.xml) : springboot logback 설정 파일
* [filebeat.yml](https://github.com/xmflr95/springboot-elkstack/blob/main/elk_yml/filebeat.yml) : filebeat 설정 파일
* [kibana.yml](https://github.com/xmflr95/springboot-elkstack/blob/main/elk_yml/kibana.yml) : kibana 설정 파일
* [logstash.conf](https://github.com/xmflr95/springboot-elkstack/blob/main/elk_yml/logstash.conf) : logstash 설정 파일


## 스프링부트 logback 설정
기본설정된 경로를 기준으로 src/main/resources 아래에 **logback-spring.xml** 파일을 생성한다.
```xml
 <!-- log 파일로 내보내질 패턴을 설정한다 -->
 <property name="FILE_LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} %5p ${PID:- } --- [%t] %logger{40} : %m%n%wEx"/>
 <property name="FILE_LOG_CHARSET" value="UTF-8"/>
 <!-- ... 기타 설정들 / other settings -->
 <property name="LOG_PATH" value="./logs"/>
 <property name="FILE_NAME" value="demo"/>
 <property name="LOG_FILE" value="${LOG_PATH}/${FILE_NAME}-json.log"/>
 <!-- ... 기타 설정들 / other settings -->
 <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <encoder>
        <!-- encoder 패턴으로 위에서 설정한 패턴을 사용함 -->
        <pattern>${FILE_LOG_PATTERN}</pattern>
        <charset>${FILE_LOG_CHARSET}</charset>
    </encoder>
    <!-- 로그 파일명 설정 -->
    <file>${LOG_FILE}</file>
    <!-- 로그 정책 설정 : 용량, 삭제기간 등등 -->
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>${${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz}</fileNamePattern>
        <cleanHistoryOnStart>false</cleanHistoryOnStart>
        <maxFileSize>10MB</maxFileSize>
        <totalSizeCap>0</totalSizeCap>
        <maxHistory>7</maxHistory>
    </rollingPolicy>
</appender>
```
위처럼 설정 후 Slf4j 등으로 소스에 로그를 남기도록 작성하여 테스트 후  
${project}/logs 밑에 log 파일이 정상적으로 작성되는지 확인한다.  

ex) [DemoController.java](https://github.com/xmflr95/springboot-elkstack/blob/main/src/main/java/com/example/demo/controller/DemoController.java)
```java
 // LoggerFactory가 아닌 @Slf4j로 어노테이션으로 사용해도 무방하다.
 private final Logger logger = LoggerFactory.getLogger(this.getClass());
 logger.info("Connect GET /status");
 logger.info("server.port : " + env.getProperty("server.port")
   + "\nlocal.server.port : " + env.getProperty("local.server.port")
   + "\nTest INFO LOG");
 logger.warn("TEST WARN LOG");
 logger.error("TEST ERR LOG");
```

> 결과물(demo-json.log)
```txt
 2022-02-22 15:21:35.244  INFO 3968 --- [http-nio-8080-exec-1] c.example.demo.controller.DemoController : Connect GET /status
 2022-02-22 15:21:35.244  INFO 3968 --- [http-nio-8080-exec-1] c.example.demo.controller.DemoController : server.port : 8080
 local.server.port : 8080
 Test INFO LOG
 2022-02-22 15:21:35.245  WARN 3968 --- [http-nio-8080-exec-1] c.example.demo.controller.DemoController : TEST WARN LOG
 2022-02-22 15:21:35.245 ERROR 3968 --- [http-nio-8080-exec-1] c.example.demo.controller.DemoController : TEST ERR LOG
```
###
