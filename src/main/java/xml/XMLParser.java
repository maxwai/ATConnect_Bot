package xml;

import commands.Countdowns.CountdownsThread;
import commands.event.EventInstance;
import commands.event.Location;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import telegram.TelegramLogger;

public class XMLParser {
	
	private static final TelegramLogger logger = TelegramLogger.getLogger("XMLParser");
	
	private static final String CONFIG_FILE_NAME = "Config.xml";
	
	// Bot Token
	private static final String BOT_TOKEN_TAG = "BotToken";
	// End Bot Token
	
	// Roles
	private static final String ROLES_TAG = "Roles";
	private static final String GUILD_TAG = "Guild"; // used by Event
	private static final String OWNER_TAG = "Owner";
	private static final String ADMIN_TAG = "Admin";
	private static final String EVENT_ORGANIZER_TAG = "Event_Organizer";
	private static final String INSTRUCTOR_TAG = "Instructor";
	private static final String TRAINED_TAG = "Trained";
	// End Roles
	
	// Timezones
	private static final String TIMEZONES_TAG = "Timezones";
	private static final String USER_TAG = "User"; // used by Event
	private static final String ID_ATTRIBUTE_TAG = "ID"; // used by Event
	// End Timezones
	
	// Telegram
	private static final String TELEGRAM_TAG = "Telegram";
	private static final String TELEGRAM_MAIN_BOT_TAG = "MainBot";
	private static final String TELEGRAM_IMPORTANT_BOT_TAG = "ImportantBot";
	private static final String USER_CHANNEL_ID_TAG = "User_Channel_ID";
	private static final String USERNAME_ATTRIBUTE_TAG = "username";
	// End Telegram
	
	// Countdown
	private static final String COUNTDOWNS_TAG = "Countdowns";
	private static final String COUNTDOWN_TAG = "Countdown";
	private static final String CHANNEL_ID_TAG = "Channel_ID";
	private static final String MESSAGE_ID_TAG = "Message_ID";
	private static final String EXTRA_TEXT_TAG = "Extra_Text";
	private static final String TIME_TAG = "Time";
	// End Countdown
	
	// Event
	private static final String EVENTS_TAG = "Events";
	
	//		Event Group
	private static final String EVENT_GROUP_TAG = "Event_Group";
	// private static final String ID_ATTRIBUTE_TAG = "ID";
	private static final String ACTIVE_EVENT_TAG = "Active";
	private static final String EVENT_TAG = "Event";
	// private static final String GUILD_TAG = "Guild";
	private static final String TITLE_TAG = "Title";
	private static final String DESCRIPTION_TAG = "Description";
	
	//			Times
	private static final String TIMES_TAG = "Times";
	private static final String EVENT_DATE_TAG = "EventDate";
	private static final String START_TIME_TAG = "StartTime";
	private static final String STOP_TIME_TAG = "StopTime";
	//			End Times
	
	//			Toggles
	private static final String TOGGLES_TAG = "Toggles";
	private static final String MAYBE_TOGGLE_TAG = "Maybe";
	private static final String BACKUP_TOGGLE_TAG = "Backup";
	private static final String VOTE_TOGGLE_TAG = "Vote";
	//			End Toggles
	
	//			Messages
	private static final String MESSAGE_TAG = "Messages";
	private static final String CHANNEL_ID_ATTRIBUTE_TAG = "channel";
	private static final String COMMAND_MESSAGE_TAG = "CommandMessage";
	private static final String PRIVATE_EMBED_MESSAGE_TAG = "PrivateEmbedMessage";
	private static final String EVENT_EMBED_TAG = "EventEmbed";
	//			End Messages
	
	private static final String MAYBE_USERS_TAG = "MaybeUsers";
	private static final String BACKUP_USERS_TAG = "BackupUsers";
	// private static final String USER_TAG = "User";
	
	//			Location
	private static final String LOCATION_TAG = "Location";
	private static final String LOCATION_NAME_ATTRIBUTE_TAG = "name";
	private static final String POSITION_TAG = "Position";
	// private static final String USER_TAG = "User";
	private static final String USER_POSITION_ATTRIBUTE = "position";
	//			End Location
	
	// 		End Event Group
	
