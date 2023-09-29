package cn.lanink.lightlogin.data.provider;

import cn.lanink.lightlogin.LightLogin;
import cn.lanink.lightlogin.data.PlayerData;
import cn.nukkit.utils.Config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author LT_Name
 */
public class YamlProvider implements Provider {

    private Config uuidMappingConfig;

    private final ConcurrentHashMap<String, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> uuidMappingCache = new ConcurrentHashMap<>();

    public YamlProvider() {
        this.uuidMappingConfig = new Config(LightLogin.getInstance().getDataFolder() + "/uuidMapping.yml", Config.YAML);
        HashMap<String, String> map = this.uuidMappingConfig.get("mapping", new HashMap<>());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            try {
                this.uuidMappingCache.put(entry.getKey(), UUID.fromString(entry.getValue()));
            } catch (Exception ignored) {

            }
        }
    }

    @Override
    public UUID getUUID(String playerName) {
        return this.uuidMappingCache.get(playerName);
    }

    @Override
    public void setUUID(String playerName, UUID uuid) {
        this.uuidMappingCache.put(playerName, uuid);
    }

    @Override
    public PlayerData loadData(String playerName) {
        if (!this.playerDataMap.containsKey(playerName)) {
            PlayerData playerData = new PlayerData(playerName);
            Config config = this.getPlayerConfig(playerName);

            playerData.setPassword(config.getString("password"));
            playerData.setEncryptEmail(config.getString("mail"));
            playerData.setEncryptPhone(config.getString("phone"));
            playerData.setClientIdentification(config.getString("clientIdentification"));
            playerData.setLastLoginQuitTime(config.getLong("lastLoginQuitTime"));
            playerData.setLoginLockTime(config.getLong("loginLockTime"));
            playerData.setLastSendEmailCodeTime(config.getLong("lastSendEmailCodeTime"));
            playerData.setLastSendSmsCodeTime(config.getLong("lastSendSmsCodeTime"));
            playerData.setLoginErrorCount(config.getInt("loginErrorCount"));

            this.playerDataMap.put(playerName, playerData);
        }
        return this.playerDataMap.get(playerName);
    }

    @Override
    public void saveData(String playerName) {
        PlayerData playerData = this.playerDataMap.get(playerName);
        if (playerData != null) {
            this.saveData(playerData);
        }
    }

    @Override
    public void saveData(PlayerData playerData) {
        Config config = this.getPlayerConfig(playerData.getName());

        config.set("password", playerData.getPassword());
        config.set("mail", playerData.getEncryptEmail());
        config.set("phone", playerData.getEncryptPhone());
        config.set("clientIdentification", playerData.getClientIdentification());
        config.set("lastLoginQuitTime", playerData.getLastLoginQuitTime());
        config.set("loginLockTime", playerData.getLoginLockTime());
        config.set("lastSendEmailCodeTime", playerData.getLastSendEmailCodeTime());
        config.set("lastSendSmsCodeTime", playerData.getLastSendSmsCodeTime());
        config.set("loginErrorCount", playerData.getLoginErrorCount());

        config.save();
    }

    @Override
    public void saveAllCache() {
        for (PlayerData data : this.playerDataMap.values()) {
            this.saveData(data);
        }

        HashMap<String, String> map = new HashMap<>();
        for (Map.Entry<String, UUID> entry : this.uuidMappingCache.entrySet()) {
            try {
                map.put(entry.getKey(), entry.getValue().toString());
            } catch (Exception ignored) {

            }
        }
        this.uuidMappingConfig.set("mapping", map);
    }

    @Override
    public void clearCache(String playerName) {
        this.playerDataMap.remove(playerName);
    }

    @Override
    public void clearAllCache() {
        this.playerDataMap.clear();
    }

    @Override
    public void close() {
        this.saveAllCache();
        this.clearAllCache();
    }

    private Config getPlayerConfig(String playerName) {
        String fileString = LightLogin.getInstance().getPlayerDataPath() + playerName.substring(0,1).toLowerCase() + "/" + playerName + ".yml";
        return new Config(fileString, Config.YAML);
    }
}
