import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.Test;

public class UnsignedLongTest {

    @Test
    public void convert(){
        long value = -3845373630684859520L;
        String unsignedString = Long.toUnsignedString(value);
        System.out.println("unsignedString -> " + unsignedString); //14601370443024692096
        long longValue = UnsignedLong.valueOf(unsignedString).longValue();
        System.out.println("longValue -> " + longValue); //-3845373630684859520
    }
}
