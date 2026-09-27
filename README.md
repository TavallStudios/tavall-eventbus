# tavall-eventbus

A Java event bus with listener registration, event domains, middleware, and event metadata.

The repository packages Tavall's current event bus implementation as a single Java library module.

## Why tavall-eventbus

- Registers and removes listener objects.
- Posts events through domain-aware and convenience entry points.
- Supports middleware hooks and event capability/status metadata.

## Features

- Listener registration and unregistration
- Event posting and convenience fire methods
- Middleware registration
- Event-domain and capability types

## Quick Start

Add the published artifact to a Gradle project:

```kotlin
dependencies {
    implementation("org.tavall:tavall-eventbus:<version>")
}
```

Use the exact published version and repository access configured for your project. See the links below for API and contribution details.

## Project Structure

This repository is a single Java library module (Module Type: LIBRARY; Runtime: None).

## Documentation

| Document | Purpose |
| --- | --- |
| [Contributing](CONTRIBUTING.md) | Contribution and development notes. |
| [Repository Git Workflow](docs/quality/GIT_WORKFLOW.md) | Applicable repository guidance. |

## Requirements / Compatibility

Java 25.

## Building From Source

```bash
./gradlew check
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

No tracked license file is present in the current repository tree.

## Documentation Update State

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | PRIMARY | TavallStudios/tavall-eventbus/README.md | 2026-09-27 12:29 PM PDT | https://github.com/TavallStudios/tavall-eventbus/pull/10. |
| Notion | NOT_APPLICABLE | — | 2026-09-27 12:29 PM PDT | README files are not synchronized as Notion twins. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:29 PM PDT | GitHub | UPDATED | TavallStudios/tavall-eventbus/README.md | Same path | https://github.com/TavallStudios/tavall-eventbus/pull/10. | Reworked the public README to describe the current project, module boundary, usage, and documentation. |

</details>
