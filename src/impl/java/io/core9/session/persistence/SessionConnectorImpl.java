package io.core9.session.persistence;

import io.core9.module.auth.session.SessionConnector;
import io.core9.plugin.database.mongodb.MongoDatabase;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.injections.InjectPlugin;

import org.apache.shiro.session.mgt.SessionFactory;
import org.apache.shiro.session.mgt.eis.SessionDAO;

@PluginImplementation
public class SessionConnectorImpl implements SessionConnector {
	
	@InjectPlugin
	private MongoDatabase db;

	@Override
	public SessionDAO getSessionDAO() {
		return new DatabaseSessionDAO(db);
	}

	@Override
	public SessionFactory getSessionFactory() {
		return Core9SessionFactoryImpl.getInstance();
	}

}
