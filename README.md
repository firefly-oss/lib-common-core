# Firefly Common Core Library
A core library for the Firefly platform that provides essential functionality for building reactive microservices with Spring WebFlux.

## Overview

The Firefly Common Core Library is a foundational component of the Firefly platform, providing common utilities and configurations for building reactive microservices. It is built on top of Spring WebFlux and offers features like transaction tracking, WebClient configuration, and reactive HTTP client utilities.

## Features

- **Transaction Tracking**: Automatically generates and propagates transaction IDs across microservices for distributed tracing
- **WebClient Configuration**: Pre-configured WebClient with transaction ID propagation
- **WebClient Template**: Simplified API for making HTTP requests with automatic header propagation
- **Auto-Configuration**: Spring Boot auto-configuration for easy integration

## Installation

Add the following dependency to your Maven project:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Or for Gradle:

```groovy
implementation 'com.catalis:lib-common-core:1.0.0-SNAPSHOT'
```

## Usage

### Transaction Tracking

The library automatically adds a transaction filter that generates and propagates a unique transaction ID (`X-Transaction-Id`) across services. This ID is added to both incoming and outgoing requests, making it easier to trace requests across multiple services.

No additional configuration is needed as the `TransactionFilter` is automatically registered as a Spring component.

### WebClient Usage

The library provides a pre-configured `WebClient` bean that automatically propagates the transaction ID. You can inject and use it directly:

```java
@Service
public class MyService {
    private final WebClient webClient;

    public MyService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<MyResponse> callExternalService() {
        return webClient.get()
                .uri("https://api.example.com/resource")
                .retrieve()
                .bodyToMono(MyResponse.class);
    }
}
```

### WebClient Template

For more convenience, you can use the `WebClientTemplate` which provides a higher-level API for making HTTP requests:

```java
@Service
public class MyService {
    private final WebClientTemplate webClientTemplate;

    public MyService(WebClientTemplate webClientTemplate) {
        this.webClientTemplate = webClientTemplate;
    }

    public Mono<MyResponse> getResource(ServerWebExchange exchange) {
        return webClientTemplate.get(
                "https://api.example.com",
                "/resource",
                MyResponse.class,
                exchange
        );
    }

    public Mono<MyResponse> createResource(MyRequest request, ServerWebExchange exchange) {
        return webClientTemplate.post(
                "https://api.example.com",
                "/resource",
                request,
                MyResponse.class,
                exchange
        );
    }
}
```

## Configuration

### WebClient Properties

You can configure the WebClient by adding properties to your `application.yml` or `application.properties` file:

```yaml
webclient:
  skip-headers:
    - connection
    - keep-alive
    - proxy-authenticate
    - proxy-authorization
    - te
    - trailer
    - transfer-encoding
    - upgrade
```

The `skip-headers` property specifies which HTTP headers should not be propagated when forwarding requests.

## Building from Source

To build the library from source:

```bash
git clone https://github.com/firefly-oss/lib-common-core.git
cd lib-common-core
./mvnw clean install
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request