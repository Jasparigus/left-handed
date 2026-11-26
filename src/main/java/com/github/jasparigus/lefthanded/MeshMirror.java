package com.github.jasparigus.lefthanded;

import net.runelite.api.Model;

/**
 * Mirrors a model mesh about its vertical (X) axis. Two operations, both self-inverse, so calling
 * {@link #mirrorModelX(Model)} twice restores the model exactly - which lets the draw-callbacks
 * wrapper flip the local player's model only for the duration of the renderer's synchronous
 * upload and restore it immediately afterwards.
 *
 * <p>The face-winding swap is mandatory, not cosmetic: the GPU renderer's sorted upload culls
 * faces by screen-space winding (cross-product area sign), so negating X alone inverts every
 * triangle and the renderer draws the inside of the mesh instead of the outside.
 *
 * <p>Vertex normals are deliberately untouched: the GPU upload paths do not read them (lighting
 * comes from pre-baked face colors), and normal arrays are shared references across actors with
 * the same appearance, so editing them would corrupt other models.
 */
final class MeshMirror
{
	private MeshMirror()
	{
	}

	/**
	 * Negates all vertex x-coordinates, swaps the second/third face indices of every face, and
	 * swaps the matching per-corner face colors so shading travels with the swapped vertices.
	 */
	static void mirrorModelX(Model model)
	{
		mirrorVerticesX(model.getVerticesX(), model.getVerticesCount());
		final int faceCount = model.getFaceCount();
		swapFaceWinding(model.getFaceIndices2(), model.getFaceIndices3(), faceCount);
		final int[] colors2 = model.getFaceColors2();
		final int[] colors3 = model.getFaceColors3();
		if (colors2 != null && colors3 != null)
		{
			swapFaceColors(colors2, colors3, faceCount);
		}
	}

	/**
	 * Swaps per-corner face colors for faces that have ordinary shading. Faces whose third color
	 * is not a real color are skipped: the renderer's convention is {@code faceColors3 == -1}
	 * (flat face: all corners use color1) and {@code faceColors3 == -2} (hidden face: skipped at
	 * upload. Swapping those markers between the two arrays would un-hide hidden faces and upload
	 * raw negative values as corner colors. Skipping is self-inverse: faceColors2 always holds a
	 * real color (>= 0), so after a swap the third slot still tests >= 0 for the restore pass, and
	 * never-swapped special faces keep failing the test forever.
	 */
	static void swapFaceColors(int[] colors2, int[] colors3, int faceCount)
	{
		final int count = Math.min(faceCount, Math.min(colors2.length, colors3.length));
		for (int i = 0; i < count; i++)
		{
			if (colors3[i] >= 0)
			{
				final int tmp = colors2[i];
				colors2[i] = colors3[i];
				colors3[i] = tmp;
			}
		}
	}

	/**
	 * Negates the first {@code vertCount} x-coordinates in place.
	 */
	static void mirrorVerticesX(float[] vx, int vertCount)
	{
		for (int i = 0; i < vertCount; i++)
		{
			vx[i] = -vx[i];
		}
	}

	/**
	 * Swaps the two arrays' entries pairwise for the first {@code count} entries (clamped to the
	 * shorter array), flipping triangle winding (indices) or the matching corner colors.
	 */
	static void swapFaceWinding(int[] indices2, int[] indices3, int faceCount)
	{
		final int count = Math.min(faceCount, Math.min(indices2.length, indices3.length));
		for (int i = 0; i < count; i++)
		{
			final int tmp = indices2[i];
			indices2[i] = indices3[i];
			indices3[i] = tmp;
		}
	}
}
