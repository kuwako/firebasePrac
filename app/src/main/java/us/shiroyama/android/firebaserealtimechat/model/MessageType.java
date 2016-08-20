package us.shiroyama.android.firebaserealtimechat.model;

/**
 * Message Type
 *
 */

public enum MessageType {
    // TODO この表現がわからない
    NORMAL(1 << 7), IMAGE(1 << 8);

    private int flag;

    MessageType(int flag) {
        this.flag = flag;
    }

    public int getFlag() {
        return flag;
    }
}
