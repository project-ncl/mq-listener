package org.jboss.pnc.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.pnc.messaging.spi.BuildStatusChanged;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;

import javax.jms.Message;
import javax.jms.Session;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Unremovable
@Startup
@ApplicationScoped
public class QueueListener implements Runnable {

    private static final Logger LOG = Logger.getLogger(QueueListener.class);

    private volatile BuildStatusChanged lastMessage = null;

    private volatile boolean connected = false;

    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    ObjectMapper om;

    @ConfigProperty(name = "queue.topic")
    String pncQueueTopic;

    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

    @PostConstruct
    void stuff(){
        scheduler.submit(this);
    }

    @PreDestroy
    void anotherStuff() {
        scheduler.shutdown();
    }

    @Override
    public void run() {
        LOG.info("Listening with queue-topic: " + pncQueueTopic);

        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue(pncQueueTopic));
            while (true) {
                connected = true;
                Message message = consumer.receive();
                if (message == null) {
                    // receive returns `null` if the JMSConsumer is closed
                    return;
                }
                lastMessage = om.readValue(message.getBody(String.class), BuildStatusChanged.class);
                LOG.info("Got a message: " +  om.writerWithDefaultPrettyPrinter().writeValueAsString(lastMessage));
        }
        } catch (Exception e) {
            LOG.error(e);
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public BuildStatusChanged getLastMessage() {
        return lastMessage;
    }
}
