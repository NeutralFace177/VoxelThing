package io.bluestaggo.voxelthing.world.storage;

import io.bluestaggo.voxelthing.math.MathUtil;
import io.bluestaggo.voxelthing.world.BlockStorage;
import io.bluestaggo.voxelthing.world.Chunk;
import io.bluestaggo.voxelthing.world.block.Block;

import java.util.List;

public class NibbleBlockStorage extends BlockStorage {
	private final byte[] blocks = new byte[Chunk.VOLUME / 2];

	public NibbleBlockStorage() {
		super();
	}

	public NibbleBlockStorage(List<Block> palette) {
		super(palette);
	}

	@Override
	protected int getBlockId(int x, int y, int z) {
		int i = MathUtil.index3D(x, y, z, Chunk.LENGTH);
		int hi = i / 2;
		if (i % 2 == 0) {
			return (blocks[hi] & 0xF0) >> 4;
		} else {
			return blocks[hi] & 0xF;
		}
	}

	@Override
	protected void setBlockId(int x, int y, int z, int id) {
		int i = MathUtil.index3D(x, y, z, Chunk.LENGTH);
		int hi = i / 2;
		if (i % 2 == 0) {
			blocks[hi] = (byte) ((id & 0xF) << 4 | blocks[hi] & 0xF);
		} else {
			blocks[hi] = (byte) (blocks[hi] & 0xF0 | id & 0xF);
		}
	}

	@Override
	protected int getMaxPaletteSize() {
		return 16;
	}

	@Override
	public BlockStorage expand() {
		return new ByteBlockStorage(this);
	}

	public byte[] getData() {
		return blocks;
	}
}