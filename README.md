# [Clogged.me](https://Clogged.me)
Sync your collection log with [Clogged.me](https://Clogged.me) and allow other players to view your clogs.

This was created as a replacement for collectionlog.net (RIP).

# How to Use
1. Install the "Clogged.me" plugin from the RuneLite plugin hub.
2. Open the plugin options and configure according to the settings denoted below in the **'Configuration'** section.
3. Open your collection log interface in-game.
   1. If you set the "Sync method" to "Manual", you must type `!clog sync` in the chatbox to sync your collection log.
   2. Otherwise, the plugin will automatically sync your collection log whenever the collection log interface is open.
   3. Regardless of the method selected, you'll receive a notification in the chatbox when your collection log is synced.
4. You can now view your collection log (and share it with others) by using any of the commands below (in the **Available Commands** section). This works in public chat, private messages, clan chat, etc.
  
# Available Commands
   - `!clog sync`: Syncs your collection log if "sync method" is set to manual. The collection log interface must be open for this to work.
   - `!clog <clogName>`: Look up your collection log for a given clog.
      - For example: `!clog barrows`, `!clog slayer`, or `!clog toa`
   - `!clog missing <clogName>`: Shows all items _not_ obtained for a given clog.
      - For example: `!clog missing barrows` will show you all the items in the Barrows collection log you have not obtained.
   - `!clog "username" <clogName>` or `!clog <clogName> "username"`: Look up another player's collection log.
      - **Note: The player must have a public profile to view their logs.**
      - For example: `!clog "zezima" moons` will look up Zezima's Moons of Peril collection log if they have a public profile. The username must surrounded by double quotes.
   - `!clog join <groupName>`: Join the provided group. Groups can be created/managed on the [Clogged.me](https://Clogged.me) website.
      - Your ability to join is determined by the group admins. If the group is set to "Public", you will join automatically. If it's set to "Closed", you will not be able to join. Otherwise, you have to be approved by a group admin to join.
      - For example: `!clog join LostAllLives` will attempt to join the "LostAllLives" group. The group admin, `Mr. Mammal`, will have to approve your request to join if it's not set to public.
   - `!clog leave <groupName>`: Leaves the provided group.
      - For example: `!clog leave LostAllLives` will atempt to leave the "LostAllLives" group.
      
# Configuration
![image](https://github.com/user-attachments/assets/8c664108-c5b6-4ae4-b711-91e422fe847a)

## General Settings

- **Enable sync with Clogged.me**
  - This will enable the synchronization of your in-game collection log with the Clogged.me servers to allow other players to see your clog when you send the `!clog <clogName>` command.

- **Enable lookups with Clogged.me**
  - This will allow you to view collection logs from other players.
  - This does not sync your log with Clogged.me and will not allow other players to view your log.
 
- **Make clog public on Clogged.me**
  - This will allow your collection log to be viewed by users on the [Clogged.me](https://Clogged.me) website.
  - This will also allow other players in-game to view your collection log with the `!clog "username" <clogName>` command.
  - After changing this, you must sync your collection log for changes to take effect.
  - **Note: This is not required if you just want to share your own collection log. This is only required to be visible on the Clogged.me website and viewable by other players in-game with the `!clog "your_username" <clogName>` command.**
 
- **Sync Method**
    - **Manual**: Must type '!clog sync' with collection log interface open.
    - **Automatic**: Syncs whenever the collection log interface is open (might cause strange behavior for a split second when opening).
    - The collection log interface must be open to sync (you do not have to visit each page however).
 
## Display Settings

 - **Display Method**
    - **Text**: Collection log items will be displayed as text in the chatbox.
    - **Icons**: Collection log items will be displayed as icons.
  
- **Show item quantities**
  - This will display how many of each item has been obtained in the collection log.
  - ![image](https://github.com/user-attachments/assets/051caf8e-27f7-4212-9009-aee21425e1ac)

 
- **Show clog totals**
  - This will display the number of items obtained out of the total possible number of items for a given collection log.
  - ![image](https://github.com/user-attachments/assets/014f4e52-0f9c-4b89-a95f-845124d7b956)

- **Show missing items**
  - This will display the items that have *not* been obtained for a given collection log.
  - You can invoke this using the "missing" command modifier. E.g. `!clog gauntlet` vs `!clog missing gauntlet`
  - ![image](https://github.com/user-attachments/assets/fa2de8b6-5fa3-4a22-bb24-2d4b399071f3)
 
- **Enable custom chat message color**
  - If enabled, all messages sent by the plugin will be of this color.
  - ![image](https://github.com/user-attachments/assets/dda7f21c-0b30-4029-9a74-6247c12ad4f2)
  - ![image](https://github.com/user-attachments/assets/cab0ae7c-ac48-45a9-bff5-7d03765cc2bd)
 
## Proxy Settings
 - Clogged.me does not store nor associate your IP address with your account or client in any way.
 - With that being said, enabling this will use the specified proxy settings to connect to the Clogged.me API.
 - Only enable these settings if you know what you're doing.
 
# Data Handling and Privacy

We understand concerns about IP addresses. Our plugin does not collect or store your IP address. With full transparency, due to the way the internet works, your IP will be exposed to the clogged.me server when storing or retrieving data.

For users who prefer to mask their IP address, proxy settings are available in the plugin. To reiterate, we do not store or use your IP address in any sort of fashion but do want to call this out.

## What's stored
- Username
- Account hash (unique identifier provided by Jagex that does not change when your username changes)
- Item IDs corresponding to items you've obtained from your collection log.
  
