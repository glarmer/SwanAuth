# Swansea_Authenticator
A unofficial Discord Authenticator bot for Swansea University servers to verify members. This bot is in no way affiliated with Swansea University.

This readme is entirely temporary, but below is a quick outline of how you can set the bot up in a server.

1.) Invite the bot (https://discord.com/api/oauth2/authorize?client_id=1022933651341709425&permissions=268437636&scope=bot%20applications.commands)

2.) You will need a verifications channel, an admin channel, a unverified role and a verified role. Ensure that the automatic SwanAuth role is higher than these roles, and that the bot can see those two channels.

3.) Run /setup, entering in your desired options. The final option can be used to apply the unverified role to everyone who doesn't have either the verified role or the unverified role already.

Once setup is completed the bot will notify users that they need to verify when they join and auto-apply the unverified role.

There is a /nonstudentverify command, this can be used for a user without a studentID to ask for verification. This is helpful in the case of alumni, to be students, or staff members.