package cn.lanink.lightlogin.data;

import cn.lanink.lightlogin.LightLogin;
import cn.lanink.lightlogin.data.provider.Provider;
import cn.nukkit.Player;
import cn.nukkit.Server;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * @author LT_Name
 */
public class PlayerDataManager {

    private static Provider provider;
    private static final ConcurrentLinkedQueue<Runnable> TASK_QUEUE = new ConcurrentLinkedQueue<>();

    public static void setProvider(Provider provider) {
        PlayerDataManager.provider = provider;
    }

    public static void init(LightLogin lightLogin) {
        Server.getInstance().getScheduler().scheduleRepeatingTask(lightLogin, () -> {
            while (!TASK_QUEUE.isEmpty()) {
                TASK_QUEUE.poll().run();
            }
        }, 1, true);
    }

    public static void setUUID(String playerName, UUID uuid) {
        TASK_QUEUE.add(() -> provider.setUUID(playerName, uuid));
    }

    public static PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getName());
    }

    public static PlayerData getPlayerData(String playerName) {
        return provider.loadData(playerName);
    }

    public static void save(PlayerData data) {
        TASK_QUEUE.add(() -> provider.saveData(data));
    }

    public static void save(Player player) {
        save(getPlayerData(player));
    }

    public static void save(String playerName) {
        TASK_QUEUE.add(() -> provider.saveData(playerName));
    }

    public static void remove(Player player) {
        TASK_QUEUE.add(() -> remove(player.getName()));
    }

    public static void remove(String playerName) {
        TASK_QUEUE.add(() -> provider.clearCache(playerName));
    }

    public static void saveAll() {
        TASK_QUEUE.add(() -> provider.close());
    }

}
