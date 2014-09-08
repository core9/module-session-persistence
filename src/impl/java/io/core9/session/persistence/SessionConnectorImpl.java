package io.core9.session.persistence;

import io.core9.module.auth.session.SessionConnector;
import io.core9.plugin.database.mongodb.MongoDatabase;
import io.core9.plugin.database.repository.CrudRepository;
import io.core9.plugin.database.repository.NoCollectionNamePresentException;
import io.core9.plugin.database.repository.RepositoryFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.PluginLoaded;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SessionFactory;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;

@PluginImplementation
public class SessionConnectorImpl extends AbstractSessionDAO implements SessionConnector, SessionDAO {
	
	private CrudRepository<SessionEntity> repository;
	private String masterDBName;
	private final String MASTER_DB_PREFIX = "";
	
	// TODO Remove mongo dependency (store sessions in virtualhost db)
	// How do we get the virtualhost?
	@PluginLoaded
	public void onDatabaseReady(MongoDatabase database) {
		masterDBName = database.getMasterDBName();
	}
		
	@PluginLoaded
	public void onRepositoryFactory(RepositoryFactory factory) throws NoCollectionNamePresentException {
		repository = factory.getRepository(SessionEntity.class);
	}

	@Override
	public SessionDAO getSessionDAO() {
		return this;
	}
	
	@Override
	public SessionFactory getSessionFactory() {
		return Core9SessionFactoryImpl.getInstance();
	}

	@Override
	public void update(Session session) throws UnknownSessionException {
		storeSession(session.getId(), session);
	}

	@Override
	public void delete(Session session) {
		if (session == null) {
            throw new NullPointerException("session argument cannot be null.");
        }
        Serializable id = session.getId();
        if (id != null) {
        	repository.delete(masterDBName, MASTER_DB_PREFIX, (SessionEntity) session);
        }
	}

	@Override
	public Collection<Session> getActiveSessions() {
		return new ArrayList<Session>(repository.getAll(masterDBName, MASTER_DB_PREFIX));
	}

	@Override
	protected Serializable doCreate(Session session) {
		Serializable sessionId = generateSessionId(session);
        ((SessionEntity) session).setId((String) sessionId);
        storeSession(sessionId, session);
        return sessionId;
	}

	protected void storeSession(Serializable sessionId, Session session) {
		if (sessionId == null) {
            throw new NullPointerException("id argument cannot be null.");
        }
		repository.upsert(masterDBName, MASTER_DB_PREFIX, (SessionEntity) session);
	}

	@Override
	protected Session doReadSession(Serializable sessionId) {
		try {
			Session session = repository.read(masterDBName, MASTER_DB_PREFIX, (String) sessionId);
			return session;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
