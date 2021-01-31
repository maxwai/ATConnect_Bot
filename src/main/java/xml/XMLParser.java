package xml;

import commands.Countdowns.CountdownsThread;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import javax.annotation.Nonnull;
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
	
	private static final String BOT_TOKEN_TAG = "BotToken";
	
	private static final String ROLES_TAG = "Roles";
	private static final String GUILD_TAG = "Guild";
	private static final String OWNER_TAG = "Owner";
	private static final String ADMIN_TAG = "Admin";
	private static final String EVENT_ORGANIZER_TAG = "Event_Organizer";
	private static final String INSTRUCTOR_TAG = "Instructor";
	private static final String TRAINED_TAG = "Trained";
	
	private static final String TIMEZONES_TAG = "Timezones";
	private static final String USER_TAG = "User";
	private static final String ID_ATTRIBUTE_TAG = "ID";
	
	private static final String TELEGRAM_TAG = "Telegram";
	private static final String TELEGRAM_MAIN_BOT_TAG = "MainBot";
	private static final String TELEGRAM_IMPORTANT_BOT_TAG = "ImportantBot";
	private static final String USER_CHANNEL_ID_TAG = "User_Channel_ID";
	private static final String USERNAME_ATTRIBUTE_TAG = "username";
	
	private static final String COUNTDOWNS_TAG = "Countdowns";
	private static final String COUNTDOWN_TAG = "Countdown";
	private static final String CHANNEL_ID_TAG = "Channel_ID";
	private static final String MESSAGE_ID_TAG = "Message_ID";
	private static final String EXTRA_TEXT_TAG = "Extra_Text";
	private static final String TIME_TAG = "Time";
	
	
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
	private static String readTextElement(Node node) {
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
	private static void xmlFormatException(String reason) {
		logger.error("XML was wrongly formatted: " + reason);
		throw new RuntimeException("XML was wrongly formatted: " + reason);
	}
	
}
