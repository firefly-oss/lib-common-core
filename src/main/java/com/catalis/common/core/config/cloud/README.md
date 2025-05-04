# Centralized Configuration with Spring Cloud Config

This package provides integration with Spring Cloud Config for centralized configuration management in microservices.

## Features

- Automatic integration with Spring Cloud Config Server
- Dynamic configuration refresh without application restart
- Configurable retry mechanism for config server connection
- Support for different environments and profiles
- Fallback to local configuration when config server is unavailable

## Usage

### 1. Enable Spring Cloud Config

Add the following to your `application.yml` or `application.properties`:

```yaml
cloud:
  config:
    enabled: true
    uri: http://config-server:8888
```

### 2. Configure Additional Properties (Optional)

```yaml
cloud:
  config:
    enabled: true
    uri: http://config-server:8888
    name: my-service  # Defaults to spring.application.name
    profile: dev      # Defaults to active profiles
    label: main       # Git branch or other version label
    fail-fast: true   # Whether to fail startup if config server is unavailable
    timeout-ms: 5000  # Timeout for config server requests
    retry: true       # Whether to retry failed requests
    refresh-enabled: true  # Whether to enable dynamic refresh
```

### 3. Enable Refresh Endpoint (for Dynamic Configuration)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: refresh,health,info
```

### 4. Use @RefreshScope for Beans That Should Be Refreshed

```java
@Service
@RefreshScope
public class MyService {
    @Value("${my.dynamic.property}")
    private String dynamicProperty;
    
    // This bean will be recreated when a refresh event occurs
}
```

### 5. Trigger Configuration Refresh

Send a POST request to the refresh endpoint:

```
POST /actuator/refresh
```

## Example Configuration

See `application-cloud-config-example.yml` for a complete example configuration.

## Spring Cloud Config Server Setup

To use centralized configuration, you need to set up a Spring Cloud Config Server. Here's a quick guide:

1. Create a new Spring Boot project with the Spring Cloud Config Server dependency
2. Add `@EnableConfigServer` to your main application class
3. Configure the server to use a Git repository or file system for configuration storage
4. Start the server

Example `application.yml` for the config server:

```yaml
server:
  port: 8888

spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/your-org/config-repo
          default-label: main
```

## Best Practices

1. Use a hierarchical configuration structure with common properties in a base file
2. Use environment-specific files for overriding properties
3. Use application-specific files for application-specific properties
4. Use profiles for different environments (dev, test, prod)
5. Store sensitive information in a secure location (e.g., HashiCorp Vault)
6. Use version control for configuration files
7. Implement proper access controls for the config server
8. Monitor the config server for availability and performance
