/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2013 Flury Trading - All rights reserved
 *
 * All information contained herein is, and remains the property of Flury Trading.
 * The intellectual and technical concepts contained herein are proprietary to
 * Flury Trading. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from Flury Trading
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * Flury Trading
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algorader.cache;

import java.util.Properties;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerAdapter;

import org.hibernate.cache.ReadWriteCache;
import org.hibernate.cache.entry.CacheEntry;
import org.hibernate.cache.entry.CollectionCacheEntry;

import com.algoTrader.ServiceLocator;

/**
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class EntityCacheEventListenerFactory extends net.sf.ehcache.event.CacheEventListenerFactory {

    @Override
    public CacheEventListener createCacheEventListener(Properties properties) {

        return new CacheEventListenerAdapter() {

            @Override
            public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {

                org.hibernate.cache.CacheKey hibernateCacheKey = (org.hibernate.cache.CacheKey) element.getKey();

                if (element.getValue() instanceof ReadWriteCache.Item) {

                    ReadWriteCache.Item item = (ReadWriteCache.Item) element.getValue();

                    if (item.getValue() instanceof CacheEntry) {
                        updateEntity(hibernateCacheKey);
                    } else if (item.getValue() instanceof CollectionCacheEntry) {
                        updateCollection(hibernateCacheKey);
                    }

                } else if (element.getValue() instanceof ReadWriteCache.Lock) {

                    ReadWriteCache.Lock lock = (ReadWriteCache.Lock) element.getValue();

                    // only process locks when they have been unlocked
                    if (lock.getUnlockTimestamp() != -1) {

                        String entityOrRoleName = hibernateCacheKey.getEntityOrRoleName();
                        if (entityOrRoleName.endsWith("Impl")) {
                            updateEntity(hibernateCacheKey);
                        } else {
                            updateCollection(hibernateCacheKey);
                        }
                    }
                }
            }

            @Override
            public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {

                System.out.println("removed " + element);
            }

            @Override
            public void notifyElementExpired(Ehcache cache, Element element) {

                System.out.println("expired " + element);
            }

            @Override
            public void notifyElementEvicted(Ehcache cache, Element element) {

                System.out.println("evicted " + element);
            }

            private void updateEntity(org.hibernate.cache.CacheKey hibernateCacheKey) {

                String entityOrRoleName = hibernateCacheKey.getEntityOrRoleName();

                try {
                    EntityCacheKey cacheKey = new EntityCacheKey(entityOrRoleName, hibernateCacheKey.getKey());
                    CacheManagerImpl cacheManager = ServiceLocator.instance().getService("cacheManager", CacheManagerImpl.class);
                    cacheManager.update(cacheKey, CacheManagerImpl.ROOT);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            private void updateCollection(org.hibernate.cache.CacheKey hibernateCacheKey) {

                String entityOrRoleName = hibernateCacheKey.getEntityOrRoleName();
                int lastDot = entityOrRoleName.lastIndexOf(".");
                String entityName = entityOrRoleName.substring(0, lastDot);
                String key = entityOrRoleName.substring(lastDot + 1);

                try {
                    EntityCacheKey cacheKey = new EntityCacheKey(entityName, hibernateCacheKey.getKey());
                    CacheManagerImpl cacheManager = ServiceLocator.instance().getService("cacheManager", CacheManagerImpl.class);
                    cacheManager.update(cacheKey, key);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
