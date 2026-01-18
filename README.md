# MC NDI 

[![](http://cf.way2muchnoise.eu/title/457015.svg)](https://www.curseforge.com/minecraft/mc-mods/mc-ndi)

Adding support for directly outputting Minecraft as NDI sources. 
Currently, outputs the main screen by default using MC - Username, you can also add more cameras that have their own output sources. 

## Supported Versions

| Minecraft | Mod Loader | Status |
|-----------|------------|--------|
| 1.21.8    | Fabric     | ✅ Current |
| 1.19.x    | Fabric/Forge | Legacy |
| 1.16.x    | Fabric/Forge | Legacy |

## Controls

| Key | Action |
|-----|--------|
| R | Place a camera at your current position |
| Y | Clear all cameras |
| Right-click on camera | Rename camera |

## Dependencies

- [NDI Runtime](https://ndi.tv) (NDI Tools or NDI SDK)
- [Fabric API](https://modrinth.com/mod/fabric-api) (for Fabric versions) 

## Building from Source

### Requirements
- Java 21 (Amazon Corretto 21 recommended)
- Gradle 8.12+

### Standard Build (Windows, Linux, Intel Mac)

For Windows, Linux, and Intel-based Macs, just build normally - the official Devolay library includes all necessary natives.

```bash
./gradlew clean :fabric:build
```

### Apple Silicon (M1/M2/M3) macOS - Custom Devolay Build Required

The official Devolay library does not include arm64 macOS natives. You need to build a custom version:

1. **Clone and build Devolay with arm64 support:**
   ```bash
   git clone https://github.com/WalkerKnapp/devolay.git
   cd devolay
   
   # Create NDI SDK headers directory
   mkdir -p ndi-sdk/include
   
   # Get NDI headers from obs-ndi (or NDI SDK if you have it)
   curl -o ndi-sdk/include/Processing.NDI.Lib.h https://raw.githubusercontent.com/obs-ndi/obs-ndi/master/headers/Processing.NDI.Lib.h
   curl -o ndi-sdk/include/Processing.NDI.structs.h https://raw.githubusercontent.com/obs-ndi/obs-ndi/master/headers/Processing.NDI.structs.h
   curl -o ndi-sdk/include/Processing.NDI.utilities.h https://raw.githubusercontent.com/obs-ndi/obs-ndi/master/headers/Processing.NDI.utilities.h
   ```

2. **Modify `devolay-natives/build.gradle.kts`** to add arm64 target:
   ```kotlin
   // Add in targetMachines:
   machines.macOS.architecture("arm64")
   ```

3. **Modify `devolay-java/src/main/java/me/walkerknapp/devolay/Devolay.java`** to detect arm64:
   ```java
   // In getArchDirectory() method, change arm64 detection for macOS:
   if (osArchProperty.contains("aarch64") || ...) {
       return isMacOS ? "arm64" : "arm64-v8a";  // macOS uses "arm64", Android uses "arm64-v8a"
   }
   ```

4. **Build and install to local Maven:**
   ```bash
   ./gradlew :devolay-natives:build :devolay-java:publishToMavenLocal
   ```

### Building MC-NDI

```bash
cd MC-NDI

# Set JAVA_HOME if needed
export JAVA_HOME=/path/to/java21

# Build Fabric version
./gradlew clean :fabric:build

# Output jar will be in fabric/build/libs/
```

### Notes
- The project uses `mavenLocal()` to find the custom Devolay build
- Forge support is currently disabled (1.21.8 uses NeoForge)
- Minecraft 1.21.8 requires Java 21

## Under the hood

Under the hood this uses the [Devolay library by WalkerKnapp](https://github.com/WalkerKnapp/devolay) which is licensed under Apache-2.0. 

## License

This is licensed under the [GNU GPLv3 license](https://github.com/Rushmead/Fabric-NDI/blob/master/LICENSE)

## Contributing

Pull Requests are welcome and encouraged! Found a bug and know how to fix it? PR It! Want a feature and know how to make it happen? PR It!

If you find a bug in the mod please open an issue on the Issues page with as much information as you can provide.

If you have any questions for me, join [my discord](https://rushmead.live/discord).
