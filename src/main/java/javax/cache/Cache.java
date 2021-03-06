/**
 *  Copyright (c) 2011 Terracotta, Inc.
 *  Copyright (c) 2011 Oracle and/or its affiliates.
 *
 *  All rights reserved. Use is subject to license terms.
 */

package javax.cache;

import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CompletionListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A Cache provides storage of data for later fast retrieval.
 * <p/>
 * This Cache interface is based on {@link java.util.concurrent.ConcurrentMap} with some modifications for
 * fast distributed performance.
 * <p/>
 * A Cache does not allow null keys or values. Attempts to store a null value or
 * to use a null key either in a get or put operation will result in a {@link NullPointerException}.
 * <p/>
 * Caches use generics throughout providing a level of type safety akin to the collections package.
 * <p/>
 * Cache implements {@link Iterable} for {@link Cache.Entry}, providing support for simplified iteration.
 * However iteration should be used with caution. It is an O(n) operation and may be
 * slow on large or distributed caches.
 * <p/>
 * The Cache API also provides:
 * <ul>
 * <li>read-through caching</li>
 * <li>write-through caching</li>
 * <li>cache loading</li>
 * <li>cache listeners</li>
 * <li>statistics</li>
 * <li>lifecycle</li>
 * <li>configuration</li>
 * </ul>
 * Though not visible in the Cache interface caches may be optionally transactional.
 * <p/>
 * User programs may make use of caching annotations to interact with a cache.
 * <p/>
 * A simple example of how to use a cache is:
 * <pre>
 * String cacheName = "sampleCache";
 * CacheManager cacheManager = Caching.getCacheManager();
 * Cache&lt;Integer, Date&gt; cache = cacheManager.getCache(cacheName);
 * if (cache == null) {
 *   cache = cacheManager.&lt;Integer,Date&gt;createCacheBuilder(cacheName).build();
 * }
 * Date value1 = new Date();
 * Integer key = 1;
 * cache.put(key, value1);
 * Date value2 = cache.get(key);
 * </pre>
 * <p/>
 * <h1>Consistency</h1>
 * <h2>Default Consistency</h2>
 * Consistency is described as if there exists a locking mechanism on each key. If a cache operation gets an exclusive read and write lock
 * on a key, then all subsequent operations on that key will block until that lock is released. The consequences are that operations
 * performed by a thread happen-before read or mutation operations performed by another thread, including threads in different Java
 * Virtual Machines.
 * <h2>Transactional Consistency</h2>
 * Where are cache is transactional it will take on the semantics of the Transaction Isolation Level configured.
 * <h2>Further Consistency Modes</h2>
 * An implementation may support additional consistency models.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 * @author Greg Luck
 * @author Yannis Cosmadopoulos
 * @author Brian Oliver
 * @since 1.0
 */
public interface Cache<K, V> extends Iterable<Cache.Entry<K, V>>, CacheLifecycle {
    /**
     * Gets an entry from the cache.
     * <p/>
     * If the cache is configured read-through, and get would return null because the entry
     * is missing from the cache, the Cache's {@link CacheLoader} is called which will attempt
     * to load the entry.
     *
     * <h1>Effects:</h1>
     * <ul>
     * <li>Expiry - updates expiry time based on the Configuration ExpiryPolicy.</li>
     * <li>Read-Through - will use the {@link CacheLoader} if enabled and key not present in cache</li>
     * </ul>
     *
     * @param key the key whose associated value is to be returned
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws NullPointerException  if the key is null
     * @throws CacheException        if there is a problem fetching the value
     * @see java.util.Map#get(Object)
     */
    V get(K key);

    /**
     * The getAll method will return, from the cache, a {@link Map} of the objects
     * associated with the Collection of keys in argument "keys".
     * <p/>
     * If the cache is configured read-through, and a get would return null because an entry
     * is missing from the cache, the Cache's {@link CacheLoader} is called which will attempt
     * to load the entry. This is done for each key in the collection for which this is the case.
     * If an entry cannot be loaded for a given key, the key will not be present in the returned Map.
     * <p/>
     *
     *
     * @param keys The keys whose associated values are to be returned.
     * @return A map of entries that were found for the given keys. Keys not found in the cache are not in the returned map.
     * @throws NullPointerException  if keys is null or if keys contains a null
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem fetching the values.
     */
    Map<K, V> getAll(Set<? extends K> keys);

