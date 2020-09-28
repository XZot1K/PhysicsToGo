<img src="https://imgur.com/cCjihnu.png" width="150px" height="150px">

# How to use PhysicsToGo (PTG) API in your plugin

In order to properly use PhysicsToGo you will need to be able to retrieve the plugin's instance. You can do this by following these instructions:

1. Download the PhysicsToGo plugin and add it to your plugin's dependencies.  
2. Make sure your plugin can grab the PhysicsToGo instance from your Main class file like below:  

```
import xzot1k.plugins.ptg.PhysicsToGo;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public class Main extends JavaPlugin
{

    private static PhysicsToGo physicsToGo;

    @Override
    public void onEnable()
    {
        if (!isPTGInstalled())
        {
            getServer().getPluginManager().disablePlugin(this);
            
            return; // This plugin is now disabled since PTG was not installed.
        }

        // PTG was found and now can be accessed with the getPTG() getter.
    }

    // This method tells you whether PTG is installed or not.
    private boolean isPTGInstalled()
    {
        PhysicsToGo ptg = (PhysicsToGo) getServer().getPluginManager().getPlugin("PhysicsToGo");
        
        if(ptg != null)
        {
            setPTG(ptg);
            return true;
        }

        return false;
    }

    public static PhysicsToGo getPTG() { return ptg; }

    private static void setPTG(PhysicsToGo ptg) { Main.ptg = ptg; }

}
```

3. Once 1 and 2 are completed, add "depend: [PhysicsToGo]" or related things inside your plugin.yml (This step is optional, but never hurts to make sure SimplePortals is installed).  
4. Everything should be all set. As a test, call the getPTG() method from your Main class and you will be able to access the Manager class!

***
I currently use DedicatedMC as the official ZotWare testing server. If you need something powerful in terms of hardware without digging a hole in your pocket give them a shot and let me know how you like them!
![image](https://imgur.com/oqEKWO3.png)
