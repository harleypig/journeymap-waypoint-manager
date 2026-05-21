package com.harleypig.jm_waypoint_manager;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = JmWaypointManager.MODID, dist = Dist.CLIENT)
public class JmWaypointManagerClient {

  public JmWaypointManagerClient(ModContainer container) {
    container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

    NeoForge.EVENT_BUS.addListener(WaypointCommand::onRegisterCommands);
  }
}