    /**
     * Returns <tt>true</tt> if this cache contains a mapping for the specified
     * key.  More formally, returns <tt>true</tt> if and only if
     * this cache contains a mapping for a key <tt>k</tt> such that
     * <tt>key.equals(k)</tt>.  (There can be at most one such mapping.)
     * <p/>
     *
     * @param key key whose presence in this cache is to be tested.
     * @return <tt>true</tt> if this map contains a mapping for the specified key
     * @throws NullPointerException  if key is null
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        it there is a problem checking the mapping
     * @see java.util.Map#containsKey(Object)
     */
    boolean containsKey(K key);

    /**
     * The loadAll method provides a means to "pre-load" objects into the cache.
     * This method will, asynchronously, load the specified objects into the
     * cache using the associated cache loader for the given keys.
     * <p/>
     * If the an object already exists in the cache, no action is taken. If no
     * loader is provided for the cache, no objects will be loaded.  If a problem
     * is encountered during the retrieving or loading of the objects, an
     * exception provided to the specified CompletionListener.  Once the operation
     * has completed, the specified CompletionListener is notified.
     * <p/>
     * Implementations may choose to load multiple keys from the provided
     * iterable in parallel.  Iteration must not occur in parallel, thus
     * allow for non-thread-sage Iterables, but loading may.
     *
     * @param keys the keys
     * @param listener the CompletionListener (may be null)
     *
     * @throws NullPointerException  if keys is null or if keys contains a null.
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem doing the load
     */
    void loadAll(Iterable<? extends K> keys, CompletionListener listener);

    /**
     * Associates the specified value with the specified key in this cache
     * If the cache previously contained a mapping for
     * the key, the old value is replaced by the specified value.  (A cache
     * <tt>c</tt> is said to contain a mapping for a key <tt>k</tt> if and only
     * if {@link #containsKey(Object) c.containsKey(k)} would return
     * <tt>true</tt>.)
     * <p/>
     * In contrast to the corresponding Map operation, does not return
     * the previous value.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @throws NullPointerException  if key is null or if value is null
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem doing the put
     * @see java.util.Map#put(Object, Object)
     * @see #getAndPut(Object, Object)
     * @see #getAndReplace(Object, Object)
     */
    void put(K key, V value);

    /**
     * Associates the specified value with the specified key in this cache,
     * returning an existing value if one existed.
     * <p/>
     * If the cache previously contained a mapping for
     * the key, the old value is replaced by the specified value.  (A cache
     * <tt>c</tt> is said to contain a mapping for a key <tt>k</tt> if and only
     * if {@link #containsKey(Object) c.containsKey(k)} would return
     * <tt>true</tt>.)
     * <p/>
     * The the previous value is returned, or null if there was no value associated
     * with the key previously.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the value associated with the key at the start of the operation or null if none was associated
     * @throws NullPointerException  if key is null or if value is null
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem doing the put
     * @see java.util.Map#put(Object, Object)
     * @see #put(Object, Object)
     * @see #getAndReplace(Object, Object)
     */
    V getAndPut(K key, V value);

    /**
     * Copies all of the mappings from the specified map to this cache.
     * The effect of this call is equivalent to that
     * of calling {@link #put(Object, Object) put(k, v)} on this cache once
     * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
     * specified map.
     * The order in which the individual puts will occur is undefined.
     * The behavior of this operation is undefined if the specified cache or map is modified while the
     * operation is in progress.
     *
     * @param map mappings to be stored in this cache
     * @throws NullPointerException  if map is null or if map contains null keys or values.
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem doing the put
     * @see java.util.Map#putAll(java.util.Map)
     */
    void putAll(java.util.Map<? extends K, ? extends V> map);

    /**
     * Atomically associates the specified key with the given value if it is
     * not already associated with a value.
     * <p/>
     * This is equivalent to
     * <pre>
     *   if (!cache.containsKey(key)) {}
     *       cache.put(key, value);
     *       return true;
     *   } else {
     *       return false;
     *   }</pre>
     * except that the action is performed atomically.
     *
     * In contrast to the corresponding ConcurrentMap operation, does not return
     * the previous value.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return true if a value was set.
     * @throws NullPointerException  if key is null or value is null
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem doing the put
     * @see java.util.concurrent.ConcurrentMap#putIfAbsent(Object, Object)
     */
    boolean putIfAbsent(K key, V value);

