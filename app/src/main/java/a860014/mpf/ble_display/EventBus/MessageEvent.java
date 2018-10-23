package a860014.mpf.ble_display.EventBus;

/**
 * Created by 860014 on 2018/5/28.
 */

public class MessageEvent {
    private String data;

    public MessageEvent(String data) {
        this.data = data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "MessageEvent{" + data + '\'' + '}';
    }
}