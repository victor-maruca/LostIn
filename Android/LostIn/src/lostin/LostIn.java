package lostin;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LostIn {

    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(Constants.HOST);
            factory.setUsername(Constants.USER);
            factory.setPassword(Constants.PASS);
            factory.setVirtualHost(Constants.VHOST);
            Connection con = factory.newConnection();
            Channel channel = con.createChannel();
            channel.queueDeclare(Constants.QUEUE, true, false, false, null);

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties,
                                           byte[] body) throws IOException{
                           
                }
            };

            channel.basicConsume(Constants.QUEUE, true, consumer);

            channel.waitForConfirms();

            channel.close();
            con.close();
        }catch(Exception e){
            System.out.println(e);
        }
    }
    
    
}