    /**
     * Removes the mapping for a key from this cache if it is present.
     * More formally, if this cache contains a mapping
     * from key <tt>k</tt> to value <tt>v</tt> such that
     * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
     * is removed.  (The cache can contain at most one such mapping.)
     * <p/>
     * <p>Returns <tt>true</tt> if this cache previously associated the key,
     * or <tt>false</tt> if the cache contained no mapping for the key.
     * <p/>
     * <p>The cache will not contain a mapping for the specified key once the
     * call returns.
     *
     * @param key key whose mapping is to be removed from the cache
     * @return returns false if there was no matching key
     * @throws NullPointerException  if key is null
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem doing the put
     * @see java.util.Map#remove(Object)
     */
    boolean remove(K key);

    /**
     * Atomically removes the mapping for a key only if currently mapped to the given value.
     * <p/>
     * This is equivalent to
     * <pre>
     *   if (cache.containsKey(key) &amp;&amp; cache.get(key).equals(oldValue)) {
     *       cache.remove(key);
     *       return true;
     *   } else {
     *       return false;
     *   }</pre>
     * except that the action is performed atomically.
     *
     * @param key      key whose mapping is to be removed from the cache
     * @param oldValue value expected to be associated with the specified key
     * @return returns false if there was no matching key
     * @throws NullPointerException  if key is null
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem doing the put
     * @see java.util.Map#remove(Object)
     */
    boolean remove(K key, V oldValue);

    /**
     * Atomically removes the entry for a key only if currently mapped to a given value.
     * <p/>
     * This is equivalent to
     * <pre>
     *   if (cache.containsKey(key)) {
     *       V oldValue = cache.get(key);
     *       cache.remove(key);
     *       return oldValue;
     *   } else {
     *       return null;
     *   }</pre>
     * except that the action is performed atomically.
     *
     * @param key key with which the specified value is associated
     * @return the value if one existed or null if no mapping existed for this key
     * @throws NullPointerException  if the specified key or value is null.
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem during the remove
     * @see java.util.Map#remove(Object)
     */
    V getAndRemove(K key);

    /**
     * Atomically replaces the entry for a key only if currently mapped to a given value.
     * <p/>
     * This is equivalent to
     * <pre>
     *   if (cache.containsKey(key) &amp;&amp; cache.get(key).equals(oldValue)) {
     *       cache.put(key, newValue);
     *       return true;
     *   } else {
     *       return false;
     *   }</pre>
     * except that the action is performed atomically.
     *
     * @param key      key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return <tt>true</tt> if the value was replaced
     * @throws NullPointerException  if key is null or if the values are null
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem during the replace
     * @see java.util.concurrent.ConcurrentMap#replace(Object, Object, Object)
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * Atomically replaces the entry for a key only if currently mapped to some value.
     * <p/>
     * This is equivalent to
     * <pre>
     *   if (cache.containsKey(key)) {
     *       cache.put(key, value);
     *       return true;
     *   } else {
     *       return false;
     *   }</pre>
     * except that the action is performed atomically.
     *
     * In contrast to the corresponding ConcurrentMap operation, does not return
     * the previous value.
     *
     * @param key   key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return <tt>true</tt> if the value was replaced
     * @throws NullPointerException  if key is null or if value is null
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem during the replace
     * @see #getAndReplace(Object, Object)
     * @see java.util.concurrent.ConcurrentMap#replace(Object, Object)
     */
    boolean replace(K key, V value);

    /**
     * Atomically replaces the entry for a key only if currently mapped to some value.
     * <p/>
     * This is equivalent to
     * <pre>
     *   if (cache.containsKey(key)) {
     *       V value = cache.get(key, value);
     *       cache.put(key, value);
     *       return value;
     *   } else {
     *       return null;
     *   }</pre>
     * except that the action is performed atomically.
     *
     * @param key   key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         <tt>null</tt> if there was no mapping for the key.
     * @throws NullPointerException  if key is null or if value is null
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem during the replace
     * @see java.util.concurrent.ConcurrentMap#replace(Object, Object)
     */
    V getAndReplace(K key, V value);

