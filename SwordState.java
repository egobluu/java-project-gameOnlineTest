import java.io.Serializable;

public class SwordState implements Serializable {
    public int x, y;
    public boolean isPickedUp = false;
    public String ownerName = null;

    public SwordState(int x, int y) {
        this.x = x;
        this.y = y;
    }
}