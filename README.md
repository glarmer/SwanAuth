# Swansea_Authenticator
A unofficial Discord Authenticator bot for Swansea University servers to verify members. This bot is in no way affiliated with Swansea University.

This readme is entirely temporary, but below is a quick outline of how you can set the bot up in a server.

1.) Invite the bot (https://discord.com/api/oauth2/authorize?client_id=1022933651341709425&permissions=268437636&scope=bot%20applications.commands)

2.) You will need a verifications channel, an admin channel and a verified role. Optionally also an unverified role. Ensure that the automatic SwanAuth role is higher than these roles, and that the bot can see those two channels.

3.) Run /setup, entering in your desired options. There are 4 required options and 3 optional settings:
Required:
- verification_channel - the channel in which users are intended to verify;
- admin_channel - the channel where any logging from the bot goes, it is also where manual verification requests go;
- verified_role - the role which is applied to users when they verify;
- mode - this lets you select the way the bot operates, you can set it to use slash commands, buttons, or a singular pinned button.
Optional:
- unverified_role - this role gets applied when a user joins the server, having this set up will bypass default discord security measures;
- apply_unverified_role - if set to true this will slowly trawl through the member list applying the unverified role to users;
- verification_logging - this can be enabled to have more logging put into the admin_channel.

Once setup is completed the bot will notify users that they need to verify when they join and auto-apply the unverified role.

There is a /nonstudentverify command, this can be used for a user without a studentID to ask for verification. This is helpful in the case of alumni, to be students, or staff members.