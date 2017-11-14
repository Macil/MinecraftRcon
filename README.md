# Rcon

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
