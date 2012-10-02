package org.citisense.utils.mongodb;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

import com.mongodb.DB;
import com.mongodb.Mongo;

public class DBFactoryBean implements FactoryBean<DB> {

	private Mongo mongo;
	private String name;

	@Override
	public DB getObject() throws Exception {
		Assert.notNull(mongo);
		Assert.notNull(name);
		return mongo.getDB(name);
	}

	@Override
	public Class<?> getObjectType() {
		return DB.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public void setMongo(Mongo mongo) {
		this.mongo = mongo;
	}

	public void setName(String name) {
		this.name = name;
	}
}