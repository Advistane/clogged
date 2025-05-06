# Clogged.Me
Sync your collection log with Clogged.me and allow other players to view your clogs.

This was created as a replacement for collectionlog.net (RIP).

# How to Use
1. Install the "Clogged.me" plugin from the RuneLite plugin hub.
2. Open the plugin options and configure according to the settings denoted below in the **'Configuration'** section.
3. Open your collection log interface in-game.
   1. If you set the "Sync method" to "Manual", you must type `!clog sync` in the chatbox to sync your collection log.
   2. Otherwise, the plugin will automatically sync your collection log whenever the collection log interface is open.
   3. Regardless of the method selected, you'll receive a notification in the chatbox when your collection log is synced.
4. You can now view your collection log (and share it with others) by typing '!clog {collectionLogName}'. This works in public chat, private messages, clan chat, etc.
   1. For example, typing `!clog barrows` will show your collection log for barrows.
   2. Or typing `!clog rumors` will show your collection log for hunter rumors.
   3. Other examples
      1. `!clog gotr` - Display Gardians of the Rift collection log.
      2. `!clog skotizo` - Display Skotizo collection log.
      3. `!clog toa` - Display Tombs of Amascut collection log.
   4. Any entry in the collection log can be used and shared.
5. You can view the items not yet collected for a given clog by using the "missing" modifier (if "Show missing items" is enabled in the plugin config).
   1. For example, typing `!clog missing gauntlet` will show all the items you have *not* collected for the Gauntlet.
      
# Configuration
![image](https://github.com/user-attachments/assets/67db4973-4b90-4a11-982b-75be2262a950)


- **Enable sync with Clogged.me**
  - This will enable the synchronization of your ingame collection log with the Clogged.me servers to allow other players to view your log.

- **Enable lookups with Clogged.me**
  - This will allow you to view collection logs from other players.
  - This does not sync your log with Clogged.me and will not allow other players to view your log.
 
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

- **Sync Method**
    - **Manual**: Must type '!clog sync' with collection log interface open.
    - **Automatic**: Syncs whenever the collection log interface is open (might cause strange behavior for a split second when opening).
    - The collection log interface must be open to sync (you do not have to visit each page however).

- **Display Method**
    - **Text**: Collection log items will be displayed as text in the chatbox.
    - **Icons**: Collection log items will be displayed as icons.

- **Proxy Settings**
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
  
