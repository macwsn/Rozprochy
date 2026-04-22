import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;

public class Z1_Consumer {

    public static void main(String[] argv) throws Exception {

        // info
        System.out.println("Z1 CONSUMER");

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        // queue
        final String QUEUE_NAME = "queue1";
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        // Tryb potwierdzen:
        //   AUTO_ACK  = true  -> ack po otrzymaniu (basicConsume autoAck=true)
        //   MANUAL    = false -> ack po przetworzeniu (basicAck)
        final boolean AUTO_ACK = false;

        // consumer (handle msg)
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {

                String message = new String(body, "UTF-8");
                System.out.println("Received: " + message);

                try {
                    int timeToSleep = Integer.parseInt(message);
                    System.out.println("Przetwarzanie " + timeToSleep + " s...");
                    Thread.sleep(timeToSleep * 1000);
                    System.out.println("Zakonczono przetwarzanie: " + message);
                } catch (NumberFormatException e) {
                    System.out.println("Wiadomosc nie jest liczba, pomijam sleep.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (!AUTO_ACK) {
                    channel.basicAck(envelope.getDeliveryTag(), false);
                    System.out.println("ACK wyslany po przetworzeniu.");
                }
            }
        };

        // start listening
        channel.basicQos(1); 
        System.out.println("Waiting for messages... (AUTO_ACK=" + AUTO_ACK + ")");
        channel.basicConsume(QUEUE_NAME, AUTO_ACK, consumer);
    }
}
