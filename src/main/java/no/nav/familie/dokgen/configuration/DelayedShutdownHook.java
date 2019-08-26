package no.nav.familie.dokgen.configuration;

import org.springframework.context.ConfigurableApplicationContext;

public class DelayedShutdownHook extends Thread {

    private final ConfigurableApplicationContext applicationContext;

    public DelayedShutdownHook(final ConfigurableApplicationContext applicationContext) {

        this.applicationContext = applicationContext;
    }

    @Override
    public void run() {
        try {
            // https://github.com/kubernetes/kubernetes/issues/64510
            // https://nav-it.slack.com/archives/C5KUST8N6/p1543497847341300
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        applicationContext.close();
        super.run();
    }
}
