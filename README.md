# Gradle Cache Action

[![CI Status](https://github.com/burrunan/gradle-cache-action/workflows/CI/badge.svg)](https://github.com/burrunan/gradle-cache-action/actions)

This ia GitHub Action for caching Gradle caches.
In other words, this is [@actions/cache](https://github.com/actions/cache) customized for Gradle.

Key improvements over `@actions/cache`
- Simplified configuration 
- Less space usage (there's overall 5GiB limit, so cache space matters)
- Native support for caching Gradle's local build cache

## Configuration

1. You might want to enable [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
For instance, add `--build-cache` option whe 

1. Add the following to `.github/workflows/...`

```yaml
- uses: burrunan/gradle-cache-action@release
  name: Cache .gradle
  with:
    # If you have multiple jobs, use distinct job-id in in case you want to split caches
    # For instance, jobs with different JDK versions can't share caches
    # RUNNER_OS is added to job-id automatically
    job-id: jdk8
```

## Configuration

The default configuration should suit for most of the cases, however, there are extra knobs.

```yaml
- uses: burrunan/gradle-cache-action@release
  name: Cache .gradle
  with:
    # If you have multiple jobs, use distinct job-id in in case you want to split caches
    # For instance, jobs with different JDK versions can't share caches
    # RUNNER_OS is added to job-id automatically
    job-id: jdk8

    # Disable caching of $HOME/.gradle/caches/*.*/generated-gradle-jars
    save-generated-gradle-jars: false

    # Disable caching of ~/.gradle/caches/build-cache-*
    save-local-build-cache: false
```

## Contributing

Contributions are always welcome! If you'd like to contribute (and we hope you do) please open a pull request.

## License

Apache 2.0

## Author

Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
