package cn.lanink.lightlogin.data.provider;

import cn.lanink.lightlogin.LightLogin;
import cn.lanink.lightlogin.PluginConfig;
import cn.lanink.lightlogin.data.PlayerData;
import com.smallaswater.easysql.exceptions.MySqlLoginException;
import com.smallaswater.easysql.mysql.data.SqlData;
import com.smallaswater.easysql.mysql.data.SqlDataList;
import com.smallaswater.easysql.mysql.utils.DataType;
import com.smallaswater.easysql.mysql.utils.TableType;
import com.smallaswater.easysql.mysql.utils.UserData;
import com.smallaswater.easysql.v3.mysql.manager.SqlManager;
import com.smallaswater.easysql.v3.mysql.utils.SelectType;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author LT_Name
 */
public class MysqlProvider implements Provider {

    private final PluginConfig.MySqlConfig mySqlConfig;
    private final SqlManager sqlManager;

    private static final String TABLE_NAME = "PlayerData";
    private static final String UUID_MAPPING_TABLE_NAME = "uuidMapping";

    private final ConcurrentHashMap<String, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> uuidMappingCache = new ConcurrentHashMap<>();

    public MysqlProvider(PluginConfig.MySqlConfig mySqlConfig) throws MySqlLoginException {
        this.mySqlConfig = mySqlConfig;
        this.sqlManager = new SqlManager(LightLogin.getInstance(), new UserData(
                mySqlConfig.getUser(),
                mySqlConfig.getPassword(),
                mySqlConfig.getHost(),
                mySqlConfig.getPort(),
                mySqlConfig.getDatabase()
        ));

        if (!this.sqlManager.isEnable()) {
            throw new MySqlLoginException("Mysql login failed");
        }

        if (!this.sqlManager.isExistTable(TABLE_NAME)) {
            this.sqlManager.createTable(TABLE_NAME,
                    new TableType("id", DataType.getID()),
                    new TableType("name", DataType.getVARCHAR()),
                    new TableType("password", DataType.getVARCHAR()),
                    new TableType("mail", DataType.getTEXT()),
                    new TableType("phone", DataType.getTEXT()),
                    new TableType("clientIdentification", DataType.getVARCHAR()),
                    new TableType("lastLoginQuitTime", DataType.getBIGINT()),
                    new TableType("loginLockTime", DataType.getBIGINT()),
                    new TableType("lastSendEmailCodeTime", DataType.getBIGINT()),
                    new TableType("lastSendSmsCodeTime", DataType.getBIGINT()),
                    new TableType("loginErrorCount", DataType.getINT())
            );
        }

        if (!this.sqlManager.isExistTable(UUID_MAPPING_TABLE_NAME)) {
            this.sqlManager.createTable(UUID_MAPPING_TABLE_NAME,
                    new TableType("name", DataType.getVARCHAR()),
                    new TableType("uuid", DataType.getUUID()));
        }
    }

    @Override
    public UUID getUUID(String playerName) {
        if (!this.uuidMappingCache.containsKey(playerName)) {
            SqlDataList<SqlData> data = this.sqlManager.getData(UUID_MAPPING_TABLE_NAME, new SelectType("name", playerName));
            if (!data.isEmpty()) {
                SqlData sqlData = data.get(0);
                this.uuidMappingCache.put(sqlData.getString("name"), UUID.fromString(sqlData.getString("uuid")));
            }
        }
        return this.uuidMappingCache.get(playerName);
    }

    @Override
    public void setUUID(String playerName, UUID uuid) {
        this.uuidMappingCache.put(playerName, uuid);
        SqlDataList<SqlData> data = this.sqlManager.getData(UUID_MAPPING_TABLE_NAME, new SelectType("name", playerName));
        SqlData sqlData = new SqlData();
        sqlData.put("name", playerName);
        sqlData.put("uuid", uuid.toString());
        if (data.isEmpty()) {
            this.sqlManager.insertData(UUID_MAPPING_TABLE_NAME, sqlData);
        } else {
            this.sqlManager.setData(UUID_MAPPING_TABLE_NAME, sqlData, new SqlData("name", playerName));
        }
    }

    @Override
    public PlayerData loadData(String playerName) {
        if (!this.playerDataMap.containsKey(playerName)) {
            SqlDataList<SqlData> data = this.sqlManager.getData(TABLE_NAME, new SelectType("name", playerName));
            PlayerData playerData = new PlayerData(playerName);
            if (!data.isEmpty()) {
                SqlData sqlData = data.get(0);
                playerData.setPassword(sqlData.getString("password"));
                playerData.setEncryptEmail(sqlData.getString("mail"));
                playerData.setEncryptPhone(sqlData.getString("phone"));
                playerData.setClientIdentification(sqlData.getString("clientIdentification"));
                playerData.setLastLoginQuitTime(sqlData.getLong("lastLoginQuitTime"));
                playerData.setLoginLockTime(sqlData.getLong("loginLockTime"));
                playerData.setLastSendEmailCodeTime(sqlData.getLong("lastSendEmailCodeTime"));
                playerData.setLastSendSmsCodeTime(sqlData.getLong("lastSendSmsCodeTime"));
                playerData.setLoginErrorCount(sqlData.getInt("loginErrorCount"));
            }
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
        SqlDataList<SqlData> data = this.sqlManager.getData(TABLE_NAME, new SelectType("name", playerData.getName()));
        SqlData sqlData = new SqlData();
        sqlData.put("name", playerData.getName());
        sqlData.put("password", playerData.getPassword());
        sqlData.put("mail", playerData.getEncryptEmail());
        sqlData.put("phone", playerData.getEncryptPhone());
        sqlData.put("clientIdentification", playerData.getClientIdentification());
        sqlData.put("lastLoginQuitTime", playerData.getLastLoginQuitTime());
        sqlData.put("loginLockTime", playerData.getLoginLockTime());
        sqlData.put("lastSendEmailCodeTime", playerData.getLastSendEmailCodeTime());
        sqlData.put("lastSendSmsCodeTime", playerData.getLastSendSmsCodeTime());
        sqlData.put("loginErrorCount", playerData.getLoginErrorCount());
        if (data.isEmpty()) {
            this.sqlManager.insertData(TABLE_NAME, sqlData);
        } else {
            this.sqlManager.setData(TABLE_NAME, sqlData, new SqlData("name", playerData.getName()));
        }
    }

    @Override
    public void saveAllCache() {
        for (PlayerData data : this.playerDataMap.values()) {
            this.saveData(data);
        }
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

        this.sqlManager.shutdown();
    }
}
