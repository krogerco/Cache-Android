# Cache for Kotlin

The Cache library makes it easy to create an in-memory key/value cache that honors configured cache policies and optionally persists entries.
It supports minimal configuration on Android by creating the cache file in the application's cache directory by default and also responds to low memory events.

- [The Problem](#the-problem)
- [Quick Start](#quick-start)
  - [Adding Dependencies](#adding-dependencies)
  - [Cache Configuration](#cache-configuration)
- [Key Components](#key-components)
  - [Overview](#overview)
  - [Snapshot Persistent Cache](#snapshot-persistent-cache)
  - [Cache Policy](#cache-policy)
  - [Memory Cache Manager](#memory-cache-manager)

## The Problem
In an application there is often a need to make data that is expensive to retrieve or create quickly accessible. The data may come from the network, a calculation, or something else. Along with making the data fast and easy to retrieve, policies may be needed to make sure the cached data does not grow unbounded and that the data does not go stale. It is possible the data should also be persisted locally so it is available between separate runs of the application. The primary purpose of this library is to make creating a cache that addresses these problems quick and easy with minimal configuration.

## Quick Start

### Adding Dependencies
The cache library modules are published on Maven Central and can be included as follows:
```kotlin
// core module
implementation("com.kroger.cache:cache:<version>")

// if using android (core module is already included)
implementation("com.kroger.cache:android:<version>")
```

### Cache Configuration
The library comes with a default file persistence implementation that uses `Json` from `kotlinx-serialization`.

```kotlin
val fileCache = SnapshotFileCacheBuilder.from(
    context, // application context used to reference application cache directory on Android
    filename = "cacheFile.json", // cache file created in application's cache directory
    keySerializer = String.serializer(), // serializer for cache keys
    valueSerializer = String.serializer(), // serializer for cache values
).build()
```

Once the `fileCache` is created a `MemoryCacheManager` can then be created that utilizes it.
```kotlin
cache = MemoryCacheManagerBuilder.from(
    context, // application context
    fileCache, // persistent cache to periodically save cache entries to
)
    .coroutineScope(coroutineScope) // CoroutineScope used to periodically save the cache
    .build()
```

Use the `cache`.

```kotlin
// add a value to the cache
cache.put("newKey", computeExpensiveValue())

// ...somewhere else retrieve and use the value
val expensiveValue = cache.get("newKey")
println(expensiveValue)
```

## Key Components

### Overview
Listed below are the key components of the cache library.

1. **SnapshotPersistentCache**: This interface has two functions. One to read data from a persistent cache and another to save data to the persistent cache. Data is saved in "snapshots", meaning the persistent cache always reads and saves the full cached data at a specific point in time.
2. **CachePolicy**: Used by the `MemoryCacheManger` to set properties such as max size and temporal policies (time to live and time to idle).
3. **MemoryCacheManager**: The `MemoryCacheManager` ties together the `CachePolicy`, `SnapshotPersistentCache` (optional), and in-memory cache in a thread safe way.

### Snapshot Persistent Cache
The library provides an implementation of `SnapshotPersistentCache` that saves to a file. This can be used independently from the `MemoryCacheManager` if needed. Writing and reading to the file is not thread safe so if multiple threads use the same `SnapshotPersistentCache` make sure the access is synchronized. On Android the file is saved to the application's cache directory when using the Android builder factory function `SnapshotFileCacheBuilder.from(...)`.

The builder factory function saves data to the file using a `StringFormat` from `kotlinx.serialization` defaulting to `Json`. A builder factory function overload is provided so a custom serialization strategy can be used. All that is required is a function with the signature `(ByteArray) -> T?` to read data from the file and a corresponding function with the signature `(T?) -> ByteArray` to write data to the file.

> **Note**: Only one `SnapshotPersistentCache` should exist at a time that references any given file.

### Cache Policy
A `CachePolicy` supports the following policies:
1. **Max Size**: The maximum number of entries the `Cache` can store before removing the least recently used (LRU) entries. Defaults to `-1` meaning the `Cache` can grow unbounded.
2. **Entry TTL**: The time to live for an entry since it was first added to the `Cache` before it will be removable from the `Cache`. Defaults to `Duration.INFINITE`.
3. **Entry TTI**: The time to idle for an entry since it was last accessed. Retrieving an entry from the `Cache` counts as accessing it.  Defaults to `Duration.INFINITE`.
> **Note**: Only one temporal policy can be active at a time. Either Entry TTL or Entry TTI.

A `CachePolicy` can be created using a `CachePolicy.builder()`.
```kotlin
val cachePolicy = CachePolicy.builder()
    .maxSize(100) // maximum 100 entries allowed in cache
    .entryTtl(24.hours) // entries become expired after 24 hours in the cache
    .build()
```

### Memory Cache Manager
When creating a `MemoryCacheManager` the following properties can be configured:

1. **Coroutine Scope**: The provided `CoroutineScope` is used to periodically update the `SnapshotPersistentCache` if one is provided. It is important this `scope` lives as long as the `Cache` is in use to make sure changes to the `cache` are persisted. Once the `scope` is cancelled a final save occurs.
2. **Coroutine Dispatcher**: The `CoroutineDispatcher` to use when saving to the persistent cache if in use. Defaults to `Dispatchers.IO`.
3. **Snapshot Persistent Cache**: An optional persistent storage to save the `cache` to. When set to `null` the `MemoryCacheManager` acts as an in-memory `cache` only that still enforces the `cachePolicy` set.
4. **Save Frequency**: How quickly a change to the `cache` will be saved to the persistent cache if in use. Defaults to 5 seconds.
5. **Cache Policy**: The cache policy the `MemoryCacheManager` should enforce. Defaults to a policy with `maxSize = 100` and `entryTtl = 24.hours`.
6. **Memory Level Notifier**: An optional argument that allows the cache to respond to low memory and critical memory notifications. When using the `Android` builder this defaults to an `AndroidMemoryLevelNotifier` that will remove expired entries from the `cache` when memory is low and will remove all entries from the `cache` when memory is critical according to `ComponentCallbacks2.onTrimMemory(level: Int)`.
7. **Telemeter**: The `telemeter` to use for logging internal events and errors. Defaults to null (no logging).

A `MemoryCacheManager` can be created using one of the builder factory functions available. Below is a minimal example using the builder factory function for Android:

```kotlin
val cache = MemoryCacheManagerBuilder.from(
    context, // application context
    snapshotPersistentCache = fileCache, // see Cache Configuration section for an example
)
    .coroutineScope(coroutineScope)
    .build()
```

When retrieving an entry from the `Cache` an expired entry will never be returned.