    /**
     * Removes entries for the specified keys.
     * <p/>
     * The order in which the individual removes will occur is undefined.
     * <p/>
     *
     * @param keys the keys to remove
     * @throws NullPointerException  if keys is null or if it contains a null key
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem during the remove
     */
    void removeAll(Set<? extends K> keys);

    /**
     * Removes all of the mappings from this cache.
     * <p/>
     * The order in which the individual removes will occur is undefined.
     * This is potentially an expensive operation as listeners are invoked. Use #clear() to avoid this.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem during the remove
     * @see #clear()
     */
    void removeAll();

    /**
     * Clears the contents of the cache, without notifying listeners or {@link CacheWriter}s.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @throws CacheException        if there is a problem during the remove
     */
    void clear();

    /**
     * Returns a Configuration.
     * <p/>
     * When status is {@link Status#STARTED} an implementation must respect the following:
     * <p/>
     * If an implementation permits mutation of configuration to a running cache, those changes must be reflected
     * in the cache. In the case where mutation is not allowed {@link InvalidConfigurationException} must be thrown on
     * an attempt to mutate the configuration.
     *
     * @return the {@link Configuration} of this cache
     */
    Configuration<K, V> getConfiguration();

    /**
     * Adds a listener to the notification service.
     * <p/>
     *
     * @param cacheEntryListener The listener to add. Listeners fire synchronously in the execution path, and after the
     *                           causing event. if a listener throws an exception it will be wrapped in a CacheException
     *                           and propagated back to the caller.  The listener may not be null
     * @param requireOldValue    whether the old value is supplied to {@link javax.cache.event.CacheEntryEvent}.
     * @param cacheEntryFilter   If present the listener will only be called if the filter evaluates to true. If null the listener
     *                           is always called.
     * @param synchronous        whether the caller is blocked until the listener invocation completes.
     * @return true if the listener is being added and was not already added
     * @throws NullPointerException if the listener is null.
     */
    boolean registerCacheEntryListener(CacheEntryListener<? super K, ? super V> cacheEntryListener,
                                       boolean requireOldValue,
                                       CacheEntryEventFilter<? super K, ? super V> cacheEntryFilter,
                                       boolean synchronous);

    /**
     * Removes a call back listener.
     *
     * @param cacheEntryListener the listener to remove
     * @return true if the listener was present
     */
    boolean unregisterCacheEntryListener(CacheEntryListener<?, ?> cacheEntryListener);

    /**
     * Passes the cache entry associated with the key to the entry
     * processor. All operations performed by the processor will be done atomically
     *
     * @param key the key to the entry
     * @param entryProcessor the processor which will process the entry
     * @return an object
     * @throws NullPointerException if key or entryProcessor are null
     * @throws IllegalStateException if the cache is not {@link Status#STARTED}
     * @see EntryProcessor
     */
    <T> T invokeEntryProcessor(K key, EntryProcessor<K, V, T> entryProcessor);

    /**
     * Return the name of the cache.
     *
     * @return the name of the cache.
     */
    String getName();

    /**
     * Gets the CacheManager managing this cache.
     * <p/>
     * A cache can be in only one CacheManager.
     *
     * @return the manager
     */
    CacheManager getCacheManager();

    /**
     * Return an object of the specified type to allow access to the provider-specific API. If the provider's
     * implementation does not support the specified class, the {@link IllegalArgumentException} is thrown.
     *
     * @param cls he class of the object to be returned. This is normally either the underlying implementation class or an interface that it implements.
     * @return an instance of the specified class
     * @throws IllegalArgumentException if the provider doesn't support the specified class.
     */
    <T> T unwrap(java.lang.Class<T> cls);

    /**
     * {@inheritDoc}
     *
     * The ordering of iteration over entries is undefined.
     * <p/>
     * During iteration, calling iterator.hasNext() determines if there is an
     * available non-expired entry in the cache to which to iterate to, that of
     * which will be returned by a subsequent call to next().  If hasNext() returns
     * true, an infinite amount of time may elapse between the call to hasNext()
     * and next(), without the prospective entry expirying.  That is, it's impossible
     * for hasNext() to return true and then a subsequent call to next() to
     * fail on the same iterator.
     * <p/>
     * During iteration, any entries that are a). read will have their appropriate
     * CacheEntryReadListeners notified and b). removed will have their appropriate
     * CacheEntryRemoveListeners notified.
     */
    Iterator<Cache.Entry<K, V>> iterator();

