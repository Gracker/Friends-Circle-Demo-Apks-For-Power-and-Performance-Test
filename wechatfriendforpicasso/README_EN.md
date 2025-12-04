# Friend Circle Performance Testing Module

This module is specifically designed to test Android scrolling performance, providing a platform for studying and optimizing Android UI performance through simulating different load levels.

*阅读[中文版](README.md)*

## Features

- **Three Load Modes**:
  - Light Load: Small computation per frame, high scrolling smoothness
  - Medium Load: Medium computation per frame, some scrolling pressure
  - Heavy Load: Large computation per frame, heavy scrolling pressure
  
- **Precise Control**:
  - Using random number generators with fixed seeds to ensure consistent test data each time
  - Control the number of comments and likes generated for different loads through position offsets
  
- **Performance Monitoring**:
  - Adding Trace points at key code locations for performance analysis using tools like Perfetto
  - Supporting continuous load simulation during scrolling, closer to real-world usage scenarios

## How to Use

1. Select the load level to test from the main interface
2. Observe UI performance during list scrolling
3. Collect performance data using Android Profiler or Perfetto
4. Analyze results and optimize code

## Implementation Details

- Using RecyclerView to efficiently display list content
- Dynamically adjusting comment and like counts for different load levels
- Controlling image loading during scrolling to optimize scrolling experience
- Implementing View object caching to reduce View creation overhead

## Optimization Suggestions

- Monitor frame rate and dropped frames to identify performance bottlenecks
- Optimize list item layout structure to reduce overdraw
- Control background computation tasks during scrolling
- Use asynchronous loading mechanisms to reduce main thread burden

## Relationship with Main Project

This is a submodule of the [High Performance WeChat Friend Circle Demo](../README_EN.md), focusing on performance testing. The project also includes the original implementation module and power consumption testing module. 