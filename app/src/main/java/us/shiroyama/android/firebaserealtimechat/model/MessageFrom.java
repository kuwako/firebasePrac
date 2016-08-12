package us.shiroyama.android.firebaserealtimechat.model;

/**
 * Message From
 *
 */

// TODO Typeは何につかっているのか調査
public enum MessageFrom {
    ME(1), OTHERS(1 << 1);

    private int flag;

    MessageFrom(int flag) {
        this.flag = flag;
    }

    public int getFlag() {
        return flag;
    }
}
