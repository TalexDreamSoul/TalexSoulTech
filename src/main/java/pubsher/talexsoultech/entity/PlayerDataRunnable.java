package pubsher.talexsoultech.entity;

import lombok.Getter;

public abstract class PlayerDataRunnable {

    @Getter
    private boolean cancelled;

    public abstract void run();

    public void cancel() {

        this.cancelled = true;

    }

}
