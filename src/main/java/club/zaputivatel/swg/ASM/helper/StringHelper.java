package club.zaputivatel.swg.ASM.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StringHelper {
    private static final Random random = new Random();
    private static List<String> names = new ArrayList<String>();

    private static char getRandomChar(int minCodePoint, int maxCodePoint) {
        return (char) (random.nextInt(maxCodePoint - minCodePoint + 1) + minCodePoint);
    }

    public static String generateWord(int length) {
        StringBuilder word = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int languageType = random.nextInt(4);
            char randomChar;

            switch (languageType) {
                case 0:
                    randomChar = getRandomChar(0x4E00, 0x9FFF);
                    break;
                case 1:
                    randomChar = getRandomChar(0x3040, 0x30FF);
                    break;
                case 2:
                    randomChar = getRandomChar(0xAC00, 0xD7AF);
                    break;
                case 3:
                    randomChar = getRandomChar(0x0600, 0x06FF);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + languageType);
            }

            word.append(randomChar);
        }
        String local = word.toString();
        if (!names.contains(local)) {
            names.add(local);
            return local;
        }
        return generateWord(length);
    }

}
