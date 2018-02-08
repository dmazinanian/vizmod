package ca.ubc.customelements.devtools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.webfolder.cdp.Launcher;
import io.webfolder.cdp.command.DOM;
import io.webfolder.cdp.command.Page;
import io.webfolder.cdp.session.Command;
import io.webfolder.cdp.session.Session;
import io.webfolder.cdp.session.SessionFactory;
import io.webfolder.cdp.type.dom.Node;

public class Driver {

	public Driver(String url) {
		Launcher launcher = new Launcher();
		List<String> params = Arrays.asList("--headless", "--disable-gpu");
		//List<String> params = new ArrayList<>();
		Session session = null;
		SessionFactory factory = null;
		try {
			factory = launcher.launch(params);
			session = factory.create();
			session.navigate(url);
			session.waitDocumentReady();
			Command command = session.getCommand();
			//Page page = command.getPage();
			//page.enable();
			//page.addScriptToEvaluateOnNewDocument(scriptSource)
			DOM dom = command.getDOM();
			dom.enable();
			Node document = dom.getDocument();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (session != null) {
				session.close();
				factory.close();
			}
		}
	}

	public static void main(String[] args) {
		String url = args[0];
		new Driver(url);
	}

}
