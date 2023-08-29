package io.bluestaggo.voxelthing;

import io.bluestaggo.voxelthing.gui.BlockInventory;
import io.bluestaggo.voxelthing.gui.DebugGui;
import io.bluestaggo.voxelthing.gui.GuiScreen;
import io.bluestaggo.voxelthing.gui.IngameGui;
import io.bluestaggo.voxelthing.renderer.MainRenderer;
import io.bluestaggo.voxelthing.renderer.world.WorldRenderer;
import io.bluestaggo.voxelthing.window.ClientPlayerController;
import io.bluestaggo.voxelthing.window.Window;
import io.bluestaggo.voxelthing.world.BlockRaycast;
import io.bluestaggo.voxelthing.world.Chunk;
import io.bluestaggo.voxelthing.world.ClientWorld;
import io.bluestaggo.voxelthing.world.World;
import io.bluestaggo.voxelthing.world.block.Block;
import io.bluestaggo.voxelthing.world.entity.IPlayerController;
import io.bluestaggo.voxelthing.world.entity.Player;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.glClearColor;

public class Game {
	public static final String VERSION;
	public static final int TICKS_PER_SECOND = 20;
	public static final float TICK_RATE = 1.0f / TICKS_PER_SECOND;

	static {
		String version = "???";

		try (InputStream stream = Game.class.getResourceAsStream("/version.txt")) {
			if (stream == null) {
				version = "dev " + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "?";
			} else {
				var reader = new BufferedReader(new InputStreamReader(stream));
				version = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		VERSION = version;

		var options = new Properties();
		options.setProperty("chunk-load-threads", "10");
		options.setProperty("chunk-render-threads", "1");
		options.setProperty("enable-multithreading", "true");

		try {
			Path dbgOptPath = Paths.get(System.getenv("APPDATA"), "VoxelThing/dbg-opt.txt");
			try (InputStream istream = Files.newInputStream(dbgOptPath, StandardOpenOption.READ)) {
				options.load(istream);
			} catch (IOException ignored) {
			}

			try {
				Files.createDirectories(dbgOptPath.getParent());
				try (OutputStream ostream = Files.newOutputStream(dbgOptPath, StandardOpenOption.CREATE)) {
					options.store(ostream, "Voxel Thing options (version " + VERSION + ")");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		boolean multithreading = options.getProperty("chunk-load-threads").equals("true");

		World.chunkLoadRate = Integer.parseInt(options.getProperty("chunk-load-threads"));
		World.multithreading = multithreading;
		WorldRenderer.chunkUpdateRate = Integer.parseInt(options.getProperty("chunk-render-threads"));
		WorldRenderer.multithreading = multithreading;
	}

	private static final String[] SKINS = {
			"joel",
			"staggo",
			"floof",
			"talon"
	};
	private int currentSkin = VERSION.contains("dev") || VERSION.equals("???") ? 1 : 0;
	private boolean thirdPerson;
	private boolean debugMenu = true;
	private boolean drawGui = true;
	private boolean viewBobbing = true;

	private static Game instance;

	public final Window window;
	public final MainRenderer renderer;

	public World world;
	public Player player;
	public IPlayerController playerController;

	public Block[] palette = new Block[9];
	public int heldItem;

	private final GuiScreen debugGui;
	private final GuiScreen inGameGui;
	private GuiScreen currentGui;

	private BlockRaycast blockRaycast;

	public float mouseSensitivity = 0.25f;

	private double tickTime;
	private double partialTick;

	public Game() {
		instance = this;

		window = new Window();
		window.grabCursor();

		renderer = new MainRenderer(this);

		debugGui = new DebugGui(this);
		inGameGui = new IngameGui(this);

		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		startWorld();
	}

	public static Game getInstance() {
		return instance;
	}

	public void run() {
		try {
			while (!window.shouldClose()) {
				update(window.getDeltaTime());
				draw();
				window.update();
			}
		} catch (Throwable e) {
			var stackTrace = new StringWriter();
			var stackTracePrinter = new PrintWriter(stackTrace);
			stackTracePrinter.println("An exception has occured and the program needs to quit!");
			e.printStackTrace(stackTracePrinter);
			JOptionPane.showMessageDialog(null, stackTrace, "Voxel Thing: Exception", JOptionPane.ERROR_MESSAGE);
		}

		close();
	}

	private void close() {
		if (world != null) {
			world.close();
		}
		renderer.unload();
		window.destroy();
	}

	public void startWorld() {
		if (world != null) {
			world.close();
		}

		world = new ClientWorld(this);
		playerController = new ClientPlayerController(this);
		player = new Player(world, playerController);
	}

	public boolean isInWorld() {
		return world != null && player != null;
	}

	private void update(double delta) {
		if (!isInWorld() && window.isKeyJustPressed(GLFW_KEY_ENTER)) {
			startWorld();
		}

		tickTime += delta;

		GuiScreen gui = currentGui != null ? currentGui : isInWorld() ? inGameGui : null;
		if (gui == inGameGui) {
			doControls();
		}

		if (gui != null) {
			gui.handleInput();
		}

		if (isInWorld()) {
			int cx = player.getBlockX() >> Chunk.SIZE_POW2;
			int cy = player.getBlockY() >> Chunk.SIZE_POW2;
			int cz = player.getBlockZ() >> Chunk.SIZE_POW2;

			world.loadSurroundingChunks(cx, cy, cz, renderer.worldRenderer.renderDistance);

			player.onGameUpdate();
			player.noClip = window.isKeyDown(GLFW_KEY_Q);
		}

		if (tickTime >= TICK_RATE) {
			tickTime %= TICK_RATE;
			if (currentGui != null) {
				currentGui.tick();
			}

			if (isInWorld()) {
				inGameGui.tick();
				player.tick();
			}
		}

		partialTick = tickTime / TICK_RATE;

		if (isInWorld()) {
			world.partialTick = partialTick;

			float px = (float) player.getPartialX();
			float py = (float) player.getPartialY();
			float pz = (float) player.getPartialZ();
			float yaw = (float) player.rotYaw;
			float pitch = (float) player.rotPitch;

			py += player.height - 0.3;
			if (viewBobbing) {
				if (thirdPerson) {
					py -= player.getPartialVelY() * 0.2;
				}
				pitch += player.getPartialVelY() * 2.5f;
			}

			renderer.camera.setPosition(px, py, pz);
			renderer.camera.setRotation(yaw, pitch);

			if (world != null) {
				blockRaycast = renderer.camera.getRaycast(5.0f);
				world.doRaycast(blockRaycast);
			}

			if (thirdPerson) {
				renderer.camera.moveForward(-4.0f);
			} else if (viewBobbing) {
				py += Math.abs(player.getRenderWalk()) * 0.2f;
				renderer.camera.setPosition(px, py, pz);
				renderer.camera.moveRight((float) player.getRenderWalk() * 0.1f);
			}
		}
	}

	private void doControls() {
		if (window.isKeyJustPressed(GLFW_KEY_F)) {
			int dist = renderer.worldRenderer.renderDistance;
			if (window.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
				if (dist < 16) {
					dist <<= 1;
				}
			} else if (dist > 1) {
				dist >>= 1;
			}

			renderer.worldRenderer.renderDistance = dist;
			renderer.worldRenderer.loadRenderers();
		}

		if (window.isKeyJustPressed(GLFW_KEY_R)) {
			player.posX = world.random.nextDouble(-1000.0, 1000.0);
			player.posY = 64.0;
			player.posZ = world.random.nextDouble(-1000.0, 1000.0);
			player.velX = 0.0;
			player.velY = 0.0;
			player.velZ = 0.0;
		}

		if (window.isKeyJustPressed(GLFW_KEY_LEFT_BRACKET) && renderer.screen.scale > 0.0f) {
			renderer.screen.scale -= 0.5f;
		}

		if (window.isKeyJustPressed(GLFW_KEY_RIGHT_BRACKET)) {
			renderer.screen.scale += 0.5f;
		}

		if (window.isKeyJustPressed(GLFW_KEY_F1)) {
			drawGui = !drawGui;
		}

		if (window.isKeyJustPressed(GLFW_KEY_F3)) {
			debugMenu = !debugMenu;
		}

		if (window.isKeyJustPressed(GLFW_KEY_F4)) {
			viewBobbing = !viewBobbing;
		}

		if (window.isKeyJustPressed(GLFW_KEY_F5)) {
			thirdPerson = !thirdPerson;
		}

		if (window.isKeyJustPressed(GLFW_KEY_F6)) {
			if (++currentSkin >= SKINS.length) currentSkin = 0;
		}

		if (window.isKeyJustPressed(GLFW_KEY_ESCAPE)) {
			window.toggleGrabCursor();
		}

		if (window.isKeyJustPressed(GLFW_KEY_E)) {
			openGui(new BlockInventory(this));
		}
	}

	private void draw() {
		renderer.draw();

		if (drawGui) {
			if (debugMenu) {
				debugGui.draw();
			}

			if (isInWorld()) {
				inGameGui.draw();
			} else {
				renderer.fonts.outlined.printCentered("Press ENTER to play", renderer.screen.getWidth() / 2.0f, renderer.screen.getHeight() / 2.0f);
			}

			if (currentGui != null) {
				currentGui.draw();
			}
		}
	}

	public void openGui(GuiScreen gui) {
		currentGui = gui;
		if (gui == null) {
			window.grabCursor();
		} else {
			window.ungrabCursor();
		}
	}

	public void closeGui() {
		openGui(null);
	}

	public boolean isGuiOpen() {
		return currentGui != null;
	}

	public String getSkin() {
		return "/assets/entities/" + SKINS[currentSkin] + ".png";
	}

	public boolean showThirdPerson() {
		return thirdPerson;
	}

	public double getPartialTick() {
		return partialTick;
	}

	public BlockRaycast getBlockRaycast() {
		return blockRaycast;
	}

	public boolean viewBobbingEnabled() {
		return viewBobbing;
	}

	public static void main(String[] args) {
//		System.setProperty("java.awt.headless", "true");
		new Game().run();
	}
}
