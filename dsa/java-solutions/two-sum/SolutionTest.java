import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class SolutionTest {
    private final Solution solution = new Solution();

    @Test
    void findsBasicPair() {
        assertArrayEquals(new int[] {0, 1}, solution.twoSum(new int[] {2, 7, 11, 15}, 9));
    }

    @Test
    void handlesDuplicateValues() {
        assertArrayEquals(new int[] {0, 1}, solution.twoSum(new int[] {3, 3}, 6));
    }

    @Test
    void handlesNegativeNumbers() {
        assertArrayEquals(new int[] {1, 2}, solution.twoSum(new int[] {-1, -2, -3}, -5));
    }
}
