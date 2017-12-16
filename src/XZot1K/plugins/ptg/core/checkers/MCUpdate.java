package XZot1K.plugins.ptg.core.checkers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

public final class MCUpdate implements Listener
{

    private Plugin pl;
    private final static String VERSION = "1.0";

    /**
     * Server received information.
     */
    private String updateMessage = "";
    private boolean upToDate = true;

    /**
     * Interval of time to ping (seconds)
     */
    private int PING_INTERVAL;

    /**
     * The scheduled task
     */
    private volatile BukkitTask task = null;

    public MCUpdate(Plugin plugin, boolean startTask)
    {
        if (plugin != null)
        {
            this.pl = plugin;
            //I should add a custom configuration for MCUpdate itself
            Bukkit.getPluginManager().registerEvents(this, plugin);
            setPingInterval(900);
            if (startTask)
            {
                start();
            }
        }
    }

    private void start()
    {
        // Is MCUpdate already running?
        if (task == null)
        {
            // Begin hitting the server with glorious data
            task = pl.getServer().getScheduler().runTaskTimerAsynchronously(pl, () ->
            {
                report();
            }, 0, PING_INTERVAL * 20);
        }

    }

    private int getOnlinePlayers()
    {
        return pl.getServer().getOnlinePlayers().size();
    }

    private void report()
    {
        String ver = pl.getDescription().getVersion();
        String name = pl.getDescription().getName();
        int playersOnline = this.getOnlinePlayers();
        boolean onlineMode = pl.getServer().getOnlineMode();
        String serverVersion = pl.getServer().getVersion();

        String osname = System.getProperty("os.name");
        String osarch = System.getProperty("os.arch");
        String osversion = System.getProperty("os.version");
        String java_version = System.getProperty("java.version");
        int coreCount = Runtime.getRuntime().availableProcessors();

        String report = "{ \"report\": {";
        report += toJson("plugin", name) + ",";
        report += toJson("version", ver) + ",";
        report += toJson("playersonline", playersOnline + "") + ",";
        report += toJson("onlinemode", onlineMode + "") + ",";
        report += toJson("serverversion", serverVersion) + ",";

        report += toJson("osname", osname) + ",";
        report += toJson("osarch", osarch) + ",";
        report += toJson("osversion", osversion) + ",";
        report += toJson("javaversion", java_version) + ",";
        report += toJson("corecount", coreCount + "") + "";

        report += "} }";

        byte[] data = report.getBytes();

        try
        {

            String BASE_URL = "http://report.mcupdate.org";
            URL url = new URL(BASE_URL);
            URLConnection c = url.openConnection();
            c.setConnectTimeout(2500);
            c.setReadTimeout(3500);

            c.addRequestProperty("User-Agent", "MCUPDATE/" + VERSION);
            c.addRequestProperty("Content-Type", "application/json");
            c.addRequestProperty("Content-Length", Integer.toString(data.length));
            c.addRequestProperty("Accept", "application/json");
            c.addRequestProperty("Connection", "close");

            c.setDoOutput(true);

            OutputStream os = c.getOutputStream();
            os.write(data);
            os.flush();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream())))
            {
                String endData = br.readLine().trim();
                
                String serverMessage = getString(endData, "message");
                String cVersion = getString(endData, "pl_Version");
                updateMessage = getString(endData, "update_Message");
                
                if (!serverMessage.equals("ERROR"))
                {
                    if (!ver.equals(cVersion))
                    {
                        upToDate = false;
                    }
                }
            }

        } catch (IOException ignored)
        {
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        Player p = e.getPlayer();
        if (p.isOp() && !upToDate)
        {
            p.sendMessage(format(updateMessage));
        }
    }

    private String getString(String data, String key)
    {
        String dat = data.replace("{ \"Response\": {\"", "");
        dat = dat.replace("\"} }", "");
        List<String> list = Arrays.asList(dat.split("\",\""));

        for (int i = -1; ++i < list.size(); )
        {
            List<String> list2 = Arrays.asList(list.get(i).split("\":\""));
            if (key.equals(list2.get(0)))
            {
                return list2.get(1);
            }
        }
        return null;
    }

    private static String toJson(String key, String value)
    {
        return "\"" + key + "\":\"" + value + "\"";
    }

    private static String format(String format)
    {
        return ChatColor.translateAlternateColorCodes('&', format);
    }

    public void setPingInterval(int PING_INTERVAL)
    {
        this.PING_INTERVAL = PING_INTERVAL;
    }
}