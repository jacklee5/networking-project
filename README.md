# Better Basic Chat App
Chat Client built on Java for school project
## Features
### Custom usernames
On started the client, after specifying the connection information, the user will be asked for a username. This username must be alphanumeric, contain no spaces, and not already be in use. If these conditions are met, the server will ask for another username
### Welcome messages
After a user specifies a username and joins the room, they will be greated with "[name] has joined"
### Chat
When the user types something in chat, it will be sent to all the other users in the room
### Quit command
#### Console Client
The user can quit the room by typing /quit and all other users will be notified with "[name] has left". 
#### GUI Client
The user can quit the room by closing the window and all other users will be notified with "[name] has left]".
### Message command
The user can message a specific user by typing /pchat [usernames] [message]
You can type a list of usernames separated by spaces. Each username should have the @ symbol before it
### Nuke command
#### Console Client
The user can nuke a phrase by typing /nuke [nukephrase]
This kicks all users with messages that match the regex [nukephrase] in the last 10 minutes
#### GUI Client
The user can nuke a phrase by clicking the nuke button and selecting a non-empty phrase.

### HEADER_CLIENT_SEND_NAME
[name]
### HEADER_CLIENT_SEND_MESSAGE
[message]
### HEADER_CLIENT_SEND_PM
[recipients] [message]
### HEADER_CLIENT_SEND_LOGOUT
no arguments!
### HEADER_CLIENT_SEND_NUKE
[nukephrase]
### HEADER_SERVER_REQ_NAME
no arguments!
### HEADER_SERVER_SEND_USERS
[list]
### HEADER_SERVER_SEND_MESSAGE
[username] [message]
### HEADER_SERVER_SEND_WELCOME
[username]
### HEADER_SERVER_SEND_PM
[username] [message]
### HEADER_SERVER_SEND_LEAVE
[username]
### HEADER_SERVER_SEND_ONLINE;
[usernames]
