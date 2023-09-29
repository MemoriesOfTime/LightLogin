package cn.lanink.lightlogin;

import cn.lanink.lightlogin.command.LightLoginCommand;
import cn.lanink.lightlogin.data.PlayerDataManager;
import cn.lanink.lightlogin.data.provider.MysqlProvider;
import cn.lanink.lightlogin.data.provider.YamlProvider;
import cn.lanink.lightlogin.utils.AES;
import cn.lanink.lightlogin.utils.PlayerVerificationData;
import cn.lanink.lightlogin.utils.Utils;
import cn.nukkit.Player;
import cn.nukkit.plugin.PluginBase;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.smallaswater.easysql.exceptions.MySqlLoginException;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * @author LT_Name
 */
public class LightLogin extends PluginBase {

    private static LightLogin lightLogin;

    public static final String PLUGIN_NAME = "§1L§2i§3g§4h§5t§6L§2o§ag§ci§en";

    @Getter
    private AES aes;

    @Getter
    private PluginConfig pluginConfig;

    @Getter
    private final Cache<Player, PlayerVerificationData> playerPhoneVerificationDataCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();
    @Getter
    private final Cache<Player, PlayerVerificationData> playerEmailVerificationDataCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();
    @Getter
    private final Cache<Player, PlayerVerificationData> playerResetVerificationDataCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();


    public static LightLogin getInstance() {
        return lightLogin;
    }

    @Override
    public void onLoad() {
        lightLogin = this;
    }

    @Override
    public void onEnable() {
        this.saveResource("介绍.txt", true);

        this.pluginConfig = new PluginConfig(this);
        this.aes = new AES("LightLogin" + this.pluginConfig.getPasswordRandomKey());

        if (this.pluginConfig.getDataProvider() == PluginConfig.DataProvider.MYSQL) {
            try {
                PlayerDataManager.setProvider(new MysqlProvider(this.pluginConfig.getMySqlConfig()));
                this.getLogger().info("数据存储方式：MySQL");
            } catch (MySqlLoginException e) {
                this.getLogger().error("§c数据库连接失败！请检查配置文件！");
                this.getPluginLoader().disablePlugin(this);
                return;
            }
        } else {
            PlayerDataManager.setProvider(new YamlProvider());
            this.getLogger().info("数据存储方式：Yaml");
        }

        PlayerDataManager.init(this);

        this.getServer().getCommandMap().register("LightLogin", new LightLoginCommand("LightLogin"));

        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);

        this.getLogger().info("插件加载完成！感谢使用！");
    }

    @Override
    public void onDisable() {
        PlayerDataManager.saveAll();

        this.getLogger().info("插件卸载完成！");
    }

    /**
     * @return 玩家数据存储路径
     */
    public String getPlayerDataPath() {
        return this.getDataFolder() + "/PlayerData/";
    }

    /**
     * 对密码进行加盐加密
     *
     * @param originalPassword 原始密码
     * @return 加密后的密码
     */
    public String encryptionPassword(String originalPassword) {
        return Utils.getMd5(this.pluginConfig.getPasswordRandomKey() + originalPassword);
    }
}
