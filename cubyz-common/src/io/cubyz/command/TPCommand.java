package io.cubyz.command;

import org.joml.Vector3f;

import io.cubyz.api.Resource;
import io.cubyz.entity.Player;

public class TPCommand extends CommandBase {

	{
		name = "tp";
	}
	
	@Override
	public Resource getRegistryID() {
		return new Resource("cubyz", "tp");
	}

	@Override
	public void commandExecute(CommandSource source, String[] args) {
		if (source.getWorld() == null) {
			source.feedback("'tp' must be executed by a player");
			return;
		}
		Player player = source.getWorld().getLocalPlayer();
		player.setPosition(new Vector3f(Float.parseFloat(args[1]), Float.parseFloat(args[2]), Float.parseFloat(args[3])));
	}
}