# JUC Learning Project - Claude Configuration

## Project Overview

This is a Java learning project focused on Java Util Concurrent (JUC) package.

## Preferences

### Coding Style
- Use English for variable names, Chinese for comments and docstrings
- Keep code examples concise and well-documented
- Each example file should demonstrate a specific JUC concept
- Use `System.out.println` for learning examples (simplicity over logging)

### Code Organization
```
src/main/java/com/example/juc/
├── basic/          # Basic concepts (Thread, Runnable, etc.)
├── locks/          # Lock mechanisms (synchronized, ReentrantLock, etc.)
├── atomic/         # Atomic operations
├── collections/    # Concurrent collections
├── tools/          # Synchronization tools (CountDownLatch, Semaphore, etc.)
├── pools/          # Thread pools
└── async/          # Async programming (CompletableFuture, Virtual Threads)
```

### When Adding New Examples
1. Create appropriate subdirectory if it doesn't exist
2. Use descriptive class names ending with `Example` or `DeepDive`
3. Add main method that can be executed directly
4. Include detailed comments explaining the concepts
5. Update README.md with new file description

### Testing
- Use JUnit 5 for unit tests
- Create test files in `src/test/java/` matching source structure
- Test concurrent scenarios with multiple runs to ensure thread safety

### Build and Run
- Use Maven for build management
- Run examples with: `mvn exec:java -Dexec.mainClass="com.example.juc.ExampleClass"`
- Java version: 11 (兼容虚拟线程需要 17+)

### Project Conventions
- Never use `Executors.newFixedThreadPool()` in production code examples
- Always use try-finally for lock/unlock operations
- Always handle InterruptedException properly
- Demonstrate best practices alongside basic usage
