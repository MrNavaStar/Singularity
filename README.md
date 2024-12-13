![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-green.svg)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](http://makeapullrequest.com)
![In Beta](https://img.shields.io/badge/InBeta-red.svg)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/G2G4DZF4D)

<img src="https://raw.githubusercontent.com/MrNavaStar/Singularity/master/loader-common/src/main/resources/assets/singularity/icon.png" width="300" height="300">


# Singularity
The easiest mod to sync server data across your velocity network!

Singularity is unique when it comes to the world of network sync mods/plugins. While tools like [InvSync](https://github.com/MrNavaStar/InvSync)
or [HuskSync](https://github.com/WiIIiam278/HuskSync) require an external database to work, Singularity does not. All data transfers are done
over the network, with velocity acting as a data broker. This allows for extremely rapid synchronization and dead simple setup/configuration.

To get started, simply install singularity on your velocity proxy and install the appropriate version on your backend server (fabric, forge, paper).

Get the latest dev build from [here!](https://github.com/MrNavaStar/Singularity/actions)

# Features
- [x] Player data sync
- [x] Player statistics sync
- [ ] Player advancements sync (in progress)
- [ ] Operator level sync (in progress)
- [ ] Whitelist sync (in progress)
- [ ] Ban sync (in progress)
- [ ] Network wide user cache (in progress)
- [X] Mod API
- [X] Config
- [ ] External database support

# Config
To configure singularity, create a singularity.yml file under `plugins/singularity` on your velocity instance. 

Example config:
```yml
# Define your sync groups. Data will only be synced between servers in the same group
# Server names are as defined in your velocity.toml
groups:
  smp: |
    serv-1
    serv-2
    serv-3
  lobby: |
    lob-1
    lob-2
    
# Define your Group rules. These are what control what data is synced per server. Mods can add custom rules.
# Any settings that are not defined here will use their default values.
smp:
  singularity.stats: true
  singularity.advancements: true
  
lobby:
  singularity.location: true # This setting is false by default
  singularity.spawn: true    # This setting is false by default
```
Currently, these are all the available settings:

| Setting               | Default Value | Info                         |
|-----------------------|---------------|------------------------------|
| singularity.ender     | true          | player ender chest           |
| singularity.food      | true          | player hunger                |
| singularity.gamemode  | true          | player gamemode              |
| singularity.health    | true          | player health points         |
| singularity.inventory | true          | items in player inventory    |
| singularity.location  | false         | player position in the world |
| singularity.score     | true          | player score                 |
| singularity.spawn     | false         | player spawn point           |                      
| singularity.xp        | true          | player experience level      |