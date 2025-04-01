package eu.smashmc.flatmc;

import eu.smashmc.api.core.Inject;
import eu.smashmc.api.core.Invoke;
import eu.smashmc.api.core.Managed;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.*;
import java.util.logging.Logger;

@Managed
public class ChunkTransformer implements Listener {

	private static final int MAX_THICKNESS = 2;

	private static final Set<Long> TRANSFORMED = new HashSet<>();

	@Inject
	Logger logger;

	@Invoke
	public void init() {
		logger.info("Starting chunk transformer...");
		for (var world : Bukkit.getWorlds()) {
			for (var chunk : world.getLoadedChunks()) {
				transformChunk(chunk);
			}
		}
		logger.info("Done transforming all loaded chunks.");
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		var chunk = event.getChunk();
		transformChunk(chunk);
	}

	private void transformChunk(Chunk chunk) {
		long chunkId = (chunk.getWorld().getUID().getLeastSignificantBits() << 32) + ((long) chunk.getX() << 16) + chunk.getZ();
		if (!TRANSFORMED.add(chunkId)) {
			return;
		}
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				transformColumn(chunk, x, z);
			}
		}
	}

	private void transformColumn(Chunk chunk, int x, int z) {
		var world = chunk.getWorld();
		int blockX = (chunk.getX() << 4) + x;
		int blockZ = (chunk.getZ() << 4) + z;
		var highestBlock = world.getHighestBlockAt(blockX, blockZ);
		// find surface materials
		var topMaterial = getMaterials(chunk, x, highestBlock.getY(), z);

		// erase terrain
		for (int y = Math.max(highestBlock.getY(), 64); y >= topMaterial.size(); y--) {
			var block = chunk.getBlock(x, y, z);
			if (block.getType() != Material.AIR)
				block.setType(Material.AIR, false, true);
		}

		// place flat surface
		Collections.reverse(topMaterial);
		int y = 0;
		for (var top : topMaterial) {
			var block = chunk.getBlock(x, y, z);
			if (block.getType() != top.left())
				block.setTypeIdAndData(top.left().getId(), top.right(), false);
			y++;
		}
	}

	private List<Pair<Material, Byte>> getMaterials(Chunk chunk, int x, int y, int z) {
		List<Pair<Material, Byte>> materials = new ArrayList<>();
		var lastmat = Material.AIR;
		while (materials.size() < MAX_THICKNESS && y >= 0) {
			var block = chunk.getBlock(x, y, z);
			var mat = block.getType();
			var data = block.getData();
			boolean isKept = mat.isSolid() && mat.isOccluding() && !mat.name().startsWith("LEAVES") || mat.name().contains("WATER");
			if (isKept && mat != lastmat) {
				materials.add(Pair.of(mat, data));
				lastmat = mat;
			}
			y--;
		}
		return materials;
	}
}
