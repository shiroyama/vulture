package us.shiroyama.android.vulture;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Fumihiko Shiroyama
 */

public abstract class PauseHandler extends Handler {
    private final List<Message> messageQueue = Collections.synchronizedList(new ArrayList<Message>());

    private boolean isResumed;

    public final synchronized void resume() {
        isResumed = true;

        while (messageQueue.size() > 0) {
            final Message message = messageQueue.get(0);
            messageQueue.remove(0);
            sendMessage(message);
        }
    }

    public final synchronized void pause() {
        isResumed = false;
    }

    @Override
    public final synchronized void handleMessage(Message message) {
        if (!isResumed) {
            final Message messageCopy = new Message();
            messageCopy.copyFrom(message);
            messageQueue.add(messageCopy);
        } else {
            processMessage(message);
        }
    }

    protected abstract void processMessage(Message message);
}
