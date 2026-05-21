package com.harleypig.jm_waypoint_manager;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = JmWaypointManager.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class
// annotated with @SubscribeEvent
@EventBusSubscriber(modid = JmWaypointManager.MODID, value = Dist.CLIENT)
public class JmWaypointManagerClient {
  public JmWaypointManagerClient(ModContainer container) {
    container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

    // RegisterCommandsEvent fires on the game bus (integrated server in SP).
    // Safe to call Minecraft.getInstance() from handlers since this mod is client-only.
    NeoForge.EVENT_BUS.addListener(WaypointCommand::onRegisterCommands);
  }

  @SubscribeEvent
  static void onClientSetup(FMLClientSetupEvent event) {
    // Some client setup code
    JmWaypointManager.LOGGER.info("HELLO FROM CLIENT SETUP");
    JmWaypointManager.LOGGER.info(
        "MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
  }
}
