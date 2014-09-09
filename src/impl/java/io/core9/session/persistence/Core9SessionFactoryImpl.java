package io.core9.session.persistence;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionFactory;
import org.apache.shiro.session.mgt.SimpleSession;

public class Core9SessionFactoryImpl implements SessionFactory {
	
	private static Core9SessionFactoryImpl singleton;
	
	private Core9SessionFactoryImpl() {}
	
	public static Core9SessionFactoryImpl getInstance() {
		if(singleton == null) {
			singleton = new Core9SessionFactoryImpl();
		}
		return singleton;
	}

	@Override
	public Session createSession(SessionContext initData) {
		SimpleSession entity = new SimpleSession();
		if (initData != null) {
            String host = initData.getHost();
            if (host != null) {
                entity.setHost(host);
                return entity;
            }
        }
        return new SimpleSession();
	}

}
