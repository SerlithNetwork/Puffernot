package gg.pufferfish.pufferfish.simd;

import java.awt.Color;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;
import org.bukkit.map.MapPalette;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class VectorMapPalette {

    private static final VectorSpecies<Integer> I_SPEC = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Float> F_SPEC = FloatVector.SPECIES_PREFERRED;

    private static final FloatVector[] CACHED_REDS;
    private static final FloatVector[] CACHED_GREENS;
    private static final FloatVector[] CACHED_BLUES;

    static {

        int length = MapPalette.colors.length;
        CACHED_REDS = new FloatVector[length];
        CACHED_GREENS = new FloatVector[length];
        CACHED_BLUES = new FloatVector[length];

        for (int c = 4; c < length; c++) {
            CACHED_REDS[c] = FloatVector.broadcast(F_SPEC, MapPalette.colors[c].getRed());
            CACHED_GREENS[c] = FloatVector.broadcast(F_SPEC, MapPalette.colors[c].getGreen());
            CACHED_BLUES[c] = FloatVector.broadcast(F_SPEC, MapPalette.colors[c].getBlue());
        }

    }

    @ApiStatus.Internal
    @SuppressWarnings("all")
    public static void matchColorVectorized(int[] in, byte[] out) {
        int speciesLength = I_SPEC.length();
        int i;
        for (i = 0; i < in.length - speciesLength; i += speciesLength) {
            float[] redsArr = new float[speciesLength];
            float[] bluesArr = new float[speciesLength];
            float[] greensArr = new float[speciesLength];
            int[] alphasArr = new int[speciesLength];

            for (int j = 0; j < speciesLength; j++) {
                alphasArr[j] = (in[i + j] >> 24) & 0xFF;
                redsArr[j] = (in[i + j] >> 16) & 0xFF;
                greensArr[j] = (in[i + j] >> 8) & 0xFF;
                bluesArr[j] = (in[i + j]) & 0xFF;
            }

            IntVector alphas = IntVector.fromArray(I_SPEC, alphasArr, 0);
            FloatVector reds = FloatVector.fromArray(F_SPEC, redsArr, 0);
            FloatVector greens = FloatVector.fromArray(F_SPEC, greensArr, 0);
            FloatVector blues = FloatVector.fromArray(F_SPEC, bluesArr, 0);
            IntVector resultIndex = IntVector.zero(I_SPEC);
            VectorMask<Integer> modificationMask = VectorMask.fromLong(I_SPEC, 0xffffffff);

            modificationMask = modificationMask.and(alphas.lt(128).not());
            FloatVector bestDistances = FloatVector.broadcast(F_SPEC, Float.MAX_VALUE);

            for (int c = 4; c < MapPalette.colors.length; c++) {
                // We're using 32-bit floats here because it's 2x faster and nobody will know the difference.
                // For correctness, the original algorithm uses 64-bit floats instead. Completely unnecessary.
                FloatVector compReds = CACHED_REDS[c];
                FloatVector compGreens = CACHED_GREENS[c];
                FloatVector compBlues = CACHED_BLUES[c];

                FloatVector rMean = reds.add(compReds).div(2.0f);
                FloatVector rDiff = reds.sub(compReds);
                FloatVector gDiff = greens.sub(compGreens);
                FloatVector bDiff = blues.sub(compBlues);

                FloatVector weightR = rMean.div(256.0f).add(2);
                FloatVector weightG = FloatVector.broadcast(F_SPEC, 4.0f);
                FloatVector weightB = FloatVector.broadcast(F_SPEC, 255.0f).sub(rMean).div(256.0f).add(2.0f);

                FloatVector distance = weightR.mul(rDiff).mul(rDiff).add(weightG.mul(gDiff).mul(gDiff)).add(weightB.mul(bDiff).mul(bDiff));

                // Now we compare to the best distance we've found.
                // This mask contains a "1" if better, and a "0" otherwise.
                VectorMask<Float> bestDistanceMask = distance.lt(bestDistances);
                bestDistances = bestDistances.blend(distance, bestDistanceMask); // Update the best distances

                // Update the result array
                // We also AND with the modification mask because we don't want to interfere if the alpha value isn't large enough.
                resultIndex = resultIndex.blend(c, bestDistanceMask.cast(I_SPEC).and(modificationMask)); // Update the results
            }

            for (int j = 0; j < speciesLength; j++) {
                int index = resultIndex.lane(j);
                out[i + j] = (byte) (index < 128 ? index : -129 + (index - 127));
            }
        }

        // For the final ones, fall back to the regular method
        for (; i < in.length; i++) {
            out[i] = MapPalette.matchColor(new Color(in[i], true));
        }
    }

}
