package XZot1K.plugins.ptg.core.checkers;

import XZot1K.plugins.ptg.PhysicsToGo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker
{

    public static PhysicsToGo plugin;

    public UpdateChecker(PhysicsToGo plugin)
    {
        UpdateChecker.plugin = plugin;
    }

    public boolean isOutdated()
    {
        try
        {
            final HttpURLConnection c = (HttpURLConnection) new URL("http://www.spigotmc.org/api/general.php")
                    .openConnection();
            c.setDoOutput(true);
            c.setRequestMethod("POST");
            c.getOutputStream().write(("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource"
                    + "=17181").getBytes("UTF-8"));
            final String oldversion = plugin.getDescription().getVersion(),
                    newversion = new BufferedReader(new InputStreamReader(c.getInputStream())).readLine();
            if (!newversion.equalsIgnoreCase(oldversion))
            {
                return true;
            }
        } catch (IOException ignored)
        {
        }
        return false;
    }

    public String getLatestVersion()
    {
        try
        {
            final HttpURLConnection c = (HttpURLConnection) new URL("http://www.spigotmc.org/api/general.php")
                    .openConnection();
            c.setDoOutput(true);
            c.setRequestMethod("POST");
            c.getOutputStream().write(("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource"
                    + "=17181").getBytes("UTF-8"));
            return new BufferedReader(new InputStreamReader(c.getInputStream())).readLine();
        } catch (IOException ex)
        {
            return UpdateChecker.plugin.getDescription().getVersion();
        }
    }
}
