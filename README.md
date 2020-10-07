# Better Basic Chat App
## Features
### Custom usernames
On started the client, after specifying the connection information, the user will be asked for a username. This username must be alphanumeric, contain no spaces, and not already be in use. If these conditions are met, the server will ask for another username
### Welcome messages
After a user specifies a username and joins the room, they will be greated with "[name] has joined"
### Chat
When the user types something in chat, it will be sent to all the other users in the room
### Quit command
The user can quit the room by typing /quit and all other users will be notified with "[name] has left"
### Message command
The user can message a specific user by typing /pchat [username] [message]
### Nuke command
The user can nuke a phrase by typing /nuke [nukephrase]
This kicks all users with messages that match the regex [nukephrase] in the last 10 minutes