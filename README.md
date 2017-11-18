# Rcon

[![Circle CI](https://circleci.com/gh/AgentME/bukkit-rcon.svg?style=shield)](https://circleci.com/gh/AgentME/bukkit-rcon)

This is a Bukkit plugin for Minecraft servers that allows console commands to
be read from a TCP port.

This is an alternative to Minecraft's native rcon functionality, which has
unresolved issues such as the following:
* https://bugs.mojang.com/browse/MC-72390
* https://bugs.mojang.com/browse/MC-87863

This plugin does not do any authentication. This plugin is intended to listen
on localhost or another restricted address for use by scripts running on the
same server. By default, the plugin listens to port 25576 on localhost. (This
is configurable in the plugin's `config.yml`.)

This plugin does not implement the native rcon protocol. It expects a client
to open a TCP connection and send "Minecraft-Rcon\n" to start the session. Then
the client may send console commands followed by a newline ("\n"). Any input
from the client that does not end in a newline will be ignored so that no
malformed commands are executed if the TCP connection is killed early. While
the session is open, all of the server's logs are mirrored to the client.

Commands can be sent by using netcat in a terminal like this:

    $ echo -en 'Minecraft-Rcon\nlist\n' | nc localhost 25576
    [Rcon] rcon(/127.0.0.1:37854): list
    There are 1/20 players online:
    DaringMacil

## Installing

Download the latest Rcon jar file from the
[project's Releases page](https://github.com/AgentME/bukkit-rcon/releases).
Place the jar file in the plugins/ directory. After the first run, the file
plugins/Rcon/config.yml file will be generated with default values.
