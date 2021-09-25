import java.io.Serializable;

public class CommonMessage implements Serializable {
    private String text;

    public CommonMessage(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
