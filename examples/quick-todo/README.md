# 工程速通用法参照

教程入口：https://jiyuebo666.github.io/dev-tools-guide/quick.html

正文从空目录手写，不要求下载工程。这里保留同一套源码用于维护时验证文档。

- Java 编译目标 21，Spring Boot 4.1.2。
- 默认 127.0.0.1:8092，H2 内存库，停止重启后清空。
- 正常请求带 X-API-Key: local-practice-key；仅用于本机单密钥练习，未实现多人登录授权。
- 运行：mvn spring-boot:run。
- 测试：mvn test。test profile 使用独立 H2 库；MySQL 差异需要单独的集成测试。
- MySQL 使用 application-mysql.yaml，单独库 todo_quick_lab，ddl-auto=validate。SQL 和环境变量在教程中提供。
- 不提交密码，不读取旧任务库，不默认暴露上传文件。
