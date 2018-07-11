# Rcon

[![Circle CI](https://circleci.com/gh/AgentME/MinecraftRcon.svg?style=shield)](https://circleci.com/gh/AgentME/MinecraftRcon)

This is a [Spigot](https://www.spigotmc.org/) plugin for Minecraft servers
that allows console commands to be received over HTTP.

This is an alternative to Minecraft's native rcon functionality, which has
unresolved issues such as the following:
* https://bugs.mojang.com/browse/MC-72390
* https://bugs.mojang.com/browse/MC-87863

This plugin does not do any authentication. This plugin is intended to listen
on localhost or another restricted address for use by scripts running on the
same server. By default, the plugin listens to port 25576 on localhost. (This
is configurable in the plugin's `config.yml`.)

This plugin does not implement the native rcon protocol. It expects a client
to make a POST request to the path "/command", and to include a POST body
parameter named "command" containing the Minecraft console command to run.

After the command is executed, all of the server's logs for a game tick are
mirrored to the response so that the client gets the output from the command.
Other output from the logs may be captured in this too.

Commands can be sent by using curl in a terminal like this:

    $ curl http://localhost:25576/command -d 'command=list'
    There are 1/20 players online:
    DaringMacil

## Installing

Download the latest Rcon jar file from the
[project's Releases page](https://github.com/AgentME/MinecraftRcon/releases).
Place the jar file in the plugins/ directory. After the first run, the file
plugins/Rcon/config.yml file will be generated with default values.

This plugin records some usage metrics to
https://bstats.org/plugin/bukkit/Rcon. You can opt out of this by
placing `enabled: false` in `plugins/bStats/config.yml`.
