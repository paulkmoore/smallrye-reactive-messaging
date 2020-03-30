package acme;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

public class Main {

    public static void main(String[] args) {
        SeContainer container = SeContainerInitializer.newInstance().initialize();
        container.getBeanManager().createInstance().select(Sender.class).get();
    }
}
