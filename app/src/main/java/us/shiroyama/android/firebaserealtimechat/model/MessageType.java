package us.shiroyama.android.firebaserealtimechat.model;

/**
 * Message Type
 *
 */

// TODO Typeは何につかっているのか調査
public enum MessageType {
    NORMAL(1 << 7), IMAGE(1 << 8);

    private int flag;

    MessageType(int flag) {
        this.flag = flag;
    }

    public int getFlag() {
        return flag;
    }
}
