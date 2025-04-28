# Clogged.Me
Sync your collection log with Clogged.me and allow other players to view your clogs.

This was created as a replacement for collectionlog.net (RIP).

# Configuration
![image](https://github.com/user-attachments/assets/75c72411-5c42-4a02-9054-4c39487b6170)
- **Enable sync with Clogged.me**
  - This will enable the synchronization of your ingame collection log with the Clogged.me servers to allow other players to view your log.

- **Enable lookups with Clogged.me**
  - This will allow you to view collection logs from other players.
  - This does not sync your log with Clogged.me and will not allow other players to view your log.

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
  
