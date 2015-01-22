package edu.stanford.bmir.facsimile.dbq.servlets;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class SessionListener implements HttpSessionListener, HttpSessionAttributeListener, HttpSessionBindingListener, HttpSessionActivationListener {

	public void valueBound(HttpSessionBindingEvent event) {
		System.out.println("[session] valueBound: " + event.getName() + "  (session: " + event.getSession().getId() + ")");
	}

	public void valueUnbound(HttpSessionBindingEvent event) {
		System.out.println("[session] valueUnbound: " + event.getName() + "  (session: " + event.getSession().getId() + ")");
	}

	public void attributeAdded(HttpSessionBindingEvent event) {
		System.out.println("[session] attributeAdded: " + event.getName() + "  (session: " + event.getSession().getId() + ")");
	}

	public void attributeRemoved(HttpSessionBindingEvent event) {
		System.out.println("[session] attributeRemoved: " + event.getName() + "  (session: " + event.getSession().getId() + ")");
	}

	public void attributeReplaced(HttpSessionBindingEvent event) {
		System.out.println("[session] attributeReplaced: " + event.getName() + "  (session: " + event.getSession().getId() + ")");
	}

	public void sessionCreated(HttpSessionEvent event) {
		System.out.println("[session] sessionCreated: " + event.getSession().getId());
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		System.out.println("[session] sessionDestroyed: " + event.getSession().getId());
	}

	public void sessionDidActivate(HttpSessionEvent event) {
		System.out.println("[session] sessionDidActivate: " + event.getSession().getId());
	}

	@Override
	public void sessionWillPassivate(HttpSessionEvent event) {
		System.out.println("[session] sessionWillPassivate: " + event.getSession().getId());
	}
}
