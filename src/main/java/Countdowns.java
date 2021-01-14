import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

public class Countdowns {

    private static final Stack<CountdownsThread> countdowns = new Stack<>();
    public static final Stack<Long> messageIds = new Stack<>();
    private static final Logger logger = LoggerFactory.getLogger("Countdown");

    public static void startNewCountdown(MessageReceivedEvent event) {
        logger.debug("detected countdown command");
        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();
        try {
            channel.deleteMessageById(event.getMessageId()).queue();
            content = content.substring(11);
            int day = Integer.parseInt(content.substring(0, content.indexOf('.')));
            content = content.substring(content.indexOf('.') + 1);
            int month = Integer.parseInt(content.substring(0, content.indexOf('.')));
            content = content.substring(content.indexOf('.') + 1);
            int year = Integer.parseInt(content.substring(0, content.indexOf(' ')));
            content = content.substring(content.indexOf(' ') + 1);
            int hour = Integer.parseInt(content.substring(0, content.indexOf(':')));
            content = content.substring(content.indexOf(':') + 1);
            int minutes;
            if(content.length() <= 2) {
                minutes = Integer.parseInt(content);
                content = "";
            } else {
                minutes = Integer.parseInt(content.substring(0, content.indexOf(' ')));
                content = content.substring(content.indexOf(' '));
            }
            Instant date = Instant.parse(year + "-" + String.format("%02d", month) + "-" +
                    String.format("%02d", day) + "T" + String.format("%02d", hour) + ":" +
                    String.format("%02d", minutes) + ":00Z");
            if(date.getEpochSecond() < Instant.now().getEpochSecond()) {
                channel.sendMessage("You tried making a countdown in the past").queue(message -> {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ignored) {}
                    message.delete().queue();
                });
                logger.warn("User tried making a countdown in the past");
                return;
            }
            logger.info("starting countdown thread");
            CountdownsThread countdownsThread = new CountdownsThread(channel, content, date);
            countdownsThread.start();
            countdowns.push(countdownsThread);
        } catch (NumberFormatException ignored) {
            channel.sendMessage("Something went wrong with your command try again").queue();
        }

    }

    public static void closeSpecificThread(long messageId) {
        int index = messageIds.indexOf(messageId);
        if(index == -1) return;
        CountdownsThread thread = countdowns.get(index);
        thread.interrupt();
        countdowns.remove(index);
        messageIds.remove(index);
    }

    public static void closeAllThreads() {
        countdowns.forEach(Thread::interrupt);
    }

    private static class CountdownsThread extends Thread {

        private final Object lock = new Object();
        private boolean stop = false;

        private Message message;
        private final String text;
        private final Instant date;

        private CountdownsThread(MessageChannel channel, String text, Instant date) {
            this.text = text;
            this.date = date;

            logger.info("sending message");
            channel.sendMessage(computeLeftTime()[0] + text).queue(message -> {
                synchronized (lock) {
                    this.message = message;
                    messageIds.push(message.getIdLong());
                    lock.notify();
                }
            });
        }

        @Override
        public void run() {
            synchronized (lock) {
                if (message == null) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            while(!stop) {
                Object[] info = computeLeftTime();
                if(info[0] instanceof Boolean) {
                    message.editMessage("Countdown finished").queue();
                    return;
                }
                logger.info("editing message: " + info[0]);
                message.editMessage(info[0] + text).queue();
                try{
                    long sleepTime = (Long) info[1];
                    sleep(sleepTime < 5000?60000:sleepTime);
                } catch (InterruptedException ignored) {}
            }
        }

        @Override
        public void interrupt() {
            stop = true;
            super.interrupt();
        }

        private Object[] computeLeftTime() {
            long differenceOG = date.getEpochSecond() - Instant.now().getEpochSecond() ;
            long dayDiff = TimeUnit.DAYS.convert(differenceOG, TimeUnit.SECONDS);
            long differenceHour = differenceOG - TimeUnit.SECONDS.convert(dayDiff, TimeUnit.DAYS);
            long hourDiff = TimeUnit.HOURS.convert(differenceHour, TimeUnit.SECONDS);
            long differenceMinutes = differenceHour - TimeUnit.SECONDS.convert(hourDiff, TimeUnit.HOURS);
            long minutesDiff = TimeUnit.MINUTES.convert(differenceMinutes, TimeUnit.SECONDS);
            long differenceSeconds = differenceMinutes - TimeUnit.SECONDS.convert(minutesDiff, TimeUnit.MINUTES);

            if(dayDiff > 7) {
                if(dayDiff % 7 == 0)
                    return new Object[]{(dayDiff / 7) + " weeks left",
                            TimeUnit.MILLISECONDS.convert(differenceHour, TimeUnit.SECONDS)};
                return new Object[]{(dayDiff / 7) + " weeks and " + (dayDiff % 7) + " days left",
                        TimeUnit.MILLISECONDS.convert(differenceHour, TimeUnit.SECONDS)};
            }
            if(dayDiff > 3 || dayDiff > 0 && hourDiff == 0)
                return new Object[]{dayDiff + " days left",
                        TimeUnit.MILLISECONDS.convert(differenceHour, TimeUnit.SECONDS)};
            if(dayDiff > 0)
                return new Object[]{dayDiff + " days and " + hourDiff + " left",
                        TimeUnit.MILLISECONDS.convert(differenceMinutes, TimeUnit.SECONDS)};
            if(hourDiff > 6 || hourDiff > 0 && minutesDiff == 0)
                return new Object[]{hourDiff + " hours left",
                        TimeUnit.MILLISECONDS.convert(differenceMinutes, TimeUnit.SECONDS)};
            if(hourDiff > 0)
                return new Object[]{hourDiff + " hours and " + minutesDiff + " minutes left",
                        TimeUnit.MILLISECONDS.convert(differenceSeconds, TimeUnit.SECONDS)};
            if(minutesDiff > 0)
                return new Object[]{minutesDiff + " minutes left",
                        TimeUnit.MILLISECONDS.convert(differenceSeconds, TimeUnit.SECONDS)};
            return new Object[]{true};
        }
    }

}
