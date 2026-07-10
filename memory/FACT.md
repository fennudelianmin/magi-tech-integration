# DoraPack Mod Project (哆啦A梦百宝袋模组)

## Project
- Path: /d/ideaProJect/magi-tech-integration (Windows, git-bash)
- Minecraft 1.12.2 Forge mod built from CleanroomMC TemplateDevEnv
- Building full mod from whitepaper: C:\Users\Administrator\Downloads\哆啦A梦百宝袋模组 · 完整产品白皮书.md

## Decisions (user-confirmed)
- Scope: FULL implementation of whitepaper
- IC2: hard dependency (curse.maven:industrial-craft-242638:3838713), resolves OK
- Metadata: mod_id=dorapack, root_package=com.dorapack, name="Doraemon's Pocket"
- Audio: skip audio assets, keep logic
- Models/textures: reuse vanilla/existing placeholders

## Build environment (CRITICAL)
- Gradle 9.2.1 wrapper. MUST run with `export JAVA_HOME=~/.jdks/temurin-25.0.3` (Java 25). Default PATH java is 8, which cannot launch Gradle 9.
- Compile toolchain (Azul Java 8) already cached in ~/.gradle/jdks
- Build command: `export JAVA_HOME=~/.jdks/temurin-25.0.3 && ./gradlew.bat compileJava --console=plain`
- Tests run on Java 8 toolchain, JUnit 5 (enable_junit_testing=true). show_testing_output configurable.
- Main package: com.dorapack.dorapack (Tags class = com.dorapack.dorapack.Tags injected at build)

## API notes (1.12.2)
- WorldSavedData is in net.minecraft.world.storage (NOT net.minecraft.world)
- CreativeTabs.getTabIconItem() is NOT overridable in this mapping; only createIcon()
- net.minecraft.client.Minecraft must be isolated in @SideOnly(CLIENT) nested class

## Progress
- DONE: foundation compiles clean (proxies, config/DoraConfig=balance.cfg, capability IDoraPlayerData/DoraPlayerData/storage/provider, research manager+keys+tiers, achievement keys+manager, bag WorldSavedData+GUI+container, anywhere door block+tile, no-unlock items: bag, copter, air cannon, tame food, headlamp; DoraItems/DoraBlocks/RegistryHandler)
- TODO: research tables+GUI, event handlers for points/achievements/flight/headlamp, tier1(10)/tier2(4)/tier3 robot+IC2, multiblocks, creative items, resources(lang/models/recipes), P3C review, unit tests
