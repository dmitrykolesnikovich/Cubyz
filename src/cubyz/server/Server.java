package cubyz.server;

import cubyz.api.Side;
import cubyz.modding.ModLoader;
import cubyz.multiplayer.UDPConnectionManager;
import cubyz.utils.Logger;
import cubyz.client.ClientSettings;
import cubyz.client.entity.ClientEntityManager;
import cubyz.utils.Pacer;
import cubyz.utils.ThreadPool;
import cubyz.world.NormalChunk;
import cubyz.world.ServerWorld;

import java.util.ArrayList;

public final class Server extends Pacer{
	public static final int UPDATES_PER_SEC = 20;
	public static final int UPDATES_TIME_NS = 1_000_000_000 / UPDATES_PER_SEC;
	public static final float UPDATES_TIME_S = UPDATES_TIME_NS / 10e9f;

	private	static final Server server = new Server();

	public static ServerWorld world = null;
	public final static ArrayList<User> users = new ArrayList<>();
	public static UDPConnectionManager connectionManager = null;

	public static void main(String[] args) {
		if(ModLoader.mods.isEmpty()) {
			ModLoader.load(Side.SERVER);
		}
		if (world != null) {
			stop();
			world.cleanup();
		}

		Server.world = new ServerWorld(args[0], null);
		ThreadPool.startThreads();

		connectionManager = new UDPConnectionManager(5678);
		users.add(new User(connectionManager, "localhost", 5679));

		try {
			server.setFrequency(UPDATES_PER_SEC);
			server.start();
		} catch (Throwable e) {
			Logger.crash(e);
			if(world != null)
				world.cleanup();
			System.exit(1);
		}
		if(world != null)
			world.cleanup();
		world = null;
		connectionManager.cleanup();
		connectionManager = null;
		users.clear();
	}
	public static void stop(){
		if (server != null)
			server.running = false;
	}

	private Server(){
		super("Server");
	}

	@Override
	public void start() throws InterruptedException {
		running = true;
		super.start();
	}

	@Override
	public void update() {
		world.update();
		// TODO: Adjust for multiple players:

		// TODO: world.clientConnection.serverPing(world.getGameTime(), world.getBiome((int)Cubyz.player.getPosition().x, (int)Cubyz.player.getPosition().y, (int)Cubyz.player.getPosition().z).getRegistryID().toString());

		// TODO: Send this through the proper interface and to every player:
		ClientEntityManager.serverUpdate(world.getEntities());
	}
}
