// Card.java
import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class Card implements Serializable {
    private static final long serialVersionUID = 1L; // Cần thiết cho Serializable
    public String value;
    public String type;

    public Card(String value, String type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public String toString() {
        return value + "-" + type;
    }

    public int getValue() {
        if ("AJQK".contains(value)) {
            if (value.equals("A")) {
                return 11;
            }
            return 10;
        }
        return Integer.parseInt(value);
    }

    public boolean isAce() {
        return value.equals("A");
    }

    public String getImagePath() {
        return "resources/cards/" + toString() + ".png";
    }
    public URL getImageURL() throws MalformedURLException {
        File cardFile = new File(getImagePath());
        return cardFile.toURI().toURL();
    }
}