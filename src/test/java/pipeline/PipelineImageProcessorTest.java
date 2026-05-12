package pipeline;

import filter.ImageFilters;
import image.ColorImage;
import image.ImageUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import parallel.ParallelStrategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PipelineImageProcessorTest {
    @TempDir
    Path tempDir;

    @Test
    void sequentialPipelineShouldMatchSingleImageSequentialReference() throws IOException {
        Path input = tempDir.resolve("input");
        Path output = tempDir.resolve("output");
        Files.createDirectories(input);

        ColorImage imageA = randomImage(17, 11, 101);
        ColorImage imageB = randomImage(13, 19, 202);
        ImageUtils.saveColor(imageA, input.resolve("a.png").toString());
        ImageUtils.saveColor(imageB, input.resolve("b.png").toString());

        PipelineImageProcessor processor = new PipelineImageProcessor();
        BatchResult result = processor.processDirectory(
                input,
                output,
                "gaussian5",
                BatchConfig.sequentialWorkers(2, 1)
        );

        assertEquals(2, result.files());
        assertImagesEqual(ImageFilters.apply(imageA, "gaussian5"), ImageUtils.loadColor(output.resolve("a.png").toString()));
        assertImagesEqual(ImageFilters.apply(imageB, "gaussian5"), ImageUtils.loadColor(output.resolve("b.png").toString()));
    }

    @Test
    void parallelPipelineShouldPreserveRelativeDirectoriesAndMatchReference() throws IOException {
        Path input = tempDir.resolve("input");
        Path nested = input.resolve("nested");
        Path output = tempDir.resolve("output");
        Files.createDirectories(nested);

        ColorImage image = randomImage(23, 15, 303);
        ImageUtils.saveColor(image, nested.resolve("sample.png").toString());

        PipelineImageProcessor processor = new PipelineImageProcessor();
        BatchResult result = processor.processDirectory(
                input,
                output,
                "sharpen3",
                BatchConfig.parallelWorkers(2, 2, ParallelStrategy.ROWS, 3)
        );

        Path outputImage = output.resolve("nested").resolve("sample.png");
        assertEquals(1, result.files());
        assertImagesEqual(ImageFilters.apply(image, "sharpen3"), ImageUtils.loadColor(outputImage.toString()));
    }

    private static ColorImage randomImage(int width, int height, long seed) {
        Random random = new Random(seed);
        byte[] data = new byte[width * height * ColorImage.CHANNELS];
        random.nextBytes(data);
        return new ColorImage(width, height, data);
    }

    private static void assertImagesEqual(ColorImage expected, ColorImage actual) {
        assertEquals(expected.width, actual.width);
        assertEquals(expected.height, actual.height);
        assertArrayEquals(expected.data, actual.data);
    }
}
