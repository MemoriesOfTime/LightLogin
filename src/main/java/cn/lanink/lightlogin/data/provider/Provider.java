package cn.lanink.lightlogin.data.provider;

import cn.lanink.lightlogin.data.PlayerData;

import java.util.UUID;

/**
 * @author LT_Name
 */
public interface Provider {

    PlayerData loadData(String playerName);

    UUID getUUID(String playerName);

    void setUUID(String playerName, UUID uuid);

    void saveData(String playerName);

    void saveData(PlayerData playerData);

    void saveAllCache();

    void clearCache(String playerName);

    void clearAllCache();

    void close();

}
