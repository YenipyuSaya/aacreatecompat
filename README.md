# Actually Additions: Create Compat

NeoForge compatibility addon between Actually Additions and Create.

| | |
|---|---|
| Mod ID | `aacreatecompat` |
| Minecraft / NeoForge | 1.21.1 / 21.1.249 |
| Version | 1.0.0 |
| Depends on | Actually Additions `1.3.26+mc1.21.1`, Create `6.0.10-281` (and Create-related Maven deps in `gradle.properties`) |

## Build

Requires JDK 21.

```bash
./gradlew build
```

Jar output: `build/libs/aacreatecompat-1.0.0.jar`

## License

- **Original addon code** in this repository: [MIT](LICENSE) © 2026 YenipyuSaya.
- This repo does **not** ship Create or Actually Additions artwork. Runtime deps are pulled from Maven under their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
- **NeoForged MDK template files**: see [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).
