package cn.lanink.lightlogin;

import cn.lanink.gamecore.utils.ConfigUtils;
import cn.lanink.lightlogin.utils.Utils;
import cn.nukkit.utils.Config;
import lombok.Getter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author LT_Name
 */
@Getter
public class PluginConfig {

    private final LightLogin lightLogin;
    private final Config config;

    private boolean debug;

    private String passwordRandomKey;

    private final PasswordStrengthRequirements passwordStrengthRequirements;

    private final boolean xboxSkipLogin;

    private final int keepLoginTime;

    private final int maxLoginErrorCount;
    private final int loginLockTime;

    private boolean enableBindEmail;
    private boolean enableBindPhone;

    private DataProvider dataProvider;

    private final MySqlConfig mySqlConfig;

    public PluginConfig(LightLogin lightLogin) {
        this.lightLogin = lightLogin;
        lightLogin.saveDefaultConfig();
        this.config = lightLogin.getConfig();

        this.debug = config.getBoolean("debug", false);
        if (this.debug) {
            lightLogin.getLogger().warning("您已开启调试模式！");
        }

        this.passwordRandomKey = config.getString("passwordRandomKey");
        if (this.passwordRandomKey.isBlank()) {
            this.passwordRandomKey = Utils.getRandomString(16);
        }

        this.passwordStrengthRequirements = new PasswordStrengthRequirements(config.get("passwordStrengthRequirements", new HashMap<>()));

        this.xboxSkipLogin = config.getBoolean("xboxSkipLogin");

        this.keepLoginTime = config.getInt("keepLoginTime");
        this.maxLoginErrorCount = config.getInt("maxLoginErrorCount");
        this.loginLockTime = config.getInt("loginLockTime");

        this.enableBindEmail = config.getBoolean("enableBindEmail");
        this.enableBindPhone = config.getBoolean("enableBindPhone");

        try {
            this.dataProvider = DataProvider.valueOf(config.getString("dataProvider").toUpperCase());
        } catch (Exception e) {
            this.dataProvider = DataProvider.YAML;
        }

        this.mySqlConfig = new MySqlConfig(config.get("MySQL", new HashMap<>()));

        this.save();

        if (this.enableBindEmail) {
            try {
                Class.forName("cn.lanink.gamecore.mailapi.MailAPI");
            } catch (Exception e) {
                this.enableBindEmail = false;
                lightLogin.getLogger().warning("检测到您开启了绑定邮箱功能，但是您没有安装MailAPI前置插件，绑定邮箱功能已自动关闭！");
            }
        }
        if (this.enableBindPhone) {
            try {
                Class.forName("cn.lanink.gamecore.smsapi.SmsApi");
            } catch (Exception e) {
                this.enableBindPhone = false;
                lightLogin.getLogger().warning("检测到您开启了绑定手机功能，但是您没有安装SmsApi前置插件，绑定手机功能已自动关闭！");
            }
        }
    }

    public void save() {
        this.config.set("passwordRandomKey", this.passwordRandomKey);

        Map<String, Object> map = new LinkedHashMap<>();
        this.passwordStrengthRequirements.saveToMap(map);
        this.config.set("passwordStrengthRequirements", map);

        this.config.set("xboxSkipLogin", this.xboxSkipLogin);

        this.config.set("keepLoginTime", this.keepLoginTime);
        this.config.set("maxLoginErrorCount", this.maxLoginErrorCount);
        this.config.set("loginLockTime", this.loginLockTime);

        this.config.set("enableBindEmail", this.enableBindEmail);
        this.config.set("enableBindPhone", this.enableBindPhone);

        this.config.set("dataProvider", this.dataProvider.getName());

        Map<String, Object> mysqlConfigMap = new LinkedHashMap<>();
        this.mySqlConfig.saveToMap(mysqlConfigMap);
        this.config.set("MySQL", mysqlConfigMap);

        this.config.save();

        Config description = new Config();
        description.load(lightLogin.getResource("ConfigDescription.yml"));
        ConfigUtils.addDescription(this.config, description);
    }

    public enum DataProvider {
        MYSQL("MySQL"),
        YAML("Yaml");

        private final String name;

        DataProvider(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    @Getter
    public static class PasswordStrengthRequirements {
        private final int minLength;
        private final int maxLength;
        private final boolean needNumber;
        private final boolean needLowercaseLetters;
        private final boolean needUppercaseLetters;
        private final boolean needSpecialCharacters;

        public PasswordStrengthRequirements(Map<String, Object> map) {
            this.minLength = (int) map.getOrDefault("minLength", 6);
            this.maxLength = (int) map.getOrDefault("maxLength", 16);
            this.needNumber = (boolean) map.getOrDefault("needNumber", true);
            this.needLowercaseLetters = (boolean) map.getOrDefault("needLowercaseLetters", false);
            this.needUppercaseLetters = (boolean) map.getOrDefault("needUppercaseLetters", false);
            this.needSpecialCharacters = (boolean) map.getOrDefault("needSpecialCharacters", false);
        }

        public void saveToMap(Map<String, Object> map) {
            map.put("minLength", this.minLength);
            map.put("maxLength", this.maxLength);
            map.put("needNumber", this.needNumber);
            map.put("needLowercaseLetters", this.needLowercaseLetters);
            map.put("needUppercaseLetters", this.needUppercaseLetters);
            map.put("needSpecialCharacters", this.needSpecialCharacters);
        }
    }

    @Getter
    public static class MySqlConfig {
        private final String host;
        private final int port;
        private final String user;
        private final String password;
        private final String database;

        public MySqlConfig(Map<String, Object> map) {
            this.host = (String) map.getOrDefault("host", "localhost");
            this.port = (int) map.getOrDefault("port", 3306);
            this.user = (String) map.getOrDefault("user", "root");
            this.password = (String) map.getOrDefault("password", "root");
            this.database = (String) map.getOrDefault("database", "LightLogin");
        }

        public void saveToMap(Map<String, Object> map) {
            map.put("host", this.host);
            map.put("port", this.port);
            map.put("user", this.user);
            map.put("password", this.password);
            map.put("database", this.database);
        }
    }

}
