# Agent Gateway

一个基于 Java 的嵌入式多智能体协作网关 (Sub-Agent 模式)。

## 核心特性
- **Sub-Agent 模式**：中心模型识别意图，分发执行，结果确认。
- **Dify 集成**：支持 Dify 架构智能体协作。
- **配置中心**：网页配置 Agent，支持热更新。
- **OpenAPI**：支持第三方系统通过标准 API 调用。
- **现代化 UI**：基于 Tailwind CSS 的配置与测试界面。

## Docker 部署 (推荐)

### 本地编译并启动镜像
```bash
# 构建镜像
docker build -t agent-gateway:latest .

# 启动容器 (映射端口 8080)
docker run -d -p 8080:8080 \
  -e GATEWAY_LLM_APIKEY=your_openai_api_key \
  --name agent-gateway \
  agent-gateway:latest
```

## 本地开发环境
- Java 21
- Maven 3.9+
- H2 数据库 (默认使用嵌入式)

### 编译运行
```bash
mvn clean install
mvn spring-boot:run -pl gateway-server
```
网页控制台：[http://localhost:8080](http://localhost:8080)
API 文档：[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
