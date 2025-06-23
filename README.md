[home]: https://pufferfish.host
[knowledgebase]: https://docs.pufferfish.host
[discord]: https://discord.gg/reZw4vQV9H
[downloads]: https://pufferfish.host/downloads
[optguide]: https://docs.pufferfish.host/optimization/pufferfish-server-optimization-guide/

# [Puffernot](https://github.com/pufferfish-gg/Pufferfish)
A highly optimized Paper fork designed for large servers requiring both maximum performance, stability, and "enterprise" features.

> [!WARNING]
> This is an unofficial port for [Pufferfish](https://github.com/pufferfish-gg/Pufferfish) to 1.21.6+ \
> All credits go to Pufferfish Host. \
> **Do not** report errors to Pufferfish Host while using this port.

> [!WARNING]
> The purpose of this project is to serve as a template for other Pufferfish forks in this organization. \
> Therefore we've made some aesthetic and dependency changes that fit our taste and needs. \
> If you want to run a Pufferfish jar closer to the original project, please refer to [Toffikk's port](https://github.com/Toffikk/Pufferfork).
> This is <u>our</u> interpretation of the Pufferfish software as a _modern implementation_ according to <u>our</u> personal style.

[Homepage][home] - [Downloads][downloads] - [Discord][discord] - [Knowledgebase][knowledgebase] - [Optimization Guide][optguide]

## Puffernot differences from Pufferfish
1. Updated Sentry dependency (and properly tested).
2. Updated [Flare](https://github.com/SerlithNetwork/Flare) dependency (and properly tested).
3. Removed usage of deprecated [Bungee ChatColor](https://github.com/pufferfish-gg/Pufferfish/blob/ver/1.21/patches/server/0003-Pufferfish-Config-and-Command.patch) in favor of [Adventure Components](https://github.com/SerlithNetwork/Puffernot/blob/ver/1.21.6/pufferfish-server/src/main/java/gg/pufferfish/pufferfish/PufferfishCommand.java#L40).
4. Migrated commands from [BukkitCommand](https://github.com/pufferfish-gg/Pufferfish/blob/ver/1.21/patches/server/0003-Pufferfish-Config-and-Command.patch) to [Brigadier](https://github.com/SerlithNetwork/Puffernot/blob/ver/1.21.6/pufferfish-server/src/main/java/gg/pufferfish/pufferfish/PufferfishCommand.java).
5. Refactor [FlareCommand](https://github.com/SerlithNetwork/Puffernot/blob/ver/1.21.6/pufferfish-server/src/main/java/gg/pufferfish/pufferfish/flare/FlareCommand.java) to use spark-like syntax in both the command completion and feedback.
6. Added missing entities with a brain to [DAB](https://github.com/SerlithNetwork/Puffernot/blob/ver/1.21.6/pufferfish-server/minecraft-patches/features/0010-Pufferfish-Dynamic-Activation-Of-Brain.patch).
7. Fixed some unused values in [PufferfishSentryAppender](https://github.com/SerlithNetwork/Puffernot/blob/ver/1.21.6/pufferfish-server/src/main/java/gg/pufferfish/pufferfish/sentry/PufferfishSentryAppender.java#L59).
8. Toffikk's reworked [ServerConfigurations](https://github.com/SerlithNetwork/Puffernot/blob/ver/1.21.6/pufferfish-server/src/main/java/gg/pufferfish/pufferfish/compat/ServerConfigurations.java) to support paper configs and redact confidential values in Flare.
9. Customized [spark](https://github.com/SerlithNetwork/Puffernot/blob/ver/1.21.6/pufferfish-server/build.gradle.kts.patch#L70) build.
10. GitHub Workflows auto-release and version fetcher, the exact code in the repository is what will be delivered.
11. Added two additional QoL patches:
    1. [Custom plugin UI](https://github.com/SerlithNetwork/Puffernot/blob/ver/1.21.6/pufferfish-server/paper-patches/files/src/main/java/io/papermc/paper/command/PaperPluginsCommand.java.patch).
    2. [Spark tps and ping aliases added by default](https://github.com/SerlithNetwork/Puffernot/blob/ver/1.21.6/pufferfish-server/paper-patches/files/src/main/resources/configurations/commands.yml.patch).

## Pufferfish Features

- **Sentry Integration** Easily track all errors coming from your server in excruciating detail
- **Better Entity Performance** Reduces the performance impact of entities by skipping useless work and making barely-noticeable changes to behavior
- **Partial Asynchronous Processing** Partially offloads some heavy work to other threads where possible without sacrificing stability
- **8x Faster Map Rendering** Reduces or eliminates lag spikes caused by plugins like ImageOnMap or ImageMaps
- **30% faster hoppers** over Paper (Airplane)
- **Reduced GC times & frequency** from removing useless allocations, which also improves CPU performance (Airplane)
- **Fast raytracing** which improves performance of any entity which utilizes line of sight, mainly Villagers (Airplane)
- **Built-in profiler** which has 0 performance hit and easy to read metrics for both server owners and developers (Airplane)
- Faster crafting, reduction in uselessly loaded chunks, faster entity ticking, faster block ticking, faster bat spawning, and more!
- Complete compatibility with any plugin compatible with Paper
- And more coming soon...

## Downloads
You can download the latest JAR file [here][downloads].

## Pufferfish Host

Are you looking for a server hosting provider to take your server's performance to the next level? Check out [Pufferfish Host][home]! We run only the best hardware so you can be sure that your server's hardware isn't bogging you down.
This fork is developed by [Pufferfish Host][home], and we can provide additional tailored performance support to customers.

## Building

```bash
./gradlew applyAllPatches
./gradlew createMojmapPaperclipJar
```

## License
Patches are licensed under GPL-3.0.
All other files are licensed under MIT.

## Additional Credits
1. PurpurMC for their paperweight setup
2. Toffikk for [ServerConfigurations](https://github.com/Toffikk/Pufferfork/blob/ver/1.21.6/pufferfork-server/src/main/java/gg/pufferfish/pufferfish/compat/ServerConfigurations.java) and [0014-Pufferfish-Better-Check-For-Useless-Packets.patch](https://github.com/Toffikk/Pufferfork/blob/ver/1.21.6/pufferfork-server/minecraft-patches/sources/net/minecraft/server/level/ServerEntity.java.patch)
3. Winds-Studio for their [auto-release script](https://github.com/Winds-Studio/Leaf/blob/ver/1.21.5/scripts/prepareRelease.sh)