    /**
     * A cache entry (key-value pair).
     */
    interface Entry<K, V> {

        /**
         * Returns the key corresponding to this entry.
         *
         * @return the key corresponding to this entry
         */
        K getKey();

        /**
         * Returns the value stored in the cache when this entry was created.
         *
         * @return the value corresponding to this entry
         */
        V getValue();
    }

    /**
     * An accessor and mutator to the underlying Cache
     * @param <K>
     * @param <V>
     */
    public interface MutableEntry<K, V> extends Entry<K, V> {

        /**
         * Checks for the existence of the entry in the cache
         * @return true if the entry exists
         */
        boolean exists();

        /**
         * Removes the entry from the Cache
         * <p/>
         *
         */
        void remove();

        /**
         * Sets or replaces the value associated with the key
         * If {@link #exists} is false and setValue is called
         * then a mapping is added to the cache visible once the EntryProcessor
         * completes. Moreover a second invocation of {@link #exists()}
         * will return true.
         * <p/>
         *
         * @param value the value to update the entry with
         */
        void setValue(V value);
    }

    /**
     * Allows execution of code which may mutate a cache entry with exclusive
     * access (including reads) to that entry.
     * <p/>
     * Any {@link Cache.Entry} mutations will not take effect till after the processor has completed;
     * if an exception is thrown inside the processor, the exception will be returned wrapped in an
     * {@link java.util.concurrent.ExecutionException}.  No changes will be made to the cache.
     * <p/>
     * This enables a way to perform compound operations without transactions
     * involving a cache entry atomically. Such operations may include mutations.
     * <p/>
     * Implementations may process in situ, avoiding expensive network transfers. e.g. appending
     * to a list. Another is computing a function on a value and returning just that.
     * <p/>
     * <h2>Chaining &amp Recursion </h2>
     * An entry processor cannot invoke any cache operations, including other processor operations
     * against another key.
     * <p/>
     * However multiple EntryProcessors can be execute against the same key. For example an EntryProcessor
     * might be a composite of entry processors or a chain of entry processors. The outermost EntryProcessor
     * will lock and unlock the entry. todo define this behaviour more thoroughly.
     * <p/>
     * <h2>Statistics</h2>
     * Invocation of an entry processor is regarded as a get operation for statistics purposes.
     * <p/>
     * If {@link MutableEntry#setValue(Object)} is called in an EntryProcessor it is considered a put
     * for statistics purposes.
     *
     * <h2>CacheEntryListeners</h2>
     * CacheEntryListeners are invoked by entry processors.
     *
     * <h2>Remote Invocation</h2>
     * If executed in a JVM remote from the one invoke was called in, an EntryProcessor equal
     * to the local one will execute the invocation. For remote execution to succeed, the
     * EntryProcessor implementation class must be in the executing class loader as must K and
     * if {@link Cache.MutableEntry#getKey()} is used and V if {@link Cache.MutableEntry#getValue()}
     * is invoked.
     * <p/>
     * In order to simplify placement of EntryProcessors in remote JVMs, arguments are passed separately,
     * thus obviating the need for specific sub-classes of EntryProcessors. EntryProcessors therefore
     * do not need to be {@link java.io.Serializable}. Arguments will need to be serializable in an implementation
     * specific way. {@link java.io.Serializable} is not mandated.
     *
     * tst
     *
     * @param <K> the type of keys maintained by this cache
     * @param <V> the type of cached values
     * @author Greg Luck
     * @author Yannis Cosmadopoulos
     */
    public interface EntryProcessor<K, V, T> {

        /**
         * Process an entry. Exclusive read and write access to the entry is obtained to
         * the entry.
         * @param entry the entry
         * @param arguments a number of arguments to the process.
         * @return the result of the processing, if any, which is user defined.
         */
        T process(Cache.MutableEntry<K, V> entry, Object... arguments);
    }
}