	// End Event
	
	
	private static void saveDummyDocument(File file) {
		try {
			PrintWriter writer = new PrintWriter(file);
			writer.write("""
					<?xml version="1.0" encoding="UTF-8" standalone="no"?>
					<root>
					  <BotToken><!--Put here your Bot Token--></BotToken>
					  <Roles>
					    <Guild><!--Put here the Guild ID--></Guild>
					    <Owner><!--Put here the User ID of the Bot Owner--></Owner>
					    <Admin><!--Put here the Role ID of the Admins--></Admin>
					    <Event_Organizer><!--Put here the Role ID of the Event Organizers--></Event_Organizer>
					    <Instructor><!--Put here the Role ID of the Instructors--></Instructor>
					    <Trained><!--Put here the Role ID of the Trained Role--></Trained>
					  </Roles>
					  <Telegram> <!--Remove this Tag if you don't want a Telegram Bot to send you Logs-->
					    <MainBot username=""><!--Put here the Main Telegram Bot Username-->
					      <!--Put here the Main Telegram Bot Token-->
					    </MainBot>
					    <ImportantBot username=""><!--Put here the Important Telegram Bot Username-->
					      <!--Put here the Important telegram Bot Token-->
					    </ImportantBot>
					    <User_Channel_ID><!--Put here the Channel ID of the Telegram User--></User_Channel_ID>
					  </Telegram>
					</root>""");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Will get the Config.xml or, if not present, create a dummy one and exit
	 *
	 * @return The Document
	 */
	private static Document getDocument() {
		try {
			File inputFile = new File(CONFIG_FILE_NAME);
			if (inputFile.createNewFile()) {
				saveDummyDocument(inputFile);
				logger.error("There was no " + CONFIG_FILE_NAME
						+ " available. Created a dummy one. Please fill it out");
				System.exit(1);
			}
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile);
		} catch (ParserConfigurationException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (SAXException e) {
			xmlFormatException("something went wrong while parsing the xml");
		}
		return null; // will never get there
	}
	
	/**
	 * Will write the new XML file to Config.xml
	 *
	 * @param doc The document
	 */
	private static void writeDocument(Document doc) {
		try {
			// remove all '\n' and ' '
			XPathFactory xfact = XPathFactory.newInstance();
			XPath xpath = xfact.newXPath();
			NodeList empty = (NodeList) xpath.evaluate("//text()[normalize-space(.) = '']",
					doc, XPathConstants.NODESET);
			for (int i = 0; i < empty.getLength(); i++) {
				Node node = empty.item(i);
				node.getParentNode().removeChild(node);
			}
			
			// pretty print the xml
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			
			// save the xml
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(CONFIG_FILE_NAME));
			transformer.transform(source, result);
			logger.info("Saved the Config.xml");
		} catch (TransformerException | XPathExpressionException e) {
			logger.error(
					"Could not save correctly the XML File. See stacktrace for more information");
			e.printStackTrace();
		}
	}
	
	/**
	 * Will retrieve the Discord Bot Token
	 *
	 * @return The Token
	 */
	@Nonnull
	public static String getBotToken() {
		NodeList nList = getDocument().getElementsByTagName(BOT_TOKEN_TAG);
		if (nList.getLength() == 1) {
			logger.info("Getting the Bot Token");
			return readTextElement(nList.item(0));
		} else
			xmlFormatException("multiple or no Bot Token Tags");
		//noinspection ConstantConditions
		return null; // will never go there
	}
	
	/**
	 * Will retrieve the Role Information
	 *
	 * @return A Map with all the available Roles
	 */
	@Nonnull
	public static Map<String, Long> getRoles() {
		NodeList nList = getDocument().getElementsByTagName(ROLES_TAG);
		if (nList.getLength() == 1) {
			try {
				Map<String, Long> output = new HashMap<>();
				Element element = (Element) nList.item(0);
				output.put(GUILD_TAG, Long.parseLong(readTextElement(element
						.getElementsByTagName(GUILD_TAG).item(0))));
				output.put(OWNER_TAG, Long.parseLong(readTextElement(element
						.getElementsByTagName(OWNER_TAG).item(0))));
				output.put(ADMIN_TAG, Long.parseLong(readTextElement(element
						.getElementsByTagName(ADMIN_TAG).item(0))));
				output.put(EVENT_ORGANIZER_TAG, Long.parseLong(readTextElement(element
						.getElementsByTagName(EVENT_ORGANIZER_TAG).item(0))));
				output.put(INSTRUCTOR_TAG, Long.parseLong(readTextElement(element
						.getElementsByTagName(INSTRUCTOR_TAG).item(0))));
				output.put(TRAINED_TAG, Long.parseLong(readTextElement(element
						.getElementsByTagName(TRAINED_TAG).item(0))));
				logger.info("Getting the Roles");
				return output;
			} catch (NullPointerException e) {
				xmlFormatException("Tag missing in Roles");
			} catch (NumberFormatException e) {
				xmlFormatException("Role ID could not be parsed to Long");
			}
		} else
			xmlFormatException("multiple or no Role Tags");
		//noinspection ConstantConditions
		return null; // will never go there
	}
	
	/**
	 * Will retrieve the saved Timezones
	 *
	 * @return A HashMap of all saved Timezones. Can be empty
	 */
	public static Map<Long, String> getTimezones() {
		NodeList nList = getDocument().getElementsByTagName(TIMEZONES_TAG);
		Map<Long, String> timezones = new HashMap<>();
		if (nList.getLength() == 1) {
			try {
				Element element = (Element) nList.item(0);
				NodeList timezoneList = element.getElementsByTagName(USER_TAG);
				for (int i = 0; i < timezoneList.getLength(); i++) {
					Element timezone = (Element) timezoneList.item(i);
					timezones.put(Long.parseLong(timezone.getAttribute(ID_ATTRIBUTE_TAG)),
							readTextElement(timezone));
				}
				logger.info("Getting saved Timezones");
			} catch (NumberFormatException e) {
				xmlFormatException("Role ID could not be parsed to Long");
			}
		} else
			logger.info("Not timezones to get");
		return timezones;
	}
	
	/**
	 * Will save the given Timezones
	 *
	 * @param timezones The Timezones to save
	 */
	public static void saveTimezones(Map<Long, String> timezones) {
		Document doc = getDocument();
		NodeList timezonesNodeList = doc.getElementsByTagName(TIMEZONES_TAG);
		
		Node timezonesNode;
		// empty the timezone Node or create a new one if not present
		if (timezonesNodeList.getLength() == 0) {
			timezonesNode = doc.createElement(TIMEZONES_TAG);
			doc.getFirstChild().appendChild(timezonesNode);
		} else
			timezonesNode = timezonesNodeList.item(0);
		
		clearNode(timezonesNode);
		timezones.forEach((user, timezone) -> {
			Element timezoneNode = doc.createElement(USER_TAG);
			
			Attr attribute = doc.createAttribute(ID_ATTRIBUTE_TAG);
			attribute.setValue(user.toString());
			timezoneNode.setAttributeNode(attribute);
			
			timezoneNode.appendChild(doc.createTextNode(timezone));
			
			timezonesNode.appendChild(timezoneNode);
		});
		
		logger.info("Saving the Timezones");
		writeDocument(doc);
	}
	
	/**
	 * Will get the Information for the Telegram Bots if available
	 *
	 * @return The Information for the Bots or {@code null} if not available
	 */
	public static ArrayList<String[]> getTelegramBots() {
		NodeList nList = getDocument().getElementsByTagName(TELEGRAM_TAG);
		if (nList.getLength() == 1) {
			try {
				ArrayList<String[]> bots = new ArrayList<>();
				Element element = (Element) nList.item(0);
				
				// get the Main Bot Information
				Element MainBotTag = (Element) element.getElementsByTagName(TELEGRAM_MAIN_BOT_TAG)
						.item(0);
				bots.add(new String[]{MainBotTag.getAttribute(USERNAME_ATTRIBUTE_TAG),
						readTextElement(MainBotTag)});
				
				// get the Important Bot Information
				Element ImportantBotTag = (Element) element
						.getElementsByTagName(TELEGRAM_IMPORTANT_BOT_TAG).item(0);
				bots.add(new String[]{ImportantBotTag.getAttribute(USERNAME_ATTRIBUTE_TAG),
						readTextElement(ImportantBotTag)});
				
				// get the user channel ID
				bots.add(new String[]{readTextElement(
						element.getElementsByTagName(USER_CHANNEL_ID_TAG).item(0))});
				
				logger.info("Got the Telegram Bot information");
				return bots;
			} catch (NullPointerException e) {
				xmlFormatException("Tag missing in Telegram Node");
			}
		} else if (nList.getLength() == 0)
			logger.warn("No Telegram Bot information. You won't get the Telegram Logs");
		else
			xmlFormatException("multiple Telegram Tags");
		return null;
	}
	
	/**
	 * Will get the saved Countdowns
	 *
	 * @return An Arraylist with the Countdowns that were saved. Can be empty
	 */
	public static ArrayList<String[]> getCountdowns() {
		NodeList nList = getDocument().getElementsByTagName(COUNTDOWNS_TAG);
		ArrayList<String[]> output = new ArrayList<>();
		if (nList.getLength() == 1) {
			try {
				Element element = (Element) nList.item(0);
				NodeList countdownList = element.getElementsByTagName(COUNTDOWN_TAG);
				for (int i = 0; i < countdownList.getLength(); i++) {
					Element countdown = (Element) countdownList.item(i);
					output.add(new String[]{
							readTextElement(countdown.getElementsByTagName(CHANNEL_ID_TAG).item(0)),
							readTextElement(countdown.getElementsByTagName(MESSAGE_ID_TAG).item(0)),
							readTextElement(countdown.getElementsByTagName(EXTRA_TEXT_TAG).item(0)),
							readTextElement(countdown.getElementsByTagName(TIME_TAG).item(0))});
				}
				logger.info("Got the saved Countdowns");
			} catch (NullPointerException e) {
				xmlFormatException("Tag missing in Countdown");
			}
		} else
			logger.info("Not Countdowns to get");
		return output;
	}
	
	/**
	 * Will save the given Countdowns
	 *
	 * @param countdowns The Countdowns to save
	 */
	public static void saveCountdowns(Stack<CountdownsThread> countdowns) {
		Document doc = getDocument();
		NodeList countdownsNodeList = doc.getElementsByTagName(COUNTDOWNS_TAG);
		Node countdownsNode;
		if (countdownsNodeList.getLength() == 0) {
			countdownsNode = doc.createElement(COUNTDOWNS_TAG);
			doc.getFirstChild().appendChild(countdownsNode);
		} else
			countdownsNode = countdownsNodeList.item(0);
		
		clearNode(countdownsNode);
		countdowns.forEach(countdownsThread -> {
			String[] infos = countdownsThread.getInfos();
			Node countdownNode = doc.createElement(COUNTDOWN_TAG);
			
			Node child = doc.createElement(CHANNEL_ID_TAG);
			child.appendChild(doc.createTextNode(infos[0]));
			countdownNode.appendChild(child);
			
			child = doc.createElement(MESSAGE_ID_TAG);
			child.appendChild(doc.createTextNode(infos[1]));
			countdownNode.appendChild(child);
			
			child = doc.createElement(EXTRA_TEXT_TAG);
			child.appendChild(doc.createTextNode(infos[2]));
			countdownNode.appendChild(child);
			
			child = doc.createElement(TIME_TAG);
			child.appendChild(doc.createTextNode(infos[3]));
			countdownNode.appendChild(child);
			
			countdownsNode.appendChild(countdownNode);
		});
		
		logger.info("Saving the Countdowns");
		writeDocument(doc);
	}
	
	/**
	 * Will get all saved Events
	 *
	 * @return The HashMap with all Events
	 */
	public static HashMap<Long, List<Object>> getEvents(JDA jda) {
		HashMap<Long, List<Object>> eventsMap = new HashMap<>();
		
		NodeList nList = getDocument().getElementsByTagName(EVENTS_TAG);
		
		if (nList.getLength() == 1) {
			try {
				Element element = (Element) nList.item(0);
				NodeList eventGroupList = element.getElementsByTagName(EVENT_GROUP_TAG);
				for (int i = 0; i < eventGroupList.getLength(); i++) {
					Element eventGroup = (Element) eventGroupList.item(i);
					
					ArrayList<Object> eventList = new ArrayList<>();
					eventList.add(1); // setup a temporary active index
					long userID = Long.parseLong(eventGroup.getAttribute(ID_ATTRIBUTE_TAG));
					
					NodeList eventGroupChildren = eventGroup.getElementsByTagName("*");
					for (int j = 0; j < eventGroupChildren.getLength(); j++) {
						Element eventGroupChild = (Element) eventGroupChildren.item(j);
						if (eventGroupChild.getParentNode().equals(eventGroup)) {
							logger.info(eventGroupChild.getNodeName());
							switch (eventGroupChild.getNodeName()) {
								case ACTIVE_EVENT_TAG -> eventList.set(0, Integer
										.parseInt(readTextElement(eventGroupChild)));
								case EVENT_TAG -> eventList.add(getEvent(jda, eventGroupChild));
							}
						}
					}
					eventsMap.put(userID, eventList);
				}
			} catch (NumberFormatException e) {
				logger.error("Could not retrieve all event. Number Format Exception");
				System.exit(1);
			}
			
		} else
			logger.info("Not Events to get");
		
		return eventsMap;
	}
	
	/**
	 * Returns one instance of an Event
	 *
	 * @param jda The JDA to create an {@link EventInstance}
	 * @param event The Event Element to parse
	 *
	 * @return The corresponding {@link EventInstance}
	 *
	 * @throws NumberFormatException If an ID could not be parsed to a Long
	 */
	private static EventInstance getEvent(JDA jda, Element event) throws NumberFormatException {
		Guild guild = null;
		String title = null;
		String desc = null;
		
		TemporalAccessor startTime = null;
		TemporalAccessor stopTime = null;
		boolean eventDateSet = false;
		boolean startTimeSet = false;
		boolean stopTimeSet = false;
		
		boolean maybeToggle = false;
		boolean backupToggle = false;
		boolean vote = false;
		
		AtomicReference<Message> commandsMessage = new AtomicReference<>();
		AtomicReference<Message> eventPrivateEmbedMessage = new AtomicReference<>();
		AtomicReference<Message> eventEmbedMessage = new AtomicReference<>();
		MessageChannel eventEmbedMessageChannel = null;
		
		ArrayList<Long> maybeUsers = new ArrayList<>();
		ArrayList<Long> backupUsers = new ArrayList<>();
		
		ArrayList<Location> locations = new ArrayList<>();
		
		NodeList eventTagChildren = event.getElementsByTagName("*");
		for (int i = 0; i < eventTagChildren.getLength(); i++) {
			Element eventChild = (Element) eventTagChildren.item(i);
			if (eventChild.getParentNode().equals(event)) {
				switch (eventChild.getNodeName()) {
					case GUILD_TAG -> {
						guild = jda.getGuildById(readTextElement(eventChild));
						if (guild == null) {
							logger.error("Could not connect to specified guild");
							System.exit(1);
						}
					}
					case TITLE_TAG -> title = readTextElement(eventChild);
					case DESCRIPTION_TAG -> desc = readTextElement(eventChild);
					case TIMES_TAG -> {
						NodeList timeChildren = eventChild.getElementsByTagName("*");
						for (int j = 0; j < timeChildren.getLength(); j++) {
							Element timeChild = (Element) timeChildren.item(j);
							String date = readTextElement(timeChild);
							switch (timeChild.getNodeName()) {
								case EVENT_DATE_TAG -> {
									eventDateSet = !date.equals(EventInstance.notSet);
									if (eventDateSet) {
										String timeStart;
										// check if we have a start Time, if not default to 00:00
										if (startTime != null) {
											timeStart = EventInstance.sdfTime.format(startTime);
										} else {
											timeStart = "00:00";
										}
										String timeStop;
										// check if we have a end Time, if not default to 00:00
										if (stopTime != null) {
											timeStop = EventInstance.sdfTime.format(stopTime);
										} else {
											timeStop = "00:00";
										}
										
										startTime = EventInstance.sdfComplete
												.parse(date + " " + timeStart);
										stopTime = EventInstance.sdfComplete
												.parse(date + " " + timeStop);
									}
								}
								case START_TIME_TAG -> {
									startTimeSet = !date.equals(EventInstance.notSet);
									if (startTimeSet) {
										startTime = getTime(date, startTime);
									}
								}
								case STOP_TIME_TAG -> {
									stopTimeSet = !date.equals(EventInstance.notSet);
									if (stopTimeSet) {
										stopTime = getTime(date, stopTime);
									}
								}
							}
						}
					}
					case TOGGLES_TAG -> {
						NodeList toggleChildren = eventChild.getElementsByTagName("*");
						for (int j = 0; j < toggleChildren.getLength(); j++) {
							Element toggleChild = (Element) toggleChildren.item(j);
							boolean toggle = Boolean.parseBoolean(readTextElement(toggleChild));
							switch (toggleChild.getNodeName()) {
								case MAYBE_TOGGLE_TAG -> maybeToggle = toggle;
								case BACKUP_TOGGLE_TAG -> backupToggle = toggle;
								case VOTE_TOGGLE_TAG -> vote = toggle;
							}
						}
					}
					case MESSAGE_TAG -> {
						NodeList messageChildren = eventChild.getElementsByTagName("*");
						for (int j = 0; j < messageChildren.getLength(); j++) {
							Element messageChild = (Element) messageChildren.item(j);
							long channelID = Long
									.parseLong(messageChild.getAttribute(CHANNEL_ID_ATTRIBUTE_TAG));
							long messageID = Long.parseLong(readTextElement(messageChild));
							switch (messageChild.getNodeName()) {
								case COMMAND_MESSAGE_TAG -> jda.openPrivateChannelById(channelID)
										.queue(privateChannel -> {
											if (privateChannel != null) {
												privateChannel.retrieveMessageById(messageID)
														.queue(message -> {
															synchronized (commandsMessage) {
																commandsMessage.set(message);
																commandsMessage.notify();
															}
														});
											} else {
												logger.error(
														"Could not find specified Private Channel");
												System.exit(1);
											}
										});
								case PRIVATE_EMBED_MESSAGE_TAG -> jda
										.openPrivateChannelById(channelID)
										.queue(privateChannel -> {
											if (privateChannel != null) {
												privateChannel.retrieveMessageById(messageID)
														.queue(message -> {
															synchronized (eventPrivateEmbedMessage) {
																eventPrivateEmbedMessage
																		.set(message);
																eventPrivateEmbedMessage.notify();
															}
														});
											} else {
												logger.error(
														"Could not find specified Private Channel");
												System.exit(1);
											}
										});
								case EVENT_EMBED_TAG -> {
									eventEmbedMessageChannel = jda.getTextChannelById(channelID);
									if (eventEmbedMessageChannel != null) {
										eventEmbedMessageChannel.retrieveMessageById(messageID)
												.queue(message -> {
													synchronized (eventEmbedMessage) {
														eventEmbedMessage.set(message);
														eventEmbedMessage.notify();
													}
												});
									} else {
										logger.error("Could not find specified Guild Channel");
										System.exit(1);
									}
								}
							}
						}
					}
					case MAYBE_USERS_TAG -> {
						NodeList maybeUserChildren = eventChild.getElementsByTagName("*");
						for (int j = 0; j < maybeUserChildren.getLength(); j++) {
							Element maybeUserChild = (Element) maybeUserChildren.item(j);
							if (maybeUserChild.getNodeName().equals(USER_TAG))
								maybeUsers.add(Long.parseLong(readTextElement(maybeUserChild)));
						}
					}
					case BACKUP_USERS_TAG -> {
						NodeList backupUserChildren = eventChild.getElementsByTagName("*");
						for (int j = 0; j < backupUserChildren.getLength(); j++) {
							Element backupUserChild = (Element) backupUserChildren.item(j);
							if (backupUserChild.getNodeName().equals(USER_TAG))
								backupUsers.add(Long.parseLong(readTextElement(backupUserChild)));
						}
					}
					case LOCATION_TAG -> locations.add(getLocation(eventChild));
				}
			}
		}
		synchronized (commandsMessage) {
			if(commandsMessage.get() == null) {
				try {
					commandsMessage.wait();
				} catch (InterruptedException ignored) {
				}
			}
		}
		synchronized (eventPrivateEmbedMessage) {
			if(eventPrivateEmbedMessage.get() == null) {
				try {
					eventPrivateEmbedMessage.wait();
				} catch (InterruptedException ignored) {
				}
			}
		}
		synchronized (eventEmbedMessage) {
			if(eventEmbedMessage.get() == null) {
				try {
					eventEmbedMessage.wait();
				} catch (InterruptedException ignored) {
				}
			}
		}
		if (guild == null || title == null || desc == null || eventEmbedMessageChannel == null ||
				commandsMessage.get() == null || eventPrivateEmbedMessage.get() == null ||
				eventEmbedMessage.get() == null) {
			logger.error("Something went wrong while parsing an event");
			logger.error("" + (guild == null) + (title == null) + (desc == null) + (
					eventEmbedMessageChannel == null) +
					(commandsMessage.get() == null) + (eventPrivateEmbedMessage.get() == null) +
					(eventEmbedMessage.get() == null));
			throw new IllegalArgumentException("Element was null when it shouldn't");
		}
		return new EventInstance(guild, title, desc, startTime, stopTime, eventDateSet,
				startTimeSet, stopTimeSet, maybeToggle, backupToggle, vote, commandsMessage.get(),
				eventPrivateEmbedMessage.get(), eventEmbedMessage.get(), eventEmbedMessageChannel,
				maybeUsers,
				backupUsers, locations);
	}
	
	/**
	 * Will get the Time from the String
	 *
	 * @param timeInput The time representation
	 * @param time The Time Instance where the Date should be kept. Can be null
	 *
	 * @return The new Time Instance
	 */
	private static TemporalAccessor getTime(@Nonnull String timeInput,
			@Nullable TemporalAccessor time) {
		try {
			String date = (time != null ? EventInstance.sdfDate.format(time)
					: EventInstance.sdfDate.format(Instant.now())) + " ";
			time = EventInstance.sdfComplete.parse(date + timeInput);
		} catch (DateTimeException | NumberFormatException e) {
			logger.error("Time is not in the wanted Format");
		}
		return time;
	}
	
	/**
	 * Retrieves one Location Instance
	 *
	 * @param location The Location Element to be parsed
	 *
	 * @return The {@link Location} Instance
	 *
	 * @throws NumberFormatException if the User Tag was not parsable to a Long
	 */
	private static Location getLocation(Element location) throws NumberFormatException {
		String locationName = location.getAttribute(LOCATION_NAME_ATTRIBUTE_TAG);
		ArrayList<String> positions = new ArrayList<>();
		ArrayList<Long> users = new ArrayList<>();
		ArrayList<String> userPositions = new ArrayList<>();
		
		NodeList locationChildren = location.getElementsByTagName("*");
		for (int i = 0; i < locationChildren.getLength(); i++) {
			Element locationChild = (Element) locationChildren.item(i);
			switch (locationChild.getNodeName()) {
				case POSITION_TAG -> positions.add(readTextElement(locationChild));
				case USER_TAG -> {
					users.add(Long.parseLong(readTextElement(locationChild)));
					userPositions.add(locationChild.getAttribute(USER_POSITION_ATTRIBUTE));
				}
			}
		}
		if (locationName == null || locationName.equals("")) {
			logger.error("Something went wrong while parsing an Location");
			return null;
		}
		return new Location(locationName, positions, users, userPositions);
	}
	
	/**
	 * Will save the given Events
	 *
	 * @param eventsMap The Events to save
	 */
	public static void saveEvents(HashMap<Long, List<Object>> eventsMap) {
		Document doc = getDocument();
		NodeList eventsNodeList = doc.getElementsByTagName(EVENTS_TAG);
		Node eventsNode;
		if (eventsNodeList.getLength() == 0) {
			eventsNode = doc.createElement(EVENTS_TAG);
			doc.getFirstChild().appendChild(eventsNode);
		} else
			eventsNode = eventsNodeList.item(0);
		
		clearNode(eventsNode);
		
		eventsMap.forEach((authorID, list) -> {
			Element eventGroupNode = doc.createElement(EVENT_GROUP_TAG);
			
			// User ID of Event Group
			eventGroupNode.setAttribute(ID_ATTRIBUTE_TAG, authorID.toString());
			
			// Active Event Index
			Node groupChild = doc.createElement(ACTIVE_EVENT_TAG);
			groupChild.appendChild(doc.createTextNode(((Integer) list.get(0)).toString()));
			eventGroupNode.appendChild(groupChild);
			
			for (int i = 1; i < list.size(); i++) {
				groupChild = doc.createElement(EVENT_TAG);
				EventInstance eventInstance = (EventInstance) list.get(i);
				
				// Guild ID
				Node guild = doc.createElement(GUILD_TAG);
				guild.appendChild(doc.createTextNode(eventInstance.guild.getId()));
				groupChild.appendChild(guild);
				
				// Title
				Node title = doc.createElement(TITLE_TAG);
				title.appendChild(doc.createTextNode(eventInstance.getTitle()));
				groupChild.appendChild(title);
				
				// Description
				Node desc = doc.createElement(DESCRIPTION_TAG);
				desc.appendChild(doc.createTextNode(eventInstance.getDescription()));
				groupChild.appendChild(desc);
				
				// Times
				Node times = doc.createElement(TIMES_TAG);
				
				Node eventDate = doc.createElement(EVENT_DATE_TAG);
				eventDate.appendChild(doc.createTextNode(eventInstance.getEventDate()));
				times.appendChild(eventDate);
				
				Node startTime = doc.createElement(START_TIME_TAG);
				startTime.appendChild(doc.createTextNode(eventInstance.getStartTime()));
				times.appendChild(startTime);
				
				Node endTime = doc.createElement(STOP_TIME_TAG);
				endTime.appendChild(doc.createTextNode(eventInstance.getStopTime()));
				times.appendChild(endTime);
				
				groupChild.appendChild(times);
				
				// Toggles
				Node toggles = doc.createElement(TOGGLES_TAG);
				
				Node maybeToggle = doc.createElement(MAYBE_TOGGLE_TAG);
				maybeToggle.appendChild(doc.createTextNode("" + eventInstance.maybeToggle));
				toggles.appendChild(maybeToggle);
				
				Node backupToggle = doc.createElement(BACKUP_TOGGLE_TAG);
				backupToggle.appendChild(doc.createTextNode("" + eventInstance.backupToggle));
				toggles.appendChild(backupToggle);
				
				Node voteToggle = doc.createElement(VOTE_TOGGLE_TAG);
				voteToggle.appendChild(doc.createTextNode("" + eventInstance.vote));
				toggles.appendChild(voteToggle);
				
				groupChild.appendChild(toggles);
				
				// Messages
				Node messages = doc.createElement(MESSAGE_TAG);
				
				Element commandMessage = doc.createElement(COMMAND_MESSAGE_TAG);
				commandMessage.setAttribute(CHANNEL_ID_ATTRIBUTE_TAG,
						((PrivateChannel) eventInstance.commandsMessage.getChannel()).getUser()
								.getId());
				commandMessage
						.appendChild(doc.createTextNode(eventInstance.commandsMessage.getId()));
				messages.appendChild(commandMessage);
				
				Element privateEmbedMessage = doc.createElement(PRIVATE_EMBED_MESSAGE_TAG);
				privateEmbedMessage.setAttribute(CHANNEL_ID_ATTRIBUTE_TAG,
						((PrivateChannel) eventInstance.eventPrivateEmbedMessage.getChannel())
								.getUser().getId());
				privateEmbedMessage.appendChild(
						doc.createTextNode(eventInstance.eventPrivateEmbedMessage.getId()));
				messages.appendChild(privateEmbedMessage);
				
				Element eventEmbedMessage = doc.createElement(EVENT_EMBED_TAG);
				eventEmbedMessage.setAttribute(CHANNEL_ID_ATTRIBUTE_TAG,
						eventInstance.eventEmbedMessage.getChannel().getId());
				eventEmbedMessage
						.appendChild(doc.createTextNode(eventInstance.eventEmbedMessage.getId()));
				messages.appendChild(eventEmbedMessage);
				
				groupChild.appendChild(messages);
				
				// maybe Users
				Node maybeUsers = doc.createElement(MAYBE_USERS_TAG);
				
				for (Long user : eventInstance.maybeUsers) {
					Node userNode = doc.createElement(USER_TAG);
					userNode.appendChild(doc.createTextNode("" + user));
					maybeUsers.appendChild(userNode);
				}
				
				groupChild.appendChild(maybeUsers);
				
				// backup Users
				Node backupUsers = doc.createElement(BACKUP_USERS_TAG);
				
				for (Long user : eventInstance.backupUsers) {
					Node userNode = doc.createElement(USER_TAG);
					userNode.appendChild(doc.createTextNode("" + user));
					backupUsers.appendChild(userNode);
				}
				
				groupChild.appendChild(backupUsers);
				
				// Locations
				for (Location location : eventInstance.locations) {
					Element locationElement = doc.createElement(LOCATION_TAG);
					locationElement.setAttribute(LOCATION_NAME_ATTRIBUTE_TAG, location.location);
					
					for (String position : location.positions) {
						Node positionNode = doc.createElement(POSITION_TAG);
						positionNode.appendChild(doc.createTextNode(position));
						locationElement.appendChild(positionNode);
					}
					
					for (int j = 0; j < location.users.size(); j++) {
						Element user = doc.createElement(USER_TAG);
						user.setAttribute(USER_POSITION_ATTRIBUTE, location.userPositions.get(j));
						user.appendChild(doc.createTextNode("" + location.users.get(j)));
						locationElement.appendChild(user);
					}
					
					groupChild.appendChild(locationElement);
				}
				
				eventGroupNode.appendChild(groupChild);
			}
			
			eventsNode.appendChild(eventGroupNode);
		});
		
		logger.info("Saving the Events");
		writeDocument(doc);
	}
	
	/*
	 <Events>
    <Event_Group ID="1354687613">
      <active>1</active>
      <Event>
        <Guild>13548</Guild>
        <Title>Title</Title>
        <Description>Descriptions</Description>
        <Times>
          <EventDate>eventDate</EventDate>
          <StartTime>startTime</StartTime>
          <StopTime>stopTime</StopTime>
        </Times>
        <Toggles>
          <Maybe>true</Maybe>
          <Backup>true</Backup>
          <Vote>false</Vote>
        </Toggles>
        <Messages>
          <CommandMessage channel="135468">13548</CommandMessage>
          <PrivateEmbedMessage channel="5464654">4654687</PrivateEmbedMessage>
          <EventEmbed channel="515646">5654684</EventEmbed>
        </Messages>
        <MaybeUsers>
          <User>13546</User>
          <User>13546</User>
        </MaybeUsers>
        <BackupUsers>
          <User>13546</User>
          <User>13546</User>
        </BackupUsers>
        <Location name="location">
          <Position>Test</Position>
          <Position>Test</Position>
          <User position="position reaction">13548</User>
        </Location>
        <Location name="location">
          <Position>Test</Position>
          <Position>Test</Position>
          <User position="position reaction">13548</User>
        </Location>
      </Event>
    </Event_Group>
  </Events>
	 */
	
	/**
	 * Will clear a node of all it's child's
	 *
	 * @param node The node to clear
	 */
	private static void clearNode(Node node) {
		while (node.hasChildNodes())
			node.removeChild(node.getFirstChild());
	}
	
	/**
	 * Will trim all '\n' and ' ' at the beginning and end of the Text Element
	 *
	 * @param node The Node were the Text Element is
	 *
	 * @return A String striped of it's unnecessary '\n' and ' '
	 */
	private static String readTextElement(@Nonnull Node node) {
		String text = node.getTextContent();
		if (text == null || text.equals(""))
			return "";
		while (text.charAt(0) == '\n' || text.charAt(0) == ' ') {
			text = text.substring(1);
		}
		while (text.charAt(text.length() - 1) == '\n' || text.charAt(text.length() - 1) == ' ') {
			text = text.substring(0, text.length() - 1);
		}
		return text;
	}
	
	/**
	 * Will output a Error Log and throw a Runtime Exception
	 *
	 * @param reason The Message that should be in the Log
	 */
	private static void xmlFormatException(@Nonnull String reason) {
		logger.error("XML was wrongly formatted: " + reason);
		throw new RuntimeException("XML was wrongly formatted: " + reason);
	}
	
}
