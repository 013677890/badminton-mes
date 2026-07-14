# 测试执行说明

后端测试按运行成本拆分为三组：

- `gradlew.bat test`：默认单元测试和无外部依赖的契约测试；排除 `integration`、`stress` 标签。
- `gradlew.bat integrationTest`：需要外部 MySQL、Redis 测试基础设施的集成测试。
- `gradlew.bat stressTest`：11 个业务模块的进程内并发压力基线，默认每模块 10,000 次操作。

调整单模块压力操作量：

```powershell
.\gradlew.bat stressTest -Dmes.stress.operations=50000
```

压力套件验证转换器、状态模型、金额计算与 Key 生成等核心无状态路径在并发下没有共享状态污染或异常。数据库锁竞争、Redis 吞吐和 HTTP 容量属于外部基础设施压测，应在隔离环境结合 `integrationTest` 和实际部署拓扑执行。
