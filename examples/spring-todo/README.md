# Spring Boot 任务接口观察示例

配套教程：https://jiyuebo666.github.io/dev-tools-guide/framework.html

## 环境

Spring Boot 4.1.2，编译目标 Java 21。建议 JDK 21 或 25，运行 Maven 的 JDK 也应符合 Spring Boot 要求。

## 运行

在本目录执行 `mvn spring-boot:run`，访问 `http://127.0.0.1:8090/api/tasks`。`PORT=8091 mvn spring-boot:run` 可以修改端口。按 Control + C 停止。

`mvn test` 执行接口行为检查。`mvn package` 打包，然后可运行 `java -jar target/spring-todo-0.0.1-SNAPSHOT.jar`。

## 范围

实现任务创建、查询、完成、删除、请求体校验、统一已知错误与意外错误处理、请求编号和耗时日志。仅绑定本机，使用内存保存任务，重启后清空。没有连接数据库，也没有提供登录、限流、上传或数据库集成测试；这些扩展的目的与接入方式在教程中说明。

在旧任务清单之外单独解压运行，可对照学习。源码没有复制原项目的数据库密码或数据。
