# Spring Boot와 ELKStack, Filebeat 연동 정리
MSA구성 시 서비스 별로 스프링부트 로그를 엘라스틱서치 및 키바나로 연동하기 위해서 공부했던 내용들을 기록하기 위해 작성하였습니다.  
> **목표 구성**  
1. 추후 컨테이너 올려서 로그파일을 남기면서 logstash로 연동을 해야했기 때문에 Filebeat를 이용하여 가볍게 로그 수집
2. logstash에서 정제한 데이터를 바로 키바나로 데이터를 보내는 것이 아닌 엘라스틱서치로 수집 후 키바나가 엘라스틱서치에서 데이터를 가져가도록 구성

#### 최종 구상도?)  

container1(filebeat) ↘  
container2(filebeat) ➡ container(Logstash) ➡ container(Elasticsearch) ⬅ container(Kibana)  
container3(filebeat) ↗

---

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
 /* other java sources */
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
## Filebeat 설정
filebeat.yml 파일에서 로그 데이터를 어떻게 전송할지 설정한다.
읽어올 로그 파일들의 경로 등 설정, 키바나, 엘라스틱서치, logstash 연결 설정 등 본인의 입맛에 맛도록 설정 가능한듯 하다.
헤매던 부분인 멀티라인 부분만 아래에 기록하겠습니다.  
> filebeat.yml  

```yml
 filebeat.inputs:
  # ... settings ...
  # 해당 패턴을 만나야 다음 라인으로 인식하는듯 함 (현재 내 로그는 스프링부트 로그이기 떄문에 날짜 패턴으로 라인을 구분하도록 설정)
  # 정규표현식 기준으로 append할 line을 정의
  multiline.pattern: ^[0-9]{4}-[0-9]{2}-[0-9]{2}[[:space:]][0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}[[:space:]]  
  # negate, match 값이 [true, after] 이면 다음 패턴값까지 로그 값을 합침
  multiline.negate: true
  multiline.match: after
```  

## Logstash 설정
> [Logstash 설정 참고 블로그 URL](https://umbum.dev/1144)  

위의 블로그를 참고하여 logstash.conf 파일을 설정하여 logstash 서버 시작시 옵션값으로 해당 파일을 사용하여 실행했습니다.
```sh
 # windows 환경 실행
 .\bin\logstash.bat -f .\config\logstash.conf
```  
logstash의 경우 filter 설정에서 애를 먹었습니다.
> logstash.conf  

```yml
 input {
  beats {
   port => 5044 # filebeat 기본포트
  }
 }
 # logstash 필터 설정(내보내질때 데이터 정제?)
 filter {
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:[loginfo][data]}\s+%{LOGLEVEL:[loginfo][level]} %{POSINT:[loginfo][pid]} --- \[\s*%{DATA:[loginfo][thread]}\] %{DATA:[loginfo][class]}\s+: %{GREEDYDATA:[loginfo][message]}" }
  }
  date {
    match => ["[loginfo][date]", "yyyy-MM-dd HH:mm:ss.SSS"]
    target => "@timestamp"
    timezone => "Asia/Seoul"
  }
  # 삭제할 필드 선택
  mutate {
    remove_field => ["host", "agent", "message"]
  }
 }
 output {
  # 콘솔로 보여지는 설정인듯함
  stdout { codec => rubydebug }
  # 엘라스틱서치 설정
  elasticsearch {
   hosts => ["http://localhost:9200"]
   index => "testlog-%{+YYYY.MM.dd}"
   ecs_compatibility => disabled
  }
 }
```
yml 설정파일에서 g.match 부분에서 message 값은 본인의 스프링 부트 logback 설정파일에서 내보내는 log파일 패턴에 맞도록 수정해주어야 정상 동작합니다.

## Elasticsearch, kibana 설정
elasticsearch의 경우 크게 설정할 부분은 없었고 kibana의 경우 elasticsearch와 연동하기 위해 설정파일에 URL 설정이 필요했습니다.
> kibana.yml  
```yml
 # 아래 두 가지를 주석 해제후 본인의 설정에 맞도록 수정한다.
 server.host: "localhost"
 elasticsearch.hosts: ["http://localhost:9200"]
```

위의 설정을 마친 후 Elasticsearch, Kibana, Logstash, Filebeat, SpringBoot Service를 실행시키고   
본인이 작성한 로그가 보이도록 테스트 후 Elasticsearch(Curl 등 이용)와 Kibana에서 정상적으로 로그가 남았는지 확인했습니다.
