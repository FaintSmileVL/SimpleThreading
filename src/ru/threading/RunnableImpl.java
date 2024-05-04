package ru.threading;

import lombok.extern.slf4j.Slf4j;

/**
 * @author : faint
 * @date : 02.05.2024
 * @time : 21:38
 */
public abstract class RunnableImpl implements Runnable {
    public abstract void runImpl() throws Exception;

    @Override
    public final void run() {
        try {
            this.runImpl();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
